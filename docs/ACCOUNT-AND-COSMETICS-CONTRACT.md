# Space account and appearance contract · 3 November 2026

The user now explicitly requests Space registration, profile nickname/skin/cape editing in website and launcher, and a Minecraft cosmetics tab. This supersedes the earlier deferral of Space registration and cosmetics. Microsoft authentication and public hosting remain outside this work. All work is local, no push/main/public deploy. Existing user profiles/worlds/processes must be preserved.

## HTTP boundary (shared by website and launcher agents)

The account module is separate from read-only content. Public types/validators belong to the new private workspace `@space/account-contracts`. No secrets or Node/Next/Electron dependencies belong there. DTO fields are fixed in its initial `src/index.ts`; notify the coordinator before a breaking change.

- `POST /v1/accounts/register`: `SpaceRegistration` -> `SpaceAccountSession`.
- `POST /v1/accounts/login`: `SpaceCredentials` -> `SpaceAccountSession`.
- `POST /v1/accounts/logout`: Bearer sessionToken -> `{ok:true}` (revoke the session).
- `GET /v1/accounts/me`: Bearer -> `{profile: SpaceAccountProfile}`.
- `PATCH /v1/accounts/me`: Bearer + `SpaceProfileEdit` -> `{profile}`.
- `PUT /v1/accounts/me/skin`: Bearer + `SpaceImageEdit` with required model -> `{profile}`.
- `PUT /v1/accounts/me/cape`: Bearer + `SpaceImageEdit` -> `{profile}`.
- `DELETE /v1/accounts/me/skin` or `/cape`: Bearer + `{expectedRevision}` -> `{profile}`.
- `GET /v1/accounts/assets/:sha256`: fixed hash only -> validated `image/png` bytes. Descriptor has no filesystem path or arbitrary URL. Cache by hash; never fetch a URL supplied in account data.

Safe errors use `{statusCode,code,message}`; stable codes: `SPACE_INVALID_INPUT`, `SPACE_INVALID_CREDENTIALS`, `SPACE_UNAUTHORIZED`, `SPACE_CONFLICT`, `SPACE_RATE_LIMITED`, `SPACE_INVALID_IMAGE`, `SPACE_UNAVAILABLE`, `SPACE_REVISION_CONFLICT`. Do not expose raw errors, passwords or tokens. Unknown fields, oversized bodies, invalid credentials/images and stale revisions are rejected. Username is an immutable Space handle, case-insensitively unique; nickname is a mutable Minecraft-compatible display/launch name. Nickname uniqueness is case-insensitive. Passwords allow paste/spaces and are 15–128 Unicode characters; store only a salted password KDF. Sessions are opaque, bounded-life, stored server-side by hash, revocable. Node24 `node:sqlite` is available locally; no new external dependency is needed.

Use persistent account data only in actual API/bootstrap/preview under ignored `.local/space-accounts` or explicit `SPACE_ACCOUNT_DATA`. Tests use independent in-memory/temp stores per application. Do not let parallel API fixtures share an accounts DB. Next uses a same-origin BFF, HttpOnly/SameSite cookie, origin checks for mutations; browser responses contain public profile only. Desktop stores a session only in main, OS-encrypted when persisted; no plaintext fallback or token IPC. Accounts must work against one shared local service in both clients, not independent fake accounts.

Skin PNG:64×64 classic/slim or64×32 classic. Cape PNG:64×32,128×64,256×128. Max256KiB encoded image; fully validate chunks/decompression/pixels, not just extension/header. No SVG, remote URL, arbitrary path or user-created file outside fixed roots. Server-generated asset names are full SHA-256 values.

## Launcher -> Minecraft appearance boundary

After the user explicitly chooses to apply the Space profile and before the next game spawn, main exports public appearance only into the selected instance:

- `config/space-appearance.properties`, UTF-8 / Java-compatible properties, schemaVersion=1.
- Fields: `accountId`, `username`, `nickname`, `skinSha256` (empty or64 lowercase hex), `skinModel` (`classic`/`slim`), `capeSha256` (empty or64 lowercase hex).
- Assets: `config/space-appearance/skin-<sha256>.png` and `cape-<sha256>.png`.

Verify descriptor size/hash and PNG before atomic write; guard symlinks/path traversal. No token, password, API URL or raw local path in these files. Do not mutate a running game's instance. Registration/login alone must not silently rename the existing local player: expose an explicit apply action; changing a local nickname can change the offline identity/inventory context. Keep the existing guest/local-nickname path.

The game reads only these fixed local files, validates bounds/hash/dimensions and falls back safely. It never receives account credentials or fetches HTTP in a render loop. Appearance initially applies to the local player in Space, without claiming an official Mojang skin or universal visibility to other clients. Client cosmetics preferences (wings/halo/cape visibility) are stored separately and edited on a dedicated Minecraft tab.

Game UI agent owns existing menu/HUD/settings and loading-screen mixins. Coordinator owns `dev.spaceclient.cosmetics/**`, appearance/skin/cape/cosmetic render hooks, corresponding new mixins, `SpaceClient.java` glue and final mixin/bundle registration. The tab opens `new dev.spaceclient.cosmetics.CosmeticsScreen(parent)`; coordinator will supply that class and message the source commit before the UI agent's final build. Do not add a conflicting stub to committed game sources.

Each agent uses its existing isolated clone, a new branch from the foundation commit, own caches/build/output, local commits and its own `docs/team/*-wave2-2026-11-03.md`. No shared handoff/README edits. Final acceptance belongs to the coordinator.
