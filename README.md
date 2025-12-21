# LostCRManager

Discord Bot for Clash Royale Clan Management in Lost Family

## Features

- Player linking system with Discord users
- Clan member management
- Kickpoints tracking system
- Clan war statistics and reminders
- Monthly wins tracking
- REST API for external integrations

## Configuration

Set the following environment variables:

```bash
# Discord Bot Configuration
CR_MANAGER_TOKEN=your_discord_bot_token
CR_MANAGER_GUILD_ID=your_guild_id

# Clash Royale API Configuration
CR_MANAGER_API_KEY=your_clash_royale_api_key

# Database Configuration
CR_MANAGER_DB_URL=jdbc:postgresql://localhost:5432/lostcrmanager
CR_MANAGER_DB_USER=your_db_user
CR_MANAGER_DB_PASSWORD=your_db_password

# Discord Role Configuration
CR_MANAGER_EXMEMBER_ROLEID=your_ex_member_role_id

# REST API Configuration
LOSTCRMANAGER_API_SECRET=generate-a-long-random-secret-key-here
LOSTCRMANAGER_PORT=7070
```

See `.env.example` for a template.

## REST API

lostcrmanager includes a REST API for external integrations.

### Configuration

Set environment variables:
```bash
LOSTCRMANAGER_API_SECRET=your-secret-key-here
LOSTCRMANAGER_PORT=7070
```

### Endpoints

#### GET /api/health

Health check endpoint.

**Response:**
```json
{
  "status": "ok",
  "service": "lostcrmanager"
}
```

**Example:**
```bash
curl http://localhost:7070/api/health
```

#### POST /api/link

Link a Clash Royale player to a Discord user.

**Headers:**
- `Authorization: Bearer {API_SECRET}` (required)
- `Content-Type: application/json`

**Request Body:**
```json
{
  "tag": "#ABC123",
  "userId": "123456789012345678",
  "source": "external-bot"
}
```

**Response (Success - 200):**
```json
{
  "success": true,
  "tag": "#ABC123",
  "userId": "123456789012345678",
  "playerName": "PlayerName",
  "playerInfo": "PlayerName (#ABC123)",
  "source": "external-bot",
  "message": "Player successfully linked"
}
```

**Response (Error - 400/401/500):**
```json
{
  "success": false,
  "error": "Error message",
  "tag": "#ABC123"
}
```

**Error Cases:**
- `401 Unauthorized`: Missing or invalid API secret
- `400 Bad Request`: Player already linked, player not found, invalid tag format, or missing required parameters
- `500 Internal Server Error`: Database errors, API errors, or request timeout

**Example:**
```bash
curl -X POST http://localhost:7070/api/link \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-secret-key" \
  -d '{"tag": "#ABC123", "userId": "123456789012345678", "source": "api"}'
```

### Security

- Keep `LOSTCRMANAGER_API_SECRET` secure and do not commit it to version control
- Don't expose the API publicly without proper network security
- Only allow access from trusted services
- Use HTTPS in production environments
- The API secret is required for all `/api/link` requests

## Building

Build the project using Maven:

```bash
mvn clean package
```

## Running

Run the bot:

```bash
java -jar target/lostcrmanager-0.0.1-SNAPSHOT.jar
```

Ensure all required environment variables are set before running.

## License

This project is for use by the Lost Family Clash Royale clan community.
