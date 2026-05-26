# §1 · Product Overview

## 1.1 Vision

CoCoShowroom is the digital version of walking into a small, well-curated specialty shop in Saigon. The shopkeeper knows where the cordyceps came from, can tell you the difference between a frame-dried and a freeze-dried lot, and is happy to pour you tea while you decide. The product is premium, the brand is editorial, and the buyer is making a considered purchase — often as a gift, often for someone older, often for postpartum recovery.

The frontend's job is to feel **calm, credible, and crafted** on a mid-tier Android over a hotel Wi-Fi connection in Đà Nẵng — not to feel like a Shopify template.

## 1.2 Product goals (v1)

| #   | Goal                              | Measure                                                                                |
| --- | --------------------------------- | -------------------------------------------------------------------------------------- |
| G1  | Convert considered buyers         | Add-to-cart rate ≥ 6% on product detail (industry benchmark for premium grocery: 4–5%) |
| G2  | Earn trust before the sell        | Avg. session time on Story page ≥ 60 s; bounce on PDP ≤ 45%                            |
| G3  | Be usable on mid-tier Android     | Lighthouse mobile ≥ 90; INP P75 ≤ 150 ms on Moto G class                               |
| G4  | Rank for the Vietnamese long tail | Indexed pages > 95% in GSC; CTR on product queries ≥ 3%                                |
| G5  | Bilingual without compromise      | EN/VI feature parity; no untranslated strings in prod (CI gate)                        |
| G6  | Theme system as a brand asset     | Three themes ship with one DOM; theme switch < 250 ms with no layout shift             |

## 1.3 Target users

### Primary persona — _"Chị Trang", 38, Saigon, marketing manager_

Buys cordyceps as a postpartum gift for her sister. Researches on phone during commute (4G, Galaxy A52 class), completes purchase on laptop at night. Reads Vietnamese, prefers VND, expects COD or bank transfer, distrusts sites that look like dropshippers. Will leave if the page jank-scrolls or autoplays anything.

### Secondary — _Vietnamese diaspora, 45–65, US/AU_

Reads English, pays by card, ships to relatives in HCMC or HN. Trust signal is heavier (origin story, lab certificates). Reads on iPad over fast Wi-Fi but very low patience for loading states.

### Tertiary — _Wholesale (clinics, tea shops)_

Email path. Drives the `/wholesale` and `/contact` routes. Low traffic, high LTV.

## 1.4 Business goals

| Goal                                           | Frontend implication                                            |
| ---------------------------------------------- | --------------------------------------------------------------- |
| 200 orders / month by month 6                  | PDP must be conversion-optimized; checkout ≤ 5 steps            |
| < 2 s page load on 4G                          | Performance budgets in [§3](./03-performance.md)                |
| Wholesale leads via form                       | `/contact` is a real form, not `mailto:`                        |
| Press / PR (the Story is the marketing)        | `/story` is treated as a landing page, not a footer link        |
| Future expansion to 3 additional product lines | Component architecture must not bake in "cordyceps" assumptions |

## 1.5 Non-goals (v1)

- Subscription / repeat-order flows
- Loyalty / points
- Reviews submission (we display, we don't collect, yet)
- Live chat / chatbot
- Native mobile app
- Social login (email + phone only when auth lands)
- AR / 3D product preview
- Multi-currency display (VND only at launch; EN locale shows VND with USD reference in tooltip)

Cutting these is deliberate. Each is a credible v2 feature but each adds 20–50 kB to the bundle and a week of testing.

## 1.6 Constraints

### Market

- Vietnamese-first content. No machine-translated UI strings.
- VND prices include 8% VAT inclusive (legal requirement; cannot show pre-tax).
- COD must be available (cultural expectation for first-time buyers > 500 k VND).

### Technical

- Must work on Chrome ≥ 110, Safari ≥ 16, Samsung Internet ≥ 22, Cốc Cốc ≥ 110 (a Vietnamese Chromium fork — pragmatically Chrome behavior).
- Must work on devices with **2 GB RAM** without OOM crashes.
- Must work on **4G with 200 kbps sustained** — i.e. degraded 4G, not LTE-A.
- No third-party scripts that block first paint. No surprises.

### Legal / regulatory

- Vietnamese consumer-protection law requires visible terms, return policy, business registration number.
- Cookie banner: GDPR-grade for EN locale visitors (we serve from Singapore CDN; we will get EU traffic).
- No medical claims. Copy review pass required on every product change.

## 1.7 Assumptions

| Assumption                                       | Risk if wrong                                        |
| ------------------------------------------------ | ---------------------------------------------------- |
| SKU count stays < 100 in year one                | Need to swap client search for Algolia               |
| Image assets come from us (no UGC)               | Need moderation pipeline if we open reviews          |
| Backend lands within 6 months of frontend launch | localStorage cart needs migration path               |
| Single warehouse / shipping origin               | Shipping calculator gets a lot more complex          |
| Brand stays editorial                            | Component density / spacing tokens would need rework |

## 1.8 Future backend considerations

The frontend is being built as if the backend already exists. Specifically:

- **All data access goes through `src/lib/api/*`** — even today's static JSON. Swapping a `fetchProducts()` impl from local JSON to `GET /api/products` is one file.
- **Auth boundary is hypothetical but reserved.** `src/lib/auth/session.ts` returns `null` today. Server Components already call it; pages that should be gated already check.
- **Cart is a Zustand store with a server-reconciliation hook stub.** When the backend lands, the hook runs; today it no-ops.
- **Forms POST to `/api/*` route handlers that today write to a `submissions.log` (dev) or a Vercel KV (preview/prod placeholder).** Real handlers swap in.
- **Webhooks/event surface is reserved.** `/api/webhooks/[provider]/route.ts` paths are documented but unimplemented.

See [§13 Backend Integration Readiness](./13-backend-integration.md) for the full contract.

## 1.9 Success criteria for "v1 done"

Concrete, testable:

1. Lighthouse mobile (4G, Moto G Power emulation) — Perf ≥ 90, A11y = 100, BP = 100, SEO = 100 on home / PDP / story / shop.
2. Zero unhandled exceptions in Sentry over a 7-day soak with synthetic traffic.
3. Bundle: route JS (gzip) ≤ 90 kB on every public route.
4. CMS-free: editing a product means editing one file (and only one file).
5. Theme swap is one keystroke and zero layout shift.
6. The Vietnamese and English builds are byte-for-byte feature-equivalent (audited via E2E).
7. CI blocks merge on: type errors, ESLint errors, failed unit tests, failed a11y axe pass, perf regression > 5%.
