# Security, Privacy & Safety Guidelines — Clutch & Gear

## 1. Safety Guardrails & Read-Only Policy
- **No Arbitrary ECU Writes**: The application strictly issues Mode 01 (current data) and Mode 03/07/0A (trouble codes) diagnostic requests. Arbitrary CAN bus frame injection, ECU configuration overrides, and speed governor modifications are forbidden.
- **Clear Codes Guardrail**: Code clearing (Mode 04) is isolated behind an explicit safety dialog, requiring two-step confirmation and educating the user that clearing does not repair mechanical issues.
- **Urgent Vehicle Safety Guidance**: If critical conditions occur (such as overheating or low oil pressure warnings), deterministic local alerts take immediate precedence over AI chat. Users are urged to pull over safely.

## 2. Secrets Management
- No server-side API keys or model tokens are stored in the client application.
- Supabase anonymous key is restricted by PostgreSQL Row Level Security (RLS).
- Server secrets reside in the Supabase Vault and server environment variables.

## 3. Privacy & Telemetry
- VIN is treated as optional, sensitive metadata.
- Diagnostic telemetry is tagged with user IDs and secured by database RLS policies.
- Voice input is processed on-device or streamed to the OS speech engine with explicit user microphone permission (`RECORD_AUDIO`).
