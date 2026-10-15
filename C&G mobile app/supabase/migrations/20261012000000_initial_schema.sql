-- Clutch & Gear (C&G) Initial Database Schema
-- Version: 20261012000000

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Profiles Table
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT,
    preferred_units TEXT DEFAULT 'metric' CHECK (preferred_units IN ('metric', 'imperial')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Vehicles Table
CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    make TEXT NOT NULL,
    model TEXT NOT NULL,
    model_year INT NOT NULL,
    fuel_type TEXT DEFAULT 'gasoline',
    engine_details TEXT,
    transmission TEXT,
    vin TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Diagnostic Sessions
CREATE TABLE IF NOT EXISTS diagnostic_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    adapter_type TEXT NOT NULL,
    adapter_name TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    status TEXT DEFAULT 'active'
);

-- 4. Telemetry Samples (Batch aggregation recommended)
CREATE TABLE IF NOT EXISTS telemetry_samples (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES diagnostic_sessions(id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    parameter_id TEXT NOT NULL,
    parameter_name TEXT NOT NULL,
    numeric_value DOUBLE PRECISION NOT NULL,
    unit TEXT NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    validity TEXT DEFAULT 'valid'
);

-- 5. Diagnostic Scans & Trouble Codes
CREATE TABLE IF NOT EXISTS diagnostic_scans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    session_id UUID REFERENCES diagnostic_sessions(id) ON DELETE SET NULL,
    mil_active BOOLEAN DEFAULT FALSE,
    scanned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes TEXT
);

CREATE TABLE IF NOT EXISTS diagnostic_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scan_id UUID NOT NULL REFERENCES diagnostic_scans(id) ON DELETE CASCADE,
    code TEXT NOT NULL,
    code_type TEXT DEFAULT 'stored' CHECK (code_type IN ('stored', 'pending', 'permanent')),
    category TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Alerts
CREATE TABLE IF NOT EXISTS alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    rule_id TEXT NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    severity TEXT NOT NULL CHECK (severity IN ('info', 'low', 'medium', 'high', 'urgent')),
    evidence JSONB,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

-- 7. Trips
CREATE TABLE IF NOT EXISTS trips (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    session_id UUID REFERENCES diagnostic_sessions(id) ON DELETE SET NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    distance_km DOUBLE PRECISION DEFAULT 0.0,
    avg_speed_kmh DOUBLE PRECISION DEFAULT 0.0,
    max_rpm DOUBLE PRECISION DEFAULT 0.0,
    sample_count INT DEFAULT 0
);

-- 8. Maintenance Records & Reminders
CREATE TABLE IF NOT EXISTS maintenance_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    service_date TIMESTAMPTZ NOT NULL,
    odometer_km DOUBLE PRECISION,
    category TEXT NOT NULL,
    work_performed TEXT NOT NULL,
    parts_replaced TEXT,
    cost NUMERIC(10, 2),
    currency TEXT DEFAULT 'USD',
    next_due_date TIMESTAMPTZ,
    next_due_km DOUBLE PRECISION,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 9. AI Conversations & Messages
CREATE TABLE IF NOT EXISTS ai_conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE,
    title TEXT DEFAULT 'Diagnostic Consultation',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ai_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,
    evidence_references JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_vehicles_user_id ON vehicles(user_id);
CREATE INDEX IF NOT EXISTS idx_telemetry_session_time ON telemetry_samples(session_id, captured_at);
CREATE INDEX IF NOT EXISTS idx_scans_vehicle_id ON diagnostic_scans(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_alerts_vehicle_id ON alerts(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_vehicle_id ON maintenance_records(vehicle_id);

-- ROW LEVEL SECURITY (RLS) POLICIES
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE vehicles ENABLE ROW LEVEL SECURITY;
ALTER TABLE diagnostic_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE telemetry_samples ENABLE ROW LEVEL SECURITY;
ALTER TABLE diagnostic_scans ENABLE ROW LEVEL SECURITY;
ALTER TABLE diagnostic_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE trips ENABLE ROW LEVEL SECURITY;
ALTER TABLE maintenance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_messages ENABLE ROW LEVEL SECURITY;

-- Vehicles Policy
CREATE POLICY "Users can only view their own vehicles"
    ON vehicles FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own vehicles"
    ON vehicles FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own vehicles"
    ON vehicles FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own vehicles"
    ON vehicles FOR DELETE
    USING (auth.uid() = user_id);

-- Telemetry Samples Policy
CREATE POLICY "Users can access telemetry belonging to their vehicles"
    ON telemetry_samples FOR ALL
    USING (EXISTS (
        SELECT 1 FROM vehicles
        WHERE vehicles.id = telemetry_samples.vehicle_id
        AND vehicles.user_id = auth.uid()
    ));

-- Diagnostic Scans Policy
CREATE POLICY "Users can access scans belonging to their vehicles"
    ON diagnostic_scans FOR ALL
    USING (EXISTS (
        SELECT 1 FROM vehicles
        WHERE vehicles.id = diagnostic_scans.vehicle_id
        AND vehicles.user_id = auth.uid()
    ));

-- Maintenance Records Policy
CREATE POLICY "Users can access maintenance belonging to their vehicles"
    ON maintenance_records FOR ALL
    USING (EXISTS (
        SELECT 1 FROM vehicles
        WHERE vehicles.id = maintenance_records.vehicle_id
        AND vehicles.user_id = auth.uid()
    ));
