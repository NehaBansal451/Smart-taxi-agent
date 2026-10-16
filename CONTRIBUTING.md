# Contributing to Smart Taxi Agent

## Golden rules

1. Never push directly to `main`. All changes go through a pull request.
2. One task = one GitHub issue = one branch = one pull request.
3. Never commit passwords, tokens, `.env` files or local database files.
4. Tell the team before changing a shared class or interface.

## Branch names

- `feature/short-description` for new features, e.g. `feature/q-learning-agent`
- `fix/short-description` for bug fixes, e.g. `fix/wall-collision`
- `docs/short-description` for documentation

## Commit messages

Use a short prefix and a clear sentence:

- `feat: add QTable class`
- `fix: stop taxi leaving the grid`
- `docs: update setup instructions`
- `test: add GridWorld wall tests`

## Workflow

1. Pick an issue and tell the team you are working on it.
2. Update your local main: `git checkout main` then `git pull`.
3. Create a branch: `git checkout -b feature/your-task`.
4. Make small commits as you work.
5. Push your branch: `git push -u origin feature/your-task`.
6. Open a pull request on GitHub and link the issue (write `Closes #12`).
7. A teammate reviews it. Fix any comments.
8. Merge only when the code compiles, tests pass and a teammate approved.

## Shared files

Ask before editing files owned by someone else. Shared interfaces
(Environment, Agent, DAO interfaces) change only after team agreement.
