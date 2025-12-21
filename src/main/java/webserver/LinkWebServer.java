package webserver;

import org.json.JSONObject;

import commands.wins.wins;
import datautil.DBUtil;
import datawrapper.Player;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class LinkWebServer {
	
	private static volatile Javalin app;
	private static String apiSecret;
	private static int port;
	private static final Object lock = new Object();
	
	public static void start() {
		synchronized (lock) {
			if (app != null) {
				System.out.println("[LinkAPI] REST API server already running");
				return;
			}
			
			// Get configuration from environment variables
			apiSecret = System.getenv("LOSTCRMANAGER_API_SECRET");
			String portEnv = System.getenv("LOSTCRMANAGER_PORT");
			
			// Parse port with validation
			try {
				port = (portEnv != null && !portEnv.isEmpty()) ? Integer.parseInt(portEnv) : 7070;
				if (port < 1 || port > 65535) {
					System.err.println("[LinkAPI] Invalid port number: " + port + ". Using default port 7070.");
					port = 7070;
				}
			} catch (NumberFormatException e) {
				System.err.println("[LinkAPI] Invalid port format: " + portEnv + ". Using default port 7070.");
				port = 7070;
			}
			
			if (apiSecret == null || apiSecret.isEmpty()) {
				System.out.println("[LinkAPI] Warning: LOSTCRMANAGER_API_SECRET not set. API authentication disabled.");
			}
			
			// Create Javalin app
			app = Javalin.create(config -> {
				config.showJavalinBanner = false;
			}).start(port);
			
			// Health check endpoint
			app.get("/api/health", ctx -> {
				JSONObject response = new JSONObject();
				response.put("status", "ok");
				response.put("service", "lostcrmanager");
				ctx.json(response.toString());
			});
			
			// Link endpoint
			app.post("/api/link", ctx -> {
				handleLinkRequest(ctx);
			});
			
			System.out.println("[LinkAPI] REST API server started on port " + port);
		}
	}
	
	public static void stop() {
		synchronized (lock) {
			if (app != null) {
				app.stop();
				app = null;
				System.out.println("[LinkAPI] REST API server stopped");
			}
		}
	}
	
	private static void handleLinkRequest(Context ctx) {
		try {
			// Verify authentication
			if (apiSecret != null && !apiSecret.isEmpty()) {
				String authHeader = ctx.header("Authorization");
				if (authHeader == null || !authHeader.startsWith("Bearer ")) {
					ctx.status(401);
					JSONObject error = new JSONObject();
					error.put("success", false);
					error.put("error", "Missing or invalid Authorization header");
					ctx.json(error.toString());
					return;
				}
				
				String token = authHeader.substring(7); // Remove "Bearer " prefix
				if (!token.equals(apiSecret)) {
					ctx.status(401);
					JSONObject error = new JSONObject();
					error.put("success", false);
					error.put("error", "Invalid API secret");
					ctx.json(error.toString());
					return;
				}
			}
			
			// Parse request body with validation
			String body = ctx.body();
			if (body == null || body.trim().isEmpty()) {
				ctx.status(400);
				JSONObject error = new JSONObject();
				error.put("success", false);
				error.put("error", "Request body is required");
				ctx.json(error.toString());
				return;
			}
			
			JSONObject requestData;
			try {
				requestData = new JSONObject(body);
			} catch (Exception e) {
				ctx.status(400);
				JSONObject error = new JSONObject();
				error.put("success", false);
				error.put("error", "Invalid JSON in request body");
				ctx.json(error.toString());
				return;
			}
			
			// Extract parameters
			String tag = requestData.optString("tag", null);
			String userId = requestData.optString("userId", null);
			String source = requestData.optString("source", "unknown");
			
			// Validate required parameters
			if (tag == null || tag.isEmpty() || userId == null || userId.isEmpty()) {
				ctx.status(400);
				JSONObject error = new JSONObject();
				error.put("success", false);
				error.put("error", "Missing required parameters: tag and userId");
				if (tag != null) {
					error.put("tag", tag);
				}
				ctx.json(error.toString());
				return;
			}
			
			// Normalize tag (add # if missing)
			if (!tag.startsWith("#")) {
				tag = "#" + tag;
			}
			
			final String finalTag = tag;
			final String finalUserId = userId;
			final String finalSource = source;
			
			System.out.println("[LinkAPI] Request from " + finalSource + ": tag=" + finalTag + ", userId=" + finalUserId);
			
			// Execute link logic synchronously (but Javalin handlers run in their own threads)
			try {
				Player p = new Player(finalTag);
				
				// Check if player account exists via CR API
				if (!p.AccExists()) {
					System.out.println("[LinkAPI] Failed to link " + finalTag + ": Player not found or API error");
					ctx.status(400);
					JSONObject error = new JSONObject();
					error.put("success", false);
					error.put("error", "Player not found or API error");
					error.put("tag", finalTag);
					ctx.json(error.toString());
					return;
				}
				
				// Check if player is already linked
				if (p.IsLinked()) {
					String linkedUserId = p.getUser().getUserID();
					System.out.println("[LinkAPI] Failed to link " + finalTag + ": Already linked to user " + linkedUserId);
					ctx.status(400);
					JSONObject error = new JSONObject();
					error.put("success", false);
					error.put("error", "Player already linked to another user");
					error.put("tag", finalTag);
					error.put("linkedUserId", linkedUserId);
					ctx.json(error.toString());
					return;
				}
				
				// Get player name from API
				String playerName = null;
				try {
					playerName = p.getNameAPI();
				} catch (Exception e) {
					System.err.println("[LinkAPI] Error getting player name: " + e.getMessage());
					e.printStackTrace();
				}
				
				// Insert into database
				DBUtil.executeUpdate("INSERT INTO players (cr_tag, discord_id, name) VALUES (?, ?, ?)", 
						finalTag, finalUserId, playerName);
				
				// Save initial wins data asynchronously
				Thread saveWinsThread = new Thread(() -> {
					wins.savePlayerWins(finalTag);
				});
				saveWinsThread.setDaemon(true);
				saveWinsThread.start();
				
				// Build success response
				System.out.println("[LinkAPI] Successfully linked " + finalTag + " to user " + finalUserId + " (source: " + finalSource + ")");
				JSONObject response = new JSONObject();
				response.put("success", true);
				response.put("tag", finalTag);
				response.put("userId", finalUserId);
				response.put("playerName", playerName);
				response.put("playerInfo", playerName + " (" + finalTag + ")");
				response.put("source", finalSource);
				response.put("message", "Player successfully linked");
				
				ctx.status(200);
				ctx.json(response.toString());
				
			} catch (Exception e) {
				System.err.println("[LinkAPI] Failed to link " + finalTag + ": " + e.getMessage());
				e.printStackTrace();
				ctx.status(500);
				JSONObject error = new JSONObject();
				error.put("success", false);
				error.put("error", "Internal server error");
				error.put("tag", finalTag);
				ctx.json(error.toString());
			}
			
		} catch (Exception e) {
			System.err.println("[LinkAPI] Error handling link request: " + e.getMessage());
			e.printStackTrace();
			ctx.status(500);
			JSONObject error = new JSONObject();
			error.put("success", false);
			error.put("error", "Internal server error");
			ctx.json(error.toString());
		}
	}
}
