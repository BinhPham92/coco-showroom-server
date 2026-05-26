# §13 · Backend Integration Readiness

> **Premise:** the backend doesn't exist yet, but the frontend is built as if it does. Every external touch goes through a typed boundary. The day the backend lands, swaps are file-by-file, not project-wide.

## 13.1 The API boundary

All data access flows through `src/lib/api/<feature>/`:

```
lib/api/
├── products/
│   ├── get-products.ts          # GET /products
│   ├── get-product-by-slug.ts   # GET /products/:slug
│   ├── schema.ts                # Zod
│   └── __mocks__/
├── cart/
│   ├── reconcile.ts             # POST /cart/reconcile (when auth lands)
│   └── schema.ts
├── orders/
│   ├── create.ts                # POST /orders
│   ├── get.ts                   # GET /orders/:id
│   └── schema.ts
├── newsletter/
│   └── subscribe.ts             # POST /newsletter
└── contact/
    └── submit.ts                # POST /contact
```

Each module exports **only async functions returning Zod-validated values**. Call sites never deal with HTTP directly.

## 13.2 The fetcher

```ts
// lib/fetcher.ts
import { z } from "zod";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    public readonly traceId?: string,
    message?: string,
  ) {
    super(message ?? code);
  }
}

type FetcherOpts<S extends z.ZodTypeAny> = {
  schema: S;
  init?: RequestInit;
  next?: { revalidate?: number | false; tags?: string[] };
};

export async function fetcher<S extends z.ZodTypeAny>(
  path: string,
  { schema, init, next }: FetcherOpts<S>,
): Promise<z.infer<S>> {
  const url = new URL(path, env.API_BASE_URL ?? env.NEXT_PUBLIC_SITE_URL);
  const res = await fetch(url, { ...init, next });
  if (!res.ok) {
    const body = (await res.json().catch(() => ({}))) as Partial<{
      code: string;
      traceId: string;
      message: string;
    }>;
    throw new ApiError(
      res.status,
      body.code ?? "unknown",
      body.traceId,
      body.message,
    );
  }
  const data = await res.json();
  return schema.parse(data); // throws ZodError; surfaced to Sentry
}
```

Every external call:

1. Has a Zod schema.
2. Throws on bad shape → surfaced to Sentry → user sees a calm error page with a `traceId`.
3. Sets Next-cache options (`revalidate`, `tags`).

## 13.3 Today vs. tomorrow

| Function                     | Today (no backend)                                  | Tomorrow (backend lands)                          |
| ---------------------------- | --------------------------------------------------- | ------------------------------------------------- |
| `getProducts()`              | reads `content/products/*.json`, validates with Zod | `fetcher('/products', { schema })`                |
| `getProductBySlug(slug)`     | finds in `content/products`                         | `fetcher('/products/:slug', { schema })`          |
| `submitContact(data)`        | logs to `submissions.log` (dev) / KV (preview)      | `fetcher('/contact', { method: 'POST', schema })` |
| `subscribeNewsletter(email)` | dev log + cookie                                    | `fetcher('/newsletter', { method: 'POST' })`      |
| `getSession()`               | returns `null`                                      | reads cookie → `fetcher('/auth/session')`         |

**Call sites never change.** Only the implementations swap.

## 13.4 REST integration readiness

The frontend assumes a clean REST shape with these conventions:

| Concern     | Convention                                                              |
| ----------- | ----------------------------------------------------------------------- |
| Errors      | JSON body `{ code, message, traceId }`, with HTTP status                |
| Pagination  | Cursor-based: `?cursor=...&limit=...` → `{ items, nextCursor }`         |
| Filtering   | Query params, flat (`?category=fresh&grade=aa`)                         |
| Sorting     | `?sort=price` / `?sort=-price`                                          |
| Auth        | `Authorization: Bearer ...` (when no cookie) or HttpOnly session cookie |
| Versioning  | Path prefix `/v1/` (we hide this inside the fetcher)                    |
| Idempotency | `Idempotency-Key` header on POSTs that create resources (orders)        |
| Trace       | `X-Request-Id` round-tripped                                            |

Documented in `docs/api-contract.md`. The backend team gets this contract before they implement.

## 13.5 GraphQL readiness

The fetcher is GraphQL-agnostic; if the backend chooses GraphQL we add `lib/gql/` with:

- `urql` (light) or **Apollo Client** (heavy) — we'll evaluate at decision time.
- Codegen via `graphql-codegen` from the backend's schema → typed hooks/functions.
- Same Zod re-validation pass (we don't trust the wire schema even when types are generated).

Decision is **TBD** and depends on backend's choice. The architecture supports either without a rewrite.

## 13.6 WebSocket / Realtime readiness

Reserved touch points:

- Inventory updates on PDP (debounce + reconnect logic ready).
- Order status on confirm page.
- Live cart sync across devices (when auth lands).

A `lib/realtime/` module is **not** implemented in v1. Documented placeholder API:

```ts
// hypothetical
const channel = subscribe<InventoryUpdate>("inventory.product." + id, (msg) => {
  cache.setInventory(id, msg.qty);
});
return () => channel.unsubscribe();
```

We expect to use **Pusher** or **Supabase Realtime** depending on backend choice. Sentry breadcrumbs already include a `channel` field placeholder.

## 13.7 Authentication

Auth is **not implemented** in v1. The reserved surface:

```ts
// lib/auth/session.ts
export async function getSession(): Promise<Session | null> {
  // TODAY: return null
  // TOMORROW: read cookie, hit /auth/session, return typed Session
  return null;
}
```

- `app/(account)/account/page.tsx` already calls `await getSession()` and redirects to `/sign-in` if null.
- The "Sign in" link is hidden in the header today but the route exists (returns 404 with a friendly "coming soon").
- `Session` type is fixed at the boundary so component code doesn't change later.

When auth lands, we expect:

- Email + password (with verification)
- Phone OTP (Vietnamese market expectation)
- Optional Google OAuth
- Magic link (low priority)

Provider TBD: **Auth.js** (own infra) vs. **Clerk** (faster, costs) vs. **Supabase Auth** (if Supabase chosen).

## 13.8 RBAC / permissions

We anticipate three roles:

- `guest` — current behavior.
- `customer` — sees `/account/*`.
- `staff` — sees a hidden `/admin/*` (post-v1).

Permissions checked **server-side**. Client checks are advisory only.

A `<Authorize roles="customer">` server component is reserved:

```tsx
// lib/auth/Authorize.tsx
export async function Authorize({ roles, children }: Props) {
  const session = await getSession();
  if (!session || !roles.includes(session.role)) {
    return <ForbiddenFallback />;
  }
  return <>{children}</>;
}
```

## 13.9 File upload

Reserved for: review images (when reviews collect), KYC docs for wholesale accounts.

Pattern: **direct-to-storage** with signed URLs.

1. Client requests signed URL from `/api/uploads/sign`.
2. Backend returns S3/R2-compatible `PUT` URL + asset ID.
3. Client `PUT`s the file with progress tracking.
4. Client posts asset ID with the form.

Library: `react-dropzone` + native `fetch` with `XMLHttpRequest` fallback for progress (until `fetch` streams progress is universal). Lazy-imported.

## 13.10 Feature flags

`features/flags/` will host:

```ts
export async function getFlag(name: FlagName): Promise<boolean> {
  // TODAY: read from env / static config
  // TOMORROW: PostHog SDK / OpenFeature
}
```

- Server-side flags: read in Server Components, deterministic per user (via session ID).
- Client-side flags: hydrated once at session start, do **not** change mid-session (to avoid layout shift).
- Naming: `<area>.<verb>.<noun>` — e.g., `checkout.enable.zalo-pay`.
- Lifecycle: every flag has an ADR + a kill date.

## 13.11 Payments (when backend lands)

Vietnamese context:

- COD: tracked in our backend.
- Bank transfer: shows account + amount + ref; customer marks "paid"; we reconcile.
- VNPay, MoMo, ZaloPay: hosted-redirect flow; we send to provider, return to `/checkout/confirm`.
- Card (Stripe via VietQR or local acquirer): hosted page; we never see card data.

Frontend never handles card numbers. **PCI scope = SAQ A** (hosted iframe / redirect).

## 13.12 Webhooks

Reserved at `app/api/webhooks/[provider]/route.ts`:

- `revalidate` — content updates.
- `inventory` — stock changes.
- `payment-status` — payment confirmations.

Each handler:

- Verifies HMAC signature (provider-specific).
- Idempotent (dedupe on `event-id`).
- Logs to Sentry with `traceId`.
- Acks within 2 s; heavy work deferred to a queue (post-v1).

## 13.13 Migration plan from static → dynamic

When the backend ships, the migration is per-feature:

| Week | Migrate                                       |
| ---- | --------------------------------------------- |
| W1   | `/api/contact` real                           |
| W2   | Products read API (still no mutations)        |
| W3   | Auth (account, sign-in, sign-out)             |
| W4   | Cart reconciliation on login                  |
| W5   | Checkout creates real orders                  |
| W6   | Order status / confirm page reads real        |
| W7+  | Reviews (collect + display), wholesale portal |

Each migration is one PR + one ADR. The frontend stays green throughout — `lib/api/*` is the only changing surface.

## 13.14 Tests during migration

- E2E suite uses MSW handlers that match the _real_ API shape (kept in sync).
- A canary test pings the real API once per CI run to detect drift.
- Schema drift surfaces as a Zod parse failure in Sentry within minutes of deploy.

## 13.15 What we do NOT couple to the backend

- **Theming.** Themes are CSS; no backend involvement.
- **Routing.** Frontend defines URL shape.
- **Validation rules.** Zod schemas in frontend are the source of truth; backend re-validates.
- **i18n catalogs.** Translations live in `lib/i18n/messages/`. CMS-driven copy is _post-v1_.

## 13.16 Anti-patterns

- ❌ Calling `fetch()` directly outside `lib/fetcher.ts` or `lib/api/*`.
- ❌ Sharing types across boundary without re-validating.
- ❌ Branching component code on `backendEnabled` flags.
- ❌ Persisting backend response shapes in the client (always re-derive).
- ❌ Letting the backend dictate the UI (the frontend owns presentation).
