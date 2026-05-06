# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Java service that polls the Alpha ESS cloud API and publishes energy data to an MQTT broker using the Home Assistant auto-discovery format. Intended to run as a Docker container alongside Home Assistant.

## Build & Run

**Prerequisites:** The HAMQTT library (`de.vdw.it:ha-mqtt-lib`) is not on Maven Central. It must be cloned from `https://bitbucket.org/vdwals/hamqtt` and installed locally with `mvn install` before building this project.

```bash
# Build fat jar
mvn package

# Run (requires env vars — see below)
java -jar target/alpha_ess_2_mqtt-*.jar

# Build Docker image (x86 — builds HAMQTT inside the image)
docker build -f Dockerfile_x86 -t alpha_ess_2_mqtt .

# Run via docker-compose (reads from .env file)
docker-compose up
```

There are no tests in this project.

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `ALPHA.USERNAME` | yes | — | Alpha ESS cloud username |
| `ALPHA.PASSWORD` | yes | — | Alpha ESS cloud password |
| `MQTT.HOST` | yes | — | MQTT broker host |
| `MQTT.PORT` | yes | — | MQTT broker port |
| `MQTT.USERNAME` | no | — | MQTT username |
| `MQTT.PASSWORD` | no | — | MQTT password |
| `MQTT.TOPIC` | no | `alpha_energy` | Base MQTT topic |
| `MQTT.DISCOVERY_TOPIC` | no | `homeassistant` | HA discovery topic |
| `MQTT.PROTOCOLL` | no | `tcp` | MQTT protocol (`tcp`/`ssl`) |
| `ALPHA.MIN_WAIT_S` | no | `0` | Minimum poll interval override (seconds) |
| `LOG_LEVEL` | no | INFO | `DEBUG` or `TRACE` |

## Architecture

### Startup Flow (`App.main`)

1. `EnvironmentService` reads all config from env vars
2. EasyDI container wires singletons (`TokenService`, `SystemService`, `ServiceFactory`, `MqttService`)
3. `ServiceFactory.init()` fetches the battery list and system IDs from Alpha ESS, then creates per-battery device services, update services, and command listeners
4. `MqttService.init()` registers all HA devices and command listeners, then connects to the broker
5. `App.start()` spawns each `Updater` as a dedicated `Thread`

### Service Layers

- **`services/alpha/get/`** — Alpha ESS API read services, all extend `AlphaService<P>`. Call `getData()` to get a typed DTO; token management is handled automatically by `TokenService` (cached JWT, auto-refreshed on expiry).
- **`services/alpha/set/`** — Write services (`BatteryControlService`) that send settings back to the Alpha ESS API.
- **`services/alpha/`** — `ChargingService` is both a command listener and a data fetcher for wallbox state.
- **`services/ha/`** — HA device definitions extending `DeviceService`. Each subclass defines its MQTT entities in its constructor and implements three `mapValues(...)` overloads to update entity state from DTOs.
- **`services/updater/`** — Poll loop threads extending `Updater`. Each runs in its own thread and calls `doUpdate()` in a loop with a configured sleep interval. `Updater.run()` catches all exceptions and retries after 30 seconds. Initial delay is randomized (1–10 s) to stagger API calls at startup.

### Key Patterns

- **Lombok `@Value`** makes all fields `private final`. Use `@NonFinal` for fields that must be mutated after construction (e.g., cached token, computed interval).
- **EasyDI** is used for DI — `@Singleton` on the class, `ed.bindInstance(...)` for externally constructed objects.
- **API authentication**: Every request requires a SHA-512 signature header (`authsignature`) built from a hardcoded key + current Unix timestamp, plus an `authtimestamp` header. See `RequestUtils.addSignatureHeader`.
- **Per-battery services**: `ServiceFactory` creates a full set of services for each battery found in the account. There is no global singleton for device/updater services — they are instantiated per battery.

### Current State

Several files have **unresolved merge conflicts** between `HEAD` and `origin/claude/check-flow-functionality-cLyGf` (visible in `ServiceFactory.java`, `ChargingService.java`, `SettingService.java`, and others). Resolve conflicts before building.
