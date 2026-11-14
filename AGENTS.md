# AGENTS.md — Rules for the AI coding agent (Codex)

You are building **Hisaab Diary** (`com.hisaab.diary`), a fully offline udhaar / lena-dena khata app for Android.
Read this file fully before you start. Then follow `PLAN.md` step by step, in order.

---

## 1. Ask the user when something is not clear

- If anything is unclear, missing, or has more than one good option, **STOP and ask the user** before you write code.
- Ask short, simple questions. Give 2–4 options and mark your recommended one, e.g.:
  > "Step 4.3: should deleting a transaction need a confirm dialog or only an Undo snackbar? (A) Undo snackbar (recommended) (B) Confirm dialog (C) Both"
- Always ask (never guess) about:
  - GitHub repo URL / remote, if `git remote -v` is empty
  - Anything that changes the database schema after Phase 2 is merged
  - Adding any new library not listed in `PLAN.md`
  - UI text, colors, or the logo, if the plan does not define them
  - Anything that needs a permission (contacts, camera, notifications, biometric)
- The user's English is simple. Use simple words and short sentences.

## 2. Git and GitHub workflow (for EVERY step)

Each step in `PLAN.md` has a branch name. For each step:

1. Make sure the working tree is clean: `git status`
2. Update the base: `git checkout main && git pull origin main`
3. Create the step branch: `git checkout -b <branch-name-from-plan>`
4. Write the code for that step only. Keep it small and focused.
5. Build and test (from Phase 0.2 onward):
   - `./gradlew assembleDebug`
   - `./gradlew testDebugUnitTest`
   - `./gradlew lint` (from Phase 0.4 onward, also `./gradlew ktlintCheck detekt`)
   - Fix every error before you commit. Never push a broken build.
6. Commit with Conventional Commits, e.g. `feat(ledger): add gave/got entry screen`
7. Push: `git push -u origin <branch-name>`
8. Open a Pull Request to `main` with `gh pr create` if `gh` is available. Put the step number, a short summary, and a "How to test" list in the PR.
9. **Merge mode** (ask the user ONCE at the very start, then remember it):
   - **Mode A (recommended): auto-merge.** After the build is green, merge the PR into `main` (`gh pr merge --squash --delete-branch=false`), so the next step starts from the updated `main`.
   - **Mode B: user review.** Do not merge. Stack the next branch on the previous step branch, and tell the user which PRs are waiting.
10. After each step, give a short report: what was done, the branch name, the PR link, and anything the user must check by hand.

First-time setup (Phase 0.1 only): if the folder is not a git repo, run `git init -b main`, make the first commit, and ask the user for the GitHub repo URL (or ask permission to create it with `gh repo create`).

**Never commit:** keystores (`*.jks`, `*.keystore`), `keystore.properties`, `local.properties`, build outputs.

## 3. Hard rules for the product

- **100% offline.** Never add the `INTERNET` permission. No analytics, no crash SDKs, no ads SDK, no Firebase, no network calls. If a feature seems to need the internet, ask the user.
- **Money is stored as `Long` in paisa** (1 Rs = 100 paisa). Never use `Float` or `Double` for money.
- **Dates** are stored as epoch millis (`Long`). Format them only in the UI.
- **Two languages:** English and Urdu. All UI text goes in `strings.xml` (`values/` and `values-ur/`). No hard-coded strings in Compose. Layouts must work in RTL.
- **Light and dark theme** both supported.
- `minSdk 24`, `targetSdk` / `compileSdk` = latest stable. Check the current stable versions before Phase 0.2; don't guess them.

## 4. Architecture and code style

- Single `app` module with clean packages (multi-module only if the user asks):
  ```
  com.hisaab.diary
  ├── core/          (common utils, money formatting, date utils, result types)
  ├── data/          (Room db, entities, DAOs, repository implementations, file storage)
  ├── domain/        (models, repository interfaces, use cases — pure Kotlin, no Android imports)
  ├── ui/
  │   ├── theme/
  │   ├── navigation/
  │   ├── components/ (shared composables)
  │   └── feature/<name>/ (Screen, ViewModel, UiState, UiEvent)
  └── di/            (Hilt modules)
  ```
- MVVM + unidirectional data flow: `ViewModel` exposes `StateFlow<UiState>`; the UI sends events; one-time effects go through a `Channel`/`SharedFlow`.
- Use Hilt for DI, Room + KSP, Coroutines + Flow, Navigation Compose (type-safe routes), DataStore for settings, WorkManager for reminders.
- No business logic in composables. No Android classes in `domain`.
- Write unit tests for every use case, repository and ViewModel you add. Write DAO tests (instrumented or Robolectric).
- Keep functions small. Use clear names. Add KDoc only where the "why" is not obvious.

## 5. Definition of done (for every step)

- [ ] Builds with no errors or new warnings
- [ ] Tests pass, and new logic has tests
- [ ] No hard-coded strings; English + Urdu strings added
- [ ] Works in light/dark and LTR/RTL
- [ ] Branch pushed, PR opened (and merged in Mode A)
- [ ] Short report given to the user
