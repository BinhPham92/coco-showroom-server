# §7 · UI/UX Engineering Standards

> **Reference:** the prototype's three themes (Royal, Midnight, Ocean) and the Đông Trùng Hạ Thảo label. The design system carries the prototype's vocabulary into engineering primitives.

## 7.1 Design system strategy

A **token-driven** system where:

- Raw tokens (`--royal-700: #1149B8`) live in `tokens.css`.
- Semantic tokens (`--bg-elevated: var(--royal-700)`) live in `tokens.css`.
- Theme files redefine only the **raw layer**; semantic tokens stay constant.

This is what lets the polymorphic theme work. The DOM, classes, and component code never change between themes — only the underlying RGB values do.

## 7.2 Token hierarchy

```css
/* tokens.css — RAW */
:root {
  --paper-50: #fbf7f2;
  --paper-100: #f5eee4;
  --royal-700: #1149b8;
  --gold-500: #f5c038;
  --ink-700: #2d241a;
  /* ... */
}

/* tokens.css — SEMANTIC (constant across themes) */
:root {
  --bg: var(--paper-50);
  --bg-elevated: #ffffff;
  --bg-sunken: var(--paper-100);
  --fg1: var(--ink-700);
  --fg2: var(--ink-500);
  --border: var(--ink-200);
  --accent: var(--royal-700);
  --accent-fg: #ffffff;
  --price: var(--ink-900);
  --price-sale: var(--coral-curve);
  /* ... */
}

/* theme-royal.css — REWRITES RAW ONLY */
:root[data-theme="royal"] {
  --paper-50: #f7fafe;
  --royal-700: #1149b8;
  /* etc */
}
```

Components reference **semantic** tokens. Themes touch **raw** values. This is the contract.

## 7.3 Typography

| Role                                                 | Family         | Weight           | Use                                          |
| ---------------------------------------------------- | -------------- | ---------------- | -------------------------------------------- |
| Display / headings (`h1`-`h3`, `.wordmark`, `.pull`) | Lora           | 400 italic / 500 | Editorial moments, brand voice               |
| Body / UI (`p`, buttons, inputs, `h4`-`h5`)          | Be Vietnam Pro | 300/400/500/600  | All UI; full Vietnamese diacritics           |
| Mono / data                                          | JetBrains Mono | 400              | Codes, IDs, eyebrow labels, prices in tables |

Type scale (modular, 1.250 ratio, mobile-first):

| Token               | Mobile | Desktop |
| ------------------- | ------ | ------- |
| `--text-2xs`        | 11px   | 11px    |
| `--text-xs`         | 12px   | 13px    |
| `--text-sm`         | 14px   | 14px    |
| `--text-md`         | 16px   | 16px    |
| `--text-lg`         | 18px   | 20px    |
| `--text-xl`         | 22px   | 24px    |
| `--text-display-sm` | 28px   | 32px    |
| `--text-display-md` | 36px   | 48px    |
| `--text-display-lg` | 48px   | 64px    |
| `--text-display-xl` | 56px   | 80px    |

Line height: `--leading-tight: 1.15` (headings), `--leading-normal: 1.55` (body), `--leading-loose: 1.7` (long-form story).

## 7.4 Spacing system

8-pt grid with half-step at 4. Tokens:

| Token        | Value |
| ------------ | ----- |
| `--space-1`  | 4px   |
| `--space-2`  | 8px   |
| `--space-3`  | 12px  |
| `--space-4`  | 16px  |
| `--space-5`  | 24px  |
| `--space-6`  | 32px  |
| `--space-7`  | 48px  |
| `--space-8`  | 64px  |
| `--space-9`  | 96px  |
| `--space-10` | 128px |

Mapped to Tailwind's spacing via `tailwind.config.ts` `extend.spacing`. We do not use raw `m-[7px]` arbitrary values in committed code (linted).

## 7.5 Responsive breakpoints

Mobile-first; min-width.

| Token | Min width | Use                       |
| ----- | --------- | ------------------------- |
| `sm`  | 480px     | Larger phones             |
| `md`  | 768px     | Tablets, landscape phones |
| `lg`  | 1024px    | Small laptops             |
| `xl`  | 1280px    | Desktops                  |
| `2xl` | 1536px    | Wide                      |

Most components reach their "final" layout at `md`; `lg`+ is refinement.

## 7.6 Component standards

### API rules

1. **Polymorphic via Radix Slot.** `<Button asChild><Link href>...</Link></Button>` — not 200 button-as-link props.
2. **`className` accepted last** and merged via `clsx`/`cn` so consumers can layer.
3. **`forwardRef` always.** Refs flow through every primitive.
4. **No prop drilling for theme.** Theme is CSS; components read tokens.
5. **One variant API: `cva`.** Used for primitives only; patterns/features compose primitives.

```tsx
// ui/primitives/Button.tsx
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";

const button = cva(
  "inline-flex items-center justify-center font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--gold-curve)] disabled:opacity-50 disabled:pointer-events-none",
  {
    variants: {
      variant: {
        primary: "bg-[var(--accent)] text-[var(--accent-fg)] hover:opacity-90",
        secondary:
          "border border-[var(--fg1)] text-[var(--fg1)] hover:bg-[var(--fg1)] hover:text-[var(--bg)]",
        ghost:
          "bg-[var(--bg-sunken)] text-[var(--fg1)] hover:bg-[var(--bg-hover)]",
        link: "underline-offset-4 hover:underline text-[var(--accent)]",
      },
      size: {
        sm: "h-9 px-4 text-sm rounded-sm",
        md: "h-11 px-6 text-md rounded-sm",
        lg: "h-14 px-8 text-lg rounded-sm",
      },
    },
    defaultVariants: { variant: "primary", size: "md" },
  },
);

type Props = React.ButtonHTMLAttributes<HTMLButtonElement> &
  VariantProps<typeof button> & { asChild?: boolean };

export const Button = React.forwardRef<HTMLButtonElement, Props>(
  ({ className, variant, size, asChild, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return (
      <Comp
        ref={ref}
        className={cn(button({ variant, size }), className)}
        {...props}
      />
    );
  },
);
Button.displayName = "Button";
```

## 7.7 Animation guidelines

| Use                               | Duration | Easing                                                  |
| --------------------------------- | -------- | ------------------------------------------------------- |
| State change (hover, focus)       | 160 ms   | `ease-out`                                              |
| Layout transition (drawer, modal) | 240 ms   | `cubic-bezier(0.16, 1, 0.3, 1)` (custom — "smooth out") |
| Page transition                   | 200 ms   | `ease-out` (opacity only)                               |
| Hero entrance                     | 480 ms   | `cubic-bezier(0.22, 1, 0.36, 1)`                        |
| Theme cross-fade                  | 240 ms   | `ease`                                                  |

- **GPU properties only**: `transform`, `opacity`.
- **`prefers-reduced-motion`** disables every non-essential animation.
- Framer Motion is **lazy-loaded** on the hero / story routes only.

## 7.8 Skeleton loading

- Every async island has a skeleton matching its real layout (same heights, same column counts).
- Skeletons pulse via a single `@keyframes shimmer` (transform: translateX(-100% → 100%) on a linear-gradient mask).
- Skeletons disappear in < 100 ms once data arrives (no awkward double-flash).
- We **don't show spinners** on data-driven routes; skeletons are the standard.
- Spinners are reserved for: form submit in-flight; small async buttons.

## 7.9 Empty states

Every list / grid that can be empty has an `<EmptyState>`:

```tsx
<EmptyState
  illustration="basket"
  title={t("cart.empty.title")}
  body={t("cart.empty.body")}
  action={<Button href={routes.shop}>{t("cart.empty.cta")}</Button>}
/>
```

Illustrations are SVG, design-system colored, ≤ 8 kB each. Located in `ui/illustrations/`.

## 7.10 Error states

Three tiers:

1. **Inline (field-level)**: a 13px caption in `--state-error`, with `role="alert"`.
2. **Section-level**: an `<ErrorBoundary>` fallback with retry. Calm copy, no stack traces.
3. **Route-level**: `error.tsx` — full-page calm fallback with home / contact links.

All error UI is theme-aware (the navy/cream/mist surface holds).

## 7.11 Dark mode strategy

We **don't ship a "dark mode" as a separate toggle**. We ship three themes, two of which are dark-leaning (Royal, Ocean) and one cream (Midnight). The user picks their preferred theme; we don't autodetect `prefers-color-scheme` (the brand designed the themes deliberately, not as auto-derived).

Where a system-dark cue _should_ be respected: the favicon / OG image (we serve a dark-mode favicon variant via `<link media="(prefers-color-scheme: dark)">`).

## 7.12 Theming strategy (engineering view)

- Theme name persists in `localStorage` + a cookie (so server-render uses the right initial value).
- Server reads cookie → sets `<html data-theme="...">` and matches `<link disabled>` flags during SSR.
- Client-side switch flips `<link disabled>` + cookie + localStorage.
- No theme-aware components branch on `theme === 'royal'`. **Everything is a token.**

If a component truly needs to behave differently per theme (e.g., the brand-mark image needs to invert on Royal), it does so via `:root[data-theme="royal"] .brand img { filter: invert(1); }`, not via JS.

## 7.13 Iconography

- One icon set: **Lucide** (tree-shakeable, MIT, dense coverage).
- Icons sized via `width` / `height` (never `font-size`).
- Icons are SVG; never font-icons.
- Decorative icons: `aria-hidden="true"`.
- Functional icons (icon-only buttons): require `aria-label`.

## 7.14 Imagery standards

- All hero / product imagery: shot or licensed. **No stock photo clichés** (smiling models in spice markets, etc.).
- Aspect ratios standardized: 1:1 (product), 4:5 (PDP gallery), 16:9 (story / hero), 21:9 (story banners).
- Color treatment: warm-toned across themes; avoid magenta/cyan extremes that fight the palette.

## 7.15 Voice & tone

Vietnamese (primary):

- Polite but warm. Uses "mình" / "bạn" (informal-respectful) for direct address.
- Avoids "anh / chị" honorifics in UI copy (too personal for a brand).
- No exclamation marks in body copy.
- Numerals always Arabic with `.` thousand-separators.

English (secondary):

- American spelling.
- Sentence case in UI labels (`Add to cart`, not `ADD TO CART` or `Add To Cart`).
- "We" never "I"; the brand speaks plural.

## 7.16 Pattern catalog (committed UI)

| Pattern       | Location                      | Notes                                           |
| ------------- | ----------------------------- | ----------------------------------------------- |
| ProductCard   | `ui/patterns/ProductCard.tsx` | Server-safe; client wrapper for favorite toggle |
| SectionHead   | `ui/patterns/SectionHead.tsx` | `eyebrow / h2 / more-link` group                |
| NavBar        | `ui/patterns/NavBar.tsx`      | Header with brand + nav + search + cart         |
| Footer        | `ui/patterns/Footer.tsx`      | Four-col grid → single col mobile               |
| BackLink      | `ui/patterns/BackLink.tsx`    | Style mirrored from prototype back button       |
| Breadcrumb    | `ui/patterns/Breadcrumb.tsx`  | Schema.org BreadcrumbList                       |
| PriceBlock    | `ui/patterns/PriceBlock.tsx`  | Now / was / per-gram                            |
| ThemeSwitcher | `features/theme-switcher/`    | Floating panel, polymorphic                     |

## 7.17 Anti-patterns

- ❌ One-off colors. Every color is a token.
- ❌ Magic numbers in `className="mt-[13px]"` outside design exceptions.
- ❌ `style={{}}` for layout (use Tailwind / tokens). `style` is for runtime-computed values only (e.g., position from drag).
- ❌ Component prop explosion. Compose, don't configure.
- ❌ Theme-specific component variants. Theme = CSS.
- ❌ Mixing `gap` and `margin` in the same flex/grid container.
