# Contributing to Glosso Studio

Thanks for helping. Issues and merge requests go to the main repository on GitLab:
https://gitlab.com/shirobyte421/glosso-studio. The GitHub repository is a read-only mirror.

## Before you open a merge request

- Build and lint the app: `./gradlew :androidApp:assembleFdroidDebug :androidApp:lintFdroidDebug`
- Run the unit tests: `./gradlew :androidApp:testFdroidDebugUnitTest`
- Keep changes focused: one fix or feature per merge request.
- Commit messages follow `type(scope): description`, for example `fix(audio): stop the chime from clipping`.

## Things to know

- User progress lives in a Room database that must never be wiped on upgrade. A schema change needs a
  real `Migration` and the exported schema under `androidApp/schemas/`.
- The scoring design (separate accuracy and completeness, mastery as a gate) is deliberate.
  See the "Not findings" section of `docs/BACKLOG.md` before changing it.
- The model and sentence databases are downloaded at runtime, not committed. Do not add large binaries to git.

## License

By contributing you agree that your contribution is licensed under the AGPL-3.0, like the rest of the project.
