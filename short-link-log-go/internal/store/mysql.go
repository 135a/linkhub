package store

import (
	"database/sql"
	"fmt"
	"shortlink-log-go/internal/model"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

type MySQLStore struct {
	db *sql.DB
}

func NewMySQLStore(host, port, user, password, database string) (*MySQLStore, error) {
	dsn := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?parseTime=true", user, password, host, port, database)
	db, err := sql.Open("mysql", dsn)
	if err != nil {
		return nil, fmt.Errorf("mysql open: %w", err)
	}
	db.SetMaxOpenConns(20)
	db.SetMaxIdleConns(5)
	return &MySQLStore{db: db}, nil
}

func (s *MySQLStore) CreateUser(username, hashedPassword string) error {
	_, err := s.db.Exec("INSERT INTO log_users (username, password) VALUES (?, ?)", username, hashedPassword)
	return err
}

func (s *MySQLStore) GetUserByUsername(username string) (*model.User, error) {
	var u model.User
	err := s.db.QueryRow("SELECT id, username, password FROM log_users WHERE username = ?", username).
		Scan(&u.ID, &u.Username, &u.Password)
	if err != nil {
		return nil, err
	}
	return &u, nil
}

func (s *MySQLStore) RecordLoginAttempt(ip string) error {
	_, err := s.db.Exec("INSERT INTO login_attempts (ip_address, attempt_time) VALUES (?, ?)", ip, time.Now())
	return err
}

func (s *MySQLStore) CountRecentLoginAttempts(ip string, window time.Duration) (int, error) {
	since := time.Now().Add(-window)
	var count int
	err := s.db.QueryRow("SELECT count(*) FROM login_attempts WHERE ip_address = ? AND attempt_time > ?", ip, since).Scan(&count)
	return count, err
}
