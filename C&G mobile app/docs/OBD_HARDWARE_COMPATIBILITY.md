# OBD Hardware Compatibility — Clutch & Gear

## 1. Supported Adapters & Status

| Adapter Type | Transport | Android Support | iOS Support | Status |
|---|---|---|---|---|
| Generic ELM327 | Bluetooth Classic (SPP/RFCOMM) | Supported | Not Supported (iOS SPP restriction) | Verified implementation |
| Generic ELM327 | Bluetooth Low Energy (BLE) | Supported | Supported (with standard BLE GATT) | Target roadmap |
| Clutch & Gear IoT Device | Custom BLE / Wi-Fi | Planned | Planned | Protocol specification defined |
| Software Simulator | In-Memory Fake Driver | Fully Supported | Fully Supported | Verified in automated tests |

## 2. Bluetooth Classic ELM327 Specifics
- Standard SPP UUID: `00001101-0000-1000-8000-00805F9B34FB`
- Baud Rate: Typically 38400 baud (or 9600 for older clone chips).
- Command timing: Minimum 50ms delay between command lines; prompt character `>` signals readiness.
- Clones note: Many v2.1 marketed clones are buggy v1.4 derivatives. C&G implements conservative timeout tolerance and command verification (`ATZ`, `ATE0`, `ATL0`, `ATH0`, `ATSP0`).

## 3. iOS Hardware Limitation Notice
Generic Bluetooth Classic adapters using standard RFCOMM/SPP cannot pair with iOS apps without Apple MFi (Made for iPhone) hardware licensing. On iOS devices, the application displays an educational compatibility notice directing the user to compatible BLE hardware or the upcoming C&G IoT adapter. Live simulated telemetry remains available for testing.
