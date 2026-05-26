# §6 · SEO Strategy

> **Why this matters here:** organic search is the marketing channel. The Story is the brand, and the Story has to rank. Product pages have to rank for long-tail Vietnamese queries ("đông trùng hạ thảo sapa giá", etc.).

## 6.1 Goals

- 95%+ of pages indexed in Google Search Console within 30 days of launch.
- CTR on product queries ≥ 3% (industry benchmark for premium specialty: 2.5%).
- Featured snippets earned for at least 5 "what is cordyceps" / "benefits" queries within 90 days.
- Bilingual rankings: VI primary, EN parallel (hreflang-correct).

## 6.2 Rendering strategy for SEO

| Route                                                                                 | Rendering           | Why for SEO                                             |
| ------------------------------------------------------------------------------------- | ------------------- | ------------------------------------------------------- |
| `/`, `/shop`, `/product/[slug]`, `/story`, `/support`, `/support/[topic]`, `/contact` | SSG / ISR           | Fully rendered HTML to crawlers; no JS required to read |
| `/cart`, `/checkout`, `/account`                                                      | Dynamic + `noindex` | Per-user, never index                                   |
| `/api/*`                                                                              | Edge, no HTML       | n/a                                                     |

The crawler experience: full HTML + structured data on first byte. No "JavaScript required" patterns.

## 6.3 Metadata architecture

Next 15's `Metadata` API, with per-route `generateMetadata` for dynamic content.

```ts
// app/[locale]/product/[slug]/page.tsx
export async function generateMetadata({ params }): Promise<Metadata> {
  const product = await getProductBySlug(params.slug);
  const t = await getTranslations({ locale: params.locale });
  return {
    title: `${product.name[params.locale]} — CoCoShowroom`,
    description: product.shortDesc[params.locale],
    openGraph: {
      title: product.name[params.locale],
      description: product.shortDesc[params.locale],
      images: [{ url: product.images[0], width: 1200, height: 630 }],
      type: "website",
      locale: params.locale === "vi" ? "vi_VN" : "en_US",
    },
    alternates: {
      canonical: `https://cocoshowroom.vn/${params.locale}/product/${product.slug}`,
      languages: {
        "vi-VN": `https://cocoshowroom.vn/vi/product/${product.slug}`,
        "en-US": `https://cocoshowroom.vn/en/product/${product.slug}`,
      },
    },
    twitter: { card: "summary_large_image" },
  };
}
```

A `lib/seo/build-metadata.ts` helper centralizes shared logic (defaults, OG image fallback).

## 6.4 Title strategy

| Page      | Title pattern                                    |
| --------- | ------------------------------------------------ |
| Home      | `CoCoShowroom — {tagline localized}`             |
| Shop      | `Mua đông trùng hạ thảo — CoCoShowroom`          |
| Category  | `{Category} — Đông trùng hạ thảo — CoCoShowroom` |
| PDP       | `{Product name} — {grade} — CoCoShowroom`        |
| Story     | `Hành trình hai năm — CoCoShowroom`              |
| FAQ topic | `{Topic} — Hỗ trợ — CoCoShowroom`                |

Length: keep under 60 characters where possible. Vietnamese diacritics are counted; we test in SERP previews.

## 6.5 Open Graph & Twitter Cards

- `og:image` is **route-specific**, 1200×630, rendered via Next 15's `opengraph-image.tsx` per route — programmatic image generation using the design system's tokens. So shares look on-brand.
- `twitter:card` is always `summary_large_image`.
- Fallback OG image at root for routes that don't override.

## 6.6 Structured data (JSON-LD)

Each route emits the right schema:

| Route              | Schema                                                               |
| ------------------ | -------------------------------------------------------------------- |
| `/`                | `Organization` + `WebSite` (with `SearchAction`)                     |
| `/product/[slug]`  | `Product` (with `Offer`, `AggregateRating` if reviews ≥ 1, `Brand`)  |
| `/story`           | `Article` (with author Organization)                                 |
| `/support/[topic]` | `FAQPage`                                                            |
| `/contact`         | `LocalBusiness` (with `address`, `geo`, `openingHoursSpecification`) |

Implementation: a `<JsonLd>` server component takes a typed schema (we type with `schema-dts`) and renders a `<script type="application/ld+json">`. Validated via Schema.org's tester in CI (Playwright + their HTTP API).

## 6.7 Sitemap & robots

- `app/sitemap.ts` generates entries for all static + dynamic routes. Includes alternate-language entries (`xhtml:link`).
- `app/robots.ts` allows all, disallows `/api/`, `/checkout/`, `/account/`. References sitemap.
- Sitemap is served at `/sitemap.xml` and split into `/sitemap-products.xml`, `/sitemap-content.xml` if total entries > 5000 (we won't be there for a while, but the splitter is in place).

## 6.8 Canonical URLs

- Every page sets `alternates.canonical` to the **locale-prefixed** version on the canonical domain (`https://cocoshowroom.vn`).
- Query parameters that don't change content (UTMs, refs) are canonicalized to the bare URL.
- Filtered shop pages canonicalize to the unfiltered category if the filter is non-indexable.

## 6.9 hreflang

For every translatable page:

```html
<link rel="alternate" hreflang="vi-VN" href="https://cocoshowroom.vn/vi/..." />
<link rel="alternate" hreflang="en-US" href="https://cocoshowroom.vn/en/..." />
<link
  rel="alternate"
  hreflang="x-default"
  href="https://cocoshowroom.vn/vi/..."
/>
```

`x-default` points to VI (primary market). The Metadata `alternates.languages` API emits these.

## 6.10 URL structure

Patterns:

- `/{locale}/product/{slug}` — slugs are lowercase, hyphenated, Vietnamese-diacritic-stripped (`đông-trùng-sapa-grade-aa`).
- `/{locale}/shop/{category}` — categories are stable English IDs (`fresh`, `dried`, `extract`).
- No trailing slashes (Next default; enforced).
- No `.html` extensions.
- No mixed case.

A `lib/seo/slug.ts` validates slug shape (kebab-case ASCII, no diacritics for SEO friendliness) and uniqueness at build time.

## 6.11 Internal linking

- Every product card links to the PDP with a descriptive `<a>` (not `<button>` → `router.push`).
- Breadcrumbs on PDP and category pages — `BreadcrumbList` schema also emitted.
- "Related" rail on PDP links to same-category products.
- Story → PDP cross-links where the narrative supports it.
- Footer surfaces every category + key article (cap: 50 links total).

## 6.12 Content strategy alignment

- One `<h1>` per page, descriptive.
- Long-form copy on `/story` and `/support/*` written by humans, not auto-generated.
- Product descriptions are **product-specific** (no copy-paste); 80–150 words minimum.
- FAQ answers are 50–250 words; first 160 characters are answer-bearing for snippet eligibility.
- No keyword stuffing; copy reads as the brand voice.

## 6.13 Image SEO

- Descriptive filenames (`san-pham-dong-trung-sapa-grade-aa.jpg` rather than `IMG_001.jpg`).
- `alt` text describes the image, not the keyword.
- `next/image` outputs `<picture>` with multiple sources; Google's image crawler handles this.
- Image sitemap entries auto-generated.

## 6.14 Performance and SEO

Lighthouse SEO score = 100 is non-negotiable. CWV thresholds (see [§3](./03-performance.md)) are also the SEO thresholds because they're ranking factors.

## 6.15 Indexability gates

- `noindex` on `/checkout`, `/cart`, `/account`, all preview deployments.
- `noindex` on filtered shop URLs deeper than one filter (avoid combinatorial explosion).
- Pagination uses `rel="next"` / `rel="prev"` (still useful for some crawlers despite Google's deprecation).
- `robots` meta + `X-Robots-Tag` both set on protected routes.

## 6.16 International SEO

- Locale path prefix (`/vi`, `/en`) — _no_ domain split. Cheaper to operate, equivalent for ranking.
- `Accept-Language` is consulted only at the bare-root `/` for a one-time redirect (with a "switch to VI" / "switch to EN" badge so users can correct).
- `hreflang` errors are checked weekly (Search Console + a custom audit script).

## 6.17 Monitoring SEO

- Search Console connected, weekly export to ClickHouse for trend tracking.
- Top 100 queries monitored for rank movement; alert on > 5-position drop.
- Indexation count tracked weekly; alert on > 10% drop.
- Soft-404 detection: any page returning 200 with `<1KB` of meaningful content is flagged in CI.

## 6.18 SEO checklist per PR (touching public routes)

- [ ] Title set via `generateMetadata`
- [ ] Description set, ≤ 160 chars
- [ ] OG image present (or falls back to default)
- [ ] Canonical correct
- [ ] hreflang pair present
- [ ] One `<h1>`, sequential headings
- [ ] Image `alt`s set
- [ ] JSON-LD validates
- [ ] No accidental `noindex`
- [ ] Sitemap regenerates without errors
