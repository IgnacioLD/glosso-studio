# Backlog — review findings

Findings from a full read of the source, tests, Gradle config and CI (2026-09-18).
None of them touch the domain logic — the scoring pipeline and the spaced-repetition
loop are in good shape. Everything below sits at the **edges**: release plumbing,
asset versioning, and coupling to repos that are not in this one.

Ordered by (impact × cheapness). Each entry is written so it can become a task
without re-deriving the context.

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
