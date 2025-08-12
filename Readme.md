# 🌍 ReactoCraft

**ReactoCraft** is a custom Minecraft-compatible server built from scratch in Java, focused on modern architecture, asynchronous chunk management, and native world generation via Rust.

⚠️ This project is in **early development**. Most systems are prototypes or work-in-progress.

---

## 🚧 Current Status

- 🧱 Basic chunk structure (`ChunkSection`, `FullChunk`) implemented
- ⚙️ Asynchronous chunk loading/unloading using **Project Reactor**
- 🧠 World generation hooked up via **Rust** (through JNA bridge)
- ❌ No player or networking support yet
- ❌ No rendering or client protocol layer

---

## 📂 Project Structure

```

reactocraft/
├── core/
│   └── chunk/           # ChunkSection, FullChunk (16×16×16 units)
├── native/
│   └── rust-worldgen/   # Rust worldgen compiled to native and accessed via JNA
├── Server.java          # Main server loop + debug world printing
```

---

## ⚙️ Technologies

| Tech            | Purpose                             |
|-----------------|-------------------------------------|
| Java 17+        | Main server implementation          |
| Project Reactor | Async/reactive chunk loading        |
| Rust            | Procedural terrain generation       |
| JNA             | Bridge between Java and Rust        |

---

## 🎮 Goals

- ✅ Generate and print chunks using native Rust logic
- ✅ Load/unload chunks asynchronously
- ✅ Implement in-memory + disk chunk caching
- ⏳ Build a custom networking layer for Minecraft clients
- ⏳ Implement basic player simulation and entity logic

---

## 🔧 Development Setup

### Prerequisites

- Java 17+
- Cargo (for compiling Rust worldgen)
- Gradle or your preferred Java build tool

### Running
In progress

---

## 📦 Chunk System Overview

* `ChunkSection`: 16×16×16 block grid
* `FullChunk`: stack of `ChunkSection`s representing a vertical column
* Chunks are loaded asynchronously and streamed from the Rust terrain generator
* Blocks are stored as `byte` arrays (block ID only for now)

---

## 🧠 WorldGen (Rust)

The Rust worldgen library handles raw terrain generation, returning a flat `byte[]` of block IDs per chunk.

Integration uses JNA and passes raw pointers back to Java for processing.

---

## 🛣 Planned Features

* [x] Chunk data streaming from Rust
* [ ] Reactive chunk cache
* [ ] Disk-based chunk serialization
* [ ] Entity system and ticking
* [ ] Minecraft networking protocol support
* [ ] Minimalistic client (or support for connecting from Minecraft)

---

## 📜 License

Apache License 2.0 — You are free to use, modify, and distribute this project under the terms of the Apache 2.0 License. See [LICENSE](./LICENSE) for details.

---

## 🙏 Credits

* Inspired by Minecraft
* Async powered by [Project Reactor](https://projectreactor.io/)
* Procedural generation via Rust

---

ReactoCraft is just getting started. Blocks will rise. 🌋

