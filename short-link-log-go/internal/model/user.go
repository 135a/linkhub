package model

type User struct {
	ID        int64  `json:"id" gorm:"primaryKey;autoIncrement"`
	Username  string `json:"username" gorm:"uniqueIndex;size:64"`
	Password  string `json:"-" gorm:"size:128"`
	CreatedAt string `json:"created_at"`
	UpdatedAt string `json:"updated_at"`
}

type LoginAttempt struct {
	ID          int64  `gorm:"primaryKey;autoIncrement"`
	IPAddress   string `gorm:"index:idx_ip_time;size:45"`
	AttemptTime string `gorm:"index:idx_ip_time"`
}

type RegisterRequest struct {
	Username string `json:"username" binding:"required,min=3,max=64"`
	Password string `json:"password" binding:"required,min=6,max=64"`
}

type LoginRequest struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
}

type LoginResponse struct {
	Token string `json:"token"`
}
