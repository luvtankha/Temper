# TEMPER — AGENT.md

## 0. Mission

Build **TEMPER**, a deployable, scalable, real-time conversation intelligence platform for the **CEREBRO Neural Sentiment Hackathon — Problem Statement 1**.

TEMPER must analyze the emotional evolution of multi-person conversations and identify conflict progression, sentiment drift, sarcasm, passive-aggression, toxicity, blame, defensiveness, frustration, anger, withdrawal, and escalation points across multi-turn conversations.

The product must support both:

1. **Live chat analysis**
2. **Imported chat analysis** from platforms such as WhatsApp, Slack, Discord, or similar exports

The final product must include a responsive dark-mode chat experience with **remote-participant animated avatars** that continuously transition between emotional states based on conversation analysis.

The final production stack must use:

- **Frontend:** React.js
- **Backend:** Java + Spring Boot
- **Database:** PostgreSQL
- **Realtime:** Spring WebSocket + STOMP
- **AI inference:** Java-side ONNX Runtime with compatible RoBERTa-family and other text-classification models
- **Avatar animation:** Rive
- **UI animation:** Framer Motion
- **Charts:** Recharts
- **Deployment:** Docker + reverse proxy + CI/CD

The project must remain runnable after every phase.

---

# 1. Non-Negotiable Product Rules

## 1.1 Phase Independence

Every phase must end with a working build.

A phase may introduce a mock, placeholder, adapter, or stub, but it must never leave the system in a broken intermediate state.

Example:

- Before real AI inference exists, frontend and backend may use mock analysis data.
- Once ONNX inference is introduced, the same API contract must remain stable.
- Before the database exists, in-memory repositories may be used.
- Once PostgreSQL is introduced, controller contracts must remain compatible.

## 1.2 Backward Context

Before starting any phase, the implementing agent must read and understand:

- this `AGENT.md`
- all previous `PHASE-XX-CONTEXT.md`
- all previous `PHASE-XX-HANDOFF.md`
- current API contracts
- current database schema
- current frontend component structure
- current model integration status
- current tests
- current known limitations

No phase may assume context from memory alone.

## 1.3 Integration Safety

At the end of each phase:

- frontend must build
- backend must build
- tests must pass
- API contracts must remain valid
- existing features must continue working
- current branch must remain deployable
- all new environment variables must be documented
- all database changes must use migrations
- all new behavior must be documented in the phase handoff

## 1.4 No Future-Phase Dependency

A phase must not require unfinished work from a later phase.

If a future dependency is needed, provide a local abstraction or mock.

## 1.5 API-First Design

Frontend must consume a stable API abstraction.

Backend implementation may change internally without forcing unnecessary frontend changes.

## 1.6 Privacy and Scientific Accuracy

TEMPER analyzes **linguistic and behavioral signals**. It must not claim to know a person's true internal emotional state.

Prefer language such as:

- "estimated frustration signal"
- "linguistic signals associated with anger"
- "model-detected sarcasm"
- "conversation conflict score"

Avoid deterministic claims such as:

- "Person 2 is definitely angry"
- "This person is emotionally unstable"

---

# 2. Product Definition

## 2.1 Core User Experience

Two people are chatting.

On **Person 1's screen**:

- Person 1 sees Person 2's avatar above the composer.
- The avatar reflects the model-estimated emotional response of Person 2.
- Person 1 may open live analytics for Person 2 and the whole conversation.

On **Person 2's screen**:

- Person 2 sees Person 1's avatar above the composer.
- The avatar reflects the model-estimated emotional response of Person 1.
- Person 2 may open live analytics for Person 1 and the whole conversation.

The bottom avatar is therefore always the **remote participant**, not the local user.

## 2.2 Visual Theme

The UI is **dark mode only**.

Primary palette:

- Background: `#09090B`
- Surface: `#15131C`
- Magenta: `#FF2DAA`
- Violet: `#8B5CF6`
- Purple: `#C026D3`
- Primary Text: `#F8F8FA`

The visual style should feel:

- premium
- cinematic
- minimal
- modern
- neon-accented
- readable
- serious enough for an AI analytics product

## 2.3 Avatar Concept

The avatar must resemble a stylized Bitmoji-like 3D/2D character but must use **original project assets**, not copied proprietary Bitmoji assets.

Required avatar states:

- Neutral
- Happy
- Concerned
- Confused
- Sad
- Frustrated
- Angry
- Surprised

Required base avatar variants:

- male
- female

The avatar must not behave like a static sticker.

It must use a rig/state machine so that states blend smoothly.

Example:

`Neutral -> slight concern -> tense brows -> narrowed eyes -> frustrated posture -> angry posture`

---

# 3. Final High-Level Architecture

```text
React.js Frontend
│
├── Chat Interface
├── Remote Participant Avatar
├── Responsive Analytics
├── Full Analysis Dashboard
├── Upload / History / Reports
│
├── Rive
├── Framer Motion
├── Recharts
│
└── REST + WebSocket/STOMP
        │
        ▼
Java Spring Boot Backend
│
├── Auth / Sessions
├── Chat
├── Conversation Context
├── Import Parsers
├── Analysis Orchestration
├── Conflict Engine
├── Emotional Arc Engine
├── Insight Engine
├── Avatar State Engine
├── Persistence
├── WebSocket
│
├── ONNX Runtime Java
│   ├── Sentiment model
│   ├── Emotion model
│   ├── Sarcasm model
│   └── Toxicity model
│
└── PostgreSQL

Optional Scaling Layer:
├── Redis
├── Object Storage
├── Reverse Proxy
└── Docker / CI-CD
```

---

# 4. Repository Structure

```text
cerebro/
│
├── AGENT.md
├── README.md
├── .gitignore
├── .env.example
├── docker-compose.yml
│
├── frontend/
│   ├── package.json
│   ├── src/
│   │   ├── app/
│   │   ├── components/
│   │   ├── features/
│   │   │   ├── chat/
│   │   │   ├── avatar/
│   │   │   ├── analytics/
│   │   │   ├── upload/
│   │   │   ├── insights/
│   │   │   └── history/
│   │   ├── api/
│   │   ├── hooks/
│   │   ├── state/
│   │   ├── models/
│   │   ├── mocks/
│   │   └── styles/
│   └── public/
│
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/.../cerebro/
│       │   │   ├── auth/
│       │   │   ├── chat/
│       │   │   ├── conversation/
│       │   │   ├── analysis/
│       │   │   ├── ai/
│       │   │   ├── conflict/
│       │   │   ├── emotion/
│       │   │   ├── avatar/
│       │   │   ├── insights/
│       │   │   ├── upload/
│       │   │   ├── websocket/
│       │   │   ├── persistence/
│       │   │   ├── security/
│       │   │   └── common/
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       └── test/
│
├── models/
│   ├── sentiment/
│   ├── emotion/
│   ├── sarcasm/
│   └── toxicity/
│
├── contracts/
│   └── openapi.yaml
│
├── docs/
│   ├── architecture/
│   ├── phases/
│   └── model-cards/
│
└── docker/
```

---

# 5. Root `.gitignore`

Create this in Phase 0.

```gitignore
# =========================
# TEMPER - ROOT .gitignore
# =========================

# OS
.DS_Store
Thumbs.db
Desktop.ini

# IDE
.idea/
*.iml
.vscode/
.project
.classpath
.settings/

# Logs
*.log
logs/
npm-debug.log*
yarn-debug.log*
yarn-error.log*
pnpm-debug.log*

# Secrets
.env
.env.*
!.env.example
*.pem
*.key
*.crt
secrets/
config/secrets/

# React / Node
frontend/node_modules/
frontend/dist/
frontend/build/
frontend/.vite/
frontend/.cache/
frontend/coverage/

node_modules/
dist/
build/
.cache/
coverage/

# Spring Boot / Java
backend/target/
backend/build/
backend/out/
target/
out/
*.class
*.jar
*.war
*.ear

!**/.mvn/wrapper/maven-wrapper.jar

# Gradle
.gradle/
gradle-app.setting
!gradle-wrapper.jar

# IntelliJ / Java temp
*.iws
*.ipr
hs_err_pid*
replay_pid*

# Local DB / data
*.db
*.sqlite
*.sqlite3
data/
postgres-data/

# AI models / caches
models/cache/
models/downloads/
*.onnx
*.bin
*.safetensors
*.pt
*.pth
.cache/huggingface/
huggingface/
transformers-cache/

# Keep model metadata
!models/**/README.md
!models/**/config.json
!models/**/tokenizer.json
!models/**/tokenizer_config.json
!models/**/vocab.json
!models/**/merges.txt

# Uploaded conversations
uploads/
temp/
tmp/
*.chat.txt
*.whatsapp.txt
private-conversations/

# Generated reports
reports/generated/
exports/
*.pdf.tmp

# Docker
docker-compose.override.yml

# Test outputs
test-results/
playwright-report/
screenshots/test/
cypress/screenshots/
cypress/videos/

# Misc
*.tmp
*.temp
*.swp
*.swo
*~
```

---

# 6. `.env.example`

```env
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080

DB_HOST=localhost
DB_PORT=5432
DB_NAME=cerebro
DB_USERNAME=cerebro
DB_PASSWORD=change_me

VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WS_URL=http://localhost:8080/ws

REDIS_HOST=localhost
REDIS_PORT=6379
```

---

# 7. Stable API Contract

Lock a stable response object early.

Example:

```json
{
  "messageId": "msg-31",
  "speakerId": "person-2",
  "sentiment": {
    "positive": 0.04,
    "neutral": 0.08,
    "negative": 0.88
  },
  "emotions": {
    "happy": 0.02,
    "sad": 0.12,
    "concerned": 0.16,
    "confused": 0.08,
    "frustrated": 0.79,
    "angry": 0.62,
    "surprised": 0.03
  },
  "signals": {
    "sarcasm": 0.84,
    "toxicity": 0.31,
    "passiveAggression": 0.67,
    "defensiveness": 0.71,
    "blame": 0.81
  },
  "conflictScore": 0.73,
  "dominantEmotion": "frustrated",
  "avatar": {
    "primaryState": "frustrated",
    "intensity": 0.79,
    "arousal": 0.76,
    "valence": -0.68
  }
}
```

---

# 8. Phase Documentation Standard

Before each phase create:

```text
docs/phases/PHASE-XX-CONTEXT.md
```

It must contain:

1. phase objective
2. current working state
3. all completed prior phases
4. current architecture
5. current API contracts
6. database schema
7. frontend modules
8. backend modules
9. model status
10. tests currently passing
11. known limitations
12. files that must not be broken
13. exact scope
14. explicit out-of-scope work
15. acceptance criteria

After each phase create:

```text
docs/phases/PHASE-XX-HANDOFF.md
```

It must contain:

1. summary
2. files added
3. files modified
4. APIs added/changed
5. schema migrations
6. tests added
7. test results
8. run instructions
9. verification steps
10. known issues
11. rollback considerations
12. next phase prerequisites

---

# 9. PHASE 00 — Repository and Integration Foundation

## Objective

Create the complete repository shell and ensure frontend and backend can run independently.

## Frontend

Create:

- React.js
- Vite
- React Router
- Tailwind CSS
- Axios/fetch API client abstraction
- basic app shell
- mock data provider

## Backend

Create:

- Java 21
- Spring Boot
- Spring Web
- Spring Validation
- Spring Boot Actuator
- Maven wrapper
- health endpoint

## Shared

Create:

- `AGENT.md`
- `.gitignore`
- `.env.example`
- `README.md`
- `contracts/openapi.yaml`
- initial phase docs

## Acceptance

- React starts
- Spring Boot starts
- frontend can call backend `/actuator/health` or a dedicated `/api/v1/health`
- no database required yet
- no AI required yet
- clean Git status

---

# 10. PHASE 01 — Permanent Responsive UI Shell

## Objective

Create the final responsive layout architecture before feature work.

## Requirements

Dark mode only.

Routes:

- `/chat`
- `/analysis`
- `/history`
- `/settings`

Responsive behavior:

### Desktop / Ultrawide
- chat occupies main area
- analytics panel on right
- analytics collapsible
- sidebar optional depending on width

### Tablet
- chat becomes primary
- analytics becomes slide-over drawer

### Mobile
- chat occupies full screen
- analytics opens as full-width overlay or sheet
- no horizontal scrolling

## Technical Rules

Use:

```css
height: 100dvh;
```

Avoid fixed-width assumptions.

Use:

- CSS Grid
- Flexbox
- `minmax()`
- `clamp()`
- responsive containers
- Recharts responsive containers

## Breakpoint Intent

- `>= 1440`: full desktop
- `1024–1439`: compact desktop
- `768–1023`: tablet/drawer analytics
- `<768`: mobile/full-screen analytics

## Acceptance

All screen ratios remain usable.

---

# 11. PHASE 02 — Chat UI and Message Composer

## Objective

Build the final chat interaction experience using mock data.

## Features

- two-person conversation
- message bubbles
- timestamps
- sender identity
- typing state
- online/active state
- input composer
- send action
- scroll-to-bottom
- message selection
- empty state
- loading state
- connection state

## Acceptance

Chat works fully using local/mock state.

---

# 12. PHASE 03 — Remote Avatar Placement

## Objective

Implement the core swapped-avatar UX.

## Rules

Person 1 screen:

- show Person 2 avatar above Person 1 composer

Person 2 screen:

- show Person 1 avatar above Person 2 composer

The avatar must always represent the remote participant.

## Placement

The avatar must be anchored to the composer wrapper, not the viewport.

Structure:

```text
composer-wrapper
├── remote-avatar-layer
└── composer
```

## Acceptance

Avatar remains correctly positioned at all screen sizes.

---

# 13. PHASE 04 — Avatar Rig and Emotional State Machine

## Objective

Build the male and female avatar systems.

## Required States

- Neutral
- Happy
- Concerned
- Confused
- Sad
- Frustrated
- Angry
- Surprised

## Animation Requirements

Do not swap PNGs.

Use one rig/state-machine character per avatar.

Use Rive.

Expose continuous state-machine inputs such as:

```text
anger
sadness
happiness
frustration
confusion
concern
surprise
valence
arousal
sarcasm
```

## Transition Principle

Example:

```text
Neutral
→ eyebrow tension
→ eye narrowing
→ mouth change
→ shoulder tension
→ posture change
→ Frustrated
→ stronger tension
→ Angry
```

## Acceptance

All state transitions are smooth and reversible.

---

# 14. PHASE 05 — Avatar Presence and Micro-Interactions

## Objective

Make the remote avatar feel present in the conversation.

## Idle State

Implement:

- breathing
- blinking
- subtle eye movement
- minor head movement
- hands resting above/around composer

## Typing Reaction

When remote participant starts typing:

- avatar becomes attentive
- rises slightly
- looks toward chat
- waits

## Message Reaction

When analysis arrives:

1. small facial micro-expression
2. head motion
3. upper-body change
4. complete emotional state transition

## Approximate Animation Timing

- face: 250–400 ms
- head: 300–500 ms
- body: 500–800 ms
- overall state: 700–1200 ms

## Acceptance

Avatar never appears frozen unless intentionally paused.

---

# 15. PHASE 06 — Live Analytics UI

## Objective

Create collapsible analytics using mock data.

## Show

### Emotion Signals
- Frustration
- Anger
- Sadness
- Happiness
- Confusion
- Concern

### Conversation Signals
- Negative sentiment
- Sarcasm
- Toxicity
- Passive aggression
- Defensiveness
- Blame

### Conversation-Level Metrics
- Conflict score
- Current sentiment
- Emotional intensity
- Conversation direction
- escalation started
- peak tension

## Responsive Rules

- desktop: right-side panel
- tablet: slide-over
- mobile: full-width overlay/sheet

## Acceptance

Analytics can be hidden without affecting chat usability.

---

# 16. PHASE 07 — Emotional Arc and Conflict Timeline UI

## Objective

Visualize conversation progression.

Use Recharts.

## Plot

- sentiment
- anger
- frustration
- sarcasm
- conflict score

X-axis:

- message sequence or timestamp

Highlight:

- escalation start
- peak tension
- major emotional shifts
- recovery points

Interaction:

- clicking a graph point focuses the corresponding message

## Acceptance

Chart remains responsive at every screen ratio.

---

# 17. PHASE 08 — Message-Level Explainability UI

## Objective

Allow detailed inspection of individual messages.

Example:

```text
Message #47

Negative Sentiment  91%
Sarcasm             84%
Frustration         79%
Anger               62%
Toxicity            31%
Blame                81%
Defensiveness        69%
```

Then:

```text
Why this matters:
The message contains model-detected linguistic signals associated
with sarcasm and blame, followed by increased defensiveness in
subsequent messages.
```

## Acceptance

Every analyzed message can expose its evidence without claiming certainty about internal emotion.

---

# 18. PHASE 09 — Full Analysis Dashboard

## Objective

Create `/analysis`.

## Required Sections

- overall emotional arc
- conflict timeline
- speaker comparison
- emotion distribution
- sarcasm timeline
- toxicity timeline
- escalation points
- conversation summary
- message inspector
- key insights

## Speaker Analysis

For each participant show:

- dominant estimated emotion
- average sentiment
- conflict contribution
- message count
- sarcasm signal
- toxicity signal
- major escalation points

## Acceptance

Works with mock data and responsive layout.

---

# 19. PHASE 10 — Spring Boot Domain Model

## Objective

Introduce persistent backend domain abstractions without requiring PostgreSQL yet.

## Core Models

- User
- Conversation
- Participant
- Message
- MessageAnalysis
- ConversationAnalysis
- EmotionSnapshot
- EscalationEvent

## Repository Strategy

Start with interfaces.

Allow in-memory adapters initially.

## Acceptance

Backend APIs return domain data and frontend still works.

---

# 20. PHASE 11 — Stable REST API

## Objective

Implement versioned API endpoints.

Recommended endpoints:

```text
GET  /api/v1/health

POST /api/v1/conversations
GET  /api/v1/conversations/{id}
GET  /api/v1/conversations/{id}/messages
POST /api/v1/conversations/{id}/messages

POST /api/v1/messages/{id}/analyze
GET  /api/v1/messages/{id}/analysis

GET  /api/v1/conversations/{id}/analytics
GET  /api/v1/conversations/{id}/timeline
GET  /api/v1/conversations/{id}/insights
```

## Acceptance

Frontend mock adapter can be switched to real backend adapter with no UI rewrites.

---

# 21. PHASE 12 — Real-Time WebSocket Chat

## Objective

Replace local chat simulation with real-time messaging.

Use:

- Spring WebSocket
- STOMP

## Flow

```text
Person 1
↓
WebSocket
↓
Spring Boot
↓
Persist/in-memory save
↓
Broadcast
↓
Person 2
```

Analysis should later publish separate events.

Suggested topics:

```text
/topic/conversations/{id}/messages
/topic/conversations/{id}/analysis
/topic/conversations/{id}/typing
/topic/conversations/{id}/presence
```

## Acceptance

Two browser sessions can chat in real time.

---

# 22. PHASE 13 — Conversation Context Engine

## Objective

Analyze current messages within multi-turn context.

Do not classify only the current isolated message.

Maintain:

```text
previous 3–5 messages
+
current message
+
speaker identity
```

Example:

```text
A: Why haven't you completed it?
B: I'm working on it.
A: You always say that.
B: Fine.
```

`Fine.` must be interpreted with prior context.

## Interface

```java
public interface ConversationContextService {
    ContextWindow buildContext(UUID conversationId, UUID messageId);
}
```

## Acceptance

Every inference request can access contextual history.

---

# 23. PHASE 14 — Java ONNX Inference Foundation

## Objective

Run local text classification from Spring Boot.

Use:

- ONNX Runtime Java
- tokenizer compatible with selected models

Models must be loaded once at startup, not per request.

Create abstraction:

```java
public interface TextClassifier {
    ClassificationResult classify(String input);
}
```

## Acceptance

One simple compatible model runs genuine local inference from Java.

---

# 24. PHASE 15 — RoBERTa Sentiment Integration

## Objective

Add a compatible RoBERTa-family sentiment classifier.

Pipeline:

```text
text/context
↓
tokenizer
↓
input IDs
attention mask
↓
ONNX Runtime
↓
logits
↓
softmax
↓
positive / neutral / negative
```

## Requirements

- verify model license
- document source
- document label mapping
- verify tokenizer compatibility
- record model hash/checksum
- cache model locally outside Git unless intentionally versioned

## Acceptance

Real sentiment inference replaces mock sentiment without changing API contract.

---

# 25. PHASE 16 — Emotion Classification

## Objective

Add emotion signals.

Target outputs:

- happy
- sad
- concerned
- confused
- frustrated
- angry
- surprised
- neutral when applicable

If the selected model labels differ, map them carefully and document mapping.

## Acceptance

Emotion values populate existing API schema.

---

# 26. PHASE 17 — Sarcasm Classification

## Objective

Add sarcasm detection as a dedicated signal.

Do not infer sarcasm solely from negative sentiment.

Use context where possible.

## Acceptance

Sarcasm score is exposed per message and in timeline analytics.

---

# 27. PHASE 18 — Toxicity and Hostility Classification

## Objective

Add:

- toxicity
- hostility
- aggressive tone where model supports it

## Acceptance

Toxicity and hostility signals integrate into message analysis and conversation analytics.

---

# 28. PHASE 19 — Passive Aggression, Blame, Defensiveness, Disagreement

## Objective

Add additional conflict signals.

Signals may come from:

- dedicated models
- rule-based heuristics
- lexical/context patterns
- model ensembles

Required signals:

- passive aggression
- blame
- defensiveness
- disagreement
- withdrawal
- repeated disagreement

These signals must be labeled as estimated indicators.

## Acceptance

Signals are separately inspectable and never hidden inside one score.

---

# 29. PHASE 20 — TEMPER Conflict Engine

## Objective

Create the project-specific conversation conflict model.

Example initial score:

```text
C_t =
0.25 * NegativeSentiment
+ 0.20 * Anger
+ 0.20 * Toxicity
+ 0.15 * Sarcasm
+ 0.10 * Blame
+ 0.10 * Defensiveness
```

Then smooth:

```text
E_t =
0.6 * C_t
+ 0.3 * C_(t-1)
+ 0.1 * C_(t-2)
```

Weights must be configurable.

## Requirements

Output:

- message conflict score
- smoothed conflict score
- trend direction
- escalation likelihood/signal

## Acceptance

Conversation conflict graph can be generated entirely from backend data.

---

# 30. PHASE 21 — Emotional Arc Engine

## Objective

Model progression over the conversation.

Detect:

- first significant deterioration
- escalation start
- peak tension
- rapid escalation
- recovery
- repeated disagreement
- speaker-specific shifts
- withdrawal after conflict

Example:

```text
Neutral
→ Concern
→ Frustration
→ Defensiveness
→ Anger
→ Withdrawal
```

## Acceptance

Backend produces a structured emotional timeline.

---

# 31. PHASE 22 — Avatar State Engine

## Objective

Convert NLP outputs into semantic avatar controls.

Backend must not send animation frames.

Example:

```json
{
  "primaryState": "frustrated",
  "intensity": 0.79,
  "valence": -0.68,
  "arousal": 0.81,
  "sarcasm": 0.54
}
```

Frontend maps these values to Rive parameters.

## Acceptance

Backend and frontend avatar logic remain decoupled.

---

# 32. PHASE 23 — Real-Time Analysis Event Flow

## Objective

Analyze new chat messages asynchronously after delivery.

Flow:

```text
message sent
↓
message delivered
↓
analysis job
↓
model inference
↓
conflict engine
↓
emotion arc update
↓
avatar state
↓
WebSocket analysis event
↓
remote avatar transitions
↓
analytics panel updates
```

The user should not wait for AI inference before the message appears.

## Acceptance

Chat stays responsive while analysis streams in.

---

# 33. PHASE 24 — PostgreSQL Persistence

## Objective

Replace in-memory persistence with PostgreSQL.

Use:

- Spring Data JPA
- Flyway
- connection pooling

Tables/entities:

- users
- conversations
- participants
- messages
- message_analysis
- conversation_analysis
- emotion_snapshots
- escalation_events

## Acceptance

Restarting backend preserves conversations and analysis.

---

# 34. PHASE 25 — Import Engine

## Objective

Support uploaded chat exports.

Adapters:

```text
WhatsAppParser
SlackParser
DiscordParser
```

All convert into:

```text
NormalizedConversation
```

## Supported Inputs

- `.txt`
- `.json`
- `.csv`

## Flow

```text
Upload
↓
Detect/choose format
↓
Platform adapter
↓
Normalize
↓
Store
↓
Batch analyze
↓
Generate timeline
```

## Acceptance

At least WhatsApp parsing works end-to-end before expanding to other platforms.

---

# 35. PHASE 26 — Insight Engine

## Objective

Generate structured explanations.

Example:

```text
Escalation began around message #31.

Primary estimated signals:
- direct blame
- increasing negative sentiment
- defensive language
- sarcasm

Peak tension:
message #47
```

Avoid unsupported certainty.

## Acceptance

Insights are generated from analysis data, not hard-coded demo text.

---

# 36. PHASE 27 — Upload and History UI Integration

## Objective

Connect frontend upload/history pages to backend.

Features:

- upload conversation
- progress indicator
- analysis progress
- conversation history
- open prior analysis
- delete local conversation when authorized
- search/filter history

## Acceptance

Imported chats can be revisited.

---

# 37. PHASE 28 — Reports and Export

## Objective

Generate/share analysis artifacts.

Supported outputs:

- PDF report
- JSON raw analysis
- CSV statistics
- graph image
- conversation summary

## Acceptance

Exported reports match stored analysis.

---

# 38. PHASE 29 — Authentication and Session Security

## Objective

Add user/session protection.

Use:

- Spring Security
- JWT or secure session strategy
- password hashing if local accounts exist
- authorization checks
- CORS restrictions
- input validation

## Acceptance

Users cannot access conversations they do not own or participate in.

---

# 39. PHASE 30 — Upload and Data Security

## Objective

Protect sensitive conversation uploads.

Implement:

- file size limits
- MIME/type validation
- filename sanitization
- temporary-file cleanup
- request size limits
- rate limiting
- secure logging practices
- no raw conversations in normal logs

## Acceptance

Common malformed and oversized uploads fail safely.

---

# 40. PHASE 31 — Performance and Model Optimization

## Objective

Make the backend suitable for real use.

Implement:

- singleton model loading
- asynchronous inference
- batched historical analysis
- DB indexes
- pagination
- lazy loading
- query optimization
- timeouts
- model inference metrics

Optional:

- Redis for session/cache/recent state

## Acceptance

Backend remains responsive during analysis workloads.

---

# 41. PHASE 32 — Frontend Performance and Accessibility

## Objective

Improve UX quality.

Implement:

- code splitting
- lazy routes
- memoization only where justified
- virtualized long chats if needed
- reduced-motion support
- keyboard navigation
- screen-reader labels
- color contrast checks
- touch-friendly controls

## Acceptance

Long chats and small screens remain usable.

---

# 42. PHASE 33 — Testing Foundation

## Objective

Establish comprehensive tests.

## Frontend

- component tests
- API adapter tests
- responsive layout tests
- avatar state mapping tests
- graph interaction tests

## Backend

- unit tests
- controller tests
- service tests
- repository tests
- inference adapter tests
- conflict engine tests
- parser tests
- WebSocket tests

## End-to-End

Test:

```text
Person 1 sends message
↓
Person 2 receives it
↓
analysis arrives
↓
Person 1 avatar updates on Person 2 screen
↓
analytics updates
↓
timeline updates
```

## Acceptance

Critical path is automated.

---

# 43. PHASE 34 — Dockerization

## Objective

Containerize the project.

Create:

- frontend Dockerfile
- backend Dockerfile
- PostgreSQL service
- optional Redis
- Docker Compose

Expected dev run:

```bash
docker compose up
```

## Acceptance

A clean machine with Docker can start the full stack.

---

# 44. PHASE 35 — Reverse Proxy and Production Routing

## Objective

Create production routing.

Example:

```text
Internet
↓
Nginx / reverse proxy
├── /       → React
├── /api    → Spring Boot
└── /ws     → Spring WebSocket
```

## Acceptance

REST, frontend routing, and WebSocket upgrades work through one deployment entry point.

---

# 45. PHASE 36 — Observability

## Objective

Make production failures diagnosable.

Use:

- Spring Boot Actuator
- structured logs
- request IDs
- inference latency metrics
- DB latency
- WebSocket connection count
- error metrics

Endpoints may include:

```text
/actuator/health
/actuator/metrics
```

## Acceptance

System health and bottlenecks are observable.

---

# 46. PHASE 37 — CI/CD

## Objective

Automate validation and deployment.

GitHub Actions flow:

```text
push / PR
↓
frontend install
↓
frontend tests
↓
frontend build
↓
backend tests
↓
backend package
↓
Docker build
↓
security checks
↓
deploy when authorized
```

## Acceptance

Broken builds cannot reach production deployment stage.

---

# 47. PHASE 38 — Production Deployment

## Objective

Deploy TEMPER.

The exact provider may vary.

Deployment must support:

- React static frontend
- Spring Boot backend
- PostgreSQL
- WebSockets
- HTTPS
- environment secrets
- persistent database storage

## Acceptance

Public deployment works end-to-end.

---

# 48. PHASE 39 — Final Hackathon Demo Mode

## Objective

Create a polished, deterministic demo path.

Prepare:

1. live two-person chat
2. visible swapped remote avatars
3. neutral-to-expression avatar transition
4. live sentiment/emotion signals
5. collapsible analytics
6. conflict score
7. emotional arc
8. escalation start
9. peak tension
10. message-level explanation
11. imported WhatsApp demo
12. full dashboard
13. report export

## Demo Example

Conversation progression:

```text
Neutral
↓
slight disagreement
↓
sarcasm
↓
blame
↓
frustration
↓
anger
↓
withdrawal
```

The avatar and charts should visibly follow this progression.

## Acceptance

A judge can understand the entire system without reading source code.

---

# 49. PHASE 40 — Final Acceptance and Release Gate

## Objective

Verify the complete integrated project.

## Must Pass

### Functional
- live chat
- real-time delivery
- AI analysis
- context-aware analysis
- sentiment
- emotion
- sarcasm
- toxicity
- passive aggression/blame/defensiveness indicators
- conflict score
- emotional arc
- escalation detection
- swapped remote avatars
- smooth avatar transitions
- collapsible analytics
- responsive design
- message explainability
- upload parser
- history
- reports

### Technical
- frontend builds
- backend builds
- migrations work
- tests pass
- Docker works
- WebSocket works behind proxy
- models load once
- no secrets committed
- `.gitignore` correct
- deployment healthy

### Responsive
Verify:

- ultrawide monitor
- normal desktop
- laptop
- tablet landscape
- tablet portrait
- large phone
- small phone

### Security
- unauthorized data access blocked
- unsafe upload types rejected
- secrets not exposed
- raw private chat content absent from normal logs

### Documentation
- README complete
- architecture documented
- run instructions correct
- deployment instructions correct
- model cards included
- all phase handoffs complete

## Final Result

When Phase 40 passes, TEMPER is considered complete.

---

# 50. Integration Milestones

| Milestone | Frontend | Backend | Working Result |
|---|---|---|---|
| I0 | Phase 00–01 | Phase 00 | React + Spring Boot foundation |
| I1 | Phase 02–03 | Phase 10–11 | Chat UI + stable APIs |
| I2 | Phase 03–05 | Phase 12 | Real-time chat + swapped avatars |
| I3 | Phase 06–08 | Phase 13–16 | Real sentiment/emotion statistics |
| I4 | Phase 06–09 | Phase 17–21 | Conflict analytics + emotional arc |
| I5 | Phase 04–05 | Phase 22–23 | AI-driven avatar transitions |
| I6 | Phase 08–09 | Phase 26 | Explainable insights |
| I7 | Phase 09 + upload/history | Phase 24–28 | Persistent imported analysis |
| I8 | all UI | Phase 29–33 | Secure, tested product |
| I9 | all UI | Phase 34–38 | Deployable production system |
| I10 | all | Phase 39–40 | Hackathon-ready final release |

---

# 51. Core Avatar Event Flow

```text
Person 1 sends:
"That's not what I said."
        ↓
Spring WebSocket
        ↓
Person 2 receives message immediately
        ↓
Context Engine
        ↓
RoBERTa sentiment + other classifiers
        ↓
Negative:      72%
Frustration:   64%
Anger:         41%
Sarcasm:       18%
        ↓
Conflict Engine
        ↓
Avatar State Engine
        ↓
{
  primaryState: "frustrated",
  intensity: 0.64,
  valence: -0.58,
  arousal: 0.61
}
        ↓
WebSocket analysis event
        ↓
Person 2 screen
        ↓
Person 1 avatar transitions:
Neutral
→ Concerned
→ Slight tension
→ Frustrated
```

Reverse direction behaves identically.

---

# 52. Core Imported Conversation Flow

```text
User uploads WhatsApp / Slack / Discord export
↓
Parser adapter
↓
NormalizedConversation
↓
Persist messages
↓
Build context windows
↓
Batch AI inference
↓
Conflict Engine
↓
Emotional Arc Engine
↓
Insight Engine
↓
Store analysis
↓
React dashboard
↓
Timeline + escalation + speaker analysis + report
```

---

# 53. Engineering Principles

## 53.1 Modular Monolith First

Do not prematurely create microservices.

Use a well-structured Spring Boot modular monolith.

Only split services when scale proves it necessary.

## 53.2 Interface-Driven AI

Example:

```java
public interface EmotionAnalysisService {
    MessageAnalysis analyze(
        String currentMessage,
        List<MessageContext> previousMessages
    );
}
```

Implementations may include:

```text
MockEmotionAnalysisService
OnnxEmotionAnalysisService
```

Frontend must never know the difference.

## 53.3 No Blocking UX

Message delivery must not wait for model inference.

Analysis arrives asynchronously.

## 53.4 Models Are Signals, Not Truth

Never treat a classifier output as unquestionable ground truth.

## 53.5 Preserve Explainability

Every aggregate score should be traceable to underlying signals.

## 53.6 Do Not Overbuild

The hackathon MVP must remain demonstrable.

Prioritize:

1. correct end-to-end flow
2. polished chat
3. smooth avatars
4. emotional arc
5. clear escalation explanation

before optional complexity.

---

# 54. Definition of Done for Every Phase

A phase is complete only when:

- scope implemented
- frontend compiles if touched
- backend compiles if touched
- tests pass
- no existing feature regresses
- API contract updated if required
- migrations added if schema changed
- env example updated if configuration changed
- context file updated
- handoff written
- run instructions verified
- acceptance criteria manually or automatically confirmed

Do not mark a phase complete based only on code presence.

---

# 55. Agent Behavior

The coding agent working on this project must:

1. inspect current repository state before changing anything
2. read all relevant context and handoff files
3. preserve existing working functionality
4. implement only the current phase scope unless a necessary compatibility fix is required
5. avoid unnecessary rewrites
6. maintain stable API contracts
7. write tests for new core behavior
8. document decisions
9. never commit secrets
10. never commit real private conversation exports
11. avoid licensing-unclear model assets
12. verify model source and license before integration
13. prefer local/offline inference for the final product
14. maintain the swapped-avatar behavior
15. maintain dark-mode-only UI
16. maintain responsive design at all screen ratios
17. keep analytics collapsible
18. keep avatar anchored above the composer
19. preserve accessibility
20. ensure each phase leaves the project runnable

---

# 56. Final Product Statement

TEMPER is a context-aware conversation intelligence system that:

- analyzes multi-turn conversations
- tracks sentiment and emotional progression
- detects sarcasm, toxicity, blame, defensiveness, passive aggression, and disagreement indicators
- identifies escalation points and peak tension
- visualizes emotional arcs
- explains why specific messages matter
- supports live and imported conversations
- represents the remote participant using a smoothly transitioning animated avatar
- mirrors the avatar between participants so each person observes the other participant's estimated conversational response
- remains responsive across desktop, tablet, and mobile
- uses React.js for the frontend
- uses Java Spring Boot for the backend
- performs local Java-side model inference through ONNX
- stores persistent analysis in PostgreSQL
- supports real-time communication over WebSocket/STOMP
- is containerized and deployable

Completion of all phases in this file constitutes completion of the TEMPER project.
