package commands;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import lostcrmanager.Bot;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class verify extends ListenerAdapter {

	
	// Command Disabled, API doesn't offer an Endpoint for Token Verification
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("verify"))
			return;

		OptionMapping tagOption = event.getOption("tag");
		OptionMapping tokenOption = event.getOption("token");

		if (tagOption == null || tokenOption == null) {
			event.reply("Beide Parameter 'tag' und 'token' sind erforderlich!").setEphemeral(true).queue();
			return;
		}

		String tag = tagOption.getAsString();
		// String apiToken = tokenOption.getAsString();
		String apiToken = Bot.api_key;

		event.deferReply().queue(); // Optional: Dem Bot Zeit geben, asynchron zu antworten

		try {
			String encodedTag = URLEncoder.encode(tag, "UTF-8");
			URL url = new URL("https://api.clashroyale.com/v1/players/" + encodedTag);

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Authorization", "Bearer " + apiToken);
			connection.setRequestProperty("Accept", "application/json");

			int responseCode = connection.getResponseCode();

			if (responseCode == 200) {
				// Optional: JSON-Antwort lesen und ggf. weitere Überprüfungen durchführen
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line;
				StringBuilder responseContent = new StringBuilder();
				while ((line = in.readLine()) != null) {
					responseContent.append(line);
				}
				in.close();

				// TODO: Optional: Parse JSON und verifiziere weitere Details

				System.out.println("Verifizierung erfolreich, Spieler existiert:");
				System.out.println(responseContent.toString());
			} else {
				System.out.println("Verifizierung fehlgeschlagen. Fehlercode: " + responseCode);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
