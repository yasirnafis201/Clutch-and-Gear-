# Acceptance Tests & Verification — Clutch & Gear

| ID | Test Category | Acceptance Criteria | Automated Test Status |
|---|---|---|---|
| AT-01 | OBD PID Decoder | Standard PID formulas (RPM, Speed, Coolant, Load, MAF, MAP, Throttle, Voltage) decode exact values. | PASS (Unit Test) |
| AT-02 | Supported PID Bitmap | 32-bit masks from `0100`, `0120`, `0140` parsed accurately into supported PID sets. | PASS (Unit Test) |
| AT-03 | ELM327 Response Parser | Handles prompt `>`, whitespaces, echoed AT commands, multiline responses, and error strings (`NO DATA`, `CAN ERROR`). | PASS (Unit Test) |
| AT-04 | DTC Parser | Extracts Powertrain, Chassis, Body, Network codes from standard hex bytes. | PASS (Unit Test) |
| AT-05 | Command Queue | Serializes commands, avoids overlapping requests, handles timeout & cancellation. | PASS (Unit Test) |
| AT-06 | Diagnostics Engine | Coolant threshold (>105°C) raises alert; stale telemetry detected after 3s; alert deduplicated. | PASS (Unit Test) |
| AT-07 | Explainable Health Score | Breaks score into DTC, Trend, Maintenance, and Data Completeness without opaque magic numbers. | PASS (Unit Test) |
| AT-08 | Local Room Database | Insert/query vehicle, telemetry, DTC scan, maintenance records, and trips with reactive Flow. | PASS (Room Tests) |
| AT-09 | Fake Adapter Simulation | Generates deterministic telemetry streams and simulated DTC scans without physical hardware. | PASS (Unit Test) |
| AT-10 | Bluetooth Permission Flow | Handles Android 12+ `BLUETOOTH_CONNECT`/`SCAN` permissions and Android <= 11 legacy permissions. | PASS (UI Flow) |
| AT-11 | Physical ELM327 Hardware | Verified against physical ELM327 Bluetooth Classic dongle connected to OBD-II port. | BLOCKED (No physical vehicle/dongle in cloud container) |
