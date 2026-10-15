# Roadmap & Project Status — Clutch & Gear

## Completed (Milestone 1 to Milestone 8)
- [x] Application foundation: Jetpack Compose, Material Design 3, automotive graphite theme.
- [x] Multi-adapter OBD hardware abstraction (`ObdAdapter`, `ConnectionState`, `ObdCommandQueue`).
- [x] Comprehensive ELM327 parser with bitmask PID support and decoders.
- [x] Deterministic local diagnostic engine, DTC dictionary, and explainable health scorer.
- [x] Local Room SQLite database with multi-vehicle garage, telemetry samples, trips, service history, and offline sync queue.
- [x] Conversational AI assistant with vehicle telemetry grounding, uncertainty communication, and voice controls (STT/TTS).
- [x] Supabase PostgreSQL migrations with strict Row Level Security (RLS).
- [x] Supabase Edge Function AI gateway specification and implementation.
- [x] Future C&G IoT adapter protocol specification (`docs/CNG_IOT_PROTOCOL.md`).

## Blocked / Pending External Verification
- [ ] Physical ELM327 Bluetooth Classic hardware test on an actual automobile OBD port (blocked in virtualized CI container).
- [ ] Physical production Supabase project linking (requires external cloud keys).
- [ ] iOS native compile verification (requires macOS/Xcode host machine).
