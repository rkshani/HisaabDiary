# PLAN.md — Hisaab Diary: phase-by-phase build plan

Follow `AGENTS.md` for the Git workflow and rules. Do the steps **in order**. One step = one branch = one PR.
Branch names: `phase-<N>/<step>-<short-name>`.
⚠️ = ask the user before you start this step.

---

## Phase 0 — Project setup

**0.1 `phase-0/0.1-git-init`**
- `git init -b main` if needed, first commit with the starter files.
- ⚠️ Ask for the GitHub repo URL (or permission to create it with `gh repo create hisaab-diary --private`).
- ⚠️ Ask the merge mode (A auto-merge / B user review). See AGENTS.md §2.

**0.2 `phase-0/0.2-gradle-setup`**
- Add the Gradle Kotlin DSL build: `settings.gradle.kts`, root `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`, the Gradle wrapper, and `gradle/libs.versions.toml` (version catalog).
- Use the latest stable AGP, Kotlin 2.x, the Compose compiler Gradle plugin, the Compose BOM, KSP, and Material 3.
- `namespace = "com.hisaab.diary"`, `applicationId = "com.hisaab.diary"`, `minSdk 24`, Java/JVM 17.
- Done when the Hello World app runs: `./gradlew assembleDebug` passes.

**0.3 `phase-0/0.3-ci`**
- GitHub Actions workflow `.github/workflows/android.yml`: on push/PR, run build + unit tests + lint.

**0.4 `phase-0/0.4-code-quality`**
- Add ktlint (or spotless) and detekt with a sensible baseline. Add `.editorconfig`.

---

## Phase 1 — App skeleton

**1.1 `phase-1/1.1-packages-hilt`**
- Create the package structure from AGENTS.md §4.
- Add Hilt: `HisaabApp : Application` with `@HiltAndroidApp`, and `@AndroidEntryPoint` on `MainActivity`.

**1.2 `phase-1/1.2-theme`**
- Material 3 theme: light + dark, dynamic color on Android 12+ with a brand fallback.
- Brand colors: green for "Mujhe milenge / You will get", red for "Mujhe dene hain / You will give". ⚠️ Confirm the main brand color with the user.
- Typography that supports Urdu well (e.g. Noto Nastaliq Urdu or Noto Sans Arabic for Urdu). ⚠️ Ask which Urdu font style.

**1.3 `phase-1/1.3-navigation`**
- Navigation Compose with type-safe routes. Empty screens for now: Home, PartyDetail, AddEntry, Reports, Settings.
- Bottom bar or top-bar actions. ⚠️ Ask: bottom navigation (Home / Reports / Settings) or a single screen with menu?

**1.4 `phase-1/1.4-localization`**
- `values/strings.xml` (English) and `values-ur/strings.xml` (Urdu). Per-app language support (`AppCompatDelegate` / `LocaleManager`, `locales_config.xml`).
- Check RTL layout in Urdu.

**1.5 `phase-1/1.5-core-utils`**
- `Money` value class (paisa `Long`), formatter → `Rs 1,250` / `₨ 1,250.50`, a parser from user input, and tests.
- Date utils (today, start/end of day/month, formatter), and tests.

---

## Phase 2 — Database and domain layer

**2.1 `phase-2/2.1-room-entities`**
- Room database `HisaabDatabase` with `exportSchema = true` (schemas folder committed).
- Entities:
  - `BookEntity` (id, name, createdAt) — e.g. "Personal", "Shop"
  - `PartyEntity` (id, bookId, name, phone?, note?, createdAt, isArchived)
  - `TransactionEntity` (id, partyId, type = GAVE/GOT, amountPaisa: Long, note?, date, dueDate?, attachmentPath?, createdAt, updatedAt)
  - `ReminderEntity` (id, partyId, remindAt, isDone)
- Indices on foreign keys, and `ON DELETE CASCADE` where it makes sense.
- ⚠️ Show the schema to the user and get an OK before you merge.

**2.2 `phase-2/2.2-daos`**
- DAOs with Flow queries: party list with computed balance (SUM GOT − SUM GAVE), a party ledger with a running balance, totals per book, and search by name/phone.
- DAO tests.

**2.3 `phase-2/2.3-domain-repos`**
- Domain models, repository interfaces in `domain`, implementations in `data`, Hilt bindings.

**2.4 `phase-2/2.4-use-cases`**
- Use cases: AddParty, UpdateParty, DeleteParty, ObserveParties, AddTransaction, UpdateTransaction, DeleteTransaction (with undo support), ObserveLedger, ObserveBookTotals. Unit tests for all.

**2.5 `phase-2/2.5-default-data`**
- On the first launch, create a default "Personal" book. Settings in DataStore (selected book, language, theme, etc.).

---

## Phase 3 — Home screen (party list)

**3.1 `phase-3/3.1-home-summary`**
- Top summary card: **You will get** (green), **You will give** (red), net balance.

**3.2 `phase-3/3.2-party-list`**
- List of parties: name, last entry date, balance with color, and an avatar with initials.
- Empty state with a friendly illustration/text and an "Add first person" button.

**3.3 `phase-3/3.3-search-sort-filter`**
- Search by name/phone. Sort: recent, name, highest amount. Filter: all / will get / will give / settled.

**3.4 `phase-3/3.4-add-edit-party`**
- Add/edit party sheet: name (required), phone (optional), note.
- "Pick from contacts" uses the contact picker intent (no READ_CONTACTS permission if possible). ⚠️ Confirm with the user.
- Archive/delete a party, with a confirm step.

---

## Phase 4 — Party ledger (khata detail)

**4.1 `phase-4/4.1-ledger-screen`**
- Party header: name, phone, current balance, and quick buttons: **Diye (Gave)** / **Liye (Got)**.
- Transaction list grouped by date, with a running balance column.

**4.2 `phase-4/4.2-add-entry`**
- Add entry screen: big amount field with a built-in mini calculator (+ − × ÷), note, date picker (default today), optional due date.
- Validation: amount > 0.

**4.3 `phase-4/4.3-edit-delete-entry`**
- Tap to edit. Delete with an Undo snackbar. ⚠️ Ask if a confirm dialog is also wanted.

**4.4 `phase-4/4.4-bill-photo`**
- Attach a bill photo (camera or gallery via Photo Picker). Save a compressed copy in app internal storage. Show a thumbnail and a full-screen view. Delete the file when the entry is deleted.

**4.5 `phase-4/4.5-settle-up`**
- "Settle up" button: adds an entry that makes the balance zero, after a confirm.

---

## Phase 5 — Reminders and sharing

**5.1 `phase-5/5.1-share-reminder`**
- "Send reminder" button: builds a polite message in English or Urdu with the name, amount and app name, and opens WhatsApp (if installed) or the share sheet / SMS. No internet is needed; it uses intents only.
- ⚠️ Show the message template to the user for approval.

**5.2 `phase-5/5.2-due-date-notifications`**
- WorkManager job: local notification on the due date ("Ali se Rs 2,000 lene hain — aaj due hai").
- Ask for POST_NOTIFICATIONS on Android 13+ at the right moment (not on the first launch).

**5.3 `phase-5/5.3-reminder-list`**
- Screen with upcoming and overdue dues. Mark as done, snooze.

---

## Phase 6 — Reports and export

**6.1 `phase-6/6.1-reports-screen`**
- Date range filter (today, this week, this month, custom). Totals gave/got, and a list of entries.
- A simple chart (Compose Canvas, no heavy library). ⚠️ Ask if a chart is wanted.

**6.2 `phase-6/6.2-party-statement-pdf`**
- Party statement PDF using Android `PdfDocument`: header, party info, table with running balance, totals, footer "Made with Hisaab Diary". Urdu text must render correctly.
- Share via FileProvider.

**6.3 `phase-6/6.3-csv-export`**
- Export all entries (or one book) to CSV through the Storage Access Framework.

---

## Phase 7 — Multiple books

**7.1 `phase-7/7.1-books`**
- Create, rename and delete books (Personal, Shop, Ghar…). A book switcher on Home. All screens are scoped to the selected book.

---

## Phase 8 — Security and privacy

**8.1 `phase-8/8.1-app-lock`**
- Optional app lock: 4-digit PIN (stored hashed with a salt) + biometric (`androidx.biometric`). Lock when the app goes to the background after X seconds. ⚠️ Ask for the default timeout.

**8.2 `phase-8/8.2-privacy-mode`**
- "Hide amounts" toggle (shows ****). `FLAG_SECURE` option to block screenshots.

---

## Phase 9 — Backup and restore (offline)

**9.1 `phase-9/9.1-backup`**
- Export a backup file `.hisaab` (zip of the Room DB + bill photos + a metadata JSON with a schema version). Optional password encryption (AES-GCM, key from PBKDF2).
- Save with SAF, so the user can save it to Downloads, a USB drive, or (by their own choice) Google Drive.

**9.2 `phase-9/9.2-restore`**
- Pick a `.hisaab` file, check the version, show a confirm ("this will replace current data"), restore, then restart the app safely.

**9.3 `phase-9/9.3-backup-reminder`**
- A local reminder every X days: "You have not taken a backup for 7 days". ⚠️ Ask for the default.

---

## Phase 10 — Settings, onboarding, polish

**10.1 `phase-10/10.1-settings`**
- Language, theme (system/light/dark), currency symbol format, app lock, backup, about, privacy policy, rate app (Play Store intent only).

**10.2 `phase-10/10.2-onboarding`**
- 3 short intro screens (offline and private, easy khata, reminders). Pick the language on the first screen.

**10.3 `phase-10/10.3-ui-polish`**
- Animations (list item placement, FAB), haptics on save, good empty/loading states, edge-to-edge, predictive back.

**10.4 `phase-10/10.4-accessibility`**
- Content descriptions, touch targets of at least 48dp, font scaling up to 200%, TalkBack check.

---

## Phase 11 — Quality and performance

**11.1 `phase-11/11.1-paging`**
- Paging 3 for long ledgers and party lists. Test with 10,000 fake entries (debug-only seed button).

**11.2 `phase-11/11.2-tests`**
- Fill test gaps: ViewModels, use cases, DAOs, and a few Compose UI tests for the main flows (add party → add entry → see balance).

**11.3 `phase-11/11.3-release-build`**
- R8/minify on, ProGuard rules for Room/Hilt, a baseline profile, and an app size check.

---

## Phase 12 — Release

**12.1 `phase-12/12.1-icon-splash`**
- Adaptive app icon + monochrome icon, and a SplashScreen API splash. ⚠️ Ask the user for the logo, or propose 2–3 simple designs.

**12.2 `phase-12/12.2-signing-versioning`**
- Signing config read from `keystore.properties` (never committed). versionCode/versionName scheme. A build of a release AAB.
- ⚠️ The user creates the keystore themselves. Give them the exact command.

**12.3 `phase-12/12.3-store-listing`**
- `docs/store/`: Play Store title ("Hisaab Diary: Udhaar Khata Book"), short and full descriptions (English + Urdu), keyword ideas, a screenshot list, a privacy policy (no data collected, fully offline), and Data Safety form answers.

**12.4 `phase-12/12.4-release-v1`**
- Final check against the AGENTS.md Definition of Done, a `CHANGELOG.md`, and a git tag `v1.0.0`.

---

## Later ideas (do NOT build unless the user asks)
- Home-screen widget (total you will get/give)
- Partial payment tracking against one udhaar entry
- Interest/profit calculation
- Import from other khata apps (CSV)
- Wear OS quick entry
