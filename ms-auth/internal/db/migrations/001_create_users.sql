-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run VARCHAR(20) UNIQUE NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    aduana VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on run for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_run ON users(run);

-- Seed users with pre-computed bcrypt hashes (cost 12)
-- Password: password123
-- Hash: $2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW

INSERT INTO users (run, nombre, correo, password_hash, rol, aduana, activo)
VALUES 
    ('12345678-9', 'María González', 'maria@sgaf.cl', '$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW', 'PASAJERO', NULL, true),
    ('87654321-0', 'Carlos Rodríguez', 'carlos@sgaf.cl', '$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW', 'FUNCIONARIO', 'Los Libertadores', true),
    ('admin001', 'Administrador SGAF', 'admin@sgaf.cl', '$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW', 'ADMINISTRADOR', 'Los Libertadores', true)
ON CONFLICT (run) DO NOTHING;
