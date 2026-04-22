package service

import (
	"fmt"
	"shortlink-log-go/internal/model"
	"shortlink-log-go/internal/store"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)

type AuthService struct {
	mysql     *store.MySQLStore
	jwtSecret string
}

func NewAuthService(mysql *store.MySQLStore, jwtSecret string) *AuthService {
	return &AuthService{mysql: mysql, jwtSecret: jwtSecret}
}

func (s *AuthService) Register(username, password string) error {
	hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return fmt.Errorf("hash password: %w", err)
	}
	return s.mysql.CreateUser(username, string(hashed))
}

func (s *AuthService) Login(username, password, ip string) (string, error) {
	count, err := s.mysql.CountRecentLoginAttempts(ip, 5*time.Minute)
	if err != nil {
		return "", fmt.Errorf("count attempts: %w", err)
	}
	if count >= 5 {
		return "", fmt.Errorf("login rate limited")
	}

	user, err := s.mysql.GetUserByUsername(username)
	if err != nil {
		s.mysql.RecordLoginAttempt(ip)
		return "", fmt.Errorf("user not found")
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password)); err != nil {
		s.mysql.RecordLoginAttempt(ip)
		return "", fmt.Errorf("invalid password")
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"username": user.Username,
		"exp":      time.Now().Add(24 * time.Hour).Unix(),
	})
	return token.SignedString([]byte(s.jwtSecret))
}
