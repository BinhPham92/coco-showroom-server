# §11 · Monitoring & Observability

> **What we want to know:** is anyone hurt right now? Is the site fast? Is the conversion funnel intact? Three questions, three signals.

## 11.1 Three pillars

| Pillar                | Tool                                           | Question                         |
| --------------------- | ---------------------------------------------- | -------------------------------- |
| Errors                | **Sentry**                                     | Is anyone hurt right now?        |
| RUM (Core Web Vitals) | **Vercel Analytics** + custom → **ClickHouse** | Is the site fast for real users? |
| Product analytics     | **Self-hosted Plausible** or **PostHog Cloud** | Are users doing what we expect?  |

Synthetic monitoring sits alongside: **Checkly** runs from a Singapore PoP every 5 min, exercises the conversion flow, alerts on failure.

## 11.2 Error tracking — Sentry

- Frontend SDK initialized in `instrumentation-client.ts` (Next 15) and `instrumentation.ts` for server.
- Source maps uploaded at build, stripped from public output.
- Release tagged with Git SHA → errors blame the right commit.
- **Sampling**:
  - Errors: 100%.
  - Transactions (perf traces): 10% in production, 100% in preview.
  - Replays: **off in v1**. Privacy + bundle weight cost.
- PII scrubbing: emails, phones, addresses replaced with `[redacted]` before send. Configured at `beforeSend`.
- Breadcrumbs: navigation, console.error, fetch failures. Console.log breadcrumbs disabled in prod.

### Alert routing

| Severity                              | Channel                | SLA                 |
| ------------------------------------- | ---------------------- | ------------------- |
| Unhandled exception, first occurrence | Slack #frontend-alerts | Triage within 4 hrs |
| Error spike (> 10/min for 5 min)      | Pagerduty              | Page on-call        |
| New high-severity error in checkout   | Pagerduty              | Page on-call        |
| Sentry quota > 80%                    | Slack                  | Address same day    |

## 11.3 Performance monitoring — RUM

A 1-kB script (`lib/analytics/vitals.ts`) wires `web-vitals` v4:

```ts
import { onLCP, onINP, onCLS, onTTFB } from "web-vitals/attribution";

function send(metric: Metric) {
  navigator.sendBeacon(
    "/api/vitals",
    JSON.stringify({
      name: metric.name,
      value: metric.value,
      rating: metric.rating,
      delta: metric.delta,
      id: metric.id,
      attribution: metric.attribution,
      page: location.pathname,
      locale: document.documentElement.lang,
      theme: document.documentElement.dataset.theme,
      device: navigator.userAgentData?.mobile ? "mobile" : "desktop",
    }),
  );
}

onLCP(send);
onINP(send);
onCLS(send);
onTTFB(send);
```

`/api/vitals` (edge route) forwards to ClickHouse over HTTPS. Truncates IP, adds geo (country + region), drops UA-CH headers.

Dashboards in Grafana:

- LCP / INP / CLS P75 per route, last 7 / 30 / 90 days
- Conversion funnel TTFB
- Worst-performing routes (P95 LCP)
- Comparison across themes (sanity check)
- Comparison across locales

## 11.4 Product analytics

Events tracked (consent-gated):

| Event              | Properties                    |
| ------------------ | ----------------------------- |
| `page_view`        | path, locale, theme, referrer |
| `product_view`     | productId, category, price    |
| `add_to_cart`      | productId, qty                |
| `remove_from_cart` | productId                     |
| `checkout_start`   | itemCount, subtotal           |
| `checkout_step`    | step (ship / pay / review)    |
| `purchase`         | orderId, total, items         |
| `search`           | query, resultCount            |
| `theme_change`     | from, to                      |
| `locale_change`    | from, to                      |

Implementation:

- **Plausible** (self-hosted) for path-level page views — privacy-friendly, no cookies.
- **PostHog** (cloud, EU region) for funnels and feature flags — opt-in.

We do NOT track: mouse position, scroll heatmaps, individual session replays.

## 11.5 Session replay considerations

Considered. Rejected for v1:

- Privacy cost (cordyceps gift purchase = sensitive context).
- Bundle weight (~30 kB).
- Cost at scale.

Revisit at year 1 if customer service flags repeated unreproducible bugs. If we adopt, it will be Sentry Replay with strict masking (`block` everything by default; allow-list specific elements).

## 11.6 Logging strategy

Frontend logs:

- `console.error` only for _unexpected_ states. Routine errors go through Sentry's `captureException`.
- `console.warn` for dev-only deprecations.
- `console.log` is stripped from production builds (`next.config.mjs` `compiler.removeConsole = true`).

Server logs (route handlers, server components):

- `lib/log.ts` wraps `pino` (fast structured logger).
- Levels: `trace`, `debug`, `info`, `warn`, `error`, `fatal`.
- Pino → stdout → Vercel logs → drained to Better Stack / Datadog (TBD).
- Request correlation: a `traceId` (cuid2) generated in middleware, propagated in `headers()`, surfaced to user on error pages so they can quote it in support.

## 11.7 Alerting strategy

| Signal                                     | Threshold         | Action              |
| ------------------------------------------ | ----------------- | ------------------- |
| LCP P75 > 3s for 10 min on `/`             | warn → Slack      | Investigate         |
| LCP P75 > 4s for 5 min                     | error → Pagerduty | Page                |
| INP P75 > 300 ms for 10 min                | warn → Slack      | Investigate         |
| 5xx rate > 1% for 5 min                    | error → Pagerduty | Page                |
| Checkly synthetic failure 3×               | error → Pagerduty | Page                |
| Sentry new high-severity issue in checkout | error → Pagerduty | Page                |
| CSP violation rate > 0.1/req               | warn → Slack      | Investigate         |
| Bundle size jump on `main`                 | warn → Slack      | Roll back or accept |
| 404 rate on indexed routes > 0.5%          | warn → Slack      | Review redirects    |

Alert fatigue is the enemy. Anything that paged twice and turned out to be noise gets re-tuned, not muted.

## 11.8 SLO / SLI

Pre-launch we define:

| SLI                                                              | Target (SLO)               | Window         |
| ---------------------------------------------------------------- | -------------------------- | -------------- |
| Availability (HTTP success ratio for `/`, `/shop`, `/product/*`) | 99.9%                      | 30-day rolling |
| LCP good rate (P75 ≤ 2.5s)                                       | 90%                        | 30-day rolling |
| INP good rate (P75 ≤ 200 ms)                                     | 90%                        | 30-day rolling |
| Checkout success rate (cart → confirm completion when started)   | TBD; baseline after launch | weekly         |
| Error budget burn rate                                           | < 2× normal                | hourly         |

Error budgets:

- 99.9% availability ⇒ 43.2 min / month allowable downtime.
- If we burn 50% of budget in 7 days, feature work pauses; reliability work begins.

## 11.9 Dashboards (Grafana)

Curated to four:

1. **"Site health right now"** — last 1 h: errors/min, 5xx rate, LCP P75, synthetic status.
2. **"Performance trend"** — 30 days: CWV per route, theme breakdown, locale breakdown.
3. **"Conversion funnel"** — last 30 days: views → product view → add to cart → checkout start → purchase.
4. **"Cost & quota"** — Sentry, Vercel, image opt usage vs. budget.

## 11.10 Privacy in monitoring

- Vitals payload contains no PII (no email, no name, no order ID).
- Sentry `beforeSend` scrubs URLs of identifying query params.
- IPs are truncated server-side before storage.
- Geo is country + region, never city.
- Replays disabled (see §11.5).

## 11.11 Runbooks

Live in `docs/runbooks/`. One per likely alert:

- `lcp-regression.md` — checklist: deploy ID, bundle delta, image optimization status, CDN cache hit rate, etc.
- `5xx-spike.md` — Vercel logs, last deploy, dependency outage check.
- `synthetic-failure.md` — Checkly logs, page status code.
- `csp-violation.md` — Sentry report, find offending resource, decide block-or-allow.
- `secret-rotation.md` — covered in §8.12.

## 11.12 Post-launch monitoring growth

Within 90 days post-launch:

- A11y RUM (track keyboard navigation completions on critical flows).
- Conversion attribution (UTM rollup).
- Anomaly detection on metrics (Grafana ML or Sentry's metric alerts).
- Quarterly chaos test (planned synthetic outage, exercise the runbooks).
