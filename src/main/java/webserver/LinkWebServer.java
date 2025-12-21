package webserver;

import io.javalin.Javalin;
import io.javalin.http.Context;
import commands.wins.wins;
import datautil.DBManager;
import datautil.DBUtil;
import datawrapper.Player;
import datawrapper.User;

public class LinkWebServer {
    
    private Javalin app;
    private int port;
    
    public LinkWebServer(int port) {
        this.port = port;
    }
    
    public void start() {
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        }).start(port);
        
        // Health endpoint
        app.get("/api/health", ctx -> {
            ctx.json(new HealthResponse("OK"));
        });
        
        // Link endpoint
        app.post("/api/link", this::handleLink);
        
        System.out.println("Web server started on port " + port);
    }
    
    public void stop() {
        if (app != null) {
            app.stop();
            System.out.println("Web server stopped");
        }
    }
    
    private void handleLink(Context ctx) {
        try {
            // Parse request body
            LinkRequest request = ctx.bodyAsClass(LinkRequest.class);
            
            // Validate request
            if (request.tag == null || request.tag.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("Tag is required"));
                return;
            }
            
            if (request.userId == null || request.userId.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("userId is required"));
                return;
            }
            
            if (request.executorUserId == null || request.executorUserId.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("executorUserId is required"));
                return;
            }
            
            // Check permissions of executor
            User userExecuted = new User(request.executorUserId);
            boolean hasPermission = false;
            for (String clantag : DBManager.getAllClans()) {
                if (userExecuted.getClanRoles().get(clantag) == Player.RoleType.ADMIN
                        || userExecuted.getClanRoles().get(clantag) == Player.RoleType.LEADER
                        || userExecuted.getClanRoles().get(clantag) == Player.RoleType.COLEADER) {
                    hasPermission = true;
                    break;
                }
            }
            
            if (!hasPermission) {
                ctx.status(403).json(new ErrorResponse("Executor must be at least a co-leader of a clan"));
                return;
            }
            
            // Process tag
            String tag = request.tag;
            if (!tag.startsWith("#")) {
                tag = "#" + tag;
            }
            
            String userId = request.userId;
            final String finalTag = tag;
            final String finalUserId = userId;
            
            // Execute link logic in a separate thread (async)
            new Thread(() -> {
                Player p = new Player(finalTag);
                
                if (p.AccExists()) {
                    String playername = null;
                    try {
                        playername = p.getNameAPI();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    if (!p.IsLinked()) {
                        DBUtil.executeUpdate("INSERT INTO players (cr_tag, discord_id, name) VALUES (?, ?, ?)", 
                                finalTag, finalUserId, playername);
                        
                        // Save initial wins data for the newly linked player (async)
                        Thread saveWinsThread = new Thread(() -> {
                            wins.savePlayerWins(finalTag);
                        });
                        saveWinsThread.setDaemon(true);
                        saveWinsThread.start();
                        
                        System.out.println("Successfully linked player " + finalTag + " to user " + finalUserId);
                    } else {
                        System.out.println("Player " + finalTag + " is already linked");
                    }
                } else {
                    System.out.println("Player " + finalTag + " does not exist or API error occurred");
                }
            }).start();
            
            // Return immediate response
            ctx.json(new LinkResponse("Link request accepted and is being processed", finalTag, finalUserId));
            
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    // Response classes
    public static class HealthResponse {
        public String status;
        
        public HealthResponse(String status) {
            this.status = status;
        }
    }
    
    public static class LinkRequest {
        public String tag;
        public String userId;
        public String executorUserId;
    }
    
    public static class LinkResponse {
        public String message;
        public String tag;
        public String userId;
        
        public LinkResponse(String message, String tag, String userId) {
            this.message = message;
            this.tag = tag;
            this.userId = userId;
        }
    }
    
    public static class ErrorResponse {
        public String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
