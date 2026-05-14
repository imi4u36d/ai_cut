# JianDou Architecture Refactor Baseline

This repository is being migrated from a single Spring Boot backend and two
standalone frontend apps into a modular monolith plus npm workspace structure.

## Backend

`apps/api-spring` is now a Maven reactor. `api-boot` is the only runnable Spring
Boot assembly module; it contains startup, database/web/OpenAPI configuration,
Flyway resources, and global API exception wiring. Feature code has been moved
into real Maven modules:

- `common`: shared properties, API path constants, error/web primitives.
- `identity-user`: user persistence entities and mappers shared by identity.
- `identity-auth`: auth, admin identity, security, invite flow, and identity
  ports.
- `model-config`: admin/user model configuration and credential persistence.
- `model-invocation`: model runtime profiles, provider clients, transports,
  prompt resolution, and invocation registries.
- `credit`: credit accounts, transactions, rules, and credit controllers.
- `media`: upload and local media artifact handling.
- `generation`: generation run orchestration, catalog, and generation web API.
- `task`: task domain, queueing, persistence, workers, runtime, and task web API.
- `workflow`: workflow/material center application and web API.
- `admin-bff`: admin dashboard/API aggregation surface.
- `health`: runtime descriptor and health endpoints.

Package names were mostly preserved during this split to keep the migration
small. New code should depend only on public application/API packages or explicit
ports from other modules, not on another module's `web`, `infrastructure`, or
`mybatis` packages. Existing boundary debt should be paid down by introducing
ports in the owning module before moving or renaming packages.

Public application routes use `/api/v3`. The OpenAPI document is available at
`/v3/api-docs` when the backend is running.

## Frontend

The root project is now an npm workspace with:

- `apps/web`
- `apps/admin`
- `packages/api-client`
- `packages/frontend-domain`
- `packages/frontend-ui`

Use `@jiandou/api-client` for shared request handling and generated OpenAPI
types. Use `@jiandou/frontend-domain` for pure business formatting and state
helpers. Use `@jiandou/frontend-ui` for shared framework-neutral view models and
future shared UI components.

## Migration Guards

Run:

```bash
npm run verify:architecture
```

This reports current migration debt without failing the default verification
pipeline. Run strict mode before final cleanup:

```bash
npm run verify:architecture:strict
```

Current expected debt:

- `Map<String, Object>` still exists in some feature modules as legacy dynamic
  response payloads.
- Some Java package names still reflect the original monolith layout even though
  the source files now live in separate Maven modules.
- Frontend business code still has dynamic `Record<string, unknown>` usage in
  legacy API adapters and large workflow/task views.

New modules and new frontend package code should not add new dynamic business
payloads.
