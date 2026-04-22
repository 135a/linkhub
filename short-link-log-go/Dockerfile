FROM golang:1.21-alpine AS builder
WORKDIR /app
COPY go.mod go.sum* ./
COPY . .
RUN go mod download && CGO_ENABLED=0 GOOS=linux go build -o /log-collector .

FROM alpine:3.18
WORKDIR /
COPY --from=builder /log-collector .
EXPOSE 8081
CMD ["./log-collector"]
