# NCARD Telegram bot

NCARD runs from this directory only. Other projects in the repository are not changed.

## Render

- Root Directory: `telegram-ai-bot`
- Runtime: Node.js 20+
- Build: `npm install`
- Start: `npm start`
- Health endpoint: `/health`

Required environment variables:
- `TELEGRAM_BOT_TOKEN`
- `WEBHOOK_SECRET` (32+ random characters)
- `DATABASE_URL` (Render PostgreSQL **Internal Database URL**)

Optional backup variables:
- `GITHUB_BACKUP_TOKEN`
- `BACKUP_ENCRYPTION_KEY` (64 hex characters)
- `GITHUB_BACKUP_OWNER=nokium10-dot`
- `GITHUB_BACKUP_REPO=ncard-backups`
- `GITHUB_BACKUP_BRANCH=main`
- `OWNER_ID=71195190`

The backup script is run with `npm run backup`. A daily GitHub Actions workflow is included at `.github/workflows/ncard-backup.yml` (03:00 UTC) and can also be started manually. Before it can run, add these repository Actions secrets: `DATABASE_URL`, `GITHUB_BACKUP_TOKEN`, `BACKUP_ENCRYPTION_KEY`, and `TELEGRAM_BOT_TOKEN`. These are separate from Render environment variables. Never commit secret values.

## Owner

Owner: @nokium, Telegram ID `71195190`. The `/nadmin` panel is available only to this account in private chat.

## Commands

- `/start` — command overview.
- `/ncard` — draw a random phone card from the 110-model catalog; one draw per 8 minutes per user.
- `/nakk` or plain `накк` — profile, avatar, group rank, ruble balance and collection value.
- `/ncollection` — owned cards, duplicates included.
- `/nachievements` — achievements and rewards.
- `/nsettings` — equip unlocked profile titles and backgrounds.
- `/nallup` — choose a rarity and confirm a bulk upgrade of every owned card at that rarity.
- `/ntop` — leaderboard for the current group.
- `/nadmin` — owner-only statistics, catalog overview, host load, internet check and block controls.
- `/nblock ID` and `/nunblock ID` — owner-only user blocking.
- `/nreply ID text` — owner-only reply to a user through the bot. Private messages to the bot are forwarded to the owner for support.\n- `/nsetphoto` — attach an official phone photo by sending it in the owner's private chat with caption `/nsetphoto card-id`. The photo is stored by Telegram file ID and used unchanged.\n- `/nclearphoto card-id` — remove the saved photo reference for a card.

## Game rules currently in code

- Duplicate cards are allowed and counted individually.
- Collection is shared between groups; group leaderboard is scoped to the current group.
- The profile balance is displayed in rubles. T-Coins and P-Coins are not used.
- Achievement rewards grant rubles and unlock profile titles/backgrounds.
- Bulk upgrade success chance is currently 50%. Successful cards move up one rarity and receive that rarity's base value; failed cards are deleted. There is no upgrade fee.
- The `/ncard` cooldown is stored persistently in PostgreSQL.

## Catalog and images

The catalog now contains 110 phone model entries in `catalog.js`. Official image assets for the exact models are still pending; `imageUrl` is intentionally null until a real manufacturer photo or official render is supplied and verified. AI-generated phone images are prohibited. Do not alter the phone itself; only card background, frame, glass, lighting, NCARD branding and manufacturer label may be styled.

## Database

On startup, the app creates or migrates:
- `ncard_users`
- `ncard_cards`
- `ncard_cooldowns`
- `ncard_group_members`
- `ncard_admin_log`
- `ncard_user_blocks`\n- `ncard_card_assets`

Without `DATABASE_URL`, storage falls back to temporary process memory and data is lost when the service restarts.