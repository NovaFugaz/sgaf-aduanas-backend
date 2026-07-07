-- Base migration for ms-auditoria
-- 
-- PRINCIPLE OF SEGREGATION:
-- The database user configured for ms-auditoria MUST ONLY be granted the following privileges:
--   GRANT INSERT, SELECT ON TABLE eventos_auditoria TO sgaf_audit_user;
-- Any UPDATE or DELETE operations MUST be revoked to guarantee the integrity of the audit ledger.
--   REVOKE UPDATE, DELETE ON TABLE eventos_auditoria FROM sgaf_audit_user;

CREATE TABLE IF NOT EXISTS eventos_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entidad VARCHAR(50) NOT NULL,       -- "TRAMITE", "USUARIO", "SESION", "INTEGRACION"
    entidad_id UUID,
    accion VARCHAR(100) NOT NULL,       -- "CREADO", "ESTADO_CAMBIADO", "ELIMINADO", "LOGIN", "LOGOUT", etc.
    usuario_id UUID,
    usuario_rol VARCHAR(30),
    detalle TEXT,
    ip_origen INET,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Optimize queries for index structures
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON eventos_auditoria(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_usuario ON eventos_auditoria(usuario_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_entidad ON eventos_auditoria(entidad, entidad_id);

-- Clean seed data spanning the last 7 days representing actual operations
INSERT INTO eventos_auditoria (entidad, entidad_id, accion, usuario_id, usuario_rol, detalle, ip_origen, timestamp) VALUES
('SESION', 'd40c0612-4217-48f5-93df-4024b4231b14', 'LOGIN', 'd40c0612-4217-48f5-93df-4024b4231b14', 'PASAJERO', 'Inicio de sesión exitoso desde el portal de Pasajeros', '186.104.22.45', NOW() - INTERVAL '7 days'),
('USUARIO', 'a38c23e8-5690-4822-ba3a-3c12f00a5d21', 'CREADO', 'c56c20ff-4e78-43be-a64e-012faee55611', 'ADMINISTRADOR', 'Creación de nuevo usuario funcionario Luis Valenzuela en aduana Los Libertadores', '192.168.10.14', NOW() - INTERVAL '6 days'),
('SESION', 'a38c23e8-5690-4822-ba3a-3c12f00a5d21', 'LOGIN', 'a38c23e8-5690-4822-ba3a-3c12f00a5d21', 'FUNCIONARIO', 'Primer inicio de sesión funcionario Luis Valenzuela', '192.168.12.3', NOW() - INTERVAL '5 days'),
('TRAMITE', 'fb8280f2-e564-42b7-8db1-1a3b4f62a123', 'CREADO', 'd40c0612-4217-48f5-93df-4024b4231b14', 'PASAJERO', 'Creación de trámite Declaración SAG con Folio SGF-20260702-000001', '186.104.22.45', NOW() - INTERVAL '5 days'),
('INTEGRACION', 'fb8280f2-e564-42b7-8db1-1a3b4f62a123', 'CONSULTA_PDI', 'd40c0612-4217-48f5-93df-4024b4231b14', 'PASAJERO', 'Consulta RUT 12345678-9 a PDI retornó habilitado sin alertas', '127.0.0.1', NOW() - INTERVAL '5 days'),
('TRAMITE', 'fb8280f2-e564-42b7-8db1-1a3b4f62a123', 'ESTADO_CAMBIADO', 'fb8280f2-e564-42b7-8db1-1a3b4f62a123', 'FUNCIONARIO', 'Cambio automático de estado a APROBADO por ausencia de productos restringidos', '127.0.0.1', NOW() - INTERVAL '5 days'),
('SESION', 'c56c20ff-4e78-43be-a64e-012faee55611', 'LOGIN', 'c56c20ff-4e78-43be-a64e-012faee55611', 'ADMINISTRADOR', 'Inicio de sesión administrador general de aduanas', '200.83.15.110', NOW() - INTERVAL '4 days'),
('USUARIO', 'e2e280ff-24bd-4ad6-ac81-42013f99aa12', 'ELIMINADO', 'c56c20ff-4e78-43be-a64e-012faee55611', 'ADMINISTRADOR', 'Desactivación (eliminación lógica) de usuario RUT 99888777-6', '200.83.15.110', NOW() - INTERVAL '4 days'),
('TRAMITE', 'a8c17b50-32b4-4b5c-b1f4-90a6e0172bf4', 'CREADO', '0898be22-300c-43f1-b924-d2e3f538e1a1', 'PASAJERO', 'Creación de trámite Salida Vehículo con Folio SGF-20260703-000002', '190.162.204.8', NOW() - INTERVAL '3 days'),
('TRAMITE', 'a8c17b50-32b4-4b5c-b1f4-90a6e0172bf4', 'ESTADO_CAMBIADO', 'a38c23e8-5690-4822-ba3a-3c12f00a5d21', 'FUNCIONARIO', 'Estado cambiado: PENDIENTE -> EN_REVISION por Funcionario Luis Valenzuela', '192.168.12.3', NOW() - INTERVAL '2 days'),
('INTEGRACION', 'a8c17b50-32b4-4b5c-b1f4-90a6e0172bf4', 'CONSULTA_ARG', 'a38c23e8-5690-4822-ba3a-3c12f00a5d21', 'FUNCIONARIO', 'Verificación de habilitación vehículo patente AB123CD con Aduana Argentina exitosa', '192.168.12.3', NOW() - INTERVAL '2 days'),
('TRAMITE', 'a8c17b50-32b4-4b5c-b1f4-90a6e0172bf4', 'ESTADO_CAMBIADO', 'a38c23e8-5690-4822-ba3a-3c12f00a5d21', 'FUNCIONARIO', 'Estado cambiado: EN_REVISION -> APROBADO tras verificación física y aduanera', '192.168.12.3', NOW() - INTERVAL '2 days'),
('SESION', 'a38c23e8-5690-4822-ba3a-3c12f00a5d21', 'LOGOUT', 'a38c23e8-5690-4822-ba3a-3c12f00a5d21', 'FUNCIONARIO', 'Cierre de sesión de funcionario', '192.168.12.3', NOW() - INTERVAL '2 days'),
('SESION', 'd40c0612-4217-48f5-93df-4024b4231b14', 'LOGIN', 'd40c0612-4217-48f5-93df-4024b4231b14', 'PASAJERO', 'Inicio de sesión pasajero via gateway', '186.104.22.45', NOW() - INTERVAL '1 day'),
('TRAMITE', '47de80a1-4322-49bd-9db3-a9de8d98b0f2', 'CREADO', 'd40c0612-4217-48f5-93df-4024b4231b14', 'PASAJERO', 'Creación de trámite Autorización Menor con Folio SGF-20260706-000003', '186.104.22.45', NOW() - INTERVAL '12 hours');
