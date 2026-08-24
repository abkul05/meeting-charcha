# Meeting Charcha

> **AI-powered meeting intelligence that turns conversations into clear decisions, actionable tasks, and searchable knowledge.**

Meeting Charcha is a full-stack AI meeting assistant that transforms meeting conversations into structured, useful intelligence.

Instead of leaving users with a long recording or an unstructured transcript, Meeting Charcha is designed to answer the questions that matter after every meeting:

- **What was discussed?**
- **What was decided?**
- **What needs to be done?**
- **Who is responsible?**
- **When is it due?**
- **What is still unresolved?**

The platform combines a modern React interface, a Java Spring Boot backend, speech-to-text processing, and Gemini-powered language intelligence to create an end-to-end meeting workflow.

---

## Table of Contents

- [Overview](#overview)
- [Why Meeting Charcha](#why-meeting-charcha)
- [Product Vision](#product-vision)
- [Key Features](#key-features)
- [End-to-End Workflow](#end-to-end-workflow)
- [System Architecture](#system-architecture)
- [AI and LLM Pipeline](#ai-and-llm-pipeline)
- [Structured Meeting Intelligence](#structured-meeting-intelligence)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Meeting Processing Lifecycle](#meeting-processing-lifecycle)
- [Quality and Evaluation](#quality-and-evaluation)
- [Engineering Decisions](#engineering-decisions)
- [Security and Reliability](#security-and-reliability)
- [Current Capabilities](#current-capabilities)
- [Future Roadmap](#future-roadmap)
- [Demo](#demo)
- [Screenshots](#screenshots)
- [Author](#author)

---

# Overview

Meetings generate a large amount of information, but the information is often trapped inside conversations.

A typical meeting can contain dozens of small but important statements:

```text
"We should probably move the release to Friday."

"I'll handle the API integration."

"Can someone review the UI before Thursday?"

"Let's discuss the deployment issue next week."
```

A normal transcript preserves these statements, but it does not necessarily make them useful.

Meeting Charcha adds an intelligence layer on top of the conversation. It converts the transcript into structured information that can be consumed immediately.

### Example

Instead of returning:

> The team discussed the release schedule, API integration, UI review, and deployment.

Meeting Charcha is designed to produce:

```text
SUMMARY
The team reviewed the release plan and agreed to target Friday.

DECISIONS
• Release target moved to Friday.

ACTION ITEMS
• Complete API integration.
• Review the UI before release.

DEADLINES
• UI review → Thursday
• Release → Friday

OPEN QUESTIONS
• Who will handle production deployment?
```

The difference is intentional: **the product is designed around actionability rather than summarization alone.**

---

# Why Meeting Charcha

Most meeting tools stop at one of two things:

1. Recording the meeting.
2. Generating a block of summary text.

Meeting Charcha focuses on the layer between the conversation and the work that follows.

### Conversation

↓

### Transcription

↓

### Understanding

↓

### Decisions + Tasks + Deadlines + Questions

↓

### Follow-up

This makes the system useful not only immediately after a meeting, but also when someone needs to revisit what happened days or weeks later.

---

# Product Vision

The long-term vision for Meeting Charcha is to become a **meeting intelligence layer** for teams.

The core idea is simple:

> **A meeting should not end when the call ends. Its decisions, responsibilities, and next steps should remain useful.**

The current architecture is intentionally designed so the product can grow from an AI summarizer into a broader meeting-management platform.

Potential extensions include:

- Historical meeting search
- Calendar integrations
- Task tracking
- Team workspaces
- Speaker identification
- Meeting analytics
- Semantic search
- Automated follow-ups
- Cross-meeting knowledge retrieval

---

# Key Features

## 1. AI-Powered Meeting Summaries

Meeting Charcha generates concise summaries from meeting transcripts while preserving the information that matters.

The summary focuses on:

- Main topics
- Important context
- Outcomes
- Major changes
- Decisions
- Next steps

The goal is to remove conversational noise without removing meaning.

---

## 2. Action Item Extraction

Tasks are extracted separately from the general summary.

For example:

```text
ACTION ITEMS

1. Complete backend API integration
   Owner: Backend Team
   Deadline: Wednesday

2. Review responsive UI
   Owner: Design Team
   Deadline: Thursday
```

This makes the output much easier to act upon than a paragraph containing embedded tasks.

---

## 3. Decision Extraction

Important decisions are surfaced independently.

For example:

```text
DECISIONS

• The release will move to Friday.
• The new authentication flow will be used.
• Production deployment will happen after final QA.
```

This allows users to quickly understand what the team actually agreed upon.

---

## 4. Smart Deadline Extraction

Meeting conversations rarely use formal deadline syntax.

People naturally say:

- "by tomorrow"
- "next week"
- "before Friday"
- "EOD"
- "by the end of the month"
- "before the next meeting"

Meeting Charcha is designed to identify these temporal references and associate them with relevant action items where the context supports it.

---

## 5. Open Question Detection

Meetings do not always end with every issue resolved.

The system distinguishes unresolved topics from decisions so users can identify what needs another discussion.

Example:

```text
OPEN QUESTIONS

• Who will own production deployment?
• Should the analytics module ship in the first release?
• What is the final approval process?
```

---

## 6. Translation

Meeting intelligence can be translated into multiple languages.

The product direction includes support for languages such as:

- English
- Hindi
- Spanish
- French
- German
- Japanese

Translation can be applied to generated meeting notes and action items without changing the underlying meeting data.

---

## 7. Meeting Management

Meeting Charcha extends beyond a single processing screen.

The application includes functionality around:

- Meetings
- Upcoming sessions
- Scheduling
- Calendar-oriented workflows
- Meeting reminders
- Processed meeting history

This turns the product from a one-time AI utility into a more complete meeting workflow.

---

## 8. Automated Email Reminders

The backend includes scheduled reminder functionality.

Using the Resend email API together with server-side scheduling, the application can send meeting reminders before an upcoming meeting.

The reminder workflow is handled by the backend rather than relying on the browser remaining open.

---

## 9. Premium Product Experience

The frontend follows a modern, immersive visual language built around:

- Glassmorphism
- Dark interface design
- Purple and electric-cyan accents
- Animated background elements
- Responsive layouts
- Clear information hierarchy
- Interactive states
- Dedicated sections for meeting intelligence

The visual design is intended to make dense AI-generated information feel approachable rather than overwhelming.

---

# End-to-End Workflow

The complete conceptual pipeline is:

```text
                 ┌─────────────────────┐
                 │    Meeting Audio    │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │       ASR           │
                 │   Speech → Text     │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Transcript Cleanup  │
                 │   & Normalization   │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │    Gemini / LLM     │
                 │ Semantic Processing │
                 └──────────┬──────────┘
                            │
             ┌──────────────┼──────────────┐
             │              │              │
             ▼              ▼              ▼
        ┌──────────┐  ┌───────────┐  ┌───────────┐
        │ Summary  │  │ Decisions │  │  Actions  │
        └──────────┘  └───────────┘  └───────────┘
             │              │              │
             └──────────────┼──────────────┘
                            │
                  ┌─────────┴─────────┐
                  ▼                   ▼
            ┌───────────┐       ┌────────────┐
            │ Deadlines │       │ Questions  │
            └───────────┘       └────────────┘
                  │                   │
                  └─────────┬─────────┘
                            ▼
                 ┌─────────────────────┐
                 │   Spring Boot API   │
                 │ Processing & Storage│
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ React + Vite UI     │
                 │ Review & Interaction│
                 └─────────────────────┘
```

### Pipeline stages

### 1. Meeting input

A meeting recording or supported meeting input is supplied to the application.

### 2. Speech recognition

The ASR layer converts spoken audio into a textual transcript.

### 3. Transcript preparation

The transcript is prepared for downstream semantic processing.

### 4. LLM analysis

Gemini receives the transcript together with a structured instruction set.

### 5. Information extraction

The model identifies:

- Summary
- Key discussion points
- Decisions
- Action items
- Deadlines
- Open questions

### 6. Backend processing

The Spring Boot backend handles application logic, persistence, scheduling, and external integrations.

### 7. Frontend presentation

The React application presents the generated intelligence in a format that can be scanned and acted upon quickly.

---

# System Architecture

Meeting Charcha follows a layered full-stack architecture.

```text
┌─────────────────────────────────────────────────────────────┐
│                         FRONTEND                            │
│                                                             │
│                       React + Vite                          │
│                                                             │
│  Meeting UI • Summary • Tasks • Calendar • Translation      │
└──────────────────────────────┬──────────────────────────────┘
                               │
                         HTTP / REST
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                         BACKEND                             │
│                                                             │
│                     Spring Boot / Java                      │
│                                                             │
│ Controllers → Services → Business Logic → Persistence       │
│                                                             │
│ Scheduling • Email • Meeting Management • AI Integration    │
└───────────────┬─────────────────────────┬───────────────────┘
                │                         │
                ▼                         ▼
       ┌─────────────────┐       ┌────────────────────────┐
       │   H2 Database   │       │     AI / External      │
       │ Local Persistence│       │ ASR • Gemini • Resend │
       └─────────────────┘       └────────────────────────┘
```

## Architectural principles

### Separation of concerns

The frontend is responsible for presentation and user interaction, while the backend owns business logic, persistence, scheduling, and external integrations.

### Service-oriented backend logic

Application behavior is separated from HTTP controllers so that business logic does not become tightly coupled to API endpoints.

### Replaceable AI integrations

Speech recognition and language processing are treated as separate layers. This allows the ASR provider or LLM provider to be changed without redesigning the complete application.

### Backend-controlled scheduling

Reminder jobs are executed by the server rather than relying on a browser tab or client-side timer.

### Persistence abstraction

H2 provides a lightweight local development database while keeping the application architecture suitable for migration to a production relational database.

---

# AI and LLM Pipeline

The AI layer is one of the most important parts of Meeting Charcha.

The system does not treat the LLM as a generic text summarizer. Instead, the transcript is passed through a prompt designed to produce structured meeting intelligence.

## LLM objective

Given a transcript, the model should identify:

1. Executive summary
2. Key discussion points
3. Decisions
4. Action items
5. Deadlines
6. Open questions

---

## Prompt design

A representative prompt is:

```text
Analyze the following meeting transcript.

Produce an action-oriented meeting report containing:

1. Executive Summary
2. Key Discussion Points
3. Decisions Made
4. Action Items
5. Deadlines
6. Open Questions

For every action item, identify the responsible person or team
when it can be reliably inferred from the transcript.

Do not invent facts, decisions, responsibilities, or deadlines
that are not supported by the transcript.

Keep the summary concise while preserving important context.

Meeting Transcript:
{{TRANSCRIPT}}
```

---

## Why structured prompting?

A generic prompt such as:

```text
Summarize this meeting.
```

can produce a readable paragraph, but it does not necessarily answer the questions users care about after the meeting.

Meeting Charcha explicitly requests separate semantic categories.

This improves:

- Consistency
- Readability
- Actionability
- Downstream processing
- UI presentation
- Future integration with task-management systems

---

## Hallucination control

Meeting intelligence needs to be more conservative than ordinary creative generation.

The prompt therefore emphasizes:

- Do not invent decisions.
- Do not invent owners.
- Do not invent deadlines.
- Use contextual inference only when reliable.
- Preserve uncertainty when information is ambiguous.
- Separate unresolved topics from confirmed decisions.

This is especially important for action-item extraction because an incorrect task owner can create real-world confusion.

---

# Structured Meeting Intelligence

The output is conceptually organized into a structured model rather than a single block of text.

Example:

```json
{
  "summary": "The team finalized the release plan and assigned the remaining work.",
  "keyPoints": [
    "Backend integration is nearly complete.",
    "The UI requires final testing before release."
  ],
  "decisions": [
    "The release is planned for Friday."
  ],
  "actionItems": [
    {
      "task": "Complete API integration",
      "owner": "Backend Team",
      "deadline": "Wednesday"
    },
    {
      "task": "Review responsive UI",
      "owner": "Frontend Team",
      "deadline": "Thursday"
    }
  ],
  "openQuestions": [
    "Who will perform the production deployment?"
  ]
}
```

The exact API/domain model can evolve as the product grows.

---

# Technology Stack

## Frontend

| Technology | Purpose |
|---|---|
| **React.js** | Component-based user interface |
| **Vite** | Fast development and build tooling |
| **React Router** | Client-side navigation |
| **CSS** | Custom visual system, animations, and glassmorphism |

---

## Backend

| Technology | Purpose |
|---|---|
| **Java** | Backend programming language |
| **Spring Boot** | Application framework and REST APIs |
| **Maven** | Build and dependency management |
| **H2** | Lightweight relational database |
| **Spring Scheduling** | Background scheduled jobs |
| **Resend** | Email delivery |

---

## AI / External Services

| Technology | Purpose |
|---|---|
| **ASR provider** | Speech-to-text transcription |
| **Google Gemini** | Semantic analysis and summarization |
| **Translation layer** | Multilingual meeting intelligence |
| **Resend API** | Meeting reminder emails |

---

# Project Structure

The repository is organized as a separate frontend and backend application.

```text
meeting-charcha/
│
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   └── ...
│   ├── package.json
│   └── vite.config.*
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   └── mvnw / mvnw.cmd
│
├── README.md
└── ...
```

A typical Spring Boot organization follows the responsibilities:

```text
Controller
    ↓
Service
    ↓
Repository / External Integration
    ↓
Database / API
```

This keeps HTTP handling, business logic, and data access from becoming unnecessarily coupled.

---

# Getting Started

## Prerequisites

Make sure the following are installed:

- Node.js 18+
- npm
- Java 17+
- Maven, or use the included Maven wrapper
- Git

Verify the environment:

```bash
node --version
npm --version
java --version
mvn --version
git --version
```

---

# Configuration

External service credentials should never be committed to source control.

## Resend

Configure the Resend API key in:

```text
backend/src/main/resources/application.properties
```

Example:

```properties
resend.api.key=YOUR_RESEND_API_KEY_HERE
```

## Gemini

Configure the Gemini credential using the application's expected configuration mechanism.

For local development, credentials can be supplied through environment variables or application configuration.

For production, use a proper secret-management mechanism.

### Important

Do not commit:

```text
API keys
Access tokens
OAuth credentials
Private keys
Production secrets
```

to the repository.

---

# Running the Application

## 1. Clone the repository

```bash
git clone https://github.com/abkul05/meeting-charcha.git
cd meeting-charcha
```

---

## 2. Start the backend

Open a terminal and navigate to the backend:

```bash
cd backend
```

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### macOS / Linux

```bash
./mvnw spring-boot:run
```

The backend is configured to run on:

```text
http://localhost:8081
```

---

## 3. Start the frontend

Open a second terminal:

```bash
cd frontend
npm install
npm run dev
```

The Vite development server will normally be available at:

```text
http://localhost:5173
```

Open the frontend URL in a browser.

---

# Meeting Processing Lifecycle

A typical meeting moves through the following lifecycle.

## Step 1 — Input

The user provides the meeting recording or supported meeting content.

---

## Step 2 — Transcription

The ASR layer converts speech into a text transcript.

The resulting transcript becomes the source material for semantic analysis.

---

## Step 3 — Transcript Review

The transcript provides a searchable textual representation of the original meeting.

This is useful for maintaining traceability between generated intelligence and the source conversation.

---

## Step 4 — AI Analysis

The transcript is passed to Gemini with the structured meeting-analysis prompt.

The model analyzes the conversation rather than simply shortening it.

---

## Step 5 — Information Extraction

The generated intelligence is separated into:

```text
Summary
   │
   ├── Key Discussion Points
   ├── Decisions
   ├── Action Items
   ├── Deadlines
   └── Open Questions
```

---

## Step 6 — Persistence

The backend stores the relevant meeting information so it can be retrieved through the application.

---

## Step 7 — Presentation

The frontend renders the processed meeting in dedicated sections so users can scan the important information quickly.

---

## Step 8 — Follow-up

Meeting intelligence can then feed into broader workflows such as:

- Scheduling
- Reminders
- Translation
- Task tracking
- Historical review

---

# Quality and Evaluation

Meeting Charcha is designed around practical product-quality criteria.

## Transcription Quality

Important factors include:

- Word accuracy
- Different accents
- Background noise
- Technical terminology
- Long meetings
- Multiple speakers

A realistic evaluation set should contain a mixture of clean and noisy meeting recordings.

---

## Summary Quality

Generated summaries should be evaluated for:

### Relevance

Does the summary focus on information that actually matters?

### Conciseness

Does it remove unnecessary conversational detail?

### Faithfulness

Does it accurately represent the meeting?

### Coverage

Does it preserve important decisions and outcomes?

### Hallucination

Does it avoid adding facts that were never discussed?

---

## Action Item Quality

Action extraction should be evaluated for:

- Correct task identification
- Correct owner attribution
- Correct deadline extraction
- No fabricated responsibilities
- No duplicated tasks
- Preservation of important context

---

## Decision Quality

A useful meeting assistant must distinguish between:

```text
Suggestion
    ≠
Decision
```

For example:

> "Maybe we should release Friday."

should not automatically be represented as:

> "Decision: Release Friday."

The semantic context of the conversation matters.

---

## Prompt Effectiveness

The prompt should consistently produce:

- Structured results
- Concise summaries
- Reliable task extraction
- Useful deadline detection
- Clear decisions
- Open questions
- Minimal unsupported inference

---

## Code Quality

The application is structured around clear frontend/backend boundaries and layered backend responsibilities.

The objective is to keep the system:

- Readable
- Maintainable
- Extensible
- Testable
- Easy to run locally

---

# Engineering Decisions

## Why React + Vite?

React provides reusable components and a strong ecosystem for building interactive applications.

Vite provides a fast development server and efficient production build tooling.

Together they provide a lightweight foundation for a responsive single-page application.

---

## Why Spring Boot?

Spring Boot is well suited to a backend that needs:

- REST APIs
- Dependency injection
- Database integration
- Scheduling
- External service integration
- Clear application structure

It also provides a natural path toward a production-grade Java backend.

---

## Why H2?

H2 provides a zero-configuration relational database for local development.

This makes it possible to run the application without requiring users to install and configure a separate database server.

The persistence layer can later be moved to a production database such as PostgreSQL.

---

## Why Gemini?

Meeting analysis requires semantic understanding.

The system needs to understand:

- Context
- Intent
- Decisions
- Responsibilities
- Temporal expressions
- Unresolved questions

A modern LLM is better suited to these tasks than simple keyword extraction.

---

## Why separate summary, decisions, and tasks?

A single paragraph is easy to generate but difficult to use.

Structured sections allow the interface to answer practical questions immediately:

```text
What happened?
        ↓
Summary

What was decided?
        ↓
Decisions

What needs to happen?
        ↓
Action Items

When?
        ↓
Deadlines

What remains unresolved?
        ↓
Open Questions
```

This structure also makes future integrations easier.

---

## Why server-side scheduling?

Meeting reminders are application events rather than UI events.

Handling scheduling in the backend means reminders can continue to work independently of whether a user's browser is open.

---

# Security and Reliability

Meeting recordings and transcripts may contain confidential information.

The application should therefore be developed and deployed with privacy and reliability in mind.

## Secrets

- Never commit API keys.
- Keep credentials outside source control.
- Use environment variables or a secret manager in production.

## Data handling

Meeting transcripts should be treated as potentially sensitive information.

A production deployment should implement:

- Authentication
- Authorization
- Secure transport
- Access controls
- Data retention policies
- Secure storage

## AI reliability

LLM output should not automatically be treated as ground truth.

Generated tasks, decisions, and deadlines should remain reviewable by users, particularly when the source conversation is ambiguous.

## External service failures

External AI and email providers can fail.

A production implementation should provide:

- Clear error states
- Retry strategies where appropriate
- Request timeouts
- Logging
- Graceful degradation
- No silent replacement of missing data with fabricated output

---

# Current Capabilities

The current product combines core meeting intelligence with additional productivity-oriented functionality.

## Core Meeting Intelligence

- Meeting input
- Speech-to-text processing layer
- Transcript generation
- AI summarization
- Decision extraction
- Action item extraction
- Deadline detection
- Open-question detection
- Structured result presentation

## Productivity Features

- Meeting organization
- Scheduling workflows
- Calendar-oriented functionality
- Automated reminders
- Email integration
- Translation
- Multilingual meeting intelligence

## Product Experience

- React-based SPA
- Responsive UI
- Glassmorphism design
- Dark premium interface
- Animated visual elements
- Dedicated information sections
- Clear separation between meeting content and generated intelligence

---

# Future Roadmap

The architecture provides room for several production-level improvements.

## Speaker Diarization

Automatically identify individual speakers:

```text
Speaker 1:
"We should ship Friday."

Speaker 2:
"I'll handle deployment."
```

This would improve transcript readability and owner attribution.

---

## Persistent Production Database

Move from local H2 storage to PostgreSQL or another production-grade relational database.

---

## Authentication and Authorization

Introduce:

- User accounts
- Secure authentication
- Role-based access
- Private meetings
- Team workspaces
- Organization-level controls

---

## Historical Meeting Search

Allow users to search across previous meetings.

For example:

```text
"Find every meeting where the API deadline was discussed."
```

This can eventually evolve into semantic search over the organization's meeting knowledge.

---

## Calendar Integrations

Integrate with calendar providers to:

- Import meetings
- Associate recordings with scheduled events
- Generate reminders
- Link meeting intelligence to calendar events

---

## Task Management

Turn extracted action items into persistent tasks with:

- Owner
- Status
- Priority
- Deadline
- Completion state

Example:

```text
[ ] Complete API integration
    Owner: Backend Team
    Due: Wednesday
    Priority: High
```

---

## Speaker-Aware Action Attribution

Combine diarization with LLM analysis to improve:

```text
Speaker → Statement → Task → Owner
```

This would make responsibility extraction more reliable.

---

## Confidence Indicators

Show confidence for information that was inferred rather than explicitly stated.

For example:

```text
Deadline
Friday
Confidence: High
```

or:

```text
Owner
Possibly assigned to Alex
Confidence: Medium
```

---

## Timestamped Intelligence

Link generated decisions and action items back to timestamps in the original recording.

Example:

```text
Action Item
Complete API integration

Source
00:37:24
```

This would provide strong traceability between AI output and the source meeting.

---

## Production ASR Improvements

Potential improvements include:

- Speaker diarization
- Noise reduction
- Automatic language detection
- Timestamped transcripts
- Large-file processing
- Streaming transcription
- Asynchronous processing

---

## Scalable Processing

Long meetings can eventually be processed asynchronously using background workers.

A scalable architecture could become:

```text
Upload
   ↓
Job Queue
   ↓
ASR Worker
   ↓
Transcript
   ↓
LLM Worker
   ↓
Structured Intelligence
   ↓
Database
   ↓
Frontend
```

This prevents long-running AI workloads from blocking normal API requests.

---

## Cross-Meeting Intelligence

A future version could connect information across multiple meetings.

For example:

```text
Meeting 1
   ↓
"We will complete the API migration next week."

Meeting 2
   ↓
"The API migration is still blocked."

Meeting 3
   ↓
"Migration is finally complete."
```

The system could eventually understand the lifecycle of decisions, tasks, and projects across time.

---

# Demo

A complete product walkthrough can follow this flow:

```text
Open Meeting Charcha
        ↓
Provide Meeting Input
        ↓
Start Processing
        ↓
View Transcript
        ↓
Generate AI Intelligence
        ↓
Review Summary
        ↓
Review Decisions
        ↓
Review Action Items
        ↓
Review Deadlines
        ↓
Review Open Questions
        ↓
Explore Additional Meeting Features
```

**Demo Video:** _Add the final demo link here._

---

# Screenshots

The README can be enhanced with screenshots from the final application.

Recommended screenshots:

### Dashboard

```markdown
![Meeting Charcha Dashboard](docs/screenshots/dashboard.png)
```

### Meeting Processing

```markdown
![Meeting Processing](docs/screenshots/meeting-processing.png)
```

### Generated Summary

```markdown
![AI Meeting Summary](docs/screenshots/meeting-summary.png)
```

### Action Items

```markdown
![Action Items](docs/screenshots/action-items.png)
```

### Calendar / Meetings

```markdown
![Meeting Calendar](docs/screenshots/calendar.png)
```

A recommended repository layout is:

```text
docs/
└── screenshots/
    ├── dashboard.png
    ├── meeting-processing.png
    ├── meeting-summary.png
    ├── action-items.png
    └── calendar.png
```

---

# Author

**Meeting Charcha**

Built with:

- React
- Vite
- Java
- Spring Boot
- H2
- Gemini AI
- Resend

---

# Closing

Meeting Charcha is built around a simple idea:

> **Turn every meeting into something the team can act on.**

The product combines speech recognition, language intelligence, structured information extraction, backend automation, and a polished frontend to transform unstructured conversations into a persistent source of decisions, responsibilities, and next steps.

**From conversation to clarity. From clarity to action.**
