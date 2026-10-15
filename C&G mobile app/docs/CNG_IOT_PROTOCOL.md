# C&G IoT Device Protocol Specification (v1.0-draft)

## 1. Physical & Transport Layer
- **Transport**: Bluetooth Low Energy (BLE 5.2) & 802.11 b/g/n Wi-Fi Direct.
- **Service UUID**: `0000C600-0000-1000-8000-00805F9B34FB` (C&G Primary Service).
- **Telemetry Characteristic (Notify)**: `0000C601-...` (Streams compressed binary telemetry packets).
- **Command Characteristic (Write/Response)**: `0000C602-...` (Bidirectional control).

## 2. Telemetry Packet Schema (Binary Framing)
| Byte Offset | Field | Type | Description |
|---|---|---|---|
| 0..1 | Magic Header | uint16 | Fixed sync word `0x4347` ('CG') |
| 2 | Protocol Version | uint8 | Version `0x01` |
| 3 | Sequence Number | uint8 | Monotonically increasing packet counter |
| 4..7 | Timestamp | uint32 | Device Unix epoch milliseconds / 100 |
| 8..9 | Engine RPM | uint16 | 0.25 rpm resolution (0–16383.75) |
| 10 | Speed | uint8 | 1 km/h resolution (0–255) |
| 11 | Coolant Temp | int8 | Range -40 to 215°C (offset -40) |
| 12 | Engine Load | uint8 | Range 0–100% (0.392% step) |
| 13..14 | Battery Voltage | uint16 | Millivolts (e.g. 14200 = 14.20V) |
| 15 | Diagnostic Flags | uint8 | Bit 0: MIL On, Bit 1: Overheat, Bit 2: Low Volts |
| 16..17 | CRC16-CCITT | uint16 | Packet integrity checksum |
