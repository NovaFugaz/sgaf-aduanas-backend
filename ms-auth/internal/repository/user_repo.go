package repository

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/sgaf/ms-auth/internal/domain"
)

type UserRepository struct {
	pool *pgxpool.Pool
}

func NewUserRepository(pool *pgxpool.Pool) *UserRepository {
	return &UserRepository{pool: pool}
}

func (r *UserRepository) FindByRUN(ctx context.Context, run string) (*domain.User, error) {
	user := &domain.User{}
	err := r.pool.QueryRow(ctx,
		"SELECT id, run, nombre, correo, password_hash, rol, aduana, activo, created_at, updated_at FROM users WHERE run = $1",
		run,
	).Scan(&user.ID, &user.RUN, &user.Nombre, &user.Correo, &user.PasswordHash, &user.Rol, &user.Aduana, &user.Activo, &user.CreatedAt, &user.UpdatedAt)

	if err != nil {
		return nil, fmt.Errorf("failed to find user by RUN: %w", err)
	}

	return user, nil
}

func (r *UserRepository) FindByID(ctx context.Context, id string) (*domain.User, error) {
	user := &domain.User{}
	err := r.pool.QueryRow(ctx,
		"SELECT id, run, nombre, correo, password_hash, rol, aduana, activo, created_at, updated_at FROM users WHERE id = $1",
		id,
	).Scan(&user.ID, &user.RUN, &user.Nombre, &user.Correo, &user.PasswordHash, &user.Rol, &user.Aduana, &user.Activo, &user.CreatedAt, &user.UpdatedAt)

	if err != nil {
		return nil, fmt.Errorf("failed to find user by ID: %w", err)
	}

	return user, nil
}

func (r *UserRepository) Create(ctx context.Context, user *domain.User) error {
	_, err := r.pool.Exec(ctx,
		`INSERT INTO users (id, run, nombre, correo, password_hash, rol, aduana, activo, created_at, updated_at)
		 VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
		user.ID, user.RUN, user.Nombre, user.Correo, user.PasswordHash, user.Rol, user.Aduana, user.Activo, user.CreatedAt, user.UpdatedAt,
	)

	if err != nil {
		return fmt.Errorf("failed to create user: %w", err)
	}

	return nil
}
