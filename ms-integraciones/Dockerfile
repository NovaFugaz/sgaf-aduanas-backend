FROM golang:1.22-alpine AS builder
WORKDIR /src
COPY . .
RUN apk add --no-cache git ca-certificates && \
    go build -o /app/server ./cmd/server

FROM alpine:3.19
RUN apk add --no-cache ca-certificates
COPY --from=builder /app/server /app/server
EXPOSE 8083
ENTRYPOINT ["/app/server"]
