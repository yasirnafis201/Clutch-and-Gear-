# API Contracts — Clutch & Gear Backend

## 1. Supabase Edge Function: `cng-ai-assistant`

### Endpoint: `POST /functions/v1/cng-ai-assistant`

#### Request Headers
```http
Authorization: Bearer <supabase_user_jwt>
Content-Type: application/json
```

#### Request Payload
```json
{
  "vehicleId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "query": "Is my coolant temperature running hot?",
  "groundingContext": {
    "vehicle": {
      "displayName": "2021 Mazda MX-5",
      "engine": "2.0L SkyActiv-G",
      "fuelType": "Gasoline"
    },
    "telemetry": [
      {
        "parameterId": "0105",
        "name": "Coolant Temperature",
        "value": 108.0,
        "unit": "°C",
        "capturedAtUtc": "2026-10-12T05:30:00Z",
        "validity": "VALID"
      },
      {
        "parameterId": "010C",
        "name": "Engine RPM",
        "value": 2400.0,
        "unit": "rpm",
        "capturedAtUtc": "2026-10-12T05:30:00Z",
        "validity": "VALID"
      }
    ],
    "diagnosticCodes": [
      {
        "code": "P0115",
        "category": "Powertrain",
        "description": "Engine Coolant Temperature Circuit Malfunction"
      }
    ],
    "activeAlerts": [
      {
        "title": "Elevated Coolant Temperature",
        "severity": "HIGH",
        "evidence": "108°C exceeded normal 90-100°C band"
      }
    ],
    "dataCoverage": {
      "liveDataAvailable": true,
      "coveragePercent": 85,
      "lastUpdatedUtc": "2026-10-12T05:30:00Z"
    }
  }
}
```

#### Response Payload (200 OK)
```json
{
  "summary": "Your engine coolant is currently at 108°C, which is elevated compared to typical operating temperatures (88°C–98°C).",
  "evidence": [
    "PID 0105 recorded 108°C at 2,400 RPM",
    "Active diagnostic trouble code P0115 recorded in latest scan"
  ],
  "possibleInterpretations": [
    "Thermostat may be sticking partially closed",
    "Coolant temperature sensor (ECT) circuit irregularity",
    "Low coolant level or airflow obstruction"
  ],
  "recommendedNextSteps": [
    "Safely pull over and let the engine idle or cool down before inspecting coolant reservoir level",
    "Do NOT open the radiator cap while the engine is hot",
    "Schedule a coolant pressure test with a certified mechanic"
  ],
  "uncertainty": "DTC P0115 indicates a circuit irregularity which could cause reading inaccuracy rather than actual physical overheating. Mechanical inspection required.",
  "dataFreshness": "Live (captured 10 seconds ago)"
}
```
