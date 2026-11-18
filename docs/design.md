# OpenSourceContributor — Design Specification

**Status:** Approved visual direction
**Reference:** Run Out Records-inspired design prompt
**Approved hero:** FIND WHERE YOU FIT. CONTRIBUTE.

## 1. Design intent

Create a bold, packed, genuinely dark developer platform with the energy of an independent record store and the clarity of a technical catalogue.

The design must be visually distinct from FirstIssue.dev and from the earlier Kestrel concept. Use the Run Out Records reference as inspiration for visual principles, not as a source to copy branding, wording, or assets.

## 2. Colour tokens

```css
:root {
  --paper: #0D0D0D;
  --ink: #F1ECE2;
  --surface: #1A1A1A;
  --muted: #A7A29A;
  --line: rgba(241,236,226,0.18);

  --label-orange: #F06522;
  --label-yellow: #F4C13D;
  --label-blue: #5B86F2;
  --label-pink: #F29BB8;

  --on-label: #0D0D0D;
}
```

Use flat accent fills. Use near-black text on accent-colour surfaces. Maintain accessible contrast and do not make metadata unreadably small.

## 3. Typography

Google Fonts:
- Anton 400 — display headlines.
- Archivo 400/500/700 — body, navigation, controls, issue titles.
- JetBrains Mono 400/500 — metadata, labels, counts, timestamps, technical details.

Suggested scale:
- Display: `clamp(64px, 12vw, 190px)`, line-height around `0.88`.
- Section headings: `clamp(44px, 6vw, 88px)`, line-height around `0.92`.
- Body: 17px, line-height 1.55.
- Issue titles: 15–18px.
- Metadata: minimum 14px.

## 4. Shape and surface

- Sharp corners; 0px radius for panels, buttons, and cards.
- No pill buttons.
- Thin low-opacity borders.
- Flat dark surfaces.
- No glassmorphism, glowing borders, purple gradients, or excessive shadows.
- Use dense structured rows, coloured blocks, and typography to create hierarchy.

## 5. Buttons

Primary:
- Orange background.
- Near-black text.
- Archivo bold uppercase.
- Sharp corners.
- Hover swaps orange to mustard; no bouncing or scaling.

Secondary:
- Off-white background and near-black text, or a restrained dark secondary style as appropriate.
- Sharp corners.
- Hover may use pink as a flat fill.

Text links:
- Archivo.
- Clear hover/focus states.
- No decorative arrow required unless useful.

## 6. Approved homepage copy

Announcement strip:
`OPEN SOURCE DISCOVERY · REAL GITHUB ISSUES · BUILT FOR DEVELOPERS`

Hero headline:
`FIND WHERE YOU FIT. CONTRIBUTE.`

Hero description:
"Discover open-source projects and real GitHub issues that match your skills, interests and experience. Explore opportunities across technologies and contribute to projects that matter."

Primary action:
`EXPLORE ISSUES`

Secondary action:
`HOW IT WORKS`

Search placeholder:
"Describe your skills, interests or the kind of open-source project you want to contribute to..."

## 7. Technology-agnostic examples

Use diverse prompts, not only Java:
- Find beginner-friendly Python issues in machine learning projects.
- Show me React and TypeScript frontend issues.
- Find Java Spring Boot backend opportunities.
- Explore Go projects with API and database issues.
- Find documentation and accessibility improvements.
- Show me good first issues across different programming languages.

Technology examples may include Java, Python, JavaScript, TypeScript, React, Angular, Vue, Node.js, Spring Boot, Django, Go, Rust, PHP, Ruby, PostgreSQL, MongoDB, Docker, and Kubernetes.

Do not imply that every technology is already supported until implementation verifies it.

## 8. Homepage composition

- Mustard announcement strip.
- Sticky dark navbar.
- Large typography-led hero.
- Search console with example prompts.
- Technical metadata strip.
- Dense issue explorer preview with issue list and selected detail.
- Three-step discovery explanation.
- Full-width coloured technology bars.
- Matching principles.
- Contribution journey.
- Orange closing CTA.
- Minimal footer.

The issue explorer must be a product-focused element, not a decorative screenshot.

## 9. Page-specific visual notes

- Explore Issues: dense catalogue/table rows, filter toolbar, selected issue detail panel.
- Issue Details: clear source metadata, issue content, match explanation, GitHub action.
- Login: editorial two-column layout; GitHub is the only sign-in method.
- Dashboard: contribution desk with real user data or honest empty states.
- Saved Issues: catalogue-style saved list.
- My Contributions: user-managed status labels and contribution records.
- Settings: compact structured forms.
- Legal pages: readable document layout with clear hierarchy.
- 404: bold editorial message, not a generic illustration.

## 10. Responsive rules

Desktop:
- Full-width sections within a consistent maximum content width.
- Dense lists and side-by-side panels where useful.
- Large display typography.

Tablet:
- Reduce display scale.
- Allow metadata to wrap.
- Collapse panels where needed.

Mobile under 768px:
- Hero around 64–72px where space allows.
- Search controls stack.
- Filters use a drawer or compact panel.
- Issue details become a dedicated view.
- Technology bars use smaller display text.
- Full-screen mobile navigation.
- No horizontal page overflow.
- Comfortable touch targets.
- All interactions work without hover.

## 11. Motion

- Hero lines may slide in with short staggered timing.
- Issue rows may fade in subtly.
- Coloured bars may shift text a few pixels on hover.
- Buttons use colour transitions, not scaling.
- Respect `prefers-reduced-motion`.
- Avoid continuous animation.

## 12. Content integrity

- Do not invent live issue counts or indexed repository totals.
- Do not invent testimonials or customer logos.
- Mark mock issue data as illustrative.
- Do not display unsupported parser/token claims such as “TOKENS: UNLIMITED”.
- Use honest interface metadata such as `MODE: NATURAL LANGUAGE SEARCH` and `SOURCE: GITHUB API` only when accurate.
