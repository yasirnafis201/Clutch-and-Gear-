# Clutch & Gear (C&G) — AI-Powered Vehicle Intelligence Platform

Clutch & Gear (C&G) is a production-minded vehicle intelligence system built to demystify automotive health. It transforms raw OBD-II parameters, diagnostic trouble codes (DTCs), and service logs into actionable diagnostic insights, maintenance alerts, and grounded AI explanations.

---

## 🚀 Key Features

- **Multi-Adapter OBD-II Abstraction Layer**:
  - **Bluetooth Classic ELM327** (Android SPP/RFCOMM socket connection).
  - **High-Fidelity Simulator & Demo Mode** (`FakeObdAdapter`) for testing with live gauge animations, DTC injection, and stress tests.
  - **Future C&G IoT Protocol Adapter** (`CngIotAdapter`) boundary with framing and checksums.
  - Serialized command queue with timeouts, retries, and collision prevention.
- **Deterministic Diagnostic Engine**:
  - Live parameter evaluation (Coolant temp thresholding, engine load anomalies, battery alternator warnings).
  - OBD-II PID Bitmap discovery (PIDs `0100`, `0120`, `0140`) and real bit-level decoders (RPM, Speed, Coolant, MAF, MAP, Load, Throttle).
  - Standard OBD-II DTC decoder (Stored, Pending, Permanent) with comprehensive Powertrain/Chassis/Body/Network definitions.
  - Safe, read-only diagnostics by default; code clearing requires explicit confirmation and safety warnings.
- **Explainable Vehicle Health Scoring**:
  - Multi-factor breakdown: Diagnostic Trouble Codes status, Sensor trend status, Maintenance currency, and Data completeness percentage.
  - Never fabricates certainty when sensor data is missing.
- **Local-First Offline Persistence (Room SQLite)**:
  - Garage & vehicle management (multi-vehicle profiles, fuel types, engine specs).
  - Telemetry samples, historical diagnostic scans, alerts, trip summaries, and maintenance history.
  - Queued cloud sync with exponential backoff and idempotency.
- **Grounded AI Vehicle Assistant**:
  - Conversational interface with text input and speech synthesis / voice recognition.
  - Vehicle telemetry snapshots and DTC status injected as grounded structured evidence.
  - Provider-independent backend architecture (Supabase Edge Function + server-side model configuration).

---

## 🛠️ Project Structure

```
├── app/
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt               # Entrypoint & navigation routing
│   │   ├── data/
│   │   │   ├── local/                    # Room Database, DAOs, and Entities
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── entities/             # Vehicle, Telemetry, Scan, Alert, Trip, Maintenance
│   │   │   │   └── dao/                  # Reactive Room DAOs (Flow<T>)
│   │   │   └── repository/               # VehicleRepository, TelemetryRepository, DiagnosticsRepository
│   │   ├── domain/
│   │   │   └── models/                   # VehicleSample, DiagnosticCode, VehicleProfile, HealthScore
│   │   ├── platform/obd/                 # Unified OBD Hardware Abstraction
│   │   │   ├── ObdAdapter.kt             # Interface definition
│   │   │   ├── ObdModels.kt              # ConnectionState, PidDefinition, DecodedValue
│   │   │   ├── PidRegistry.kt            # Standard OBD-II PIDs and bitmap parser
│   │   │   ├── Elm327Parser.kt           # ELM327 AT response & hex parser
│   │   │   ├── ObdCommandQueue.kt        # Serialized request dispatcher
│   │   │   ├── Elm327ClassicAdapter.kt   # Android Bluetooth Classic RFCOMM driver
│   │   │   ├── FakeObdAdapter.kt         # Deterministic simulator
│   │   │   └── CngIotAdapter.kt          # Future IoT custom protocol driver
│   │   ├── services/
│   │   │   ├── DiagnosticsEngine.kt      # Deterministic local safety rules
│   │   │   ├── HealthScoreService.kt     # Explainable 4-factor health scorer
│   │   │   ├── DtcDictionary.kt          # Local OBD-II fault code dictionary
│   │   │   ├── SpeechService.kt          # Android SpeechRecognizer & TextToSpeech
│   │   │   └── AiAssistantService.kt     # Grounded AI client & mock provider
│   │   └── ui/
│   │       ├── theme/                    # Automotive Graphite & Electric Cyan design system
│   │       ├── components/               # Gauges, charts, status chips, bottom bar
│   │       ├── screens/
│   │       │   ├── DashboardScreen.kt    # Primary telemetry & quick action hub
│   │       │   ├── TelemetryScreen.kt    # Real-time multi-gauge telemetry
│   │       │   ├── DiagnosticsScreen.kt  # DTC scanner & fault inspector
│   │       │   ├── GarageScreen.kt       # Multi-vehicle profiles & switcher
│   │       │   ├── HealthScreen.kt       # Health breakdown & sensor trends
│   │       │   ├── TripsScreen.kt        # Driving logs & speed/sample statistics
│   │       │   ├── MaintenanceScreen.kt  # Service logbook & reminder alerts
│   │       │   ├── AiAssistantScreen.kt  # Grounded chat & voice assistant
│   │       │   ├── ObdConnectScreen.kt   # Hardware scanner, paired devices, simulator toggle
│   │       │   └── SettingsScreen.kt     # Units, speech toggles, sync status
├── supabase/
│   ├── migrations/                       # PostgreSQL migrations with RLS policies
│   └── functions/cng-ai-assistant/       # Provider-independent AI Edge Function
└── docs/                                 # Complete architecture, security, and hardware documentation
```

---

## 🔧 Building and Running

1. **Gradle Build**:
   ```bash
   gradle assembleDebug
   ```
2. **Run Unit & Robolectric Tests**:
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. **Environment Setup**:
   Copy `.env.example` to `.env` and fill in Supabase credentials when connecting to cloud synchronization.
