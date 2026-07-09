# Xuan ERP Frontend Design

## Context

Xuan ERP currently has backend modules and an Astro documentation site, but no runnable business frontend. The new frontend will be created under `D:\xuan-erp\frontend` as a standalone Vue 3 application.

The user approved the first preview direction on 2026-07-09:

- Login page follows `https://careercompassai.vercel.app/login` closely.
- The left login illustration uses several simple geometric people whose eyes follow the mouse.
- The login page must connect to the real backend login contract, not a mock flow.
- Backend app pages follow the inventory/ERP admin layout shown in the provided screenshot: left menu, top breadcrumb/tools, tab strip, dense query toolbar, and table-first work area.
- The frontend supports language switching, theme color switching, and a dedicated component management page.

This work does not change database schema, indexes, constraints, seed data, or Flyway migrations.

## Goals

Build the first runnable Xuan ERP frontend shell:

- Vue 3 project in `frontend`.
- Real login request to `POST /api/iam/auth/login`.
- Current-user request to `GET /api/iam/auth/current-user`.
- Admin layout matching the approved ERP screenshot style.
- Language switch between Simplified Chinese and English.
- Theme color switch with persisted local preference.
- Component management page that showcases reusable project UI patterns.
- Dashboard and product management preview pages to validate navigation and table workflows.

## Non-Goals

- No backend API changes.
- No database or Flyway migration changes.
- No public sign-up flow.
- No Google login integration.
- No password reset integration.
- No full product CRUD integration in the first shell.
- No generated menu/permission seed data in this slice.

## Technology

- Vue 3
- Vite
- TypeScript
- Vue Router
- Pinia
- Axios
- Element Plus
- Vue I18n
- lucide-vue-next for icon buttons

The project should keep frontend dependencies isolated under `frontend/package.json`.

## Confirmed Backend Contract

Login goes through IAM or Gateway with this request shape:

```http
POST /api/iam/auth/login
Content-Type: application/json
```

```json
{
  "tenantId": 0,
  "username": "super_admin",
  "password": "password"
}
```

After login, the frontend calls:

```http
GET /api/iam/auth/current-user
Authorization: Bearer <accessToken>
```

The API base URL is configured by `VITE_API_BASE_URL`. The default development value should point to Gateway, for example `http://localhost:8100`.

## Login Page Design

The login page should visually match the CareerCompass login page, adapted to Xuan ERP:

- Full viewport two-column layout on desktop.
- Left column hidden on smaller screens.
- Left column uses gray gradient background with subtle grid texture.
- Top-left brand row shows the Xuan ERP logo and name.
- Center-left animated geometric people illustration.
- The people illustration reacts to mouse movement by moving pupils and face details toward the cursor.
- Bottom-left has legal/help links, but these can remain non-navigating placeholders in the first shell.
- Right column centers a `420px` wide form.
- Form title: `Welcome back!`
- Subtitle can be localized.
- Rounded pill inputs for `tenantId`, `username`, and `password`.
- Password field has a visibility toggle.
- Remember option stores tenant/user preference locally.
- The forgotten-password and sign-up concepts from CareerCompass are replaced with backend-accurate content:
  - `联系管理员`
  - `账号由租户管理员开通`
- A secondary pill button may show the backend login target, but must not imply a separate auth provider.

## Application Shell Design

After login, the app uses an ERP admin shell:

- Fixed left sidebar with project logo, search box, and nested menu groups.
- Top bar with breadcrumb, tenant display, language switch, theme color switch, user entry, and logout.
- Tab strip for visited pages.
- Content area optimized for dense business pages, not marketing cards.
- Primary business page pattern: page title, query toolbar, table, pagination, row actions.

Initial routes:

- `/login`: login page.
- `/`: redirect to dashboard when authenticated.
- `/dashboard`: dashboard preview.
- `/components`: component management page.
- `/inventory/products`: product management page.

## Component Management Page

The component management page is a project component center. It should demonstrate real reusable frontend building blocks:

- Query form pattern.
- Table toolbar pattern.
- Status tag pattern.
- Permission button pattern.
- Modal or drawer form pattern.
- Theme and language preview controls.

The page should be useful as a development reference for later business pages.

## State And Data Flow

- `auth` store owns token, current user, login status, and logout.
- `settings` store owns language, theme color, and layout preferences.
- Axios request interceptor injects bearer token.
- Axios response interceptor handles 401 by clearing auth state and returning to `/login`.
- Router guard prevents unauthenticated access to protected routes.
- Route meta supports future permissions with `meta.permission`.

## Error Handling

- Login validates that tenant ID, username, and password are present before sending the request.
- Failed login shows a localized error message.
- Network errors show a clear localized message.
- 401 clears local token and returns to login.
- Disabled/unimplemented entries must not appear as working backend functions.

## Verification

Minimum verification for this slice:

- `npm install`
- `npm run build`
- Start Vite dev server.
- Open `/login` and verify layout on desktop.
- Verify left illustration reacts to mouse movement.
- Verify mobile layout hides the left illustration and keeps the form usable.
- Verify login request payload contains `tenantId`, `username`, and `password`.
- Verify authenticated navigation enters dashboard after a successful backend response.

## Preview Artifacts

Preview artifacts used during design:

- `D:\xuan-erp\.codex\brainstorm-preview\login-v2.html`
- `D:\xuan-erp\.codex\brainstorm-preview\login-v2.png`
- `D:\xuan-erp\.codex\brainstorm-preview\preview-board.png`

These preview files are local design evidence and should not be treated as production frontend source.
