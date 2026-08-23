import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/api/meetings';
const USE_MOCK_API = false; // Set to true to bypass backend since JDK is missing

export interface Meeting {
    id?: number;
    title: string;
    content: string;
    summary?: string;
    actionItems?: string;
    decisions?: string;
    openQuestions?: string;
    createdAt?: string;
}

// In-memory mock database for frontend-only testing
let mockMeetings: Meeting[] = [];
let nextId = 1;

export const meetingApi = {
    getAll: async (): Promise<Meeting[]> => {
        if (USE_MOCK_API) {
            return [...mockMeetings].sort((a, b) => 
                new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime()
            );
        }
        const response = await axios.get(API_BASE_URL);
        return response.data;
    },
    
    getById: async (id: number): Promise<Meeting> => {
        if (USE_MOCK_API) {
            const meeting = mockMeetings.find(m => m.id === id);
            if (!meeting) throw new Error("Not found");
            return meeting;
        }
        const response = await axios.get(`${API_BASE_URL}/${id}`);
        return response.data;
    },
    
    create: async (meeting: { title: string; content: string }): Promise<Meeting> => {
        if (USE_MOCK_API) {
            // Simulate network delay
            await new Promise(resolve => setTimeout(resolve, 1500));
            
            const newMeeting: Meeting = {
                id: nextId++,
                title: meeting.title,
                content: meeting.content,
                summary: "This is an AI-generated mock summary (Frontend Only Mode). " +
                         "The original transcript contained " + meeting.content.split("\\s+").length + " words. ",
                actionItems: "- [ ] **John Doe**: Review the project plan by Friday.\n- [ ] **Jane Smith**: Follow up with the client regarding the new requirements.",
                decisions: "- Proceed with the React frontend and Spring Boot backend architecture.\n- Use PostgreSQL for production and H2 for local testing.",
                openQuestions: "- Do we need to support video file uploads in the future?\n- What is the expected monthly budget for the AI API?",
                createdAt: new Date().toISOString()
            };
            
            mockMeetings.push(newMeeting);
            return newMeeting;
        }
        
        const response = await axios.post(API_BASE_URL, meeting);
        return response.data;
    },

    uploadAudio: async (title: string, file: File): Promise<Meeting> => {
        if (USE_MOCK_API) {
            await new Promise(resolve => setTimeout(resolve, 2500));
            
            const newMeeting: Meeting = {
                id: nextId++,
                title: title,
                content: "[Transcribed from audio file: " + file.name + "]\n\n" +
                         "This is a simulated transcription of the uploaded audio file. " +
                         "In a real environment, this audio would be sent to OpenAI Whisper or Google Gemini API.",
                summary: "This is an AI-generated mock summary (Frontend Only Mode) from audio.",
                actionItems: "- [ ] **AI Integration**: Wire up OpenAI Whisper API to backend.\n- [ ] **UI Polish**: Add progress bar for uploads.",
                decisions: "- Build audio upload feature first, live dictation next.",
                openQuestions: "- Which cloud storage should we use to store the audio files permanently?",
                createdAt: new Date().toISOString()
            };
            
            mockMeetings.push(newMeeting);
            return newMeeting;
        }

        const formData = new FormData();
        formData.append('title', title);
        formData.append('file', file);

        const response = await axios.post(`${API_BASE_URL}/audio`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        return response.data;
    },

    delete: async (id: number): Promise<void> => {
        if (USE_MOCK_API) {
            mockMeetings = mockMeetings.filter(m => m.id !== id);
            return;
        }
        await axios.delete(`${API_BASE_URL}/${id}`);
    },

    translate: async (id: number, targetLanguage: string): Promise<Meeting> => {
        if (USE_MOCK_API) {
            await new Promise(resolve => setTimeout(resolve, 1500));
            const meeting = mockMeetings.find(m => m.id === id);
            if (!meeting) throw new Error("Not found");
            const translated = {
                ...meeting,
                summary: `[Translated to ${targetLanguage}]\n${meeting.summary}`,
                actionItems: `[Translated to ${targetLanguage}]\n${meeting.actionItems}`,
                decisions: `[Translated to ${targetLanguage}]\n${meeting.decisions}`,
                openQuestions: `[Translated to ${targetLanguage}]\n${meeting.openQuestions}`
            };
            // update mock db
            mockMeetings = mockMeetings.map(m => m.id === id ? translated : m);
            return translated;
        }
        const response = await axios.post(`${API_BASE_URL}/${id}/translate?target=${encodeURIComponent(targetLanguage)}`);
        return response.data;
    }
};

export interface ScheduledMeeting {
    id?: number;
    title: string;
    scheduledTime: string;
    userEmail: string;
    reminderSent?: boolean;
}

export const scheduleApi = {
    getAll: async (): Promise<ScheduledMeeting[]> => {
        if (USE_MOCK_API) return [];
        const response = await axios.get('http://localhost:8081/api/schedule');
        return response.data;
    },
    
    schedule: async (meeting: ScheduledMeeting): Promise<ScheduledMeeting> => {
        if (USE_MOCK_API) {
            return { ...meeting, id: Math.floor(Math.random() * 1000) };
        }
        const response = await axios.post('http://localhost:8081/api/schedule', meeting);
        return response.data;
    }
};
