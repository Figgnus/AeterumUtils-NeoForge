# AeterumUtils

Server-side utility mod for NeoForge `1.21.1` focused on AFK handling with optional Create: Numismatics economy integration.

## Features

- `/afk` command toggles AFK protection.
- Non-AFK players are kicked after configurable inactivity timeout.
- Inactivity warnings are shown in the action bar before kick.
- AFK breaks on:
  - player movement
  - camera movement (mouse look)
- Chat messages do not cancel AFK.
- Configurable Create: Numismatics requirements:
  - one-time AFK activation cost
  - configurable activation price cooldown (no re-charge within cooldown window)
  - recurring AFK upkeep cost (interval + amount)
- Runtime config reload command:
  - `/aeterumutils reload`

## Commands

- `/afk`
  - Enables/disables AFK protection.
  - When enabling, applies balance checks/cost rules from config.
- `/aeterumutils reload`
  - Reloads mod server config from disk without restarting the server.
  - Requires permission level `2` (operator/admin).

## Config

Config file path:

- Dedicated server: `config/aeterumutils/afk-server.toml`
- Dev run (this workspace): `run/config/aeterumutils/afk-server.toml`

### AFK Settings (`[afk]`)

- `timeoutSeconds`: Inactivity kick timeout.
- `warningWindowSeconds`: Seconds before kick to start countdown warnings.
- `numismaticsRequiredSpurs`: One-time cost to enable AFK.
- `afkPriceCooldownSeconds`: Cooldown for AFK activation price. If active, `/afk` re-enable does not charge again.
- `afkUpkeepSpurs`: Recurring AFK upkeep charge amount.
- `afkUpkeepIntervalSeconds`: Time between upkeep charges.

### Message Settings (`[messages]`)

All user-facing messages are configurable, including placeholders:

- `{seconds}` in `kickCountdown`
- `{required}` and `{balance}` in balance-related messages
- `{amount}` and `{balance}` in `afkUpkeepCharged`

## Create: Numismatics Integration

- Built for Create: Numismatics `1.0.19` (NeoForge).
- AFK activation and upkeep use player bank balance (in spur).
- If balance is insufficient, AFK is not enabled (or is canceled during upkeep).
- On successful upkeep charge, player is notified.

If you do not want economy gating, set:

- `numismaticsRequiredSpurs = 0`
- `afkUpkeepSpurs = 0`

## Build

```powershell
.\gradlew.bat build
```

Output jar:

- `build/libs/aeterumutils-<version>.jar`
