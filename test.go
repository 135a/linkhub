package main
import (
    "encoding/json"
    "fmt"
    "time"
)
type T struct {
    Timestamp time.Time \json:"timestamp"\
}
func main() {
    var t T
    err := json.Unmarshal([]byte(\{"timestamp":"2026-04-23T14:43:44.3161792+08:00"}\), &t)
    fmt.Println(err, t.Timestamp)
}