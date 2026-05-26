# §3 · Performance & Optimization

> **Thesis:** The site is read by a 38-year-old marketing manager on a Galaxy A52 over 4G in a coffee shop. Every architecture decision is graded against that scene. If a feature looks good on a MacBook but jitters on her phone, it does not ship.

## 3.1 Performance budgets

These are **CI-enforced**, not aspirational. PRs that breach them fail the build.

### Field metrics (CrUX / real users, P75)

| Metric       | Good     | Needs improvement | Poor     | Budget                |
| ------------ | -------- | ----------------- | -------- | --------------------- |
| LCP          | < 2.5 s  | < 4.0 s           | ≥ 4.0 s  | **≤ 2.0 s**           |
| INP          | < 200 ms | < 500 ms          | ≥ 500 ms | **≤ 150 ms**          |
| CLS          | < 0.1    | < 0.25            | ≥ 0.25   | **≤ 0.05**            |
| TTFB         | < 800 ms | < 1.8 s           | ≥ 1.8 s  | **≤ 500 ms**          |
| FID (legacy) | < 100 ms | —                 | —        | n/a (replaced by INP) |

### Lab metrics (Lighthouse, mobile, simulated 4G, 4× CPU throttle)

| Route             | Performance | A11y | BP  | SEO |
| ----------------- | ----------- | ---- | --- | --- |
| `/` (home)        | ≥ 92        | 100  | 100 | 100 |
| `/shop`           | ≥ 90        | 100  | 100 | 100 |
| `/product/[slug]` | ≥ 90        | 100  | 100 | 100 |
| `/story`          | ≥ 92        | 100  | 100 | 100 |
| `/checkout`       | ≥ 88        | 100  | 100 | n/a |

### Asset budgets

| Asset class                      | Soft   | Hard fail |
| -------------------------------- | ------ | --------- |
| JS per route (gzip)              | 80 kB  | 90 kB     |
| CSS total (gzip)                 | 18 kB  | 25 kB     |
| Image above the fold (per route) | 150 kB | 200 kB    |
| Fonts (total, woff2 subset)      | 60 kB  | 80 kB     |
| Initial HTML (gzip)              | 14 kB  | 20 kB     |
| Total page weight (mobile, home) | 350 kB | 500 kB    |

### CPU / memory targets

| Constraint                           | Target                                       |
| ------------------------------------ | -------------------------------------------- |
| JS execution time on Moto G Power    | ≤ 1.5 s total, ≤ 200 ms per main-thread task |
| JS heap after navigation             | ≤ 30 MB                                      |
| Long tasks (> 50 ms) on landing      | ≤ 1                                          |
| Memory growth over 5 SPA navigations | ≤ 5 MB / nav                                 |

## 3.2 Core Web Vitals — concrete tactics

### 3.2.1 LCP (≤ 2.0 s)

**The LCP element on every page is identified up front and treated as a first-class citizen.**

| Route | LCP element                           | Strategy                                                                                                                        |
| ----- | ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| Home  | Hero image (label-reference at 1920w) | `<Image priority fetchPriority="high">`; preload in `<head>` via `next/image` priority; AVIF + WebP fallbacks; `sizes` accurate |
| Shop  | First product card image OR `<h1>`    | Server-rendered; `priority` on first 2 cards                                                                                    |
| PDP   | Main product image                    | Same as home hero; preload from `generateMetadata`                                                                              |
| Story | Hero portrait                         | Same                                                                                                                            |

Tactics:

- **No JS required for LCP.** Hero is HTML + CSS. Theme is already applied before paint (cookie-driven `data-theme` on `<html>`).
- **Preconnect / DNS-prefetch** to image CDN, Sentry (no preconnect to ad/analytics — there are none).
- **Cache-Control: `public, max-age=31536000, immutable`** on every static asset under `/_next/`.
- **Streaming HTML.** The shell flushes before below-the-fold data resolves.

### 3.2.2 CLS (≤ 0.05)

| Source of shift                    | Mitigation                                                                                                                                   |
| ---------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| Web fonts                          | `font-display: optional` + self-hosted woff2 + `<link rel="preload" as="font">`. Fallback metrics matched via `size-adjust` in `@font-face`. |
| Images without dimensions          | `next/image` mandatory; `<img>` lint rule fails the build                                                                                    |
| Late-loaded ads / banners          | None. (Cookie banner reserves space.)                                                                                                        |
| Theme switch                       | Tokens swap; layout doesn't. Verified in CI Lighthouse pass.                                                                                 |
| Skeleton → real content            | Skeleton has identical box dimensions                                                                                                        |
| Dynamic content (cart count, etc.) | Width reserved with `min-width`                                                                                                              |

### 3.2.3 INP (≤ 150 ms)

INP is the silent killer on cheap Android. Tactics:

- **Click handlers stay under 50 ms.** Anything heavier yields (`scheduler.yield()` or `requestIdleCallback`).
- **No state setter cascades** — Zustand with selectors, no Context for hot paths.
- **Drawer / modal open is CSS-only** where possible (`<dialog>` element + Radix).
- **Form inputs are uncontrolled** (RHF). Per-keystroke renders are zero.
- **Theme switch** mutates one DOM attr + toggles a `<link>`. Measured at ~12 ms on Moto G.

### 3.2.4 TTFB (≤ 500 ms)

- **Edge runtime** for cacheable routes (most marketing). Cold start ~5 ms.
- **Node runtime** only where required (image opt is automatic; route handlers that touch Node-only deps).
- **`generateStaticParams`** for products → SSG at build, served from CDN.
- **`revalidate: 3600`** on shop/story/support; revalidate-on-demand on product mutations once backend lands.

## 3.3 Rendering strategy

| Page                             | Rendering mode                                                       | Why                                            |
| -------------------------------- | -------------------------------------------------------------------- | ---------------------------------------------- |
| `/`                              | Static (SSG) + ISR (revalidate: 3600)                                | Marketing; rarely changes                      |
| `/shop`                          | Static + ISR; filters via URL state                                  | Filter changes are client-cheap, server-cached |
| `/product/[slug]`                | Static (SSG) + ISR (`revalidate: 3600`, `revalidateTag('products')`) | Most-visited; SSG is free wins                 |
| `/story`, `/support`, `/contact` | Static                                                               | No personalization                             |
| `/cart`                          | Dynamic (no caching; uses client store)                              | Per-user                                       |
| `/checkout`, `/account`          | Dynamic + `noindex`                                                  | Per-user                                       |
| `/api/*`                         | Edge                                                                 | Forms, webhooks, revalidate hooks              |

## 3.4 Streaming & Suspense

PDP example flow:

```tsx
// app/[locale]/product/[slug]/page.tsx — Server Component
export default async function Page({ params }) {
  const product = await getProductBySlug(params.slug); // blocks shell
  return (
    <>
      <ProductHero product={product} />
      <Suspense fallback={<RelatedSkeleton />}>
        <RelatedProducts category={product.category} excludeId={product.id} />
      </Suspense>
      <Suspense fallback={<ReviewsSkeleton />}>
        <Reviews productId={product.id} />
      </Suspense>
    </>
  );
}
```

- **Above-fold is awaited in the shell.** Everything else is `<Suspense>`'d.
- Skeletons match the real layout's dimensions to keep CLS at zero.
- Reviews and "related" parallelize at the data layer.

## 3.5 Caching strategy

Three layers:

1. **Next.js Data Cache** (`fetch` with `next: { revalidate, tags }`). Default for all `lib/api/*` calls.
2. **Route Cache** (Full Route Cache for static; dynamic routes opt in via `force-static`).
3. **CDN cache** (Vercel default; `Cache-Control` overrides where stricter).

Revalidation triggers (when backend lands):

- `revalidateTag('products')` on product mutation webhook
- `revalidateTag('inventory')` on inventory webhook (cart-page only)
- `revalidatePath('/shop')` on category change

## 3.6 Image optimization

- **`next/image` everywhere.** Raw `<img>` lint-fails.
- **AVIF preferred, WebP fallback, JPEG last.** Configured in `next.config.mjs`.
- **Sizes attribute is accurate** (else `next/image` ships the wrong width).
- **Source images at 2× display width max.** Larger originals are downscaled at build (a `scripts/optimize-images.ts` pre-commit).
- **LQIP** (low-quality image placeholder) via `placeholder="blur"` for hero/PDP gallery, with the blur token generated at build (plaiceholder or @squoosh).
- **CDN**: Vercel Image Optimization, with `images.remotePatterns` locked to our CDN host.

## 3.7 Font optimization

The brand requires **Lora**, **Be Vietnam Pro**, **JetBrains Mono**.

- **Self-host** via `next/font/local` — Google Fonts CDN adds an extra DNS hop and prevents CSP tightening.
- **Subset to Latin Extended + Vietnamese** (Be Vietnam Pro has full diacritics; subset is huge — we ship the full Vietnamese block).
- **`font-display: optional`** for body — fallback metric-matched (size-adjust 102.5% experimentally), so a font-late-load swap is unnoticeable.
- **`font-display: swap`** for headings (Lora italic) — the editorial feel is worth a brief swap.
- **Preload only the 2 weights used above the fold** (Lora 500 italic, Be Vietnam Pro 400).
- **`unicode-range`** scoped per font face to skip irrelevant ranges.

Targets: total font weight ≤ 60 kB gzip across all faces.

## 3.8 Script loading

- **No third-party blocking scripts.** Analytics goes via `next/script` `strategy="afterInteractive"`; consent-gated.
- **`<script>` for the theme init is the _only_ inline script** in `<head>` (to prevent FOUC). It's minified, ~600 bytes, and CSP-hashed.
- **Framer Motion is lazy-loaded** behind a `dynamic(() => import(...), { ssr: false })` and gated by `prefers-reduced-motion`.

## 3.9 Code splitting & dynamic imports

- Every route is its own chunk (App Router default).
- **Heavy client components are `dynamic()`'d**: gallery carousel, theme switcher panel, motion-heavy hero animations.
- **Per-feature lazy modules:** `features/checkout` is not imported on `/`.
- **Vendor split**: React, Next runtime, then a `commons` chunk for shared utilities (set explicitly via `next.config.mjs` `webpack`).

## 3.10 Bundle analysis & gates

- `@next/bundle-analyzer` on every build (CI artifact).
- `size-limit` config blocks the build at the route-JS threshold (see §3.1).
- **Per-PR comment** from the bundle-analysis Action: ±delta vs `main`.

## 3.11 Memory optimization

- **No leaks on SPA nav.** Event listeners registered in `useEffect` cleanups verified.
- **Images use `loading="lazy"` below the fold** (next/image default).
- **No `Array.prototype.with(...)`-style copy patterns** on large product lists; we virtualize when results > 50.
- **Heap snapshots** captured in CI for the home → shop → PDP → cart flow (Playwright + `Page.metrics`). Regression > 10% fails.

## 3.12 Mobile / low-end device optimization

| Constraint                                                   | Behavior                                                                    |
| ------------------------------------------------------------ | --------------------------------------------------------------------------- |
| `navigator.connection.effectiveType === '2g' \|\| 'slow-2g'` | Skip hero blur-up; serve placeholder + low-res hero; no autoplay video      |
| `navigator.deviceMemory <= 2`                                | Disable Framer Motion entry animations; force `prefers-reduced-motion`      |
| `navigator.hardwareConcurrency <= 4`                         | Use `IdleScheduler` for non-critical effects; defer reviews fetch by 500 ms |
| `prefers-reduced-data`                                       | Skip auto-rotating PDP gallery; serve smaller images                        |

These detection paths are centralized in `lib/device.ts`. **They never block paint** — they only adjust optional enhancements.

## 3.13 Animation performance

- **Transform & opacity only** on the main thread. No `top`/`left`/`width` animations.
- **`will-change`** scoped (added before, removed after) — never permanent.
- **GPU layer count budget**: ≤ 8 simultaneously promoted layers.
- **Reduced-motion respected globally** via a single CSS media query block AND a JS hook (`useReducedMotion()`).

## 3.14 Re-render prevention

- Zustand selectors (`useStore(state => state.cart.items.length)`) — component re-renders only on length change.
- `useMemo` / `useCallback` reserved for expensive computations and stable refs passed to memoized children — **not sprinkled prophylactically**.
- `React.memo` on `ProductCard` only (it appears 50× in a grid).
- ESLint rule: warn on inline object/function props passed to memoized components.

## 3.15 Virtualization & pagination

- **Shop grid uses CSS grid with native scroll** for ≤ 60 items. Above 60, switch to `@tanstack/react-virtual`.
- **Infinite scroll**: cursor-based, `useInfiniteQuery` (when backend lands). URL-state tracks the last-loaded cursor for back-button parity.
- **Search results** are paginated, not infinite — predictable for SEO and a11y.

## 3.16 PWA / offline strategy

- **Phase 1**: App Manifest (installable), no service worker yet.
- **Phase 2** (post-launch): Workbox-based SW that caches the shell + the last viewed PDP for offline read-only.
- **No offline cart mutation in v1**; cart writes require the page to be online or they queue with user notification.

## 3.17 Edge & CDN

- Marketing routes run at **edge** (Vercel Edge Functions).
- Cache headers from Next; we override only where we want longer immutability.
- **Smart Image at edge**; Vercel handles cache-key normalization.
- **Singapore PoP** is the primary for VN traffic (already Vercel default for that geo). HCM/HN are < 30 ms RTT.

## 3.18 Compression

- Brotli (quality 11 static, 4 dynamic) — Vercel default.
- HTML, CSS, JS, JSON, SVG, woff2 all compressed.
- AVIF/WebP for raster.

## 3.19 Anti-patterns we will refuse

- ❌ Loading a charting library on a route that doesn't render a chart.
- ❌ "Just put it in Context" for anything that changes more than once a minute.
- ❌ Pre-fetching every linked route (overflows mobile data).
- ❌ Polling for inventory on the PDP. (Use SSE or webhook when backend lands.)
- ❌ Animating in everything that scrolls into view. Reserve motion for the hero and one section per page.
- ❌ Custom font loaders. Use `next/font/local`.
- ❌ Re-creating Framer Motion at the top of a re-rendering component.
- ❌ Spinning on the network. Skeletons that match real layout.

## 3.20 Verification

Every release passes:

1. `pnpm build` produces no warnings; bundle-analyzer artifact uploaded.
2. `pnpm lighthouse:ci` runs against preview URL, blocks if any score regresses > 5 points or any CWV breaks budget.
3. `pnpm test:perf` (Playwright + `web-vitals`) measures field-equivalents on a throttled context; uploads to ClickHouse.
4. Synthetic monitoring (Checkly) runs every 5 min from VN PoP; pages alert if LCP > 3 s for 3 consecutive checks.
