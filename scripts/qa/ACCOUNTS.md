# Shared account integration check

Build the account/content contracts, API, website and game first. Node24 and the repository's JDK21 are required.

```powershell
node scripts/qa/cross-client-account.mjs
```

The check starts a private API with its own ignored SQLite store and a separate loopback-only Next process. It registers through the website BFF and logs into that same account through the actual launcher main service. Website nickname/skin edits, launcher cape upload, revision conflicts, explicit Apply and independent logout are checked across both clients.

The actual exported files are passed to the production Java `AppearanceProfile` parser. No session, password, API URL or arbitrary path may enter the fixed appearance schema. Temporary public PNGs/configuration and the report remain under `.reports/cross-client-account/run-*`; credentials/cookies stay in memory and are not logged.

This runner uses an in-memory vault to isolate the wire contract. It does not claim Windows encryption, native file dialogs or a real Minecraft launch: those are covered separately by `apps/launcher/scripts/check-account-recovery-ui.mjs` and `scripts/qa/cosmetics`. Both private servers are stopped in `finally`. Existing user preview processes, userData and worlds are not touched.
