# §4 · Security

> **Stance:** the frontend is treated as untrusted territory. Even before the backend exists, we lock down what a hostile network, a malicious extension, or a compromised dependency can do.

## 4.1 Threat model

| Actor                                | Capability                       | Mitigation owner                                      |
| ------------------------------------ | -------------------------------- | ----------------------------------------------------- |
| Hostile network (public Wi-Fi)       | Read/inject HTTP                 | HTTPS-only, HSTS, secure cookies                      |
| Malicious extension                  | Read DOM, intercept fetch        | No secrets in DOM; tokens in HttpOnly cookies         |
| Compromised dependency               | Run arbitrary code               | Lockfile, audit, CI npm provenance, CSP               |
| XSS via user input (reviews, future) | Inject script                    | DOMPurify + Zod + escaped rendering                   |
| CSRF                                 | State change from another origin | SameSite=Lax cookies + double-submit token on POSTs   |
| Bot / scraper                        | Scrape, abuse forms              | Rate limiting at edge, Turnstile on forms             |
| Curious user                         | Inspect prod, find secrets       | No secrets in `NEXT_PUBLIC_*`; source maps not public |

## 4.2 Content Security Policy

Strict, per-environment, **nonce-based**.

```http
Content-Security-Policy:
  default-src 'self';
  script-src 'self' 'nonce-{nonce}' 'strict-dynamic';
  style-src 'self' 'unsafe-inline';            /* tightened post-launch */
  img-src 'self' data: https://cdn.cocoshowroom.vn;
  font-src 'self';
  connect-src 'self' https://*.sentry.io https://vitals.vercel-insights.com;
  frame-ancestors 'none';
  base-uri 'self';
  form-action 'self';
  object-src 'none';
  upgrade-insecure-requests;
  report-uri https://o0.ingest.sentry.io/api/.../security/?sentry_key=...;
```

- Nonces generated per request in middleware; passed to `<Script>` via Next's official mechanism.
- `'unsafe-inline'` on `style-src` is a temporary concession to Tailwind's runtime-injected styles in dev. **Production tightens this** with hashed style blocks (Next handles).
- CSP violations report to Sentry.

## 4.3 Security headers

| Header                         | Value                                                          |
| ------------------------------ | -------------------------------------------------------------- |
| `Strict-Transport-Security`    | `max-age=63072000; includeSubDomains; preload`                 |
| `X-Content-Type-Options`       | `nosniff`                                                      |
| `X-Frame-Options`              | `DENY` (also via CSP `frame-ancestors`)                        |
| `Referrer-Policy`              | `strict-origin-when-cross-origin`                              |
| `Permissions-Policy`           | `camera=(), microphone=(), geolocation=(), interest-cohort=()` |
| `Cross-Origin-Opener-Policy`   | `same-origin`                                                  |
| `Cross-Origin-Resource-Policy` | `same-origin`                                                  |
| `Cross-Origin-Embedder-Policy` | `credentialless`                                               |

All set in `next.config.mjs` `headers()` and verified in CI (Playwright `page.request.head()`).

## 4.4 XSS prevention

- React's escaping by default. **No `dangerouslySetInnerHTML` without a DOMPurify pass.** ESLint rule (`react/no-danger`) is error-level.
- User-supplied content (reviews, when added) is sanitized server-side AND escaped at render.
- Markdown content (story, FAQ) is parsed with `@mdx-js/mdx` + `rehype-sanitize`, strict schema.
- `eval`, `new Function`, and dynamic script injection are forbidden — `no-new-func` and `no-eval` lint errors.

## 4.5 CSRF

Until auth lands, no state-changing requests carry credentials. Forms POST to `/api/*`:

- `SameSite=Lax` is default for any cookie we set.
- For POSTs, a **double-submit token** (random value in a cookie + same value in form body) is verified server-side. Token is regenerated per session.
- **No GET requests perform side effects.** Period.

When auth lands, the token rotates per session and a `X-CSRF` header is added for SPA-style fetches.

## 4.6 Token & session handling

Reserved for when auth lands:

- Session tokens in `HttpOnly`, `Secure`, `SameSite=Lax` cookies.
- Access token short-lived (15 min); refresh in a separate, HttpOnly cookie with stricter `SameSite=Strict` and a path scoped to `/api/auth/*`.
- **No tokens in `localStorage` or `sessionStorage`.** Ever.
- `crypto.randomUUID()` for client-generated IDs; nothing security-sensitive.

## 4.7 Secure storage

- `localStorage` is used **only** for cart and theme. Both versioned; both non-sensitive.
- A migration utility (`lib/cart/migrate.ts`) handles `cart.v1` → `cart.v2` so we never crash on schema drift.
- No PII in storage. Ever.

## 4.8 Dependency security

| Control                    | Tool                                                              |
| -------------------------- | ----------------------------------------------------------------- |
| Lockfile pinned, committed | pnpm-lock.yaml                                                    |
| Vulnerability scan         | `pnpm audit` in CI; Dependabot weekly                             |
| Supply chain               | `pnpm install --frozen-lockfile --ignore-scripts=true` in CI      |
| Provenance                 | npm package provenance verified for our own publishes             |
| Auto-merge                 | Patch updates auto-merge after CI green; minor/major are reviewed |
| SBOM                       | CycloneDX SBOM generated per release, attached to GitHub Release  |

Known-bad packages are blocked via `pnpm.overrides`. Postinstall scripts are disabled by default (`pnpm config set enable-scripts=false`) and only allow-listed per package.

## 4.9 Input sanitization

Every input → Zod schema → typed value. Schemas live next to the form. **No `as any` in form code** — ESLint error.

Phone validation: `+84` or `0` prefix, then 9 digits. Server re-validates (same Zod schema reused).
Email validation: Zod default plus a length cap (320 chars per RFC).
Free-text fields (note, address): 500-char cap, trimmed, control characters stripped.

## 4.10 Rate limiting

Edge middleware on `/api/*`:

| Endpoint                     | Limit                                               |
| ---------------------------- | --------------------------------------------------- |
| `/api/contact`               | 5 / 10 min / IP                                     |
| `/api/newsletter`            | 3 / 10 min / IP                                     |
| `/api/cart/*` (when backend) | 60 / min / session                                  |
| `/api/auth/*` (when backend) | 5 / min / IP for login; 3 / hour for password reset |

Implementation: Vercel KV (Upstash) sliding window. Identified by IP + (optionally) session cookie. **429 returned with `Retry-After`** — never silent.

## 4.11 Bot / abuse mitigation

- Cloudflare Turnstile on `/contact`, `/newsletter` — invisible challenge, fall back to interactive.
- Honeypot field on forms (a `<input name="company">` hidden via CSS that real users don't see; bots fill it).
- Submissions with both a honeypot value AND a < 2 s submission time are dropped silently.

## 4.12 SSR-specific risks

- **No `headers()` or `cookies()` value reflected back into HTML** without escaping. The default React escaping covers us; we lint for template strings that interpolate request headers into JSX (banned via custom ESLint rule).
- **`searchParams` are validated** at the route boundary with Zod. Untyped read of `searchParams.q` is forbidden — lint rule.
- **Server Actions** (Next 15) get the same CSRF + Zod treatment.

## 4.13 Environment variable security

| Rule                                                     | Why                                         |
| -------------------------------------------------------- | ------------------------------------------- |
| Only `NEXT_PUBLIC_*` reaches the client                  | Anything else stays server-side             |
| `.env.example` is checked in with _names only_           | New devs know what's needed                 |
| Real secrets live in Vercel env vars (encrypted at rest) | Not in git, not in CI logs                  |
| Secrets are loaded once at module init (Zod-parsed)      | Failed parse blocks the build, not the user |
| CI lint rule blocks `process.env.X` outside `lib/env.ts` | One choke point for env access              |

## 4.14 Source map strategy

- **Source maps uploaded to Sentry only**; not served publicly.
- `productionBrowserSourceMaps: false` in `next.config.mjs`.
- CI step: upload sourcemaps to Sentry, then delete from build output before deploy.

## 4.15 Error leakage prevention

- Production error pages show _user-facing_ messages only.
- Stack traces, internal IDs, and DB error strings never reach the browser.
- Server logs and Sentry get the full context; the user gets a `traceId` they can quote in support tickets.
- `error.tsx` catches unhandled errors per segment and renders a calm fallback.

## 4.16 Analytics & privacy

- **No analytics until consent** for EU/EEA visitors (geolocated; explicit "Accept" gates loading).
- Vietnamese visitors get an informational banner but analytics loads by default (per local norm), with an opt-out.
- We collect: page paths, web-vitals, route timings. **We do not collect: cursor position, scroll heatmaps, session replays** for v1.
- All analytics use `navigator.sendBeacon` to avoid blocking unload.
- IPs are truncated server-side before storage (`/24` for IPv4, `/64` for IPv6).
- No cross-site cookies. First-party only.

## 4.17 Privacy

- Vietnamese law (Decree 13/2023) requires consent for personal data processing. Forms include explicit consent language.
- We name the data controller in the privacy policy.
- A `dataRequests@cocoshowroom.vn` mailbox is published for SAR/erasure.
- Cookie consent UI is component-driven (`features/consent/`), region-aware (EU vs. VN UX), stores choice in a first-party cookie with explicit expiry (12 months max).

## 4.18 Security checklist (per release)

- [ ] CSP unchanged or tightened (no new `unsafe-*`)
- [ ] No new `dangerouslySetInnerHTML`
- [ ] No new package without a security review
- [ ] `pnpm audit --prod` clean for High/Critical
- [ ] All new forms have Turnstile + honeypot + Zod
- [ ] All new env vars are in `lib/env.ts` schema
- [ ] No new third-party script without a CSP `connect-src` review
- [ ] Headers (HSTS, COOP/COEP, Permissions-Policy) snapshot test passes

See [appendix](./appendix-checklists.md) for the launch-time security gate.
