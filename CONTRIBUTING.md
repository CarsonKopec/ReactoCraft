# Contributing to ReactoCraft

Thanks for your interest in contributing to ReactoCraft!
This guide explains setup, coding standards, branching, and how to submit issues or pull requests.

---

## Prerequisites

You will need:

* Java 21
* Gradle 8.x
* Rust (latest stable)
* Git
* Internet access for dependencies

---

## Getting Started

1. **Fork the repository**
   Click the **Fork** button on the main repo page.

2. **Clone your fork**

   ```bash
   git clone https://github.com/YOUR_USERNAME/ReactoCraft.git
   cd ReactoCraft
   ```

3. **Build the native Rust module**

   ```bash
   cd native-worldgen
   cargo build --release
   cp target/release/libworldgen.so ../run-server/
   ```

4. **Run the server**

   ```bash
   ./gradlew :run-server:run
   ```

5. **Alternative run (specify native library path)**

   ```bash
   ./gradlew :run-server:run --args="-Djava.library.path=./run-server"
   ```

---

## Project Structure

* **server-core** → Main game loop, native bridge
* **protocol** → Packets, networking, phase routing
* **plugins-api** → Interfaces for plugins
* **run-server** → CLI and entrypoint
* **rust-native** → Rust native via JNI

---

## Branching Model

We use:

* `master` → Stable releases
* `dev` → Active development
* Feature branches: `feature/NAME`

---

## Coding Standards

**Java**

* Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
* Use descriptive variable and method names

**Rust**

* Use `cargo fmt` before committing
* Keep functions small and focused

---

## Commit Guidelines

* Use descriptive commit messages:

  ```
  Add packet decoder for handshake phase
  Fix chunk generator memory leak
  Implement plugin event dispatch system
  ```
* Group related changes into one commit

---

## Submitting Changes

1. **Open an Issue**
   Use the [Issue Template](.github/ISSUE_TEMPLATE.md) when creating a new issue.
   Include:

    * Clear description
    * Steps to reproduce (for bugs)
    * Expected vs actual behavior
    * Environment details

2. **Create a Branch**

   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make Your Changes**

    * Follow the coding standards
    * Keep changes focused on one task

4. **Open a Pull Request**
   Use the [Pull Request Template](.github/PULL_REQUEST_TEMPLATE.md) when submitting.
   Include:

    * Summary of changes
    * Related issues
    * Testing steps
    * Checklist confirmation

---

## How to Find Something to Work On

* Check the **GitHub Projects board**
* Look for issues labeled `good-first-issue` or `help-wanted`
* Ask in Discussions if unsure

---

## Reporting Bugs

* Check if the issue already exists
* Include steps to reproduce, expected vs actual results, and environment details

---

## Suggesting Features

* Open an `enhancement` issue with:

    * Problem description
    * Proposed solution
    * Alternatives considered

---
