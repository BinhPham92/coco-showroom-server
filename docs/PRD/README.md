# CoCoShowroom — Frontend PRD & Technical Foundation

> **Status:** Draft v1 · **Owner:** Frontend Platform · **Last updated:** 2026-05-19
> **Scope:** Frontend application only. Backend (orders, auth, inventory, payments) is out of scope for v1 but the architecture is designed to integrate cleanly when it lands.

## Why this document exists

CoCoShowroom is a bilingual (VI/EN) editorial commerce surface for premium Vietnamese cordyceps (_Đông Trùng Hạ Thảo_). It currently exists as a high-fidelity HTML prototype with three polymorphic themes (Royal, Midnight, Ocean) sharing one DOM and one component tree. This PRD describes how we take that prototype to production as a real Next.js application — with the performance, accessibility, security, and operational maturity expected of an editorial commerce site that customers will read on a 2018 mid-tier Android over a Vietnamese 4G connection.

This document is **deliberately engineering-first**. It is the contract between product intent and the codebase. Claude Code, contractors, or new hires should be able to read it, scaffold the repo, and ship Phase 1 without further direction.

## How to read this

| You are…              | Start here                                                                                                           |
| --------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Product / stakeholder | [§1 Product Overview](./01-product-overview.md) → [§15 Implementation Plan](./15-implementation-plan.md)             |
| Frontend lead         | [§2 Architecture](./02-architecture.md) → [§3 Performance](./03-performance.md) → [§15](./15-implementation-plan.md) |
| Security / SRE        | [§4 Security](./04-security.md) → [§11 Monitoring](./11-monitoring.md) → [§12 Deployment](./12-deployment.md)        |
| Designer / UX         | [§5 A11y](./05-accessibility.md) → [§7 UI/UX](./07-ui-ux-standards.md)                                               |
| Claude Code           | [§15 Implementation Plan](./15-implementation-plan.md), phase-by-phase                                               |

## Section index

| #   | Section                             | File                                                       |
| --- | ----------------------------------- | ---------------------------------------------------------- |
| 1   | Product Overview                    | [01-product-overview.md](./01-product-overview.md)         |
| 2   | Technical Architecture              | [02-architecture.md](./02-architecture.md)                 |
| 3   | Performance & Optimization          | [03-performance.md](./03-performance.md)                   |
| 4   | Security                            | [04-security.md](./04-security.md)                         |
| 5   | Accessibility                       | [05-accessibility.md](./05-accessibility.md)               |
| 6   | SEO Strategy                        | [06-seo.md](./06-seo.md)                                   |
| 7   | UI/UX Engineering Standards         | [07-ui-ux-standards.md](./07-ui-ux-standards.md)           |
| 8   | GitHub & Repository Setup           | [08-github-repo.md](./08-github-repo.md)                   |
| 9   | Development Workflow                | [09-development-workflow.md](./09-development-workflow.md) |
| 10  | Testing Strategy                    | [10-testing.md](./10-testing.md)                           |
| 11  | Monitoring & Observability          | [11-monitoring.md](./11-monitoring.md)                     |
| 12  | Deployment                          | [12-deployment.md](./12-deployment.md)                     |
| 13  | Backend Integration Readiness       | [13-backend-integration.md](./13-backend-integration.md)   |
| 14  | Risks & Mitigation                  | [14-risks.md](./14-risks.md)                               |
| 15  | Implementation Plan (Claude Code)   | [15-implementation-plan.md](./15-implementation-plan.md)   |
| A   | Checklists (perf, security, launch) | [appendix-checklists.md](./appendix-checklists.md)         |

## Executive summary

### Product

A bilingual editorial commerce site for cordyceps. Homepage, shop, product detail, story, support, contact, checkout, account. Three swappable visual themes share one DOM (Royal / Midnight / Ocean). Vietnamese-first; English secondary.

### Stack (locked)

| Concern        | Choice                                                                   | Why                                                                |
| -------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------ |
| Framework      | **Next.js 15+** (App Router)                                             | Server Components, streaming, edge runtime, image opt, mature i18n |
| Language       | **TypeScript 5+ (strict)**                                               | Type safety is non-negotiable for long-lived commerce              |
| Styling        | **Tailwind CSS v4** + CSS variables for theme tokens                     | Token-driven theming, zero runtime, JIT                            |
| State (client) | **Zustand**                                                              | 1 kB, no provider tree, fits cart/UI state                         |
| State (server) | **TanStack Query**                                                       | Cache + revalidation for the future API layer                      |
| Forms          | **React Hook Form + Zod**                                                | Uncontrolled fast, schema-validated, type-inferred                 |
| UI primitives  | **Radix UI** + custom skin (NOT bare shadcn)                             | A11y-correct primitives, our visual layer on top                   |
| Motion         | **Framer Motion** (lazy, behind reduced-motion gate)                     | Only on landing/PDP hero; gated                                    |
| i18n           | **next-intl**                                                            | RSC-friendly, message catalogs, ICU plurals                        |
| Package mgr    | **pnpm**                                                                 | Speed, disk, strict hoisting                                       |
| Lint/format    | **ESLint 9 (flat) + Prettier + Husky + lint-staged**                     | Pre-commit gate                                                    |
| Tests          | **Vitest** (unit), **Playwright** (E2E + a11y + visual)                  | One toolchain for tests                                            |
| Monitoring     | **Sentry** + **Vercel Analytics** (RUM) + custom Web-Vitals → ClickHouse | Errors + RUM + flexible analytics                                  |
| Host           | **Vercel** (primary), **Cloudflare Pages** (DR option)                   | Edge, image opt, preview deploys                                   |

### Performance budgets (mobile, 4G, Moto G Power class device)

| Metric                    | Target          | Hard fail          |
| ------------------------- | --------------- | ------------------ |
| LCP                       | ≤ 2.0 s         | > 2.5 s            |
| INP                       | ≤ 150 ms        | > 200 ms           |
| CLS                       | ≤ 0.05          | > 0.10             |
| TTFB                      | ≤ 500 ms (edge) | > 800 ms           |
| JS shipped (route, gzip)  | ≤ 90 kB         | > 130 kB           |
| Image weight (above fold) | ≤ 200 kB        | > 400 kB           |
| Lighthouse Perf (mobile)  | ≥ 90            | < 80 blocks deploy |
| Lighthouse A11y           | = 100           | < 95 blocks deploy |

### Architectural commitments (non-negotiable)

1. **Server Components by default.** A client component requires a comment justifying its hooks/events/portals.
2. **Zero unused JS shipped.** Every client component is route-scoped or dynamically imported.
3. **Themes are CSS, not JS.** Three CSS files swap via `<link disabled>`; no re-renders, no theme context.
4. **Mobile-first, mid-tier-first.** Every PR is checked on the device emulation profile in CI, not on the dev's MacBook.
5. **The frontend never trusts itself.** Validation runs on the edge layer too once the backend lands.
6. **Public APIs are typed at the boundary.** Zod schemas are the source of truth; types are inferred from them, not declared twice.
7. **Editorial pages stream.** Product detail and story pages render shell first, defer reviews / "you may also like" via Suspense.

### Out of scope (v1)

- Backend services, payments, real inventory
- Native apps
- Live chat
- Customer-generated content beyond reviews (no UGC pipeline yet)
- Multi-region (HCMC PoP only at launch; we'll add HN once we have a reason)

### Open questions for product

These are flagged inline in [§1](./01-product-overview.md) but listed here for visibility:

- **Cart persistence model** before backend exists: localStorage only, or anonymous server cart via cookie? (Default: localStorage + server reconciliation when account lands.)
- **Image CDN**: Vercel Image vs. Cloudinary vs. self-hosted (Imgix-style on R2). Default: Vercel Image until volume justifies otherwise.
- **Search**: client-side Fuse.js for v1 (50–100 SKUs) or Algolia from day one? Default: client-side; revisit at 200 SKUs.
- **Language detection**: `Accept-Language` + path prefix (`/vi`, `/en`), or path-only? Default: path-only, with a one-time interstitial on first visit.

See each section for the deep-dive.
