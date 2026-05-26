# §14 · Risks & Mitigation

> **Surfaced early, planned for early.** Risks worth listing are ones with non-trivial probability and non-trivial impact. We track them; for each, the mitigation owner and the trigger that escalates it.

## 14.1 Risk register

| ID   | Risk                                                                   | Probability | Impact   | Owner               |
| ---- | ---------------------------------------------------------------------- | ----------- | -------- | ------------------- |
| R-01 | Bundle size creeps past budget as features land                        | Medium      | High     | Frontend lead       |
| R-02 | Theme system breaks on a new Vercel/Next release                       | Low         | High     | Frontend lead       |
| R-03 | Vietnamese fonts (Be Vietnam Pro full subset) inflate font weight      | High        | Medium   | Frontend            |
| R-04 | LCP regression on mid-tier Android over 4G                             | Medium      | High     | Frontend            |
| R-05 | Cookie consent UX hurts conversion in EN locale                        | Medium      | Medium   | Product             |
| R-06 | Vercel pricing scales unexpectedly with image opt                      | Low         | Medium   | Platform            |
| R-07 | Single-region (Singapore) latency for HN users at peak                 | Low         | Low      | Platform            |
| R-08 | Major Next.js upgrade breaks RSC contracts                             | Medium      | High     | Frontend            |
| R-09 | Dependency supply-chain compromise                                     | Low         | Critical | Platform / Security |
| R-10 | Mobile Safari WebKit bug breaks checkout                               | Medium      | High     | Frontend            |
| R-11 | i18n string desync between VI and EN                                   | High        | Medium   | Frontend            |
| R-12 | Content edits require deploy (no CMS)                                  | Medium      | Medium   | Product             |
| R-13 | SEO indexation lags due to JS-heavy filters                            | Low         | High     | Frontend            |
| R-14 | Sentry quota burn from a noisy error                                   | Medium      | Low      | Frontend            |
| R-15 | Backend API contract drifts from frontend Zod schemas                  | Medium      | High     | Cross-team          |
| R-16 | Accessibility regression slips past axe (axe doesn't catch everything) | Medium      | Medium   | Frontend            |
| R-17 | Cốc Cốc-specific rendering quirks                                      | Low         | Medium   | Frontend            |
| R-18 | Cart data loss on storage clearing / private mode                      | Medium      | Low      | Frontend            |
| R-19 | Localized payment provider (VNPay/MoMo) integration delay              | High        | Medium   | Platform            |
| R-20 | Domain / DNS misconfig breaks email deliverability                     | Low         | High     | Platform            |

## 14.2 Scalability risks

### R-01 — Bundle creep

**Trigger:** Any PR raises route JS > 90 kB.
**Mitigation:**

- `size-limit` hard gate in CI.
- Bundle-analysis comment on every PR.
- Monthly "diet review": top 10 chunks audited; replace anything > 20 kB.
- Refuse `lodash` / `moment` / similar.

### R-08 — Next major upgrade

**Trigger:** Next 16 ships.
**Mitigation:**

- Wait ≥ 30 days after a major before adopting.
- Run upgrade on a `chore/next-N` branch with full CI.
- ADR records the upgrade decision + breaking-change matrix.
- Roll back is one PR revert.

### R-15 — API contract drift

**Trigger:** Zod parse errors surface in Sentry.
**Mitigation:**

- Source of truth: a shared OpenAPI / typespec doc the backend exports.
- Codegen Zod schemas from it (post-v1).
- Daily CI ping at staging API; alerts on drift.

## 14.3 Performance risks

### R-03 — Font weight

**Trigger:** Total font weight > 80 kB.
**Mitigation:**

- Subset Be Vietnam Pro to required diacritics + Latin Extended (one Unicode-range).
- Self-host; preload only the two faces used above the fold.
- `font-display: optional` on body, `swap` on headings.
- Fall back to system serif/sans with metric-matched `size-adjust`.

### R-04 — LCP on mid-tier Android

**Trigger:** Real-user P75 LCP > 2.5 s for 3 consecutive days.
**Mitigation:**

- Hero is HTML+CSS, not JS-driven.
- Image preloaded via `priority`.
- `Cache-Control: immutable` on hashed assets.
- Edge SSR for warm-cache TTFB.
- Synthetic monitoring from a slow-4G Singapore PoP.

### R-17 — Cốc Cốc quirks

**Trigger:** Reports from VN users; QA spot-check.
**Mitigation:**

- Cốc Cốc ≈ Chrome, so most issues won't appear, but we install it on a real test device and run the conversion flow before each release.

## 14.4 Security risks

### R-09 — Supply chain

**Trigger:** Dependabot/Renovate alert; new install with postinstall script.
**Mitigation:**

- `pnpm install --frozen-lockfile --ignore-scripts` in CI.
- Postinstall allow-list per package.
- npm package provenance verified.
- `pnpm audit --prod` gates builds.
- Quarterly review of top 20 deps for ownership/health.

### R-20 — DNS / email deliverability

**Trigger:** Customer reports "your confirmation didn't arrive" (rare today; common post-launch).
**Mitigation:**

- SPF + DKIM + DMARC published from day one for `cocoshowroom.vn`.
- `noreply@` mailbox warmed gradually.
- Use a reputable email provider (Resend / Postmark) once transactional email lands.

## 14.5 Mobile risks

### R-10 — Mobile Safari bug

**Trigger:** Checkout E2E fails on `webkit-mobile` project.
**Mitigation:**

- Playwright projects include `webkit-mobile`.
- Pre-launch: manual pass on a real iPhone in private browsing (Safari is stricter).
- Polyfill graveyard documented; avoid features < 90% Safari support.

### R-18 — Cart data loss

**Trigger:** Customer reports lost cart.
**Mitigation:**

- `localStorage` + versioned schema (`cart.v1`).
- Migration path on schema change.
- Once auth lands, server reconciliation.
- Display a "we saved your cart for 30 days" microcopy near the cart icon.

## 14.6 SEO risks

### R-13 — Indexation lag

**Trigger:** GSC indexed count < 90% after 30 days.
**Mitigation:**

- SSG/ISR for every public route.
- Sitemap submitted on launch.
- Server-rendered JSON-LD on every product.
- Filtered shop pages canonicalize to category root.
- Internal linking ensures crawlability.

## 14.7 Build-time risks

### R-12 — No CMS, edits require deploy

**Trigger:** Product change is urgent; deploy pipeline is broken; we can't ship a copy fix.
**Mitigation:**

- Content (`src/content/`) is checked in; non-engineers PR via GitHub web UI.
- A `chore/content-only` branch pattern enables fast paths.
- Vercel deploys are fast (< 4 min); a urgent typo fix is 10 min end-to-end.
- Post-v1: add a small admin route for the most-edited fields (price, stock) so non-engineers can edit without PR.

## 14.8 Dependency risks

### R-14 — Sentry quota burn

**Trigger:** Sentry events > 80% of monthly quota.
**Mitigation:**

- Sampling rate dialed in.
- `beforeSend` deduplicates known-noisy errors.
- Alert at 70% utilization (not 100%).
- Quota-burn dashboard reviewed weekly.

### R-19 — Payment provider integration

**Trigger:** VNPay/MoMo integration takes longer than expected.
**Mitigation:**

- Launch with COD + bank transfer only (no provider dependency).
- Add online payment as a post-launch enhancement.
- Payment-method UI is data-driven (a list of `PaymentMethod` records); adding one is JSON + flag.

## 14.9 Accessibility risks

### R-16 — Axe doesn't catch everything

**Trigger:** Real-user complaint; quarterly manual audit finds an issue.
**Mitigation:**

- Quarterly manual SR pass on all critical routes.
- Keyboard-only walkthrough on every release for the changed routes.
- Tone-of-voice review on error states (calm, not technical).
- Hire an a11y consultant for a one-time audit pre-launch + once per year.

## 14.10 i18n risks

### R-11 — String desync

**Trigger:** A VI message exists, EN equivalent missing.
**Mitigation:**

- Single message catalog per locale.
- CI gate: `pnpm i18n:check` fails if any key missing in a non-default locale.
- TypeScript wraps `t()` so unknown keys are compile errors.

## 14.11 Risk review cadence

- **Weekly:** triage Sentry top issues; review CI health.
- **Monthly:** revisit risk register; update probability/impact.
- **Quarterly:** chaos test (planned synthetic outage); manual a11y audit; dependency health review.
- **Per major release:** re-rate every risk; close completed mitigations.
