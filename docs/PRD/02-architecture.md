# §2 · Technical Architecture

## 2.1 Architectural principles

1. **Server Components are the default.** A component being a Client Component is a justified exception, not a habit. Every `"use client"` directive must be accompanied by a one-line comment explaining which hook, event, or browser API forces it.
2. **The browser is a thin renderer.** Data fetching, validation, formatting, i18n message loading happen on the server. The client gets a small, hydratable shell.
3. **Composition over configuration.** Components take children and slots, not 30 props. Props that toggle behavior are a smell.
4. **Boundaries are typed and validated.** Anything that crosses a process boundary (network, storage, user input) is parsed with Zod. Inside the boundary, TypeScript carries the contract.
5. **Cheap to delete.** Features are folders. Deleting a feature should mean deleting one folder + grep'ing for its export.
6. **One way to do each thing.** One toast, one modal, one form, one button. No two of anything.

## 2.2 Folder structure

```
cocoshowroom/
├── .github/
│   ├── workflows/                # CI: lint, type, test, lighthouse, deploy
│   ├── ISSUE_TEMPLATE/
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── CODEOWNERS
├── .husky/
├── apps/                         # (reserved — single-app today, monorepo-ready)
│   └── web/
│       ├── src/
│       │   ├── app/              # Next.js App Router
│       │   │   ├── (marketing)/  # Route group — story, contact, support
│       │   │   │   ├── story/
│       │   │   │   ├── contact/
│       │   │   │   └── support/
│       │   │   ├── (shop)/       # Route group — shop & PDP
│       │   │   │   ├── shop/
│       │   │   │   └── product/[slug]/
│       │   │   ├── (checkout)/
│       │   │   │   ├── cart/
│       │   │   │   ├── checkout/
│       │   │   │   └── confirm/[orderId]/
│       │   │   ├── (account)/    # Reserved; gated when auth lands
│       │   │   │   └── account/
│       │   │   ├── api/          # Route handlers — forms, webhooks
│       │   │   ├── [locale]/     # Localized roots (vi, en)
│       │   │   ├── layout.tsx
│       │   │   ├── error.tsx     # Global error boundary
│       │   │   ├── not-found.tsx
│       │   │   ├── opengraph-image.tsx
│       │   │   └── sitemap.ts
│       │   ├── features/         # Feature-first slices (see §2.4)
│       │   │   ├── cart/
│       │   │   ├── catalog/
│       │   │   ├── checkout/
│       │   │   ├── story/
│       │   │   ├── support/
│       │   │   └── theme-switcher/
│       │   ├── ui/               # Design system primitives
│       │   │   ├── primitives/   # Button, Input, Tag, Price, ...
│       │   │   ├── patterns/     # ProductCard, SectionHead, NavBar
│       │   │   └── motion/       # Lazy-loaded motion wrappers
│       │   ├── lib/
│       │   │   ├── api/          # Data access (today: static; tomorrow: HTTP)
│       │   │   ├── auth/         # Session abstraction (no-op today)
│       │   │   ├── cart/         # Zustand store + persistence
│       │   │   ├── i18n/         # next-intl config, dictionaries
│       │   │   ├── analytics/    # vitals, events, Sentry init
│       │   │   ├── env.ts        # Zod-validated env
│       │   │   ├── fetcher.ts    # Typed fetch wrapper
│       │   │   ├── formatters.ts # VND, dates (vi-VN), plurals
│       │   │   └── utils.ts      # cn(), invariant(), small helpers
│       │   ├── styles/
│       │   │   ├── globals.css
│       │   │   ├── tokens.css    # CSS vars (semantic + raw)
│       │   │   ├── theme-royal.css
│       │   │   ├── theme-midnight.css
│       │   │   └── theme-ocean.css
│       │   ├── content/          # Static content (MDX, JSON) — products, story, FAQ
│       │   │   ├── products/
│       │   │   ├── story/
│       │   │   └── faq/
│       │   └── types/
│       │       └── index.ts      # Cross-feature types only
│       ├── public/
│       │   ├── assets/           # logo, label reference, og defaults
│       │   ├── fonts/            # Self-hosted; see §3
│       │   └── robots.txt
│       ├── tests/
│       │   ├── unit/             # vitest
│       │   ├── e2e/              # playwright
│       │   └── a11y/             # axe via playwright
│       ├── next.config.mjs
│       ├── tailwind.config.ts
│       ├── tsconfig.json
│       └── package.json
├── packages/                     # (reserved — empty in v1, monorepo-ready)
│   ├── eslint-config/
│   ├── tsconfig/
│   └── ui-tokens/                # Theme token JSON exported for design tools
├── docs/
│   ├── ADR/                      # Architecture Decision Records
│   └── PRD/                      # This document
├── .editorconfig
├── .gitignore
├── .nvmrc                        # pinned Node
├── package.json                  # pnpm workspace root
├── pnpm-workspace.yaml
└── README.md
```

### Why feature folders and not type folders?

Type folders (`components/`, `hooks/`, `utils/`) scale linearly with project size and break locality of behavior. A change to checkout touches `components/`, `hooks/`, `utils/`, `pages/` — four PR scopes for one feature. Feature folders co-locate everything a feature owns; cross-feature shared code lives in `ui/` or `lib/`. The test is: "if I delete `features/checkout/`, what breaks?" Answer should be: only checkout. Today this is true on day one of the project, so we lock it in before bad habits form.

### Why `apps/` and `packages/` when there's one app?

Insurance, not aspiration. The monorepo skeleton is free if you start with it and expensive if you bolt it on later. We expect at least three packages to want extraction within a year: `ui-tokens` (for design tools), `eslint-config` (for backend repo when it lands), `email-templates` (transactional). Costs ~30 min of setup; saves a week of migration.

## 2.3 Module boundaries

Boundary rules — enforced via ESLint `no-restricted-imports`:

| Layer            | May import from                 | Must NOT import from                |
| ---------------- | ------------------------------- | ----------------------------------- |
| `app/`           | `features/`, `ui/`, `lib/`      | other `app/` routes (use a feature) |
| `features/<x>/`  | `ui/`, `lib/`                   | other `features/<y>/`               |
| `ui/patterns/`   | `ui/primitives/`, `lib/utils`   | `features/`, `app/`                 |
| `ui/primitives/` | nothing (pure)                  | everything else                     |
| `lib/`           | `lib/` (siblings), nothing else | `features/`, `ui/`, `app/`          |

This is the **dependency graph as DAG, not soup**. Violations are caught at lint time, not at code review.

## 2.4 Feature-based architecture

Anatomy of a feature folder (`features/catalog/`):

```
features/catalog/
├── api/
│   ├── get-products.ts           # Server: returns Product[]
│   ├── get-product-by-slug.ts
│   └── schema.ts                 # Zod schemas for Product
├── components/
│   ├── ProductGrid.tsx           # Server Component
│   ├── ProductCard.tsx           # Server Component
│   ├── ProductFilters.tsx        # "use client" — Radix Popover
│   └── ProductGallery.tsx        # "use client" — touch carousel
├── hooks/
│   └── use-filters.ts            # URL state via nuqs
├── lib/
│   └── ranking.ts                # Pure: sort/filter logic
├── tests/
│   └── ranking.spec.ts
└── index.ts                      # Public barrel (re-exports only what's needed outside)
```

Only `index.ts` exports are reachable from outside the feature. Anything else is `@internal`. This is enforced by `eslint-plugin-import` boundary rules.

## 2.5 Component architecture

Three tiers:

| Tier                                   | Examples                                                          | Rules                                                                                                                                      |
| -------------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| **Primitive** (`ui/primitives/`)       | `Button`, `Input`, `Tag`, `Price`, `Badge`, `Spinner`             | Pure presentation. No business logic. No data fetching. Forwarded refs. Polymorphic via `asChild` (Radix Slot). Both server + client safe. |
| **Pattern** (`ui/patterns/`)           | `NavBar`, `Footer`, `SectionHead`, `EmptyState`, `Modal`, `Toast` | Compose primitives. Layout-aware. Usually Server-Component-safe; rare client wrappers.                                                     |
| **Feature** (`features/*/components/`) | `ProductCard`, `CheckoutSummary`, `ThemeSwitcher`                 | Know about their feature's data and intent. May fetch (Server) or use stores (Client).                                                     |

### Client vs Server checklist

A component goes Client **only** if it uses any of:

- `useState`, `useEffect`, `useRef` on a browser-only API
- `onClick`, `onChange`, or any synthetic event handler (excluding `<form action={...}>` Server Actions)
- Browser-only globals (`window`, `document`, `IntersectionObserver`, ...)
- Portals
- Third-party libs that import the above

If none apply, it's a Server Component. **Server is the default; client is the exception with a justifying comment.**

## 2.6 Shared UI strategy

We **start with Radix primitives + Tailwind, not shadcn install**. Reason: shadcn ships well-written defaults that are visually generic. CoCoShowroom has a strong visual identity (Lora italic display, gold-on-navy accents, Đông Trùng label DNA). We will write our own ~25-component design system, taking _patterns_ from shadcn (the slot composition, the cva variants) but not its `tailwind.config` or its styles.

Components we will own:

- `Button` (5 variants × 3 sizes, all themes)
- `Input`, `Textarea`, `Select`, `Checkbox`, `Radio`, `Toggle`, `FormField`
- `Modal`, `Drawer`, `Toast`, `Popover`, `Tooltip`, `Tabs`, `Accordion`
- `Price`, `Tag`, `Badge`, `Stars`, `Eyebrow`, `Stamp`, `Quote`
- `SectionHead`, `Container`, `Grid`, `Stack`, `Divider`
- `NavBar`, `Footer`, `Breadcrumb`
- `ProductCard`, `EmptyState`, `Skeleton`, `Spinner`

We build off Radix primitives where applicable (Dialog, Popover, Tooltip, etc.) — they handle a11y semantics that we would otherwise get wrong.

## 2.7 Route architecture

App Router with **route groups for layout sharing without URL nesting**:

```
/                                 → home
/[locale]/shop                    → catalog grid
/[locale]/shop/[category]         → category filter (URL-state-driven)
/[locale]/product/[slug]          → PDP
/[locale]/story                   → brand story
/[locale]/support                 → help center
/[locale]/support/[topic]         → FAQ topic
/[locale]/contact                 → contact form
/[locale]/cart                    → cart drawer is the primary; this is a deep link
/[locale]/checkout                → multi-step (server component pages, client component forms)
/[locale]/checkout/confirm/[id]   → order confirmation
/[locale]/account                 → gated (reserved)
```

- `[locale]` is `vi` (default) or `en`. Middleware redirects bare `/` to `/vi`.
- Route groups: `(marketing)`, `(shop)`, `(checkout)`, `(account)` for layout sharing only.
- `loading.tsx`, `error.tsx`, `not-found.tsx` per group.
- `generateStaticParams` for product slugs (SSG at build), `revalidate: 3600` for the rest.

## 2.8 State management strategy

| Concern                            | Mechanism                              | Why                                                  |
| ---------------------------------- | -------------------------------------- | ---------------------------------------------------- |
| Server data (products, FAQ, story) | RSC fetch + Next.js cache              | Free. No client cache needed.                        |
| URL state (filters, sort, query)   | `nuqs` (typed search params)           | Shareable, bookmarkable, SSR-friendly                |
| Cart                               | **Zustand** + `localStorage` persister | Cart survives reloads; tiny store                    |
| Toasts, modals (open/closed)       | Zustand (UI store)                     | Easier than prop drilling, smaller than context-tree |
| Form state                         | `react-hook-form`                      | Uncontrolled = fast on cheap devices                 |
| Async server state (post-API)      | **TanStack Query**                     | Cache, revalidation, optimistic updates              |
| Theme                              | DOM attribute + CSS link toggle        | Not React state. See §2.13.                          |

**We do not use React Context for state.** Context is for _config_ (locale, theme name once read), never for frequently-changing values. Zustand stores are accessed via selectors, which means components re-render only when _their_ slice changes.

### Cart store shape

```ts
// features/cart/store.ts
type CartItem = { sku: string; qty: number; addedAt: number };
type CartStore = {
  items: CartItem[];
  add: (sku: string, qty?: number) => void;
  setQty: (sku: string, qty: number) => void;
  remove: (sku: string) => void;
  clear: () => void;
  // Reserved for backend reconciliation
  syncFromServer: (items: CartItem[]) => void;
};
```

Persisted with `zustand/middleware`'s `persist` to `localStorage` under `cocoshowroom.cart.v1`. Versioned so we can migrate.

## 2.9 API abstraction layer

```ts
// src/lib/api/products.ts — TODAY
import { products } from "@/content/products";
import { ProductSchema } from "./schema";

export async function getProducts(): Promise<Product[]> {
  // Validate even local data — guards against shape drift
  return products.map((p) => ProductSchema.parse(p));
}

// src/lib/api/products.ts — TOMORROW (backend lands)
import { fetcher } from "@/lib/fetcher";
import { ProductSchema } from "./schema";
import { z } from "zod";

export async function getProducts(): Promise<Product[]> {
  return fetcher("/api/products", {
    schema: z.array(ProductSchema),
    next: { revalidate: 3600, tags: ["products"] },
  });
}
```

The **call sites do not change**. Server Components call `await getProducts()` and don't know whether it's coming from disk or the API. Swap is one file.

`src/lib/fetcher.ts` is a thin wrapper around `fetch`:

- Adds auth header from `cookies()` when present
- Parses with the supplied Zod schema (throws on shape mismatch, surfaces to Sentry)
- Wraps errors into a typed `ApiError` (status, code, traceId)
- Uses Next's extended `fetch` cache options

## 2.10 Error handling architecture

| Boundary         | Mechanism                                                                         |
| ---------------- | --------------------------------------------------------------------------------- |
| Per-route        | `app/**/error.tsx` — Client Component, gets `error` and `reset`                   |
| Global           | `app/global-error.tsx` — catches root layout crashes                              |
| Per-feature      | `<ErrorBoundary>` wrapper around risky islands (gallery, theme switcher)          |
| Form validation  | RHF + Zod resolver; field-level errors only                                       |
| API call site    | `try` in Server Components; surface user-facing message; log full error to Sentry |
| Suspense / async | `loading.tsx` skeletons; never a spinner-only state on critical routes            |

User-facing errors are **soft and bilingual**, never "TypeError: x is undefined". The map from internal `ApiError.code` to user message lives in `features/<x>/copy/errors.ts`.

## 2.11 Validation architecture

- **Zod is the source of truth.** Every external boundary (form input, route params, search params, env vars, fetched data) has a Zod schema.
- **Types are inferred, not declared.** `type Product = z.infer<typeof ProductSchema>`.
- **Schemas co-locate with the feature.** `features/catalog/api/schema.ts`.
- **Shared primitives in `lib/schemas/`** (VND amount, phone VN, email, slug, locale).

## 2.12 Environment variable strategy

```ts
// src/lib/env.ts
import { z } from "zod";

const Env = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]),
  NEXT_PUBLIC_SITE_URL: z.string().url(),
  NEXT_PUBLIC_SENTRY_DSN: z.string().url().optional(),
  NEXT_PUBLIC_ANALYTICS_KEY: z.string().optional(),
  // Server-only (no NEXT_PUBLIC_)
  REVALIDATE_TOKEN: z.string().min(32).optional(),
  CONTACT_WEBHOOK_URL: z.string().url().optional(),
});

export const env = Env.parse(process.env);
```

- Validation happens at module load — **a missing required env crashes the build**, not the user's first request.
- `NEXT_PUBLIC_*` is the only client-exposed prefix; CI lint rule forbids referencing any other `process.env.*` in `"use client"` files.
- `.env.example` is checked in; `.env.local` is git-ignored.

## 2.13 Theme system (carrying the prototype's polymorphism forward)

The prototype's theme system is good and we keep it. Three CSS files that redefine the same custom-property layer; exactly one is enabled at a time.

```html
<link rel="stylesheet" href="/styles/theme-royal.css" data-theme-link="royal" />
<link
  rel="stylesheet"
  href="/styles/theme-midnight.css"
  data-theme-link="midnight"
  disabled
/>
<link
  rel="stylesheet"
  href="/styles/theme-ocean.css"
  data-theme-link="ocean"
  disabled
/>
```

A 1.5-kB script reads `localStorage.cocoshowroom.theme`, toggles the `disabled` flags, and mirrors `data-theme="..."` on `<html>` for any CSS that needs to discriminate. This is **not React state.** The theme switch is a DOM mutation and a media swap; no components re-render.

The initial choice is read in a `<script>` injected before paint to avoid theme-flash (the script reads `cookies()` on the server too and sets the correct initial `data-theme` attribute on `<html>`, so SSR renders the right theme).

## 2.14 Configuration strategy

Three layers, in increasing specificity:

1. **Hard defaults in code** — `src/config/defaults.ts`. Things like `DEFAULT_LOCALE`, `PRODUCTS_PER_PAGE`.
2. **Build-time env** — secrets, integration URLs, feature-flag bootstraps.
3. **Per-request overrides** — middleware injects request-scoped config (locale, experiment buckets) into a Server Component-readable `headers()` value.

There is no runtime mutable config. If something needs to change, it changes via a deploy (which is fast — see [§12](./12-deployment.md)).

## 2.15 Tradeoffs and rejected alternatives

| Decision       | Alternative considered | Why rejected                                                                                 |
| -------------- | ---------------------- | -------------------------------------------------------------------------------------------- |
| Tailwind v4    | Vanilla CSS modules    | Slower iteration; loses utility consistency; designers' tokens map cleaner to Tailwind theme |
| Zustand        | Redux Toolkit          | RTK is 3× the bundle for one cart store; we don't need devtools-time-travel for 1 store      |
| TanStack Query | SWR                    | Either works; TQ has better Suspense support and DX is marginally better                     |
| Radix + custom | shadcn pre-installed   | Visual identity is too specific; we'd override 80% anyway                                    |
| next-intl      | next-i18next           | next-i18next is App-Router-hostile; next-intl is RSC-first                                   |
| App Router     | Pages Router           | RSC + streaming + per-segment caching is the entire reason we picked Next 15                 |
| Vercel         | Self-host on Fly.io    | Image opt, edge, preview deploys — Vercel pays for itself until we hit 1M req/mo             |
| pnpm           | npm                    | Disk + speed + strict hoisting prevent phantom deps                                          |

All architecture decisions are logged as ADRs in `docs/ADR/`. See template in [§9](./09-development-workflow.md).
