# Budget Buddy 🪙

An immersive, production-ready Android personal finance tracking application built for mobile devices. **Budget Buddy** combines clean financial architecture, dynamic time-series data analytics, and integrated gamification components into an elegant, high-performance user experience.

---

## 📱 Features & Architecture

### 1. Unified Authentication Gate
* **Multi-Provider Social Sign-In:** Secure, fully functional integration for Google, Facebook, and Apple Sign-In authentication states alongside regular registration flows.
* **Smart UI Recovery:** Registration elements feature dynamic password toggles, contextual character length hints (minimum 6 characters), and strict validation parameters to stop client-side credential crashes before they happen.
* **Account Recovery Navigation:** Clickable recovery linkages hook backward to dedicated layout contexts without disrupting background tasks.

### 2. Live-Synchronized Dashboard & Financial Engine
* **Dynamic Central Progress Ring:** An interactive central donut layout wheel that aggregates multi-category consumption in real time and highlights active focal sectors dynamically.
* **Live Mathematical Balance Scaling:** Tracks running expenditure values against live-mutating account balances (`allowance`, `grocery`, etc.) without requiring a manual window context refresh.
* **Nested Infinite Layout Scrolling:** Optimized view component structures wrapped within vertical layout nodes, preventing UI truncation and element clipping behind the floating navigation bar.

### 3. Circular Multi-Action Navigation Overlay
* **Grid Presentation Architecture:** Tapping the primary tab menu button expands an elegant grid presentation layer overlay displaying 4 distinct, rounded circular action anchors:
  * **Analytics Overview** 📈
  * **Categories Suite** 🗂️
  * **Budget Allocation Track** 💼
  * **Transaction History Logs** 📜

### 4. Interactive Categories & Tab Synchronization
* **Seeded Initial Data Nodes:** Launches out of the box with 5 baseline spending streams: *Food & Drink*, *Shopping*, *Transport*, *Rent*, and *Grocery*.
* **Dual-Context Allocation Filter:** Incorporates distinct *Expenses* and *Income* state adapters. Selecting a segment alters data routing matrices to let users append expenses against category targets or increment wallet values effortlessly.

---

## 🏅 Advanced Unique Features (opsc Custom Additions)

### Feature A: Experience Engine & Gamification Progress Track 🎮
To incentivize consistent financial responsibility, **Budget Buddy** incorporates a native gamification leveling system. Positive real-world user behaviors (such as maintaining tracking streaks or under-spending defined category thresholds) award active Experience Points (XP).
* **State Preservation:** Tracks milestones sequentially (e.g., `650 / 1000 XP`) inside persistent storage profiles.
* **Dynamic Badging Framework:** Automatically scales user titles (e.g., *Level 5 Finance Master*) and unlocks hidden achievement badge icons natively whenever tracking goals are completed.

### Feature B: Centralized Event-Driven Notification Ledger 🔔
A decoupled, reactive log repository that dynamically populates real-time system alerts and tracking confirmations.
* **Asynchronous Log Interceptors:** Automatically intercepts background lifecycle events (such as fresh registrations, badge upgrades, or goal completion milestones) and appends a structured, time-stamped card feed to the user's view pane.
* **Preference Thread Control:** Wired directly to the profile preferences screen toggle configurations to pause or resume real-time updates seamlessly without losing historical data rows.

---

## 🛠️ Automated CI/CD Testing Pipeline

This repository leverages an automated execution pipeline via **GitHub Actions** to enforce continuous quality assurance and verify overall code stability:
* **Compilation Assertions:** Automatically sets up virtual environments on every remote code push to analyze syntax formatting and verify structural build script paths.
* **Mathematical Unit Invalidation:** Executes automated test scripts against mathematical calculation threads to guarantee exact balance tracking across wallets.
* **Production Packaging:** Builds and compiles the verified source files into a stable, deployable `.apk` mobile installer package upon pipeline success.

---

## 📝 Project Deliverables Checklist
* [x] Complete, crash-insulated source code committed on GitHub.
* [x] No raw `.zip` attachments or unparsed library dependencies.
* [x] Extensive implementation comments injected into backend files to show code understanding.
* [x] Centralized, professional Markdown documentation (`README.md`).
* [x] Application compiled into a standalone, executable Android `.apk` binary.
