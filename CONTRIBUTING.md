# Contributing

Thanks for taking the time to contribute! This document outlines how to report issues, submit changes, and validate your work.

## Opening Issues

- Search existing issues before opening a new one to avoid duplicates.
- Provide clear reproduction steps, expected behavior, and actual behavior.
- Include version details, logs, and minimal repro code when possible.

## Submitting Pull Requests

- Fork the repository and create a feature branch from the default branch.
- Keep PRs focused and scoped to a single change.
- Link related issues in the PR description.
- Update or add tests when behavior changes.
- Ensure documentation is updated when relevant.

## Coding Style

- Follow standard Java conventions and keep code readable.
- Use consistent naming for classes, methods, and variables.
- Keep methods small and single-purpose when possible.
- Favor clarity over cleverness; prefer straightforward implementations.

## Tests

- Run the test suite locally before submitting a PR:

  ```sh
  mvn test
  ```

- If you add new functionality, include unit tests covering the new behavior.
- If a test fails in CI, please investigate and update your PR accordingly.
