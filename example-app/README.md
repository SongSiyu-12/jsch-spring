# Example App: SSH + SFTP with Spring Boot

This example demonstrates how to use the jsch-spring-boot-starter to:
- Execute an SSH command
- Transfer a file over SFTP

It is fully properties-driven using the `ssh.*` configuration.

## Quick start (run the example against a local container)

1) Start an SSH/SFTP server locally using Docker (LinuxServer OpenSSH):

```
docker run -d --name openssh-test -p 2222:2222 \
  -e PUID=1000 -e PGID=1000 -e TZ=UTC \
  -e PASSWORD_ACCESS=true \
  -e USER_NAME=test -e USER_PASSWORD=testpass \
  lscr.io/linuxserver/openssh-server:latest
```

2) Run the example app, enabling the demo runner and pointing to the container:

```
./mvnw -pl :example-app -am spring-boot:run \
  -Dspring-boot.run.arguments="--example.run-on-startup=true \
    --example.host=test \
    --example.command=\"echo Hello from local container\" \
    --ssh.defaults.known-hosts.mode=off \
    --ssh.hosts.test.host=localhost \
    --ssh.hosts.test.port=2222 \
    --ssh.hosts.test.username=test \
    --ssh.hosts.test.authentication.type=password \
    --ssh.hosts.test.authentication.password=testpass"
```

You should see logs showing the SSH exec result and successful SFTP round-trip.

## Configuration

See `src/main/resources/application.yml` for a complete example of properties. Key points:
- `ssh.hosts.<alias>.*` defines host connection settings
- `ssh.defaults.known-hosts.mode=off` is convenient for local/dev usage
- `example.*` controls the demo CommandLineRunner

## Testcontainers integration test

An end-to-end integration test is provided and runs by default:

- It starts a containerized OpenSSH server via Testcontainers
- Binds properties dynamically for the host discovered at runtime
- Verifies both SSH command execution and SFTP upload/download

Run just the example app tests with:

```
./mvnw -pl :example-app -am test
```

