# ✎ Memento Text Editor — Smart Editor (Undo/Redo)

<p align="center">
  <img width="1366" height="728" alt="image" src="https://github.com/user-attachments/assets/9a675a1b-d82a-4f39-8e56-f43bb8fb43bd" />
</p>

**Repository:** `https://github.com/Tharindu714/Memento-Text-Editor-Application.git`

> A sleek, colourful Java Swing demo that implements the **Memento** design pattern to provide reliable multi-step undo/redo for a text editor. Snapshots (mementos) are captured on demand and automatically (debounced), allowing users to safely revert to any previous editing state.

---

## ✨ Highlights

* ✅ **Memento Pattern**: `Snapshot` (memento), `EditorOriginator` (originator), `HistoryManager` (caretaker).
* ⏱️ **Debounced auto-save**: snapshots are created after idle so typing isn’t saved on every keystroke.
* 🔁 **Multi-level Undo / Redo**: step backward or forward through saved snapshots; double-click a snapshot to restore.
* 🎛️ **Attractive UI**: smart-home–inspired gradient header, editor pane with monospace font, history panel, shortcuts (Ctrl+Z / Ctrl+Y / Ctrl+S).
* 🛡️ **Anti-pattern rules applied**: immutable mementos, capped history to avoid memory bloat, decoupled UI and history management.

---

## 🚀 Features

* Manual snapshot save and automatic snapshot on idle.
* Undo / Redo buttons and keyboard shortcuts (Ctrl+Z / Ctrl+Y).
* History list with timestamps and short previews; double-click to restore any snapshot.
* Configurable history cap (default 60).
* Status messages show snapshot actions and undo/redo feedback.

---

## 🛠️ Build & Run

Requires **Java 8+**.

```bash
# from repo root
javac TextEditor_Memento_GUI.java
java TextEditor_Memento_GUI
```

The app opens a colourful Smart Editor window. Snapshots are stored in memory (demo). For persistence add disk-write logic in `HistoryManager`.

---

## 🧭 Design Overview

**Core components**

* `Snapshot` — **Memento**: immutable object that stores the editor content and timestamp.
* `EditorOriginator` — **Originator**: knows how to create and restore `Snapshot`s from current editor content.
* `HistoryManager` — **Caretaker**: stores a list of snapshots, the current index, and supports undo/redo and pruning to a maximum size.
* `EditorFrame` — Swing UI: editor area, history panel, controls and header.

**Flow**

1. User edits text → after idle (debounce) or on manual save → `Originator.createSnapshot()` → `HistoryManager.add(snapshot)`.
2. Undo → `HistoryManager.undo()` returns previous snapshot → `Originator.restore(snapshot)` → UI updates text area.
3. Redo similarly returns later snapshot.

---

## ✅ Anti-patterns avoided (rules applied)

* ❌ **No exposing mutable editor internals** — snapshots are immutable and only `Originator` creates/restores them.
* ✅ **No unlimited history** — history is capped (default 60); oldest snapshots removed when cap exceeds.
* ✅ **No saving every keystroke** — debounce implemented (1.2s) to limit snapshot frequency.
* ✅ **Separation of concerns** — `HistoryManager` does not directly manipulate UI, only stores `Snapshot` objects.

---

## 📐 UML (PlantUML)

Paste into [https://www.plantuml.com/plantuml](https://www.plantuml.com/plantuml) to render diagrams.

### Class Diagram

<p align="center">
  <img width="910" height="683" alt="UML-Light" src="https://github.com/user-attachments/assets/d7294786-abe4-4b81-88ce-a453c53603a4" />
</p>

### Sequence Diagram (Save & Undo)

<p align="center">
  <img width="478" height="509" alt="Seq-Light" src="https://github.com/user-attachments/assets/e8b1f9c7-45b5-48c7-8031-54c458cd6ade" />
</p>

---

## 📸 Scenario

<p align="center">
  <img width="799" height="197" alt="Scenario 9" src="https://github.com/user-attachments/assets/d7a92095-42e6-4e72-9310-488c955931e1" />
</p>

---

## 🧪 Example usage

1. Launch the app.
2. Type some text, wait 1–2 seconds to allow auto-snapshot, then continue editing.
3. Click **Undo** or press `Ctrl+Z` to step back to previous snapshots.
4. Double-click any item in the history list to restore that specific snapshot.

---

## 🔧 Production Notes & Extensions

* Store snapshots as diffs (deltas) or compress snapshots for very large docs to reduce memory.
* Persist snapshots to disk or cloud for session recovery.
* Make `HistoryManager` thread-safe and add pruning policies (LRU, time-based).
* Add visual diff preview when hovering a snapshot in history.

---

## 📮 Contribution

Fork, add features (persistence, diff-snapshots, collaborative undo), and open PRs. Include tests for undo/redo correctness.

---

## 📝 License

MIT — reuse and adapt freely.

---

Made with care — Tharindu's Memento Text Editor demo. Happy editing! 🎉

