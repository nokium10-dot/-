# MatrixWall Telegram AI bot

A small Node.js webhook service for the existing Telegram bot. It does not modify the Android app or APK build workflow.

## Render setup

Create a **Web Service** from this repository with:

- Root Directory: `telegram-ai-bot`
- Runtime: Node
- Build Command: `npm install`
- Start Command: `npm start`
- Health Check Path: `/health`

Add these environment variables in Render:

- `TELEGRAM_BOT_TOKEN` — the existing Telegram bot token (never commit it).
- `OPENAI_API_KEY` — API key for the selected OpenAI-compatible provider.
- `WEBHOOK_SECRET` — a random string of 32+ characters.
- `AI_MODEL` — optional; defaults to `gpt-4o-mini`.
- `AI_BASE_URL` — optional; defaults to `https://api.openai.com/v1`.
- `BOT_OWNER_ID` — optional numeric Telegram user ID for owner-level controls.

After the service is deployed, Render provides `RENDER_EXTERNAL_URL` automatically. The app registers the Telegram webhook at startup.

## Commands

- `/ask your question` — ask the AI.
- `/clear` — clear your own conversation history.
- `/ai_on` and `/ai_off` — group administrators can enable or disable AI replies.

Conversation history is stored in memory and is reset when the service restarts. Each group member has a separate history.

## Important

The Telegram bot token is the same token already used by the GitHub Actions APK publisher. Sending APK files with Telegram's `sendDocument` API remains possible while the bot uses a webhook for incoming updates. Do not configure another polling process for this same bot token.

The Render free web service may sleep when idle. The first request after sleeping can be delayed. The AI provider may charge separately; free hosting does not make model API usage free.
