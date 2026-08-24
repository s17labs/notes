# AGENTS.md

Guidance for AI coding agents (OpenCode, Claude Code, etc.) working in this repository.

## Project Overview

Notes is a simple Android notes app with markdown-style formatting, pinning, trash/restore, search
and sort, a kanban board view, and JSON backup import/export.

- Language/stack: Java 8 (View-based UI — AppCompat + Material Components, no Compose, no Kotlin), raw SQLite via `SQLiteOpenHelper`
- Package/module: `com.s17labs.notesapp`
- Toolchain: minSdk 24, compile/target SDK 30, AGP 4.1.3, Gradle wrapper 6.7.1 (Groovy DSL), built on JDK 11 (per release workflow), `sourceCompatibility`/`targetCompatibility` 1.8
- Author/maintainer: yungsamd17 (https://github.com/yungsamd17)

## Build & Verify

```bash
chmod +x gradlew && ./gradlew assembleDebug   # build debug APK (what CI/release runs)
./gradlew testDebugUnitTest                   # unit tests (placeholder ExampleUnitTest sourceset)
./gradlew lint                                # Android lint (not part of any workflow)
```

- CI (`.github/workflows/ci.yml`) runs `assembleDebug` (JDK 11) on every push to `main` and every PR,
  uploading the debug APK as an artifact; the `build` check is required by branch protection.
  `.github/workflows/android.yml` remains manual (`workflow_dispatch`, debug or release input): it
  builds a debug APK with JDK 11; the "release" input additionally tags `v<versionName>` and
  publishes the APK to GitHub Releases with generated notes.
- Local sandboxes often lack the Android SDK/JDK or other toolchains — if builds can't run locally,
  rely on careful code review and let CI verify. Never skip updating tests when changing shared interfaces.

## Architecture

```
app/src/main/java/com/s17labs/notesapp/
  MainActivity.java          # note list, sidebar drawer, search/sort/trash views
  NoteActivity.java          # note editor: formatting toolbar, 2s debounced auto-save
  NotePreviewActivity.java   # rendered preview of a note's markdown formatting
  NoteAdapter.java           # RecyclerView adapter for the note list
  BoardActivity.java         # kanban board view with three fixed columns
  BoardColumnAdapter.java    # RecyclerView adapter for board columns
  BoardItemAdapter.java      # RecyclerView adapter for items inside a board column
  BoardItemTouchHelper.java  # drag-and-drop callbacks for board cards
  BoardUtils.java            # board helpers incl. JSON backup import/export of boards
  NoteDbHelper.java          # SQLiteOpenHelper: notes.db ("notes" + "board_items" tables)
                             # plus settings helpers (SharedPreferences "NotesAppPrefs")
  SettingsActivity.java      # settings screen (e.g. auto-save toggle)
  AboutActivity.java         # about screen
```

Key patterns:

- Storage is raw SQLite in one helper class (`NoteDbHelper`): the `notes` table uses soft-delete
  (`deleted` flag = trash) plus flags for `pinned`, `color`, `column_id`, `completed`,
  `board_enabled`; board cards live in `board_items`. Schema changes must update both
  `onCreate` and the `onUpgrade` migration path.
- Settings persist in SharedPreferences `"NotesAppPrefs"` (e.g. `auto_save`) and board UI state in
  `"board_prefs"` — access them through the existing helpers rather than new preference files.
- Auto-save is debounced ~2000 ms via a `Handler` in `NoteActivity`; respect the user's auto-save
  setting from prefs before scheduling saves.
- Backup/restore is JSON export/import (including boards via `BoardUtils`) — keep the export format
  backward compatible so older backups keep importing.
- RecyclerView adapters recycle views; attach/change listeners inside `onBindViewHolder` carefully —
  a recycled-checkbox listener crash here was fixed before and is easy to reintroduce.

## UI Conventions

- Clean dark theme defined by XML styles/colors; screens use classic XML layouts with Material
  Components, RecyclerViews and CardViews (no Compose).
- Confirmations and prompts reuse dedicated dialog layouts (`dialog_delete.xml`,
  `dialog_unsaved.xml`, ...) — follow that pattern instead of ad-hoc `AlertDialog`s.

## Commit Messages

Format: `type(scope): short imperative summary` — lowercase after type, no trailing period.
Keep commits atomic — one logical change per commit.

| Type | Use for |
|---|---|
| `feat` | new user-facing feature |
| `fix` | bug fix |
| `refactor` | code change that neither fixes nor adds behavior |
| `style` | formatting/UI polish without logic change |
| `test` | adding or fixing tests |
| `docs` | documentation only |
| `chore` | build, deps, CI, tooling |
| `release` | version bump / release tagging |

Scope is a short area name for this project (e.g. `app`, `notes`, `board`, `db`, `ci`).
Use plain `type:` only when a change genuinely spans everything (rare).

Examples:

```
feat(board): add column colors
fix(notes): keep pinned notes on top after edit
chore(ci): bump jdk in release workflow
docs(readme): document commit message types
release: v1.0.6
```

## Agent Guardrails

- Never commit or push directly to `main`; all changes land through pull requests.
- Never open a PR unless the developer explicitly asks for it.
- One concern per change. If the description says "also", split it into another branch/PR.
- Do not commit secrets, keystores, or local-only files (e.g. `.and-code/`).
- Never modify the JSON backup format in a breaking way; users restore real data from it.
- When watching CI/bot feedback on your PRs: poll checks and comments newer than the last push,
  verify each bot finding against the source before "fixing" it, dismiss false positives with a
  written reason, and stop when checks are green on the latest commit.

## Pull Requests

All changes land on `main` through pull requests.

1. Create a branch off `main`: `<type>/<short-description>` (e.g. `feat/board-archive`, `fix/trash-restore`).
2. Commit there using the format from **Commit Messages**; keep commits atomic.
3. Push the branch and open a PR against `main`.

PR rules:

- One feature/fix per PR — small and focused beats large and thorough.
- Title follows the commit message format: `type(scope): short imperative summary`
  (e.g. `feat(board): add column colors`) — it becomes the squash-merge commit message.
- Body stays concise, following the PR template: what changed and why, bullet list of touched areas,
  evidence if applicable, testing checklist (tick before merge).
- UI changes must include clear before/after screenshots; motion/timing changes need a short video.
  Upload evidence directly to GitHub — never commit PR-only screenshots or asset files.
- End the body with an AI attribution line stating exactly which model and agent made the changes,
  in this exact format:

  ```
  Built with {model} in the {agent} harness.
  ```

  Example: `Built with ox-alpha in the OpenCode harness.`

- Do **not** put AI attribution in GitHub Release notes — releases stay clean.
- CI must pass before merging. (No automatic CI exists here — trigger `android.yml` manually if asked.)

## Releases

1. Ensure `versionName` / `versionCode` are correct in `app/build.gradle` (Groovy DSL).
2. Run the "Build and Release" workflow (`android.yml`) manually with release type "release":
   it tags `v<versionName>` and publishes the APK to GitHub Releases with generated notes.

## Gotchas

- The toolchain is legacy (AGP 4.1.3, Gradle 6.7.1, JDK 11, Java 8 sources) — do not bump versions
  or introduce Kotlin opportunistically; match the existing style.
- `compileSdk 30` means newer androidx APIs may be unavailable; check dependency versions before using them.
