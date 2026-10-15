-- C&G Schema Patch: Missing RLS Policies + owner_id alias column
-- Version: 20261012000001
-- Run AFTER 20261012000000_initial_schema.sql

-- ── 1. Alerts — add RLS policies (missing from initial migration) ─────────────────
CREATE POLICY "Users can access their own vehicle alerts"
    ON alerts FOR ALL
    USING (EXISTS (
        SELECT 1 FROM vehicles
        WHERE vehicles.id = alerts.vehicle_id
        AND vehicles.user_id = auth.uid()
    ));

-- ── 2. Trips — add RLS policies (missing from initial migration) ──────────────────
CREATE POLICY "Users can access their own vehicle trips"
    ON trips FOR ALL
    USING (EXISTS (
        SELECT 1 FROM vehicles
        WHERE vehicles.id = trips.vehicle_id
        AND vehicles.user_id = auth.uid()
    ));

-- ── 3. AI Conversations — add full CRUD RLS policies ─────────────────────────────
CREATE POLICY "Users can view their own AI conversations"
    ON ai_conversations FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own AI conversations"
    ON ai_conversations FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own AI conversations"
    ON ai_conversations FOR DELETE
    USING (auth.uid() = user_id);

-- ── 4. AI Messages — RLS via conversation ownership ──────────────────────────────
CREATE POLICY "Users can access messages in their conversations"
    ON ai_messages FOR ALL
    USING (EXISTS (
        SELECT 1 FROM ai_conversations
        WHERE ai_conversations.id = ai_messages.conversation_id
        AND ai_conversations.user_id = auth.uid()
    ));

-- ── 5. Diagnostic Codes — RLS via scan/vehicle ownership chain ───────────────────
CREATE POLICY "Users can access DTC codes from their vehicle scans"
    ON diagnostic_codes FOR ALL
    USING (EXISTS (
        SELECT 1 FROM diagnostic_scans
        JOIN vehicles ON vehicles.id = diagnostic_scans.vehicle_id
        WHERE diagnostic_scans.id = diagnostic_codes.scan_id
        AND vehicles.user_id = auth.uid()
    ));

-- ── 6. Diagnostic Sessions — RLS ─────────────────────────────────────────────────
CREATE POLICY "Users can access their own diagnostic sessions"
    ON diagnostic_sessions FOR ALL
    USING (auth.uid() = user_id);

-- ── 7. Profiles — add CRUD RLS ───────────────────────────────────────────────────
CREATE POLICY "Users can view their own profile"
    ON profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
    ON profiles FOR UPDATE
    USING (auth.uid() = id);

CREATE POLICY "Profile is auto-created on signup via trigger"
    ON profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

-- ── 8. Auto-create profile on new user signup ─────────────────────────────────────
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
  INSERT INTO public.profiles (id, display_name, preferred_units)
  VALUES (NEW.id, NEW.raw_user_meta_data->>'display_name', 'metric')
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END;
$$;

CREATE OR REPLACE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ── 9. Indexes for alert and trip queries ─────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_alerts_vehicle_detected
    ON alerts(vehicle_id, detected_at DESC);

CREATE INDEX IF NOT EXISTS idx_trips_vehicle_start
    ON trips(vehicle_id, start_time DESC);

CREATE INDEX IF NOT EXISTS idx_maintenance_vehicle_service
    ON maintenance_records(vehicle_id, service_date DESC);

CREATE INDEX IF NOT EXISTS idx_ai_conversations_user
    ON ai_conversations(user_id, created_at DESC);
