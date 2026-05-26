# §9 · Development Workflow

## 9.1 Local development setup

```bash
# One-time
curl -fsSL https://get.pnpm.io/install.sh | sh
nvm install                  # honors .nvmrc
pnpm install --frozen-lockfile

# Daily
pnpm dev                     # localhost:3000, hot reload
pnpm dev:turbo               # Turbopack (experimental; opt-in)
```

Target: zero-config first run. After `pnpm install`, `pnpm dev` works without extra environment setup (apart from optional `.env.local` for analytics/sentry — defaults absent gracefully).

## 9.2 Scripts

Every project has the same set of scripts so muscle memory works across repos.

| Script               | What it does                           |
| -------------------- | -------------------------------------- |
| `pnpm dev`           | Next dev server                        |
| `pnpm build`         | Production build                       |
| `pnpm start`         | Run prod build locally                 |
| `pnpm lint`          | ESLint                                 |
| `pnpm lint:fix`      | ESLint + autofix                       |
| `pnpm format`        | Prettier write                         |
| `pnpm format:check`  | Prettier check (CI)                    |
| `pnpm typecheck`     | `tsc --noEmit`                         |
| `pnpm test`          | Vitest run (unit)                      |
| `pnpm test:watch`    | Vitest watch                           |
| `pnpm test:a11y`     | Playwright + axe sweep                 |
| `pnpm test:e2e`      | Playwright E2E                         |
| `pnpm test:visual`   | Playwright visual regression           |
| `pnpm test:perf`     | Playwright + web-vitals capture        |
| `pnpm lighthouse`    | Lighthouse CI against `localhost`      |
| `pnpm lighthouse:ci` | Lighthouse CI against preview URL      |
| `pnpm analyze`       | Bundle analyzer (opens browser)        |
| `pnpm size`          | `size-limit` check                     |
| `pnpm sb`            | Storybook (optional; see §9.10)        |
| `pnpm clean`         | `rm -rf .next node_modules/.cache`     |
| `pnpm check`         | Run lint + typecheck + test (pre-push) |

## 9.3 Environment setup

`.env.example` lives at repo root:

```bash
# Public — exposed to client
NEXT_PUBLIC_SITE_URL=http://localhost:3000
NEXT_PUBLIC_SENTRY_DSN=
NEXT_PUBLIC_ANALYTICS_KEY=

# Server only
REVALIDATE_TOKEN=
CONTACT_WEBHOOK_URL=
TURNSTILE_SECRET=
```

`lib/env.ts` (Zod) parses on import. Build fails fast on missing required vars.

## 9.4 Onboarding (Day 1)

Steps a new contributor follows, all in `CONTRIBUTING.md`:

1. Clone repo, install Node (`nvm use`), install pnpm.
2. Run `pnpm install` and `pnpm dev`.
3. Open the [PRD/README](../PRD/README.md). Read sections 1, 2, 7 (≈ 30 min).
4. Find a `good-first-issue` and pick one.
5. Branch (`feature/your-name-something`), code, commit (conventional), open PR.
6. CI runs; preview deploys; review happens.

Estimated time to first merged PR: **half a day for a senior, one day for a junior**.

## 9.5 Code review standards

What reviewers look for, in this priority order:

1. **Correctness**: does it do what the PR says?
2. **Boundary respect**: does it cross module boundaries (§2.3)? Does it bypass `lib/api`?
3. **Performance**: any new client component, big dep, or unbounded render?
4. **A11y**: keyboard reachable, labelled, contrast-safe?
5. **Tests**: edge case coverage, not just happy path?
6. **DX**: would the next maintainer find this surprising?
7. **Style**: lint passes; we don't bikeshed past that.

**Reviewers leave one of three outcomes:**

- ✅ Approve
- 💬 Comment (no blockers, just thoughts)
- 🛑 Request changes (specific, actionable, with rationale)

Reviewers do not block on "I would have written this differently". They block on "this is wrong, slow, insecure, or unmaintainable".

## 9.6 Linting rules

ESLint flat config (`eslint.config.mjs`), aggregated from:

- `eslint:recommended`
- `@typescript-eslint/recommended-type-checked`
- `@typescript-eslint/strict-type-checked`
- `next/core-web-vitals`
- `plugin:react/recommended`
- `plugin:jsx-a11y/recommended`
- `plugin:import/recommended` + boundary rules
- `plugin:tailwindcss/recommended`
- Custom rules:
  - `cocoshowroom/no-process-env-outside-env-ts` — `process.env.X` allowed only in `lib/env.ts`.
  - `cocoshowroom/no-direct-fetch` — bare `fetch` allowed only in `lib/fetcher.ts`.
  - `cocoshowroom/no-img-tag` — must use `next/image`.
  - `cocoshowroom/no-untyped-search-params` — page-level `searchParams` must be Zod-parsed.

Errors fail CI. Warnings fail CI too (no warning rot).

## 9.7 Formatting rules

Prettier with project config:

```json
{
  "semi": true,
  "singleQuote": false,
  "trailingComma": "all",
  "printWidth": 100,
  "arrowParens": "always",
  "plugins": ["prettier-plugin-tailwindcss"]
}
```

- Tailwind class order auto-sorted (`prettier-plugin-tailwindcss`).
- LF line endings (`.gitattributes`).
- Pre-commit: `lint-staged` runs ESLint + Prettier on changed files only.

## 9.8 Type safety rules

`tsconfig.json` non-negotiables:

```jsonc
{
  "compilerOptions": {
    "strict": true,
    "noImplicitOverride": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "exactOptionalPropertyTypes": true,
    "forceConsistentCasingInFileNames": true,
    "moduleResolution": "bundler",
  },
}
```

- `any` is forbidden (lint error). Use `unknown` and narrow.
- `as` casts require a comment justifying them.
- `// @ts-ignore` is forbidden; `// @ts-expect-error` is allowed for a documented reason.
- Branded types for IDs (`type ProductId = string & { __brand: "ProductId" }`).

## 9.9 Husky + lint-staged

```jsonc
// package.json
{
  "lint-staged": {
    "*.{ts,tsx,js,jsx,mjs,cjs}": ["eslint --fix", "prettier --write"],
    "*.{md,mdx,json,css,yml,yaml}": ["prettier --write"],
  },
}
```

Hooks:

- `pre-commit`: lint-staged
- `commit-msg`: commitlint
- `pre-push`: `pnpm check` (type + lint + unit) — bails the push if broken

## 9.10 Storybook (optional, post-launch)

Storybook is **not on the v1 critical path**. We will add it post-launch when the design-system surface stabilizes. Until then, the design canvas (HTML prototype) is the visual reference, and the ADR `docs/ADR/00xx-no-storybook-v1.md` records why.

## 9.11 Testing workflow (developer's loop)

```
write a component → write its test (or alongside) → pnpm test:watch
                                                  → red? fix.
                                                  → green? commit.
                                                  → ship.
```

For a feature touching the API layer: mock the new endpoint in `lib/api/<feature>/__mocks__/`, write unit tests against the mock, then write one E2E that exercises the real endpoint against the preview deploy.

## 9.12 Build validation workflow

A PR that touches `src/` triggers:

1. **Install** (cached)
2. **Lint** (parallel)
3. **Typecheck** (parallel)
4. **Unit test** (parallel)
5. **A11y axe sweep** (parallel)
6. **Build** (after lint/type/test)
7. **Bundle size check** (after build)
8. **Deploy preview**
9. **Lighthouse CI** (after preview)
10. **E2E** (sharded, against preview)
11. **Visual regression** (against preview)

Failures at any step block merge. The PR comment aggregates the deltas.

## 9.13 ADR template

```md
# ADR-NNNN: <Title>

- Status: proposed | accepted | superseded | deprecated
- Date: YYYY-MM-DD
- Deciders: @name1 @name2

## Context

What problem are we solving? What forces are at play?

## Decision

What did we decide?

## Consequences

- Positive: ...
- Negative: ...
- Neutral: ...

## Alternatives considered

1. <option> — why not
2. <option> — why not
```

Every major architecture choice (state lib, image CDN, theme system, etc.) is logged. The discipline is light; the value at year two is enormous.

## 9.14 Definition of Done

A PR is **done** when:

- [ ] Code matches the PRD section it relates to (or that section is updated).
- [ ] All CI gates green.
- [ ] No `TODO` without a tracked issue (`// TODO(#123): ...`).
- [ ] No new `console.log` in source.
- [ ] Tests cover at least the happy path + the one most-likely-broken edge.
- [ ] Translations exist for VI and EN (or the missing-string CI gate fails).
- [ ] Reviewer has signed off.
- [ ] Preview URL has been manually clicked through on a real phone (or simulator) at least once.
