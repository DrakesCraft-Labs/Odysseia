<p align="center">
  <img src="https://raw.githubusercontent.com/DrakesCraft-Labs/Odysseia/main/odysseia_banner.svg" width="100%" alt="Odysseia animated banner" />
</p>

# Odysseia

**The operational Paper/Purpur suite behind DrakesCraft.** Odysseia centralises the server-side systems that need consistent permissions, protected-world behaviour, durable purchase delivery, and clear staff controls.

> Runtime target: Java 21, Paper/Purpur 1.21.11. The shipped Minecraft plugin is the Java Maven artifact. This repository also contains a Rust workspace for research and future services; it is not required to boot the current Bukkit JAR.

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ea8b23?style=flat-square" alt="Java 21" />
  <img src="https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur%201.21.11-4b8bbe?style=flat-square" alt="Paper Purpur 1.21.11" />
  <img src="https://img.shields.io/badge/Delivery-Idempotent%20SQLite-8b5cf6?style=flat-square" alt="Idempotent SQLite delivery" />
</p>

## What Odysseia owns

Odysseia is deliberately the integration layer for DrakesCraft rather than a replacement for every plugin on the network.

| Area | What it does |
| --- | --- |
| Store delivery | Receives a trusted Tebex console command, validates a configured product, records an idempotent transaction in SQLite, grants the configured reward, and exposes staff recovery commands. Failed public announcements never invalidate a completed delivery. |
| Store navigation | Provides the native commerce hub behind the single public `/tienda` entrypoint, preventing a maze of overlapping shop commands. |
| Kits and passes | Handles configured kits, pending/offline delivery, timed rank/pass workflows, and the controlled SFMaster/SF Cheat path. |
| Mythic bosses | Manages Greek, Norse, and Egyptian boss families, boss rewards, the isolated `boss_arena`, and `/bosswarp` sessions. Boss combat stays outside ordinary player claims. |
| Events | Runs Blood Moon, meteor and horror-fog controls with explicit staff permissions and protection-aware behaviour. |
| Seasonal chat games | Runs compact weekly game formats under Odysseia: math, runes, trivia, reflexes and map codes. The sequence restarts on the first day of each month and unanswered games reveal their solution. |
| Protection and automation | Supplies bounded integrations for ProtectionStones, Slimefun machines, automation safeguards, and staff bypasses. It must not grant access to another player's protection by default. |
| Moderation and operations | Includes chat warning tools, safe restart support, staff utilities, owner-only tools, DiscordSRV hooks, and diagnostics/reload controls. |

## Runtime integrations

These are soft dependencies where possible. Odysseia degrades gracefully when an optional integration is absent, but production configuration should be validated before enabling a feature that depends on one.

- **Vault / Essentials / LuckPerms**: economy, ranks and server command integration.
- **Tebex**: commands are issued by Tebex; the plugin keeps the transaction ledger and performs delivery. Do not place Tebex secrets in the repository.
- **DiscordSRV**: purchase and operational notifications. A broken Discord webhook is reported but cannot make a paid transaction eligible for duplicate delivery.
- **ProtectionStones**: claim-aware machine, event, teleport and destructive-effect checks.
- **Slimefun**: SFMaster controls, machine integration and supported server-side economy flows.
- **DiosesDrakes**: divine bosses and rewards can consume the optional divine API without making it a hard boot dependency.
- **Floodgate / nLogin / WorldwideChat / PlaceholderAPI**: Bedrock-aware and community-facing compatibility hooks.

## Player-facing commands

The server's public command policy is intentionally small. Menus should lead players to categories instead of duplicating commands.

| Command | Purpose |
| --- | --- |
| `/tienda` | Opens the DrakesCraft commerce hub. This is the public shop command. |
| `/kit [name]` | Opens or claims an allowed configured kit. |
| `/bosswarp <boss> [solo\|group]` | Starts or spectates a bounded boss-arena session when permitted. |
| `/daily` | Shows the daily reward streak. |
| `/sellinv` | Sells approved resources from the main inventory. |

Actual availability is controlled by permissions and the deployed configuration. Commands registered under plugin namespaces are implementation details and are not an invitation to publish aliases to players.

## Staff commands

| Command | Purpose |
| --- | --- |
| `/odysseia reload` | Reloads the supported runtime configuration and services. Validate output before declaring a change active. |
| `/odysseia status` | Prints the operational status summary. |
| `/odysseiapurchase deliver\|test\|status\|pending\|retry\|history\|validate\|dry-run` | Transactional purchase operations and diagnostics. Restrict to trusted staff. |
| `/odysseiapendingkit <player> <kit>` | Queues a configured kit/rank delivery safely. |
| `/kitgive <player> <kit>` | Item-only kit testing. It does not perform a store transaction. |
| `/boss spawn\|spawnall\|give` | Mythic boss administration. |
| `/bloodmoon start\|stop\|status` | Blood Moon controls. |
| `/meteorito ...` | Protection-aware meteor administration. |
| `/drakeswarn` | Chat-warning management. |

`/odysseiaannounce` is a delivery-notification command for the trusted Tebex workflow; it is not a public command. Owner, troll, dragon, fog, aura and restart commands are also staff-only by design.

## Purchase delivery contract

The safe path is:

```text
Tebex payment -> Tebex console command -> Odysseia validation -> SQLite transaction ledger
  -> configured reward/rank/kit -> optional Discord announcement
```

1. Tebex must invoke the exact configured `odysseiapurchase deliver` command and supply the expected transaction identifier.
2. Odysseia validates the product against the locally deployed catalog before granting anything.
3. SQLite stores the transaction state. Replays of the same transaction are rejected or reported instead of granting the reward twice.
4. Reward delivery is the source of truth. Announcement failures are tracked separately and must not downgrade a successful delivery into a retryable purchase.
5. Use `status`, `history`, `pending`, `retry`, and `dry-run` before manually repairing a transaction.

Never commit store credentials, webhook URLs, API keys, player databases, transaction databases, or production logs.

## Bosses and the arena

Bosses are a server-owned combat activity, not a reason to expose survival claims to destructive skills.

- `/bosswarp` creates or uses the isolated `boss_arena` world and bounded cells for a session.
- The boss families include the original Olympian and Norse content plus the Egyptian expansion (`ra`, `isis`, `anubis`, and `set`) exposed through `/boss spawn` for staff control.
- Boss rewards, drop rates, equipment definitions and ability tuning belong in configuration. Test a boss in the arena before publishing balance changes.
- Arcana and divine integrations may damage bosses in `boss_arena`; their effects must never edit blocks or bypass claims in normal worlds.

## Configuration

Odysseia is built to avoid recompilation for routine operations. Use the files under the plugin data folder for product definitions, kits, rewards, cooldowns, command routing, boss tuning and integration switches. Keep the source defaults as a reference, then retain only local operational values on the server.

Recommended change procedure:

1. Take a timestamped copy of the exact production configuration and SQLite ledger before a large commercial or boss change.
2. Edit only the relevant YAML file and preserve indentation and identifiers.
3. Run `/odysseia reload` when the changed subsystem supports it, then read the console for validation errors.
4. Use a staff test account, `dry-run`, or a test package before putting a paid product live.
5. Record the change in staff notes. Do not use reload as a substitute for testing.

## Build and test

The production plugin is Maven-based:

```bash
mvn test
mvn package
```

The deployable artifact is produced under `target/`. Check the final JAR name and SHA-256 rather than copying an old `.next-*` artifact by accident.

The Rust workspace is optional development work:

```bash
cargo test --workspace
```

Passing a build proves source consistency, not that a production server is running the new JAR.

## Controlled deployment

1. Build and test from a clean, reviewed source tree.
2. Back up the current production JAR and the configuration/database files that change.
3. Ensure there is exactly one active `Odysseia.jar` in `plugins/`; archive stale `.next-*` candidates outside the live plugin directory.
4. Replace the JAR only during a planned restart window. Do not hot-swap a Paper plugin on a live server.
5. Restart the server, then verify the startup log shows the expected version, no duplicate plugin warning, and no configuration errors.
6. Exercise a non-commercial purchase test, `/odysseia status`, `/tienda`, and a boss-arena smoke test.

## Repository layout

```text
Odysseia/
├── src/main/java/                 # Production Java Paper/Purpur plugin
├── src/main/resources/            # plugin.yml and default configuration
├── src/test/                      # Java tests for transactional and game logic
├── pom.xml                        # Maven build for the Bukkit JAR
├── odysseia-core/                 # Rust research/workspace component
├── odysseia-automation/           # Rust automation experiments
├── odysseia-server/               # Rust service experiment
├── odysseia-ffi/                  # Rust FFI experiment
└── odysseia_banner.svg            # Repository artwork
```

## Security model

- Default players receive only explicitly declared public permissions.
- Staff commands are permission-gated and should be delegated through LuckPerms, not OP by habit.
- Claims are the default boundary: effects may be visual in a claim, but must not destroy blocks, move machines, or grant someone else's access.
- Transaction IDs and SQLite state are authoritative for paid deliveries.
- Logs are useful audit records but can contain player names and operational detail; keep them out of public Git repositories.

## License and authorship

MIT License. Created and maintained for DrakesCraft by **JackStar**.
