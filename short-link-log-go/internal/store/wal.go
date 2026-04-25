package store

import (
	"bufio"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"shortlink-log-go/internal/model"
	"sync"
)

// ErrWALLimitReached is returned when the WAL file has reached its maximum allowed size.
var ErrWALLimitReached = errors.New("wal size limit reached")

// WAL is a simple append-only write-ahead log that persists log entries to disk
// so they can be recovered and retried after a ClickHouse write failure.
type WAL struct {
	mu      sync.Mutex
	path    string
	maxSize int64 // bytes
}

// NewWAL creates a WAL at the given path with a maximum file size of maxSize bytes.
// The parent directory is created if it does not exist.
func NewWAL(path string, maxSize int64) *WAL {
	_ = os.MkdirAll(filepath.Dir(path), 0o755)
	return &WAL{path: path, maxSize: maxSize}
}

// AppendBatch serialises each entry as a JSON line and appends it to the WAL file.
// If appending would exceed the configured size limit ErrWALLimitReached is returned
// before any bytes are written.
// It returns the number of entries successfully written.
func (w *WAL) AppendBatch(batch []model.LogEntry) (int, error) {
	w.mu.Lock()
	defer w.mu.Unlock()

	// Check current file size before writing.
	if info, err := os.Stat(w.path); err == nil {
		if info.Size() >= w.maxSize {
			return 0, ErrWALLimitReached
		}
	}

	f, err := os.OpenFile(w.path, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0o644)
	if err != nil {
		return 0, fmt.Errorf("wal open: %w", err)
	}
	defer f.Close()

	written := 0
	for _, entry := range batch {
		line, err := json.Marshal(entry)
		if err != nil {
			continue
		}
		line = append(line, '\n')

		// Re-check size limit during writing to avoid exceeding it mid-batch.
		if info, err := f.Stat(); err == nil && info.Size()+int64(len(line)) > w.maxSize {
			return written, ErrWALLimitReached
		}

		if _, err := f.Write(line); err != nil {
			return written, fmt.Errorf("wal write: %w", err)
		}
		written++
	}
	return written, nil
}

// ReadAll reads all log entries from the WAL file.
// If the file does not exist an empty slice is returned without error.
func (w *WAL) ReadAll() ([]model.LogEntry, error) {
	w.mu.Lock()
	defer w.mu.Unlock()

	f, err := os.Open(w.path)
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return nil, nil
		}
		return nil, fmt.Errorf("wal read open: %w", err)
	}
	defer f.Close()

	var entries []model.LogEntry
	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		line := scanner.Bytes()
		if len(line) == 0 {
			continue
		}
		var e model.LogEntry
		if err := json.Unmarshal(line, &e); err != nil {
			// Skip malformed lines rather than aborting the recovery.
			continue
		}
		entries = append(entries, e)
	}
	if err := scanner.Err(); err != nil {
		return entries, fmt.Errorf("wal scan: %w", err)
	}
	return entries, nil
}

// Delete removes the WAL file from disk.
// If the file does not exist the call is a no-op.
func (w *WAL) Delete() error {
	w.mu.Lock()
	defer w.mu.Unlock()

	if err := os.Remove(w.path); err != nil && !errors.Is(err, os.ErrNotExist) {
		return fmt.Errorf("wal delete: %w", err)
	}
	return nil
}
