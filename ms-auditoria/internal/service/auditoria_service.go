package service

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"go.uber.org/zap"
	"github.com/sgaf/ms-auditoria/internal/domain"
	"github.com/sgaf/ms-auditoria/internal/repository"
)

type AuditoriaService interface {
	RegistrarEvento(ctx context.Context, event *domain.EventoAuditoria) (bool, error)
	ObtenerEventos(ctx context.Context, filters map[string]interface{}, page, size int) ([]*domain.EventoAuditoria, int, error)
	ObtenerResumen(ctx context.Context) (int, map[string]int, map[string]int, int, error)
	Exportar(ctx context.Context, filters map[string]interface{}) (pgx.Rows, error)
}

type auditoriaService struct {
	repo   repository.EventoRepo
	logger *zap.Logger
}

func NewAuditoriaService(repo repository.EventoRepo, logger *zap.Logger) AuditoriaService {
	return &auditoriaService{repo: repo, logger: logger}
}

func (s *auditoriaService) RegistrarEvento(ctx context.Context, event *domain.EventoAuditoria) (bool, error) {
	// Wrap insert in a short timeout (40ms) to ensure non-blocking behavior
	insertCtx, cancel := context.WithTimeout(ctx, 40*time.Millisecond)
	defer cancel()

	err := s.repo.Insert(insertCtx, event)
	if err != nil {
		// If context times out (pool exhausted) or pool errors occur, log locally and return 202
		if errors.Is(err, context.DeadlineExceeded) || strings.Contains(err.Error(), "timeout") || strings.Contains(err.Error(), "pool") {
			s.logger.Warn("Database insert timed out or pool exhausted. Event logged locally.",
				zap.String("entidad", event.Entidad),
				zap.String("entidad_id", event.EntidadID),
				zap.String("accion", event.Accion),
				zap.String("usuario_id", event.UsuarioID),
				zap.String("usuario_rol", event.UsuarioRol),
				zap.String("detalle", event.Detalle),
				zap.String("ip", event.IPOrigen),
				zap.Error(err),
			)
			return false, nil // Persisted = false, err = nil
		}
		return false, err
	}
	return true, nil // Persisted = true
}

func (s *auditoriaService) ObtenerEventos(ctx context.Context, filters map[string]interface{}, page, size int) ([]*domain.EventoAuditoria, int, error) {
	return s.repo.FindAll(ctx, filters, page, size)
}

func (s *auditoriaService) ObtenerResumen(ctx context.Context) (int, map[string]int, map[string]int, int, error) {
	return s.repo.GetResumen(ctx)
}

func (s *auditoriaService) Exportar(ctx context.Context, filters map[string]interface{}) (pgx.Rows, error) {
	return s.repo.ExportStream(ctx, filters)
}
