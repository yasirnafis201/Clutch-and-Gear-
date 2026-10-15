# Technical Decisions Record (ADR) — Clutch & Gear

## ADR-01: Serialized OBD-II Command Queue
- **Context**: Generic ELM327 Bluetooth adapters crash or drop frames when concurrent requests are fired.
- **Decision**: All commands run sequentially through `ObdCommandQueue` using Kotlin Coroutines Mutex. Next command dispatches only after receiving the prompt character (`>`) or on timeout.
- **Consequences**: Rock-solid stability across cheap clone adapters; predictable 10–20 Hz telemetry rates.

## ADR-02: Deterministic Local Diagnostic Engine Independent of Cloud/AI
- **Context**: Drivers need instantaneous feedback if coolant temperature spikes or a check engine code appears, even with zero network coverage.
- **Decision**: Local rules evaluate thresholds and DTC definitions directly in Kotlin. The AI engine only provides qualitative explanations and context.
- **Consequences**: 100% offline diagnostic reliability; zero latency for safety alerts.

## ADR-03: Provider-Independent AI Architecture
- **Context**: Vehicle intelligence needs to support evolving foundation models (Gemini, Claude, GPT) without client refactoring.
- **Decision**: Client talks exclusively to a standardized backend Edge Function (`cng-ai-assistant`) using structured schemas. Model routing and provider keys remain server-side.
