FROM golang:1.21-alpine AS builder

WORKDIR /app
COPY go.mod ./
RUN go mod download 2>/dev/null || true
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -mod=mod -o /log-collector .

FROM alpine:3.18
WORKDIR /
COPY --from=builder /log-collector .
EXPOSE 8081
CMD ["./log-collector"]
