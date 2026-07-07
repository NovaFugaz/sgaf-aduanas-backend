-- Create folio sequence
CREATE SEQUENCE IF NOT EXISTS tramite_folio_seq START WITH 1 INCREMENT BY 1;

-- Create tramites table
CREATE TABLE IF NOT EXISTS tramites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    folio VARCHAR(50) UNIQUE NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    solicitante_id UUID NOT NULL,
    funcionario_id UUID,
    aduana VARCHAR(255) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    motivo_rechazo TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_tramites_solicitante_id ON tramites(solicitante_id);
CREATE INDEX IF NOT EXISTS idx_tramites_folio ON tramites(folio);
