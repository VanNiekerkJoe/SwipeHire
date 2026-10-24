# Interview Scheduler and Application Pipeline (User Defined Features 4 and 5)

Both features live on a mutual match, inside the existing chat screen.

## Feature 4: Interview Scheduler
- The company proposes a date, time, duration (15/30/45/60 min), format (in person / video / phone) and an optional note.
- The student accepts or declines; the proposer can cancel. Alerts go to the other person each time.
- Accepted interviews can be added to the device calendar (system calendar intent, no permission needed).
- Validation: at least 1 hour in the future, 15-120 minutes, no overlap with another proposed or accepted
  interview, and only one pending proposal at a time.

## Feature 5: Application Pipeline Tracker
Stages: **Matched -> Interview -> Offer -> Hired**, or **Declined** at any live stage.

| Role | Allowed moves |
|---|---|
| Company | Matched -> Interview, Interview -> Offer, reject at Matched/Interview/Offer |
| Student | Offer -> Hired (accept offer), withdraw at Matched/Interview/Offer |

Accepting an interview moves a fresh match from Matched to Interview automatically.
The stage is shown as a timeline in the chat and as a chip on the Matches list, and updates live on both devices.

## Firestore structure
```
matches/{matchId}
  stage: "MATCHED" | "INTERVIEW" | "OFFER" | "HIRED" | "DECLINED"
  stageUpdatedAt: timestamp
  interviews/{interviewId}
    proposedBy, startAt, durationMinutes, mode, note, status, createdAt, respondedAt
notifications/{id}   (type INTERVIEW or STAGE, picked up by push notifications)
```
Security rules for `interviews` are in `firestore.rules` (deploy with `firebase deploy --only firestore:rules`).

## Code map
| File | Purpose |
|---|---|
| `data/Pipeline.kt` | Stage, interview models |
| `util/PipelineUtils.kt` | Pure rules (unit tested) |
| `data/repository/PipelineRepository.kt` | Firestore reads/writes + notifications |
| `viewmodel/PipelineViewModel.kt` | UI state, validation, actions |
| `ui/components/PipelineComponents.kt` | Panel, timeline, interview cards, scheduling dialog |
| `ui/PipelineStrings.kt` | isiZulu, Afrikaans, Sesotho strings |

## Release notes (final POE)
- NEW: Interview Scheduler (propose, accept, decline, cancel, add to calendar) with alerts.
- NEW: Application Pipeline Tracker with role-based stage actions, live timeline and stage chips on Matches.
- NEW: Interview and stage alerts in the Alerts tab, translated into isiZulu, Afrikaans and Sesotho.
- Unit tests for stage transitions and interview validation (`PipelineUtilsTest`).
