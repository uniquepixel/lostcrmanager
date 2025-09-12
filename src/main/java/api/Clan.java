package api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import lostcrmanager.Bot;
import util.Tuple;

public class Clan {

	public static ArrayList<Tuple<String, String>> getMemberTagNameList(String clanTag) {
		ArrayList<Tuple<String, String>> list = new ArrayList<>();

		JSONObject jsonObject = new JSONObject(getJson(clanTag));

		JSONArray members = jsonObject.getJSONArray("memberList");

		for (int i = 0; i < members.length(); i++) {
			JSONObject member = members.getJSONObject(i);
			if (member.has("tag") && member.has("name")) {
				util.Tuple<String, String> t = new util.Tuple<String, String>(member.getString("tag"),
						member.getString("name"));
				list.add(t);
			}
		}

		return list;

	}

	private static String getJson(String clanTag) {
		// URL-kodieren des Spieler-Tags (# -> %23)
		String encodedTag = java.net.URLEncoder.encode(clanTag, java.nio.charset.StandardCharsets.UTF_8);

		String url = "https://api.clashroyale.com/v1/clans/" + encodedTag;

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
