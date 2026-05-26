# §5 · Accessibility

> **Target:** WCAG 2.2 Level AA. We aim for AAA where it's cheap (contrast, focus visibility). Accessibility is not an audit at the end; it's a CI gate from day one.

## 5.1 WCAG goals

| Level | Status                                                 |
| ----- | ------------------------------------------------------ |
| A     | Required                                               |
| AA    | Required (full conformance)                            |
| AAA   | Best-effort (contrast, error identification, headings) |

Conformance audited per release. Axe-core via Playwright fails the build on any violation rated _serious_ or _critical_.

## 5.2 Keyboard navigation

Every interactive element must be reachable and operable via keyboard alone.

- **Tab order matches visual order.** Verified per page with a Playwright sweep.
- **Skip link**: first focusable element is `<a href="#main">Bỏ qua đến nội dung chính / Skip to main content</a>`. Hidden until focused.
- **Focus traps** in modals, drawers, search popovers — Radix handles this for us; we audit it.
- **Escape closes overlays.** Confirmed in E2E.
- **Arrow-key nav** on listbox-style components (filters, gallery, tabs).
- **No keyboard trap** outside intentional modals. Verified with the `tab-cycle` Playwright check.

## 5.3 Focus management

- **Visible focus on every interactive element.** A 2px outline in `--gold-curve` with a 2px offset, themed per theme. Never `outline: none` without a replacement.
- **Focus moves on route change**: focus the new page's `<h1>` after a 100 ms delay (so SR announces the new page). Implemented in a `<RouteAnnouncer>` server-injected, client-mounted component.
- **Focus moves on modal open** to the modal's first focusable element; on close, returns to the trigger.
- **`useId()`** for any aria-controls / labelled-by relationship — no string concatenation.

## 5.4 Screen reader support

| Pattern          | Standard                                                                                                             |
| ---------------- | -------------------------------------------------------------------------------------------------------------------- |
| Landmarks        | `<header>`, `<main>`, `<nav>`, `<footer>` per page. One `<main>` per route.                                          |
| Headings         | One `<h1>` per page (route title). No skipped levels.                                                                |
| Buttons vs links | Buttons trigger action; links navigate. Misuse fails ESLint.                                                         |
| Images           | `alt` mandatory. Decorative images: `alt=""`. Functional images: descriptive. Product images: include grade and SKU. |
| Icons            | All icons are `aria-hidden="true"` unless they are the _only_ label, in which case they get `aria-label`.            |
| Live regions     | Cart count is `aria-live="polite"`. Toasts are `role="status"` / `role="alert"`.                                     |
| Forms            | Every input has a visible `<label>`. Errors are `aria-describedby` and announced.                                    |
| Tables           | Real `<table>` for the order summary; not div soup.                                                                  |
| Modal            | `role="dialog"`, `aria-modal="true"`, labelled by title. Radix gives us this.                                        |

Tested in CI against **NVDA + Firefox** and **VoiceOver + Safari** narration patterns (we don't test live; we test that aria attributes are present and DOM order is correct).

## 5.5 Reduced motion

- `prefers-reduced-motion: reduce` disables: hero entrance animation, product card lift, scroll-triggered fades, theme cross-fade.
- The system is global:
  ```css
  @media (prefers-reduced-motion: reduce) {
    *,
    *::before,
    *::after {
      animation-duration: 0.01ms !important;
      transition-duration: 0.01ms !important;
      scroll-behavior: auto !important;
    }
  }
  ```
- A JS hook `useReducedMotion()` (matchMedia-backed) is also exported for components that conditionally render motion variants (Framer Motion).

## 5.6 Color contrast

All color pairs verified against WCAG **AA for normal text (4.5:1) and AAA for large text (4.5:1)**.

| Theme    | Surface           | Text                         | Ratio                                         |
| -------- | ----------------- | ---------------------------- | --------------------------------------------- |
| Royal    | `#061838` (navy)  | `#FFFFFF`                    | 16.0 ≥ 7.0 (AAA)                              |
| Royal    | `#061838` (navy)  | `#F5C038` (gold)             | 7.8 ≥ 7.0 (AAA)                               |
| Midnight | `#F6F4EE` (cream) | `#061838` (navy ink)         | 15.1 ≥ 7.0 (AAA)                              |
| Midnight | `#F6F4EE`         | `#D9A93B` (gold accent text) | 4.7 ≥ 4.5 (AA) — _body avoided; eyebrow only_ |
| Ocean    | `#F4F7F9` (mist)  | `#102A3F` (deep ocean)       | 14.2 ≥ 7.0 (AAA)                              |
| Ocean    | `#F4F7F9`         | `#4F8FB0` (mid ocean)        | 3.4 — _used only on borders, never text_      |

**The contrast script (`scripts/check-contrast.ts`) re-validates token combinations on every commit.** Adding a token requires updating the matrix.

## 5.7 Responsive accessibility

- **Reflow at 320 px** without horizontal scroll. Tested in CI viewports: 320, 375, 414, 768, 1024, 1280, 1920.
- **Touch targets ≥ 44×44 px** (WCAG 2.5.5). Enforced via CSS audit script that grep's for buttons smaller than that and fails the build.
- **Tap delay**: `touch-action: manipulation` on interactive elements to kill the 300 ms iOS click delay.
- **Pinch-zoom is not blocked.** `<meta name="viewport">` does not include `user-scalable=no`.

## 5.8 Mobile accessibility

- iOS VoiceOver rotor lists all landmarks and headings (verified manually pre-launch).
- Android TalkBack: linear swipe nav works on PDP and checkout.
- Form fields use correct `inputmode`, `autocomplete`, and `type`:
  - Phone: `inputmode="tel" autocomplete="tel"`
  - Email: `inputmode="email" autocomplete="email"`
  - Postal code: `inputmode="numeric" autocomplete="postal-code"`
  - Name: `autocomplete="name"`
- "Done" / "Next" keyboard buttons in Vietnamese keyboards work end-to-end.

## 5.9 Form accessibility

- Every field has a `<label>` (not a placeholder-as-label).
- Required fields marked with `aria-required="true"` AND visible `*`.
- Error messages appear adjacent to the field, are `role="alert"` on first appearance, and are referenced by `aria-describedby`.
- Server-validated errors (after submit) move focus to the first invalid field.
- Success states are announced via `role="status"`.

## 5.10 Bilingual accessibility

- `<html lang="vi">` or `<html lang="en">` per locale, set server-side.
- Mid-paragraph foreign words use `<span lang="en">…</span>` so SR pronunciation is correct.
- Date formats follow locale (`vi-VN`: `dd/MM/yyyy`; `en`: `MMM d, yyyy`).
- Number formats follow locale (`vi-VN`: `1.250.000 ₫`; `en`: `1,250,000 VND`).

## 5.11 Testing

| Test                      | Tool                                      | Frequency                                   |
| ------------------------- | ----------------------------------------- | ------------------------------------------- |
| Automated axe sweep       | `@axe-core/playwright`                    | Every PR                                    |
| Keyboard-only sweep       | Playwright custom (`tab-cycle`)           | Every PR                                    |
| Contrast token matrix     | `scripts/check-contrast.ts`               | Every commit                                |
| Screen reader manual pass | NVDA + VoiceOver                          | Pre-launch + per release for changed routes |
| Mobile SR                 | TalkBack + iOS VO                         | Pre-launch + quarterly                      |
| Reduced motion check      | Playwright preference flag                | Every PR                                    |
| Real-user a11y monitoring | Sentry breadcrumbs for keyboard nav drops | Continuous                                  |

## 5.12 A11y checklist per PR

- [ ] All interactive elements reachable by Tab
- [ ] Focus visible at 2px gold outline
- [ ] Labels on every input
- [ ] Image alts present (or `alt=""` if decorative)
- [ ] Headings sequential (no h1 → h3 jumps)
- [ ] Contrast checked for any new color usage
- [ ] No `outline: none` without a replacement
- [ ] No keyboard trap
- [ ] No motion that ignores `prefers-reduced-motion`
- [ ] axe passes 0 serious / critical

## 5.13 Anti-patterns we will refuse

- ❌ `<div onClick>` for actions (use `<button>`)
- ❌ Placeholder as label
- ❌ Color as the only indicator of state (always pair with icon / text)
- ❌ Auto-focus on page load (steals focus from skip-link)
- ❌ `tabindex` > 0
- ❌ Carousels without pause / prev / next + keyboard nav
- ❌ Disabling pinch-zoom
- ❌ Tooltips that hold critical info (not reachable on touch)
