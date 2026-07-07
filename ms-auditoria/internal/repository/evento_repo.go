package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/sgaf/ms-auditoria/internal/domain"
)

type EventoRepo interface {
	Insert(ctx context.Context, event *domain.EventoAuditoria) error
	FindAll(ctx context.Context, filters map[string]interface{}, page, size int) ([]*domain.EventoAuditoria, int, error)
	CountToday(ctx context.Context) (int, error)
	GetResumen(ctx context.Context) (int, map[string]int, map[string]int, int, error)
	ExportStream(ctx context.Context, filters map[string]interface{}) (pgx.Rows, error)
}

type postgresEventoRepo struct {
	pool *pgxpool.Pool
}

func NewEventoRepo(pool *pgxpool.Pool) EventoRepo {
	return &postgresEventoRepo{pool: pool}
}

func (r *postgresEventoRepo) Insert(ctx context.Context, event *domain.EventoAuditoria) error {
	query := `
	INSERT INTO eventos_auditoria (entidad, entidad_id, accion, usuario_id, usuario_rol, detalle, ip_origen)
	VALUES ($1, $2, $3, $4, $5, $6, $7)`

	_, err := r.pool.Exec(ctx, query,
		event.Entidad,
		event.EntidadID,
		event.Accion,
		event.UsuarioID,
		event.UsuarioRol,
		event.Detalle,
		event.IPOrigen,
	)
	if err != nil {
		return fmt.Errorf("failed to insert audit event: %w", err)
	}
	return nil
}

func (r *postgresEventoRepo) buildFilterQuery(filters map[string]interface{}) (string, []interface{}) {
	where := " WHERE 1=1"
	args := []interface{}{}
	argID := 1

	if entidad, ok := filters["entidad"]; ok && entidad != "" {
		where += fmt.Sprintf(" AND entidad = $%d", argID)
		args = append(args, entidad)
		argID++
	}

	if accion, ok := filters["accion"]; ok && accion != "" {
		where += fmt.Sprintf(" AND accion = $%d", argID)
		args = append(args, accion)
		argID++
	}

	if usuarioID, ok := filters["usuario_id"]; ok && usuarioID != "" {
		where += fmt.Sprintf(" AND usuario_id = $%d", argID)
		args = append(args, usuarioID)
		argID++
	}

	if desde, ok := filters["desde"]; ok {
		if t, ok := desde.(time.Time); ok && !t.IsZero() {
			where += fmt.Sprintf(" AND timestamp >= $%d", argID)
			args = append(args, t)
			argID++
		}
	}

	if hasta, ok := filters["hasta"]; ok {
		if t, ok := hasta.(time.Time); ok && !t.IsZero() {
			where += fmt.Sprintf(" AND timestamp <= $%d", argID)
			args = append(args, t)
			argID++
		}
	}

	return where, args
}

func (r *postgresEventoRepo) FindAll(ctx context.Context, filters map[string]interface{}, page, size int) ([]*domain.EventoAuditoria, int, error) {
	where, args := r.buildFilterQuery(filters)

	// Get total count
	countQuery := "SELECT COUNT(*) FROM eventos_auditoria" + where
	var total int
	err := r.pool.QueryRow(ctx, countQuery, args...).Scan(&total)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to count audit events: %w", err)
	}

	// Fetch page
	limit := size
	offset := page * size
	args = append(args, limit, offset)
	argLimitIdx := len(args) - 1
	argOffsetIdx := len(args)

	query := fmt.Sprintf(`
	SELECT id, entidad, entidad_id, accion, usuario_id, usuario_rol, detalle, ip_origen, timestamp
	FROM eventos_auditoria
	%s
	ORDER BY timestamp DESC
	LIMIT $%d OFFSET $%d`, where, argLimitIdx, argOffsetIdx)

	rows, err := r.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to query audit events: %w", err)
	}
	defer rows.Close()

	events := []*domain.EventoAuditoria{}
	for rows.Next() {
		var e domain.EventoAuditoria
		var entidadIDNull, usuarioIDNull, rolNull, detalleNull, ipNull *string
		err := rows.Scan(
			&e.ID,
			&e.Entidad,
			&entidadIDNull,
			&e.Accion,
			&usuarioIDNull,
			&rolNull,
			&detalleNull,
			&ipNull,
			&e.Timestamp,
		)
		if err != nil {
			return nil, 0, fmt.Errorf("failed to scan audit event: %w", err)
		}

		if entidadIDNull != nil {
			e.EntidadID = *entidadIDNull
		}
		if usuarioIDNull != nil {
			e.UsuarioID = *usuarioIDNull
		}
		if rolNull != nil {
			e.UsuarioRol = *rolNull
		}
		if detalleNull != nil {
			e.Detalle = *detalleNull
		}
		if ipNull != nil {
			e.IPOrigen = *ipNull
		}

		events = append(events, &e)
	}

	return events, total, nil
}

func (r *postgresEventoRepo) CountToday(ctx context.Context) (int, error) {
	query := "SELECT COUNT(*) FROM eventos_auditoria WHERE timestamp >= CURRENT_DATE"
	var count int
	err := r.pool.QueryRow(ctx, query).Scan(&count)
	if err != nil {
		return 0, fmt.Errorf("failed to count today's records: %w", err)
	}
	return count, nil
}

func (r *postgresEventoRepo) GetResumen(ctx context.Context) (int, map[string]int, map[string]int, int, error) {
	// 1. total_hoy
	totalHoy, err := r.CountToday(ctx)
	if err != nil {
		return 0, nil, nil, 0, err
	}

	// 2. por_accion
	queryAccion := "SELECT accion, COUNT(*) FROM eventos_auditoria GROUP BY accion"
	rowsAccion, err := r.pool.Query(ctx, queryAccion)
	if err != nil {
		return 0, nil, nil, 0, fmt.Errorf("failed to group by action: %w", err)
	}
	defer rowsAccion.Close()

	porAccion := make(map[string]int)
	for rowsAccion.Next() {
		var action string
		var count int
		if err := rowsAccion.Scan(&action, &count); err != nil {
			return 0, nil, nil, 0, err
		}
		porAccion[action] = count
	}

	// 3. por_entidad
	queryEntidad := "SELECT entidad, COUNT(*) FROM eventos_auditoria GROUP BY entidad"
	rowsEntidad, err := r.pool.Query(ctx, queryEntidad)
	if err != nil {
		return 0, nil, nil, 0, fmt.Errorf("failed to group by entity: %w", err)
	}
	defer rowsEntidad.Close()

	porEntidad := make(map[string]int)
	for rowsEntidad.Next() {
		var entity string
		var count int
		if err := rowsEntidad.Scan(&entity, &count); err != nil {
			return 0, nil, nil, 0, err
		}
		porEntidad[entity] = count
	}

	// 4. usuarios_activos_hoy
	queryActivos := "SELECT COUNT(DISTINCT usuario_id) FROM eventos_auditoria WHERE timestamp >= CURRENT_DATE AND usuario_id IS NOT NULL"
	var activos int
	err = r.pool.QueryRow(ctx, queryActivos).Scan(&activos)
	if err != nil {
		return 0, nil, nil, 0, fmt.Errorf("failed to count active users: %w", err)
	}

	return totalHoy, porAccion, porEntidad, activos, nil
}

func (r *postgresEventoRepo) ExportStream(ctx context.Context, filters map[string]interface{}) (pgx.Rows, error) {
	where, args := r.buildFilterQuery(filters)

	// Stream limit: 10000 records
	args = append(args, 10000)
	limitIdx := len(args)

	query := fmt.Sprintf(`
	SELECT timestamp, entidad, entidad_id, accion, usuario_id, usuario_rol, detalle, ip_origen
	FROM eventos_auditoria
	%s
	ORDER BY timestamp DESC
	LIMIT $%d`, where, limitIdx)

	rows, err := r.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, fmt.Errorf("failed to execute export stream: %w", err)
	}
	return rows, nil
}
