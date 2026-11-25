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

The backup script is run with `npm run backup`. Scheduling is not configured yet. Render's free web-service plan does not include a Cron Job; a GitHub Actions workflow still needs to be configured.

## Owner

Owner: @nokium, Telegram ID `71195190`. The `/nadmin` panel is available only to this account in private chat.

## Commands

- `/start` — command overview.
- `/ncard` — draw a random phone card; one draw per 8 minutes per user.
- `/nakk` or plain `накк` — profile, avatar, group rank, ruble balance and collection value.
- `/ncollection` — owned cards, duplicates included.
- `/nachievements` — achievements and rewards.
- `/nsettings` — equip unlocked profile titles and backgrounds.
- `/nallup` — choose a rarity and confirm a bulk upgrade of every owned card at that rarity.
- `/ntop` — leaderboard for the current group.
- `/nadmin` — owner-only statistics and catalog overview.

## Game rules currently in code

- Duplicate cards are allowed and counted individually.
- Collection is shared between groups; group leaderboard is scoped to the current group.
- The profile balance is displayed in rubles. T-Coins and P-Coins are not used.
- Achievement rewards grant rubles and unlock profile titles/backgrounds.
- Bulk upgrade success chance is currently 50%. Successful cards move up one rarity and receive that rarity's base value; failed cards are deleted. There is no upgrade fee.
- The `/ncard` cooldown is stored persistently in PostgreSQL.

## Catalog and images

The current starter catalog has 10 verified model entries, not 110. It is not yet the final catalog. The final 110-card set and official image assets still need to be added. AI-generated phone images are prohibited; use real photos or official manufacturer renders for the exact model. Do not alter the phone itself; only card background, frame, glass, lighting, NCARD branding and manufacturer label may be styled.

## Database

On startup, the app creates or migrates:
- `ncard_users`
- `ncard_cards`
- `ncard_cooldowns`
- `ncard_group_members`
- `ncard_admin_log`

Without `DATABASE_URL`, storage falls back to temporary process memory and data is lost when the service restarts.