import React, { useState, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { meetingApi } from '../api';

export const UploadMeeting: React.FC = () => {
    const navigate = useNavigate();
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [audioFile, setAudioFile] = useState<File | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [activeTab, setActiveTab] = useState<'text' | 'audio' | 'live'>('text');
    
    // Live Recording state
    const [isRecording, setIsRecording] = useState(false);
    const [recordedTranscript, setRecordedTranscript] = useState('');
    const recognitionRef = useRef<any>(null);

    const handleStartRecording = () => {
        // Basic implementation of Web Speech API for simulation
        const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
        if (!SpeechRecognition) {
            setError("Your browser doesn't support speech recognition.");
            return;
        }

        const recognition = new SpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = true;

        recognition.onresult = (event: any) => {
            let finalTranscript = '';
            for (let i = event.resultIndex; i < event.results.length; ++i) {
                if (event.results[i].isFinal) {
                    finalTranscript += event.results[i][0].transcript;
                }
            }
            if (finalTranscript) {
                setRecordedTranscript(prev => prev + ' ' + finalTranscript);
            }
        };

        recognition.onerror = () => {
            setError("Error recording audio.");
            setIsRecording(false);
        };

        recognition.onend = () => setIsRecording(false);

        recognition.start();
        recognitionRef.current = recognition;
        setIsRecording(true);
        setError('');
    };

    const handleStopRecording = () => {
        if (recognitionRef.current) {
            recognitionRef.current.stop();
        }
        setIsRecording(false);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        if (!title.trim()) {
            setError('Please enter a meeting title.');
            return;
        }

        setLoading(true);
        setError('');

        try {
            let result;
            if (activeTab === 'audio' && audioFile) {
                result = await meetingApi.uploadAudio(title, audioFile);
            } else if (activeTab === 'live' && recordedTranscript) {
                result = await meetingApi.create({ title, content: recordedTranscript });
            } else if (activeTab === 'text' && content) {
                result = await meetingApi.create({ title, content });
            } else {
                throw new Error("Please provide input data.");
            }
            navigate(`/meeting/${result.id}`);
        } catch (err: any) {
            console.error("Failed to upload meeting", err);
            setError(err.message || 'Failed to generate summary. Please try again.');
            setLoading(false);
        }
    };

    return (
        <div className="animate-fade-in" style={{ maxWidth: '800px', margin: '0 auto' }}>
            <div className="flex items-center mb-8" style={{ gap: '1rem' }}>
                <Link to="/" style={{ color: 'var(--text-secondary)' }}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="19" y1="12" x2="5" y2="12"></line>
                        <polyline points="12 19 5 12 12 5"></polyline>
                    </svg>
                </Link>
                <h1>Summarize Meeting</h1>
            </div>

            <div className="card mb-4">
                <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '1rem' }}>
                    <button 
                        type="button"
                        className={activeTab === 'text' ? '' : 'btn-secondary'} 
                        onClick={() => setActiveTab('text')}>Paste Text</button>
                    <button 
                        type="button"
                        className={activeTab === 'audio' ? '' : 'btn-secondary'} 
                        onClick={() => setActiveTab('audio')}>Upload Audio</button>
                    <button 
                        type="button"
                        className={activeTab === 'live' ? '' : 'btn-secondary'} 
                        onClick={() => setActiveTab('live')}>Live Record</button>
                </div>
            </div>

            <div className="card">
                {error && (
                    <div style={{ padding: '1rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', borderRadius: '8px', marginBottom: '1.5rem', border: '1px solid rgba(239, 68, 68, 0.2)' }}>
                        {error}
                    </div>
                )}
                
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="title">Meeting Title</label>
                        <input 
                            type="text" 
                            id="title" 
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            placeholder="e.g. Q3 Roadmap Planning"
                            disabled={loading}
                        />
                    </div>
                    
                    {activeTab === 'text' && (
                        <div className="form-group animate-fade-in">
                            <label htmlFor="content">Transcript / Notes</label>
                            <textarea 
                                id="content" 
                                value={content}
                                onChange={(e) => setContent(e.target.value)}
                                placeholder="Paste your meeting transcript or rough notes here..."
                                style={{ minHeight: '300px' }}
                                disabled={loading}
                            />
                        </div>
                    )}

                    {activeTab === 'audio' && (
                        <div className="form-group animate-fade-in">
                            <label htmlFor="audioFile">Upload Audio File (.mp3, .wav)</label>
                            <input 
                                type="file" 
                                id="audioFile" 
                                accept="audio/*"
                                onChange={(e) => setAudioFile(e.target.files ? e.target.files[0] : null)}
                                disabled={loading}
                                style={{ padding: '2rem', border: '2px dashed var(--border-color)', textAlign: 'center' }}
                            />
                            {audioFile && <p style={{ color: 'var(--primary-color)', marginTop: '0.5rem' }}>Selected: {audioFile.name}</p>}
                        </div>
                    )}

                    {activeTab === 'live' && (
                        <div className="form-group animate-fade-in">
                            <label>Live Dictation</label>
                            <div style={{ padding: '2rem', border: '2px dashed var(--border-color)', textAlign: 'center', borderRadius: '8px', backgroundColor: 'var(--bg-color)' }}>
                                {isRecording ? (
                                    <div>
                                        <div className="spinner" style={{ borderColor: 'red', borderTopColor: 'transparent', margin: '0 auto', marginBottom: '1rem' }}></div>
                                        <p style={{ color: '#ef4444' }}>Recording... Speak now.</p>
                                        <button type="button" onClick={handleStopRecording} style={{ backgroundColor: '#ef4444' }}>Stop Recording</button>
                                    </div>
                                ) : (
                                    <div>
                                        <p style={{ marginBottom: '1rem' }}>Click below to start transcribing your meeting live.</p>
                                        <button type="button" onClick={handleStartRecording}>Start Recording</button>
                                    </div>
                                )}
                            </div>
                            
                            {recordedTranscript && (
                                <div style={{ marginTop: '1rem' }}>
                                    <label>Transcript Preview:</label>
                                    <textarea 
                                        readOnly
                                        value={recordedTranscript}
                                        style={{ minHeight: '150px' }}
                                    />
                                </div>
                            )}
                        </div>
                    )}
                    
                    <div className="flex justify-between mt-8">
                        <Link to="/">
                            <button type="button" className="btn-secondary" disabled={loading}>Cancel</button>
                        </Link>
                        <button type="submit" disabled={loading}>
                            {loading ? (
                                <>
                                    <div className="spinner"></div>
                                    Generating AI Summary...
                                </>
                            ) : (
                                'Generate Summary'
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};
