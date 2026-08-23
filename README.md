<div align="center">
  <img src="https://raw.githubusercontent.com/abkul05/meeting-charcha/main/frontend/public/icons.svg" alt="Meeting Charcha Logo" width="120" />

  # 🔮 Meeting Charcha
  
  **Transform your messy meeting audio into beautifully structured intelligence instantly.**
  
  [![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](#)
  [![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)](#)
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](#)
  [![Resend](https://img.shields.io/badge/Resend-000000?style=for-the-badge&logo=minutemailer&logoColor=white)](#)
  [![Gemini AI](https://img.shields.io/badge/Google_Gemini-4285F4?style=for-the-badge&logo=google&logoColor=white)](#)
</div>

<br />

> **Meeting Charcha** (charcha = discussion) is an ultra-premium, AI-driven meeting assistant. It takes your raw meeting transcripts and automatically extracts actionable intelligence like tasks, deadlines, and critical decisions—all wrapped in a stunning Cyberpunk-inspired glassmorphism UI.

---

## ✨ Features That Feel Like Magic

- 🧠 **AI-Powered Summaries**: Instantly extracts Action Items, Decisions, and Open Questions from natural conversation.
- ⏰ **Smart Deadline Extraction**: Scans spoken words for temporal phrases (e.g., "by tomorrow", "next week", "EOD") and automatically flags them as concrete deadlines in your task list.
- 🌍 **Real-Time Translation**: Instantly translates meeting notes and action items into 5+ global languages including Hindi, Spanish, French, German, and Japanese.
- 📅 **Integrated Calendar & Scheduling**: Plan upcoming sessions directly within the app.
- 📧 **Automated Email Reminders**: Powered by the **Resend API**, attendees receive automated email reminders exactly 10 minutes before a meeting begins.
- 🎨 **Premium Aesthetic**: A deeply immersive, two-tone (Deep Purple & Electric Cyan) cyberpunk UI featuring floating animated background blobs, glassmorphism cards, and glowing hover states.

---

## 🛠️ Tech Stack

### Frontend
- **React.js + Vite**: Blazing fast development and optimized builds.
- **Vanilla CSS**: Custom-built, zero-dependency glassmorphism and animation system.
- **React Router**: Seamless Single Page Application (SPA) navigation.

### Backend
- **Java Spring Boot**: Robust, enterprise-grade backend architecture.
- **H2 In-Memory Database**: Zero-config database for instant local setup.
- **Spring Scheduling**: Background cron workers for the automated email reminder system.
- **Google Translate / Gemini API**: AI-driven text processing and translation.

---

## 🚀 Getting Started

### Prerequisites
- Node.js (v18+)
- Java 17 (JDK)
- Maven

### 1. Clone the Repository
```bash
git clone https://github.com/abkul05/meeting-charcha.git
cd meeting-charcha
```

### 2. Start the Backend (Spring Boot)
Open a terminal and navigate to the `backend` directory:
```bash
cd backend
# Windows
.\mvnw.cmd spring-boot:run
# Mac/Linux
./mvnw spring-boot:run
```
*The backend will start on `http://localhost:8081`*

### 3. Start the Frontend (Vite)
Open a new terminal and navigate to the `frontend` directory:
```bash
cd frontend
npm install
npm run dev
```
*The frontend will start on `http://localhost:5173`*

### 4. Configuration (Optional)
To enable real email sending, add your Resend API key to `backend/src/main/resources/application.properties`:
```properties
resend.api.key=YOUR_RESEND_API_KEY_HERE
```

---

<div align="center">
  <i>Built with ❤️ to make meetings suck less.</i>
</div>