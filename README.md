# LostCRManager

Discord Bot for Clash Royale Clan Management in Lost Family

## REST API

The bot includes a REST API server built with Javalin. The server runs on port 8080 by default (configurable via the `CR_MANAGER_WEB_PORT` environment variable).

### Endpoints

#### Health Check
```
GET /api/health
```

Returns the health status of the server.

**Response:**
```json
{
  "status": "OK"
}
```

#### Link Player
```
POST /api/link
```

Links a Clash Royale player to a Discord user. This endpoint executes the same logic as the `/link` slash command.

**Request Body:**
```json
{
  "tag": "#PLAYERTAG",
  "userId": "123456789012345678",
  "executorUserId": "987654321098765432"
}
```

- `tag`: The Clash Royale player tag (with or without the # prefix)
- `userId`: The Discord user ID to link the player to
- `executorUserId`: The Discord user ID of the person executing the link (must be at least co-leader of a clan)

**Response (Success):**
```json
{
  "message": "Link request accepted and is being processed",
  "tag": "#PLAYERTAG",
  "userId": "123456789012345678"
}
```

**Response (Error):**
```json
{
  "error": "Error message"
}
```

**Status Codes:**
- `200`: Success - link request accepted
- `400`: Bad Request - missing required fields
- `403`: Forbidden - executor does not have permission
- `500`: Internal Server Error

**Notes:**
- The linking process happens asynchronously after the response is returned
- The executor must be at least a co-leader (Vize-Anführer) of a clan
- If the player is already linked, the operation will be logged but no error is returned to the API caller

## Environment Variables

- `CR_MANAGER_TOKEN`: Discord bot token
- `CR_MANAGER_GUILD_ID`: Discord guild ID
- `CR_MANAGER_API_KEY`: Clash Royale API key
- `CR_MANAGER_DB_URL`: Database URL
- `CR_MANAGER_DB_USER`: Database user
- `CR_MANAGER_DB_PASSWORD`: Database password
- `CR_MANAGER_EXMEMBER_ROLEID`: Ex-member role ID
- `CR_MANAGER_WEB_PORT`: Web server port (default: 8080)

## Building

```bash
mvn clean package
```

## Running

```bash
java -jar target/lostcrmanager-0.0.1-SNAPSHOT.jar
```
