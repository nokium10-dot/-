# NCARD Telegram bot

NCARD replaces the previous AI assistant in this service. Only this folder is used for the bot; Android projects elsewhere in the repository are not changed.

## Render setup

- Root Directory: `telegram-ai-bot`
- Runtime: Node
- Build Command: `npm install`
- Start Command: `npm start`
- Health Check Path: `/health`

Required environment variables:

- `TELEGRAM_BOT_TOKEN` — Telegram bot token (keep it secret).
- `WEBHOOK_SECRET` — random string of 32+ characters.
- `DATABASE_URL` — PostgreSQL connection string. Add a PostgreSQL database in Render and copy its **internal** connection string into this variable. Without it, the bot falls back to temporary in-memory storage and player data will be lost on restart.

Render provides `RENDER_EXTERNAL_URL` automatically. The app registers the Telegram webhook at startup.

## Owner

Owner Telegram ID: `71195190` (@nokium). Authorization uses the numeric ID. The private-chat command `/nadmin` opens the owner panel.

## Commands currently implemented

- `/start` — introduction.
- `/ncard` — draw a random phone card; personal cooldown is 8 minutes.
- `/nakk` — profile summary.
- `/ntop` — leaderboard for the current group.
- `/nsettings` — settings placeholder.
- `/nadmin` — owner-only panel in private chat.

## Database

When `DATABASE_URL` is present, the app creates the following tables automatically:

- `ncard_users` — player identity and profile fields.
- `ncard_cards` — each card copy owned by a player.
- `ncard_cooldowns` — persistent personal cooldowns.
- `ncard_group_members` — group membership for group-specific rankings.
- `ncard_admin_log` — audit log table for future owner grants.

The current leaderboard counts duplicate cards and is scoped to the group where `/ntop` is used. The player's collection is shared between groups.

## Development status

This is the persistent-storage foundation, not the finished game. The starter catalogue currently contains 10 models. The planned 110-card catalogue, original manufacturer images, full upgrade and achievement systems, profile titles/backgrounds, and complete admin card-grant workflow are still to be implemented.

No AI-generated phone images are used. Official manufacturer images must be sourced for the exact model and must not be altered.

## Deployment note

The GitHub code has been updated, but the Render service has not been configured or deployed from this change. Add `DATABASE_URL` in Render before relying on persistent storage. Then deploy and check `/health`; it reports whether the process is using PostgreSQL or temporary memory.
