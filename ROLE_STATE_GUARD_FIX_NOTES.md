# Manga Series Role and State Guard Fix

Baseline fixes preserved:

- Missing or invalid authentication returns HTTP 401.
- Authenticated permission failures return HTTP 403.
- Business/state validation errors return HTTP 400.
- Unexpected failures return HTTP 500.
- `User.passwordHash` is excluded from all direct and nested JSON responses.
- Frontend npm packages resolve through `https://registry.npmjs.org/`.

## Live defects fixed

1. Only users with `ROLE_MANGAKA` may create manga series.
2. Only the owning Mangaka may edit, delete, or change the lifecycle status of a series.
3. Ownership failures throw `AccessDeniedException` and therefore return HTTP 403.
4. The generic Mangaka status endpoint cannot approve or reject a series in `REVIEWING`; final decisions are Admin-only.
5. Admin final decisions are accepted only when the current status is `REVIEWING`.
6. Admin approval requires at least one Editorial Board approval vote.
7. An optional assignee on Admin approval must actually have the `Tantou Editor` role.

## Automated validation

- Spring context test: passed.
- Exception status tests: passed.
- Password serialization tests: passed.
- Manga series role/state unit tests: passed.
- Total backend tests: 13 passed, 0 failed.
- Frontend production build: passed.
