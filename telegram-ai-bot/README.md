# NCARD Telegram bot

NCARD replaces the previous AI assistant in this service. The Android app folders and workflows elsewhere in the repository are not changed.

## Render setup

Create or keep the Web Service using:

- Root Directory: `telegram-ai-bot`
- Runtime: Node
- Build Command: `npm install`
- Start Command: `npm start`
- Health Check Path: `/health`

Required environment variables:

- `TELEGRAM_BOT_TOKEN` — the existing Telegram bot token (never commit it).
- `WEBHOOK_SECRET` — a random string of 32+ characters.

Render provides `RENDER_EXTERNAL_URL` automatically. The app registers the Telegram webhook at startup.

## Owner

The owner is configured in code as Telegram ID `71195190` and username `@nokium`. Admin authorization uses the numeric Telegram ID, not the mutable username. The private-chat command `/nadmin` opens the owner panel.

## Current commands

- `/start` — introduction.
- `/ncard` — draw a random phone card; personal cooldown is 8 minutes.
- `/nakk` — profile.
- `/ntop` — current in-memory leaderboard.
- `/nsettings` — profile settings placeholder.
- `/nadmin` — owner-only panel in private chat.

## Development status

This is the initial NCARD replacement foundation. The current catalogue is a small starter set; the planned 110 verified models, original manufacturer images, full rarity ladder, upgrades, achievements, profile customization, and group-scoped persistent rankings still need to be added.

State is currently held in process memory and will reset when the Render service restarts or redeploys. A persistent database must be connected before treating collections and cooldowns as permanent. No AI-generated phone images are used.
