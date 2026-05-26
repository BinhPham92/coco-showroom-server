# §12 · Deployment

## 12.1 Hosting

**Primary: Vercel.** Reasons:

- Native Next.js (App Router, RSC, streaming, edge) without config gymnastics.
- Image Optimization, font CDN, edge runtime built-in.
- Preview deployments per PR are first-class.
- Singapore PoP serves Vietnam at < 30 ms RTT.

**Disaster recovery option: Cloudflare Pages + Workers.** Documented in `docs/runbooks/dr-failover.md`. Not active; the migration path is kept tested via a quarterly dry run.

## 12.2 CDN strategy

- **Vercel Edge Network** for static + edge functions.
- **Cloudflare in front** (optional, post-launch) for:
  - WAF rules
  - Bot management
  - Aggregated cache stats
  - Independent attack surface (if Vercel falters)

When CF is in front, we route via DNS (CNAME → Vercel), set CF to "DNS only" for the apex initially and graduate to proxied once we're confident in the cache-key behavior.

## 12.3 Edge vs. Node runtime

| Surface                                               | Runtime             | Why                                    |
| ----------------------------------------------------- | ------------------- | -------------------------------------- |
| Middleware (locale redirect, theme cookie, CSP nonce) | Edge                | Sub-10ms TTFB                          |
| Marketing routes (`/`, `/story`, `/support`)          | Edge (Static + ISR) | Free; cacheable                        |
| Shop & PDP                                            | Edge (SSG + ISR)    | Cacheable; massive                     |
| `/api/contact`, `/api/vitals`                         | Edge                | Lightweight                            |
| `/api/checkout/*` (when backend)                      | Node                | DB clients, Stripe SDK needs Node APIs |
| Image opt                                             | Vercel-managed      | Automatic                              |

## 12.4 Cache invalidation

Three handles:

1. **Time-based** — `revalidate: 3600` on most ISR routes. Cheap default.
2. **Tag-based** — `revalidateTag('products')` from a webhook handler.
3. **Path-based** — `revalidatePath('/shop')` for batch invalidations.

Webhook handler (`app/api/revalidate/route.ts`) verifies a shared secret + tag/path payload. Lives behind rate limiting.

When the backend lands:

- Product mutation → fires webhook → `revalidateTag('products')`.
- Inventory delta → `revalidateTag('inventory')` (only the PDP "in stock?" boundary uses this tag; rest stays warm).
- Story / FAQ edits → manual `revalidatePath` via admin button.

## 12.5 Rollback strategy

- **Instant rollback** via Vercel's deployment history. Any prior deployment becomes the "current" production in 1 click / API call.
- The deploy ID is recorded in Sentry releases; rolling back rolls back the release tag.
- After rollback, an `incident-<ts>.md` is opened in `docs/postmortems/` within 24 hrs.

## 12.6 Canary / blue-green considerations

Vercel doesn't expose blue-green natively but supports:

- **Branch-based environments** (we use this for staging).
- **Aliasing** — `prod.cocoshowroom.vn` aliases the current; we could alias a separate "canary" to a percentage of traffic via Cloudflare WAF rules (a small post-launch project).

For v1, our risk profile (low traffic, fast rollback) makes blue-green overkill. We do:

- Staging → 24h soak before any high-risk deploy (header changes, theme system changes).
- "Friday freeze" — no production deploys after 14:00 Friday Saigon time unless it's a fix for a P0.

## 12.7 Deploy workflow

```
PR merge to main
       │
       ▼
GitHub Actions: release.yml
       │
       ├─ pnpm install --frozen-lockfile
       ├─ pnpm typecheck && pnpm lint && pnpm test
       ├─ pnpm build (with telemetry off)
       ├─ size-limit check
       ├─ Sentry release create + sourcemaps upload + sourcemaps deleted from build
       ├─ Vercel deploy --prod
       ├─ Run Lighthouse CI against new prod
       │     └─ if Perf regressed > 5 pt → rollback automatically
       └─ Notify Slack #deploys with summary (bundle delta, lighthouse delta, changeset)
```

Total wall time: ~6 minutes from merge to prod.

## 12.8 Secrets in production

- All secrets stored in Vercel env vars (encrypted at rest, scoped per environment).
- A staging deploy can read staging secrets; a prod deploy reads prod secrets; preview deploys read preview secrets.
- Rotation happens via runbook; CI does **not** modify secrets.

## 12.9 Domains

| Domain                           | Purpose         |
| -------------------------------- | --------------- |
| `cocoshowroom.vn` (apex)         | Production      |
| `www.cocoshowroom.vn`            | 301 → apex      |
| `staging.cocoshowroom.vn`        | Staging         |
| `*.cocoshowroom-pr-*.vercel.app` | Previews (auto) |

DNS: Cloudflare. TXT for domain verification, ALIAS/ANAME for apex (Vercel-friendly).

## 12.10 SSL / TLS

- Managed certificates via Vercel (Let's Encrypt-backed).
- TLS 1.2 minimum; TLS 1.3 preferred.
- HSTS preload submitted to chromium.org preload list (apex + subdomains).
- OCSP stapling enabled (Vercel default).

## 12.11 Maintenance mode

If we need to take prod offline (incident, migration):

- A pre-built static `maintenance.html` is deployed with `Cache-Control: no-store`.
- Vercel rewrite rule routes all traffic to it (toggled via env var).
- The site continues serving cached HTML for 5–10 min anyway, gracefully degrading.

## 12.12 Backups

Frontend has no DB. The "backup" surface:

- Source: GitHub (already mirrored).
- Build artifacts: Vercel keeps last 100 deploys.
- Sentry data: 90-day retention.
- ClickHouse RUM: hosted, with automated backups.
- Content (products, story, FAQ): in-repo JSON / MDX → versioned with code.

## 12.13 Deployment checklist (per release)

- [ ] Changesets aggregated and version bumped
- [ ] CHANGELOG.md updated
- [ ] All CI gates green
- [ ] Sentry release created
- [ ] Sourcemaps uploaded, then stripped from public output
- [ ] Lighthouse CI on staging shows no regression
- [ ] E2E pass on staging
- [ ] Visual regression approved
- [ ] Slack #deploys notified pre-deploy
- [ ] Post-deploy: Lighthouse on prod confirms no regression
- [ ] Post-deploy: synthetic monitor green for 15 min
- [ ] No new alerts firing
- [ ] Smoke test on real Android + real iPhone

## 12.14 Cost ceiling

Pre-launch budget (monthly):

- Vercel Pro: ~$20
- Sentry Team: ~$26
- ClickHouse Cloud (small): ~$30
- Checkly Hobby: ~$10
- Plausible self-hosted (Hetzner): ~$5
- **Total: < $100 / mo**

Scaling alerts:

- Image opt > 5k transformations/day → review caching
- Vercel function invocations > 2M/mo → review middleware/edge usage
- Bandwidth > 100 GB / mo → review cache hit rate
