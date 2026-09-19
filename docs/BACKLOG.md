# Backlog — review findings

Findings from a full read of the source, tests, Gradle config and CI (2026-09-18).
None of them touch the domain logic — the scoring pipeline and the spaced-repetition
loop are in good shape. Everything below sits at the **edges**: release plumbing,
asset versioning, and coupling to repos that are not in this one.

Ordered by (impact × cheapness). Each entry is written so it can become a task
without re-deriving the context.

---

## P1 — `AGENTS.md` contradicts the build

**Where:** `AGENTS.md` ("Version: `versionCode = 2209`, `versionName = "2.2.8"`")
**Reality:** `androidApp/build.gradle.kts` → `versionCode = 2212`, `baseVersionName = "2.3.2"`.

That file exists specifically to orient a reader (human or agent) before it touches
code, so a stale version is a trap set for its only reader. AGENTS.md also points at
a `data/build/topics.py` (see below) and carries a stale `UpdateUserProgressUseCase`
reference that `GEMINI.md` already flags.

**Fix:** bump the version line, drop or re-point the dangling references, and make
version bumps touch both files (`baseVersionName` + `AGENTS.md`) in one commit.

---

## P1 — CI may be publishing the wrong model assets

**Where:** `.gitlab-ci.yml` publishes `allosaurus_eng2102.onnx`, `phone_eng.txt` and
`sentences_0..5.db`; `README.md`/`AGENTS.md`/code expect
`wav2vec2_espeak_cv_ft_int8.onnx` and `espeak_vocab.json`.

`MigrationRunner.kt` still mentions allosaurus, so the CI names may simply be
historical leftovers — but `DatabaseDownloader` builds its URLs from the GitLab
generic package registry, so a stale CI job means **releases download the wrong
model**. This needs checking against the real GitLab project (the mirror can't tell
us which job actually last ran).

**Fix:** reconcile the CI asset names with what the app requests; if allosaurus is
genuinely retired, delete the last mentions of it.

---

## P2 — Asset version is pinned behind the app version

**Where:** `AppModule` passes `dataVersion = "v2.2.6"` while `baseVersionName` is
`2.3.2`.

Every release therefore pulls assets from the v2.2.6 package. That may be
deliberate (the sentence DBs don't change every release), but app and assets now
diverge silently — nothing warns when they drift, and nothing forces a deliberate
decision to keep them pinned.

**Fix:** either make the asset version an explicit, commented constant that says "do
not bump unless data changes", or derive/validate it at release time.

---

## P2 — `*.aab` is not tracked by LFS

**Where:** `.gitattributes` covers `*.apk` only, while `apks/` holds three `.aab`
files of ~43 MB each.

They are stored as ordinary blobs, so every clone pays for them forever and every
future release adds ~43 MB permanently to history — the APKs were fixed, the
App Bundles were missed.

**Fix:** add `*.aab filter=lfs diff=lfs merge=lfs -text`, `git lfs migrate import
--include="*.aab"` (rewrites history — coordinate before pushing), or stop
committing build outputs entirely and keep them as CI artifacts.

---

## P2 — `TopicOrder` is coupled to a file outside this repo

**Where:** `TopicOrder` carries a "keep in sync with `data/build/topics.py`" warning;
`data/build/topics.py` is not in this repo.

The canonical CEFR topic progression is asserted twice, in two repos, with a
comment as the only link. That is exactly the shape a stale-topic bug takes: the
generator renames a topic, the app keeps the old order, and nothing fails.

**Fix:** pick one owner — move `topics.py` (or a shared JSON) into this repo, or add
a test that asserts the generated sentence DBs contain exactly the topics
`TopicOrder` declares.

---

## P3 — Dead field: `LanguageConfig.downloadRepo`

**Where:** `shared/.../domain/model/LanguageConfig.kt:18` —
`val downloadRepo: String = "shirobyte421/glosso-studio"`, declared and never read
anywhere in the codebase.

Leftover from a HuggingFace-era asset path. Dead config is worse than no config: it
looks authoritative and invites new callers to trust it.

**Fix:** delete it. If it's kept for a planned migration, document that.

---

## P3 — `analyzeSpeech` crashes rather than failing to compile

**Where:** `GlossoRepositoryImpl.analyzeSpeech` throws
`UnsupportedOperationException("Cloud analysis is disabled")`.

It is declared on the interface, so any future implementation or caller gets a
runtime crash at the user, rather than a compile error at the desk. The offline-only
decision is right; its expression is not.

**Fix:** remove `analyzeSpeech` from the interface (and the implementations), or
return a sealed `Unsupported` result so callers must handle it.

---

## Not findings (recorded so nobody re-litigates them)

- **`accuracy` vs `completeness` split, and mastery-as-gate** (`ScoringConfig`,
  `UpdateMasteryUseCase`) — deliberate. A single averaged score dilutes as sentences
  lengthen, so a long sentence with one destroyed word used to outscore a short one
  with one wrong sound. `UpdateMasteryUseCase` deliberately refuses to re-derive
  mastery from the score. **Do not undo either.**
- **Destructive Room fallback scoped to `fallbackToDestructiveMigrationFrom(1..10)`** —
  user progress can never be silently wiped, while re-downloadable level DBs keep
  full destructive fallback. Correct split; keep it.
- **`OutOfMemoryError` catch in the recognizer** — an `Error`, not an `Exception`;
  deliberate, degrades to "scoring off" instead of killing the process.
