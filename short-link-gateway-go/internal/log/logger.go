package log

import (
	"os"
	"shortlink-gateway-go/internal/config"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"gopkg.in/natefinch/lumberjack.v2"
)

var Logger *zap.Logger

// InitLogger initializes the global zap logger
func InitLogger(cfg *config.Config) {
	writeSyncer := getLogWriter(cfg)
	encoder := getEncoder()
	
	level := zap.InfoLevel
	if cfg.Log.Level == "debug" {
		level = zap.DebugLevel
	}

	core := zapcore.NewCore(encoder, writeSyncer, level)
	Logger = zap.New(core, zap.AddCaller())
}

func getEncoder() zapcore.Encoder {
	encoderConfig := zap.NewProductionEncoderConfig()
	encoderConfig.EncodeTime = zapcore.ISO8601TimeEncoder
	encoderConfig.EncodeLevel = zapcore.CapitalLevelEncoder
	return zapcore.NewJSONEncoder(encoderConfig)
}

func getLogWriter(cfg *config.Config) zapcore.WriteSyncer {
	if cfg.Log.Path == "" {
		return zapcore.AddSync(os.Stdout)
	}

	lumberJackLogger := &lumberjack.Logger{
		Filename:   cfg.Log.Path,
		MaxSize:    100, // megabytes
		MaxBackups: 7,
		MaxAge:     30,   // days
		Compress:   true, // disabled by default
	}
	return zapcore.NewMultiWriteSyncer(zapcore.AddSync(os.Stdout), zapcore.AddSync(lumberJackLogger))
}
