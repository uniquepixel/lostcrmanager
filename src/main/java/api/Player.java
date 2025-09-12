package api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

import lostcrmanager.Bot;

public class Player {

	public static boolean AccExists(String playerTag) {
		try {
			String encodedTag = URLEncoder.encode(playerTag, "UTF-8");
			URL url = new URL("https://api.clashroyale.com/v1/players/" + encodedTag);

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Authorization", "Bearer " + Bot.api_key);
			connection.setRequestProperty("Accept", "application/json");

			int responseCode = connection.getResponseCode();

			if (responseCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line;
				StringBuilder responseContent = new StringBuilder();
				while ((line = in.readLine()) != null) {
					responseContent.append(line);
				}
				in.close();

				return true;
			} else {
				System.out.println("Verifizierung fehlgeschlagen. Fehlercode: " + responseCode);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static String getPlayerName(String playerTag) {
		JSONObject jsonObject = new JSONObject(getJson(playerTag));
        return jsonObject.getString("name");
	}
	
	public static String getClanTag(String playerTag) {
		JSONObject jsonObject = new JSONObject(getJson(playerTag));

        // Prüfen, ob der Schlüssel "clan" vorhanden ist und nicht null
        if (jsonObject.has("clan") && !jsonObject.isNull("clan")) {
            JSONObject clanObject = jsonObject.getJSONObject("clan");
            if (clanObject.has("tag")) {
                return clanObject.getString("tag");
            }
        }
        
        // Wenn kein Clan vorhanden oder kein Tag, dann null zurückgeben
        return null;
	}
	
	

	private static String getJson(String playerTag) {
		// URL-kodieren des Spieler-Tags (# -> %23)
		String encodedTag = java.net.URLEncoder.encode(playerTag, java.nio.charset.StandardCharsets.UTF_8);

		String url = "https://api.clashroyale.com/v1/players/" + encodedTag;

		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
				.header("Authorization", "Bearer " + Bot.api_key).header("Accept", "application/json").GET().build();

		HttpResponse<String> response = null;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}

		if (response.statusCode() == 200) {
			String responseBody = response.body();
			// Einfacher JSON-Name-Parser ohne Bibliotheken:
			return responseBody;
		} else {
			System.err.println("Fehler beim Abrufen: HTTP " + response.statusCode());
			System.err.println("Antwort: " + response.body());
			return null;
		}
	}

}
