/**
 * C&G AI Assistant — Supabase Edge Function
 *
 * Provides grounded AI responses backed by the Gemini API.
 * All AI credentials are stored as Supabase secrets (never in the APK).
 *
 * Authentication: Requires a valid Supabase JWT (anon or user token).
 * Rate limiting: Enforced at the Supabase project level.
 *
 * Environment variables (set in Supabase Dashboard > Project Settings > Edge Functions > Secrets):
 *   GEMINI_API_KEY — Google AI Studio API key for Gemini.
 *
 * Request body (JSON):
 * {
 *   "query": "string",
 *   "vehicle": { "make": "...", "model": "...", "year": 2021, "fuelType": "..." },
 *   "telemetryContext": [
 *     { "parameterId": "010C", "parameterName": "Engine RPM", "value": 1850, "unit": "rpm" }
 *   ],
 *   "dtcContext": [
 *     { "code": "P0300", "description": "Multiple Cylinder Misfire", "severity": "HIGH" }
 *   ],
 *   "isLiveConnected": true
 * }
 *
 * Response body (JSON):
 * {
 *   "summary": "string",
 *   "interpretations": ["string"],
 *   "nextSteps": ["string"],
 *   "evidence": ["string"],
 *   "uncertainty": "string",
 *   "groundingSource": "live_obd | historical | none"
 * }
 */

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const GEMINI_API_URL =
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface TelemetryEntry {
  parameterId: string;
  parameterName: string;
  value: number;
  unit: string;
}

interface DtcEntry {
  code: string;
  description: string;
  severity: string;
}

interface VehicleInfo {
  make: string;
  model: string;
  year: number;
  fuelType?: string;
  engineDetails?: string;
}

interface RequestBody {
  query: string;
  vehicle: VehicleInfo;
  telemetryContext?: TelemetryEntry[];
  dtcContext?: DtcEntry[];
  isLiveConnected?: boolean;
}

serve(async (req: Request) => {
  // Handle CORS pre-flight
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: CORS_HEADERS });
  }

  if (req.method !== "POST") {
    return jsonError("Method not allowed", 405);
  }

  // ── Validate auth header ─────────────────────────────────────────────────
  const authHeader = req.headers.get("authorization");
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return jsonError("Unauthorized: missing Bearer token", 401);
  }

  // ── Parse and validate request body ──────────────────────────────────────
  let body: RequestBody;
  try {
    body = await req.json();
  } catch {
    return jsonError("Invalid JSON request body", 400);
  }

  const { query, vehicle, telemetryContext = [], dtcContext = [], isLiveConnected = false } = body;

  if (!query || query.trim().length === 0) {
    return jsonError("'query' field is required and must be non-empty", 400);
  }
  if (!vehicle?.make || !vehicle?.model || !vehicle?.year) {
    return jsonError("'vehicle' field must include make, model, and year", 400);
  }
  if (query.length > 1500) {
    return jsonError("Query exceeds maximum length of 1500 characters", 400);
  }

  // ── Retrieve Gemini API key from environment ──────────────────────────────
  const geminiApiKey = Deno.env.get("GEMINI_API_KEY");
  if (!geminiApiKey) {
    console.error("GEMINI_API_KEY secret is not configured");
    return jsonError(
      "AI service is not configured. Please contact the administrator.",
      503,
    );
  }

  // ── Build grounded context string ─────────────────────────────────────────
  const connectionLabel = isLiveConnected
    ? "LIVE OBD-II telemetry (actively polling)"
    : "Saved historical records (adapter disconnected)";

  const telemetryLines = telemetryContext.length > 0
    ? telemetryContext
        .map((t) => `  • ${t.parameterName} (${t.parameterId}): ${t.value} ${t.unit}`)
        .join("\n")
    : "  [No live sensor data available]";

  const dtcLines = dtcContext.length > 0
    ? dtcContext
        .map((d) => `  • ${d.code} [${d.severity}]: ${d.description}`)
        .join("\n")
    : "  [No active diagnostic trouble codes]";

  const systemPrompt = `You are the C&G (Clutch & Gear) Vehicle Intelligence AI Assistant.

You are an expert automotive diagnostic advisor grounded strictly in the data provided to you.

MANDATORY RULES:
1. Base every response ONLY on the sensor data and trouble codes provided. Never invent readings.
2. Always distinguish between what the sensors directly report versus what you infer.
3. Never claim to diagnose a specific failed component — you can identify symptoms and possible causes.
4. Always recommend professional mechanic inspection for any safety-related concerns.
5. When data is absent or stale, say so clearly — never fill gaps with assumptions.
6. Respond in a calm, expert tone. Be concise but informative.
7. Always include an uncertainty notice that accurately reflects data quality.

RESPONSE FORMAT (JSON only, no markdown):
{
  "summary": "1-2 sentence plain-language summary",
  "interpretations": ["key interpretation 1", "key interpretation 2"],
  "nextSteps": ["actionable step 1", "actionable step 2"],
  "evidence": ["evidence fact used 1", "evidence fact used 2"],
  "uncertainty": "clear statement of what is uncertain or unverifiable from this data"
}`;

  const userMessage = `VEHICLE: ${vehicle.year} ${vehicle.make} ${vehicle.model}${vehicle.fuelType ? ` (${vehicle.fuelType})` : ""}${vehicle.engineDetails ? ` — ${vehicle.engineDetails}` : ""}

DATA SOURCE: ${connectionLabel}

CURRENT SENSOR READINGS:
${telemetryLines}

ACTIVE TROUBLE CODES:
${dtcLines}

USER QUESTION: ${query.trim()}`;

  // ── Call Gemini API ───────────────────────────────────────────────────────
  let geminiResponseText: string;
  try {
    const geminiRes = await fetch(`${GEMINI_API_URL}?key=${geminiApiKey}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: systemPrompt }] },
        contents: [{ role: "user", parts: [{ text: userMessage }] }],
        generationConfig: {
          temperature: 0.3,         // Low temperature for grounded factual responses
          maxOutputTokens: 800,
          responseMimeType: "application/json",
        },
        safetySettings: [
          { category: "HARM_CATEGORY_DANGEROUS_CONTENT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
          { category: "HARM_CATEGORY_HARASSMENT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
          { category: "HARM_CATEGORY_HATE_SPEECH", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
          { category: "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
        ],
      }),
    });

    if (!geminiRes.ok) {
      const errorBody = await geminiRes.text();
      console.error(`Gemini API error ${geminiRes.status}: ${errorBody}`);
      return jsonError("AI provider returned an error. Please try again.", 502);
    }

    const geminiJson = await geminiRes.json();
    geminiResponseText =
      geminiJson?.candidates?.[0]?.content?.parts?.[0]?.text ?? null;

    if (!geminiResponseText) {
      console.error("Gemini response missing content:", JSON.stringify(geminiJson));
      return jsonError("AI provider returned an empty response.", 502);
    }
  } catch (err) {
    console.error("Network error calling Gemini:", err);
    return jsonError("Could not reach AI provider. Check network connectivity.", 503);
  }

  // ── Parse and validate Gemini JSON response ───────────────────────────────
  let aiResponse: {
    summary?: string;
    interpretations?: string[];
    nextSteps?: string[];
    evidence?: string[];
    uncertainty?: string;
  };

  try {
    aiResponse = JSON.parse(geminiResponseText);
  } catch {
    console.error("Failed to parse Gemini JSON response:", geminiResponseText);
    // Return raw text as summary fallback
    aiResponse = {
      summary: geminiResponseText.slice(0, 500),
      interpretations: [],
      nextSteps: ["Please review the response and consult a mechanic if needed."],
      evidence: [],
      uncertainty: "Response format could not be parsed. Treat with caution.",
    };
  }

  const groundingSource = isLiveConnected ? "live_obd" :
    (telemetryContext.length > 0 || dtcContext.length > 0) ? "historical" : "none";

  return new Response(
    JSON.stringify({
      summary: aiResponse.summary ?? "Unable to generate summary.",
      interpretations: aiResponse.interpretations ?? [],
      nextSteps: aiResponse.nextSteps ?? [],
      evidence: aiResponse.evidence ?? [],
      uncertainty: aiResponse.uncertainty ??
        "Diagnostic assessments are based on standard OBD-II data and should not replace professional inspection.",
      groundingSource,
    }),
    {
      status: 200,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
});

function jsonError(message: string, status: number): Response {
  return new Response(
    JSON.stringify({ error: message }),
    {
      status,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
}
