# 0001. Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-10-29

## Context

Clientele is changing in planned steps: error handling, validation, security configuration, roles, pagination, a dashboard and API documentation. Each step involves choices that are hard to understand later from the code alone, such as why one approach was preferred over another.

## Decision

Record each significant architectural decision as an Architecture Decision Record (ADR) in `docs/adr/`.

- Files are numbered and named `NNNN-kebab-title.md`, starting from [`0000-template.md`](0000-template.md).
- Each ADR has the sections Context, Decision, Consequences and Alternatives considered.
- ADRs are written in the same pull request as the change they describe.
- An accepted ADR is not rewritten. If a decision changes, a new ADR supersedes it.

## Consequences

- Reviewers and new contributors can see why the system looks the way it does.
- Writing an ADR adds a small amount of work to each significant change.

## Alternatives considered

- **Comments in code:** good for local reasoning, but they do not capture alternatives or cross-cutting decisions.
- **A wiki:** lives outside the repository and drifts away from the code it describes.
