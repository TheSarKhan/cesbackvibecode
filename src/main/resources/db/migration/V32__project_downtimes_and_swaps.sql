CREATE TABLE IF NOT EXISTS project_downtimes (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE,
    reason_type VARCHAR(50) NOT NULL,
    reason_description TEXT,
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    standby_rate NUMERIC(12, 2),
    auto_extend_end_date BOOLEAN NOT NULL DEFAULT TRUE,
    resolved_notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS project_equipment_swaps (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    old_equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    old_equipment_final_counter NUMERIC(10, 2),
    old_equipment_next_status VARCHAR(50) NOT NULL DEFAULT 'IN_REPAIR',
    new_equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    new_equipment_initial_counter NUMERIC(10, 2),
    swap_date DATE NOT NULL,
    swap_reason TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_project_downtimes_project_id ON project_downtimes(project_id);
CREATE INDEX IF NOT EXISTS idx_project_equipment_swaps_project_id ON project_equipment_swaps(project_id);
