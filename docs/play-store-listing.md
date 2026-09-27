# Play Store listing draft — LivePulse

Working copy for the Play Console "Main store listing" and "App content" forms.
Char limits enforced by Play: title 30, short description 80, full description 4000.

## Title (30 max)

`LivePulse: Live Polls & Voting`

## Short description (80 max)

`Create polls in seconds, friends vote live. For matches, meetups & plans.` (73)

## Full description

LivePulse turns group decisions into live polls. Create one, share the code, and watch
votes land in real time — no group chat chaos, no "which pub?" debates.

REAL-TIME GROUP VOTING
• Live results update the moment someone votes
• Multiple choice or single pick — up to 8 options
• Add time slots, venues, and durations to every option
• RSVP with headcounts and plus-ones so you know who's actually coming

MADE FOR REAL PLANS
• Ready-made templates: match day, drinks, dinner, events
• Deadline countdowns — polls close themselves
• Join any poll with a 6-character code
• Anonymous voting when you want honest answers

SHARE & DECIDE
• One-tap share to any chat app with the join code
• Live charts, leading choice, and winner spotlight
• Participant roster: who voted for what (when not anonymous)
• Notes & suggestions on every poll
• Export results anytime

WORKS FOR EVERYONE
• Sign in with Google to sync across devices
• Or join as a guest with just a display name — nothing else
• Works offline; syncs when you're back online

Built with Firebase — polls and votes sync instantly for everyone. Free, no ads, no
tracking.

## App content form answers

- **Privacy policy URL**: `https://trantankhoa.github.io/livepoll/privacy.html`
- **Ads**: Contains no ads
- **Content rating**: No objectionable content (questionnaire: no violence, gambling, etc.)
- **Target audience**: 13+ (declare 18+ age group; app is general audience)
- **Data safety** (per data type — all "app functionality" only):
  - Name — collected, not shared
  - Email address — collected (Google Sign-In), not shared
  - User ID — collected, not shared
  - App interactions / user content (polls, votes, notes) — collected, not shared
  - Encrypted in transit: Yes (all)
  - Deletion mechanism: Yes — email request (address in privacy policy)
  - No data collected for guests beyond a display name
  - No analytics, no crash reporting, no advertising IDs, no sale/sharing of data

## Graphics checklist

All assets ready in `docs/play-assets/`:

- App icon: `icon-512.png` (512×512, gradient + logo in adaptive safe zone)
- Feature graphic: `feature-1024x500.png`
- Screenshots (1078×2399, 9:16): `screens/ux_poll_list_demo.png`,
  `ux_voting.png`, `ux_analytics.png`, `ux_create_poll.png`,
  `ux_poll_list_empty.png` — rendered headless by `UxCatalogScreenshotTest`
  (`./gradlew :app:testDebugUnitTest --tests '*UxCatalog*' -PdemoData=true`)
