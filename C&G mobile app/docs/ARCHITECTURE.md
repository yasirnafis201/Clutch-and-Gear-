# Architecture & Data Flow — Clutch & Gear (C&G)

## 1. System Overview

Clutch & Gear is designed around a decoupled, layered architecture:

```text
[ Vehicle ECU (CAN/K-Line) ]
            │
            ▼
[ Hardware Transports ]
  ├─ ELM327 Bluetooth Classic (Android RFCOMM SPP)
  ├─ Compatible BLE Adapter
  ├─ Future C&G IoT Device (Custom BLE/WiFi GATT)
  └─ FakeObdAdapter (Deterministic Simulator / Test Driver)
            │
            ▼
[ Unified ObdAdapter Interface ]
  └─ ConnectionState Flow, Telemetry Flow, Discovery, Init, PID Support
            │
            ▼
[ ObdCommandQueue & Protocol Parsers ]
  ├─ Serialized Command Dispatcher (Prevents ELM bus collisions)
  ├─ Elm327Parser (AT frames, prompt handling, error tokens)
  └─ PidRegistry (Bitmaps 0100/0120/0140, standard decoders)
            │
            ▼
[ Validated Normalized Vehicle Samples ]
  ├─ Parameters: RPM, Speed, Coolant, Load, MAP, MAF, Throttle, Voltage
  └─ Metadata: capturedAtUtc, qualityFlags, validity, sourceAdapter
            │
      ┌─────┴─────────────────────────┐
      ▼                               ▼
[ Local Diagnostics Engine ]   [ Local Room Persistence ]
  ├─ Threshold rules (Coolant)   ├─ VehicleEntity
  ├─ Voltage anomalies           ├─ TelemetrySampleEntity
  ├─ DTC Bitfield interpretation ├─ DiagnosticScanEntity & AlertEntity
  ├─ Trend & coverage analysis   ├─ TripEntity & MaintenanceEntity
  └─ 4-Factor Health Score       └─ Offline SyncQueueEntity
      │                               │
      └──────────────┬────────────────┘
                     ▼
           [ Jetpack Compose UI ]
             ├─ Main Dashboard & Gauges
             ├─ DTC Scanner & Fault Explanations
             ├─ Garage & Maintenance
             └─ Grounded AI Voice/Chat Assistant
                     │
                     ▼ (Secure Network Sync)
           [ Supabase Backend ]
             ├─ PostgreSQL with Row-Level Security (RLS)
             └─ Edge Function AI Gateway (Model-Agnostic Grounding)
```

## 2. Key Components

### 2.1 Unified OBD Adapter Layer
- Interface: `ObdAdapter`
- Decouples UI and diagnostic services completely from the underlying transport protocol.
- Normalizes states: `Disconnected`, `Connecting`, `Connected`, `StaleData`, `PermissionDenied`, `UnsupportedHardware`, `Error`.
- Command serialization through `ObdCommandQueue`: prevents overlapping requests that cause ELM327 lockups.

### 2.2 Local Diagnostics Engine
- Fully deterministic and local. Never relies on internet access or an LLM to identify critical alerts.
- Evaluates:
  1. High engine coolant temperature (sustaining > 105°C).
  2. Electrical charging system voltage (< 12.2V with engine running).
  3. Diagnostic Trouble Code (DTC) emergence and recurrence.
  4. Telemetry freshness (stale data warning after 3000ms inactivity).
  5. Maintenance schedule thresholds (mileage and calendar intervals).

### 2.3 Explainable Health Scorer
Calculates a 0–100 health index composed of:
1. **DTC Impact (40%)**: Clean scan vs active stored/pending faults.
2. **Sensor Trend Health (30%)**: Engine load and temperature stability.
3. **Maintenance Health (20%)**: Service currency against intervals.
4. **Data Coverage (10%)**: Percentage of standard PIDs verified on the vehicle.
When data coverage is low, health is explicitly flagged as "Partial Evidence — Connect to vehicle for complete diagnostics".

### 2.4 AI Gateway Architecture
- The mobile application connects to the secure Supabase Edge Function (`/cng-ai-assistant`).
- Injects structured JSON grounding data:
  ```json
  {
    "vehicle": {"make": "Mazda", "model": "Miata", "year": 2021},
    "telemetry": [{"param": "010C", "name": "RPM", "value": 850, "unit": "rpm"}],
    "dtcs": [{"code": "P0115", "description": "Engine Coolant Temperature Circuit Malfunction"}],
    "coverage": {"isLive": true, "coveragePercent": 85}
  }
  ```
- The backend function delegates to the configured model (e.g. Gemini, Anthropic, OpenAI) with prompt guardrails ensuring it never invents non-existent faults or executes vehicle commands.
