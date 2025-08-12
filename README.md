# ReactoCraft — Reactive Minecraft Server (Java + Rust)

A polyglot Minecraft server project using:

* **Java 21 + Project Reactor + JNova** for the core logic
* **Rust** for native performance (chunk generation, compression, etc.)
* Modular, plugin-based, event-driven architecture

---

## Goals (Phase 1 MVP)

* [x] Java project structure with Gradle
* [x] Native Rust module with JNI
* [x] Game loop using Reactor (20 TPS)
* [x] Call Rust `generateChunk(x, z)` from Java
* [ ] Build TCP networking with JNova
* [ ] Implement packet system
* [ ] Add status + login protocol support
* [ ] Create plugin loading system
* [ ] Send players into an empty world

---

## Project Structure

```
reactocraft/
├── build.gradle.kts
├── settings.gradle.kts
├── server-core/          ← Game loop, native bridge
│   └── src/main/java/reactocraft/core/
├── protocol/             ← Packets, routing, phases
│   └── src/main/java/reactocraft/protocol/
├── plugins-api/          ← Plugin interfaces
│   └── src/main/java/reactocraft/api/
├── run-server/           ← CLI / main entrypoint
│   └── src/main/java/reactocraft/
├── rust-native/          ← Rust module via JNI
│   ├── src/lib.rs
│   └── Cargo.toml
```

---

## Java Components

### server-core

* `ReactoServer` with `Flux.interval()` tick loop
* JNI bridge to `generateChunk(x, z)`
* Loads native library with `System.loadLibrary("worldgen")`

### protocol

* Define `Packet`, `PacketDecoder`, `PacketHandler`
* Handshake/status/login phases

### plugins-api

```java
public interface Plugin {
    void onEnable(ServerContext ctx);
    void onDisable();
}
```

### run-server

* Loads config
* Starts `ReactoServer.main()`

---

## Rust (rust-native)

**Dependencies:**

```toml
[dependencies]
jni = "0.21.1"
```

**Function:**

```rust
#[no_mangle]
pub extern "system" fn Java_reactocraft_core_ReactoServer_generateChunk(...) { ... }
```

Build output:

* `target/release/librust-native.so`
* Copy to `run-server/` or set `-Djava.library.path`

---

## Build & Run

```bash
# Rust native build
cd rust-native
cargo build --release
cp target/release/librust-native.so ../run-server/

# Run Java
./gradlew :run-server:run
```

Or with VM option:

```
-Djava.library.path=./run-server
```

---

## Contributing

We welcome contributions of all kinds — code, documentation, testing, and ideas.

Please read our **[Contributing Guide](CONTRIBUTING.md)** for:

* Development setup instructions
* Branching and commit guidelines
* Code style conventions
* How to use our [Issue Template](.github/ISSUE_TEMPLATE.md) and [Pull Request Template](.github/PULL_REQUEST_TEMPLATE.md)

---

## Roadmap

### Protocol System

* [ ] VarInt handling (packet length, packet ID)
* [ ] `@PacketHandler` annotation support
* [ ] Per-phase routing: handshake → status → login → play

### Plugin Loader

* [ ] Load `.jar` plugins from `plugins/`
* [ ] Hook into tick, player events

### Game State

* [ ] `PlayerContext` class
* [ ] Basic `World` and `Chunk` system
* [ ] Hook chunk generator to Rust backend