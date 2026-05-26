# §8 · GitHub & Repository Setup

> **Stance:** the repo is the operating manual. A new contributor should be productive on Day 1 from `README.md` + `CONTRIBUTING.md` alone, without a Slack walkthrough.

## 8.1 Repository structure (top level)

```
cocoshowroom/
├── .github/
│   ├── workflows/                      # CI/CD
│   │   ├── ci.yml                      # lint + type + unit + a11y (every PR)
│   │   ├── e2e.yml                     # Playwright E2E (every PR, sharded)
│   │   ├── lighthouse.yml              # Lighthouse CI vs preview deploy
│   │   ├── bundle-size.yml             # size-limit + bundle delta comment
│   │   ├── deploy-preview.yml          # auto on PR open/update
│   │   ├── deploy-production.yml       # on main push, gated
│   │   ├── codeql.yml                  # weekly + on PR
│   │   ├── dependency-review.yml       # PR-time
│   │   └── release.yml                 # changesets → tag → release notes
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug.yml
│   │   ├── feature.yml
│   │   └── perf-regression.yml
│   ├── PULL_REQUEST_TEMPLATE.md
│   ├── CODEOWNERS
│   ├── dependabot.yml
│   └── renovate.json                   # (pick one — see §8.13)
├── docs/
│   ├── ADR/                            # Architecture Decision Records
│   │   ├── 0001-app-router.md
│   │   ├── 0002-zustand-over-redux.md
│   │   └── _template.md
│   └── PRD/                            # this document
├── apps/web/                           # see §2
├── packages/                           # reserved
├── .editorconfig
├── .gitignore
├── .gitattributes                      # LF endings, lockfile binary mode
├── .nvmrc                              # 20.18.0
├── .npmrc                              # engine-strict=true, public-hoist-pattern=...
├── pnpm-workspace.yaml
├── package.json                        # workspace root
├── CONTRIBUTING.md
├── SECURITY.md
├── LICENSE                             # private — proprietary
└── README.md
```

## 8.2 Branch strategy

A two-branch flow, optimized for solo / small team velocity without losing safety:

- `main` — production. Only deployment-tagged commits. Protected.
- `dev` — integration. PRs target here. Auto-deploys to staging.
- `feature/<short-name>` — one-feature-per-branch, deleted on merge.
- `fix/<short-name>`, `chore/<short-name>`, `docs/<short-name>` — same shape.
- `release/<version>` — short-lived; cuts from `dev`, merges to `main`.

`main` and `dev` are **protected**:

- Require PR with ≥ 1 approving review (CODEOWNERS).
- Require all CI checks passing (lint, type, test, a11y, lighthouse, bundle).
- Require linear history (squash merges).
- Disallow force-push and deletion.
- Require signed commits.

## 8.3 Commit conventions

**Conventional Commits**, enforced by `commitlint` in Husky `commit-msg` hook:

```
<type>(<scope>): <subject>

[optional body]

[optional footer(s)]
```

Types: `feat`, `fix`, `perf`, `refactor`, `style`, `test`, `docs`, `build`, `ci`, `chore`, `revert`.

Scopes match feature folders (`cart`, `catalog`, `checkout`, `theme`, `seo`, `a11y`, `ci`, ...).

Examples:

```
feat(catalog): add category filter via URL state
perf(images): preload PDP hero, switch to AVIF
fix(theme): prevent flash on first paint when cookie set
docs(prd): finalize §15 implementation plan
```

The first line is the commit subject and the only thing visible in `git log --oneline`. Make it count.

## 8.4 PR conventions

- Title follows Conventional Commit shape.
- Description follows the PR template (problem, approach, screenshots/CWV deltas, risk).
- One feature per PR. PRs > 500 lines are flagged for split (a CI bot comments).
- Every PR has a Vercel preview URL.
- Every PR shows: bundle delta, Lighthouse delta, axe delta, perf-budget pass/fail.

### PR template (excerpt)

```md
## What

<!-- one-line product effect, e.g. "Adds /shop category filter URL state" -->

## Why

<!-- the user/business reason; link to issue -->

## How

<!-- key architectural choices; what to look for in review -->

## Verification

- [ ] CI green
- [ ] Lighthouse mobile no regression
- [ ] Bundle delta < +5 kB per route
- [ ] axe 0 serious/critical
- [ ] Manual: tested on Chromium + iOS Safari + Cốc Cốc
- [ ] Manual: VI + EN parity

## Screenshots / Recordings

<!-- before / after for visual changes; thrown-out for backend-only -->

## Risk

<!-- "low/medium/high" + what could break -->
```

## 8.5 CODEOWNERS

```
# .github/CODEOWNERS
* @cocoshowroom/frontend

# Critical paths require platform review
apps/web/src/app/**         @cocoshowroom/frontend-leads
apps/web/src/lib/auth/**    @cocoshowroom/platform
apps/web/src/lib/api/**     @cocoshowroom/platform
apps/web/next.config.mjs    @cocoshowroom/platform
.github/workflows/**        @cocoshowroom/platform
docs/PRD/**                 @cocoshowroom/leadership
```

CODEOWNERS reviews are **required** on `main`-targeting merges.

## 8.6 Issue templates

Three structured templates:

- **Bug** — repro steps, expected, actual, device/browser/locale.
- **Feature** — user story, success criteria, scope check, related PRD section.
- **Perf regression** — route, metric, before/after, suspect commit range.

Free-form blank template is **disabled**.

## 8.7 Labels

Curated, lint-checked. We start with this set:

- `type/`: `bug`, `feat`, `perf`, `chore`, `docs`, `security`, `a11y`
- `area/`: `cart`, `catalog`, `checkout`, `theme`, `i18n`, `seo`, `ci`, `infra`
- `priority/`: `p0`, `p1`, `p2`, `p3`
- `status/`: `triage`, `in-progress`, `blocked`, `needs-design`, `needs-product`
- `size/`: `xs`, `s`, `m`, `l`, `xl`

## 8.8 Release strategy

**Semantic Versioning** + **Changesets**:

- `feat` bumps minor; `fix`/`perf` bump patch; breaking changes bump major.
- A `.changeset/*.md` file accompanies any PR that's user-visible. CI fails on a `feat`/`fix` PR without a changeset.
- On `main` merge, the release workflow:
  1. Aggregates pending changesets,
  2. Bumps versions,
  3. Tags the commit (`v1.4.0`),
  4. Generates release notes,
  5. Deploys.

Pre-1.0 we follow `0.<minor>.<patch>` with the same rules.

## 8.9 GitHub Actions — CI/CD overview

| Workflow                | Trigger                      | Duration target      |
| ----------------------- | ---------------------------- | -------------------- |
| `ci.yml`                | PR open/sync                 | < 5 min              |
| `e2e.yml`               | PR open/sync                 | < 8 min (sharded ×4) |
| `lighthouse.yml`        | After preview deploy         | < 3 min              |
| `bundle-size.yml`       | PR open/sync                 | < 2 min              |
| `deploy-preview.yml`    | PR open/sync                 | < 2 min              |
| `deploy-production.yml` | `main` push (after CI green) | < 4 min              |
| `codeql.yml`            | PR + weekly cron             | < 6 min              |
| `dependency-review.yml` | PR                           | < 30 s               |
| `release.yml`           | `main` push                  | < 5 min              |

### `ci.yml` (shape)

```yaml
name: ci
on:
  pull_request:
  push: { branches: [dev, main] }
permissions: { contents: read }
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true
jobs:
  install:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
      - uses: actions/setup-node@v4
        with: { node-version-file: .nvmrc, cache: pnpm }
      - run: pnpm install --frozen-lockfile --ignore-scripts
      - uses: actions/cache@v4
        with:
          path: |
            .next/cache
            node_modules/.cache
          key: ${{ runner.os }}-build-${{ hashFiles('pnpm-lock.yaml') }}-${{ github.sha }}
  lint:
    needs: install
    steps: [..., pnpm lint, pnpm format:check]
  typecheck:
    needs: install
    steps: [..., pnpm typecheck]
  test:
    needs: install
    steps: [..., pnpm test --coverage]
  a11y:
    needs: install
    steps: [..., pnpm test:a11y]
  build:
    needs: [lint, typecheck, test]
    steps: [..., pnpm build]
```

All jobs run in parallel after `install`. Total wall time targets < 5 min on a 4-core runner.

## 8.10 Preview deployments

- Every PR → Vercel preview with a unique URL.
- Preview URL is commented back on the PR.
- Lighthouse, bundle, and visual regression run against the preview.
- Preview is set to `noindex`, `Referrer-Policy: no-referrer`.
- Preview password-protected when stakeholder review is sensitive.

## 8.11 Environment strategy

| Env         | URL                              | Auto-deploy from | Indexed | Sentry env |
| ----------- | -------------------------------- | ---------------- | ------- | ---------- |
| Dev (local) | `http://localhost:3000`          | n/a              | no      | dev        |
| Preview     | `cocoshowroom-pr-<n>.vercel.app` | every PR         | no      | preview    |
| Staging     | `staging.cocoshowroom.vn`        | `dev` branch     | no      | staging    |
| Production  | `cocoshowroom.vn`                | `main` branch    | yes     | production |

Staging is **isomorphic to production** (same CDN, same image opt, same headers) so perf measurements there are meaningful.

## 8.12 Secrets handling

- Vercel UI for env vars (encrypted at rest).
- GitHub Actions reads secrets via `secrets.NAME` — no fall-through to env.
- Local: `.env.local` is gitignored; `.env.example` documents the schema.
- Rotation: a runbook (`docs/runbooks/secret-rotation.md`) documents how to rotate each secret.
- Pre-commit hook scans for accidentally-committed secrets via `gitleaks`.

## 8.13 Dependency update strategy

**Renovate** (preferred over Dependabot for grouping):

```jsonc
// renovate.json
{
  "extends": ["config:recommended", ":automergeMinor"],
  "schedule": ["before 6am on Monday"],
  "labels": ["deps"],
  "packageRules": [
    {
      "matchUpdateTypes": ["minor", "patch"],
      "matchDepTypes": ["devDependencies"],
      "automerge": true,
    },
    {
      "matchPackagePatterns": ["^next$", "^react$", "^typescript$"],
      "schedule": ["before 6am on the first day of the month"],
      "minimumReleaseAge": "7 days",
    },
    {
      "matchDatasources": ["npm"],
      "matchUpdateTypes": ["major"],
      "automerge": false,
      "labels": ["deps", "major"],
    },
  ],
  "vulnerabilityAlerts": { "labels": ["security"], "automerge": true },
}
```

Major-version bumps require manual review + ADR if the API surface changed.

## 8.14 Monorepo considerations

We start as a workspace with **one app and a reserved `packages/`** folder. Triggers to extract a package:

- Two consumers exist (web + a future admin/mobile).
- Code is genuinely portable (no `next/*` imports).
- Tests are self-contained.

We anticipate: `packages/ui-tokens` (CSS variables + Figma JSON), `packages/eslint-config`, `packages/tsconfig-base`. Not extracted in v1.

## 8.15 README contract

The root `README.md` must include, in order:

1. One-line product description
2. Stack badges (Next version, Node version, TypeScript version)
3. Quick start (`pnpm install`, `pnpm dev`)
4. Scripts table
5. Environment variables (links to `.env.example` and `lib/env.ts`)
6. Architecture pointer to `docs/PRD/`
7. Contributing pointer to `CONTRIBUTING.md`
8. License + contact

Readme is enforced by CI: a missing section fails the docs check.

## 8.16 Security policy

`SECURITY.md` documents:

- Supported versions
- Reporting channel (`security@cocoshowroom.vn`, PGP key)
- Disclosure window (90 days)
- Bug bounty stance (none in v1, friendly acknowledgment)
