package commands.links;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import datautil.APIUtil;
import datautil.DBManager;
import datawrapper.Player;
import datawrapper.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import util.MessageUtil;

public class playerinfo extends ListenerAdapter {

	enum ConvertionType {
		USERTOACCS, ACCTOUSER
	}

	/**
	 * Formats player API JSON data into a human-readable text format
	 * @param jsonString The raw JSON string from the API
	 * @return Formatted text string
	 */
	private static String formatPlayerJsonToText(String jsonString) {
		StringBuilder sb = new StringBuilder();
		JSONObject json = new JSONObject(jsonString);
		
		sb.append("=".repeat(60)).append("\n");
		sb.append("  PLAYER INFORMATION\n");
		sb.append("=".repeat(60)).append("\n\n");
		
		// Basic Information
		sb.append("BASIC INFO:\n");
		sb.append("-".repeat(60)).append("\n");
		if (json.has("name")) sb.append("  Name: ").append(json.getString("name")).append("\n");
		if (json.has("tag")) sb.append("  Tag: ").append(json.getString("tag")).append("\n");
		if (json.has("expLevel")) sb.append("  Experience Level: ").append(json.getInt("expLevel")).append("\n");
		if (json.has("trophies")) sb.append("  Trophies: ").append(json.getInt("trophies")).append("\n");
		if (json.has("bestTrophies")) sb.append("  Best Trophies: ").append(json.getInt("bestTrophies")).append("\n");
		if (json.has("wins")) sb.append("  Wins: ").append(json.getInt("wins")).append("\n");
		if (json.has("losses")) sb.append("  Losses: ").append(json.getInt("losses")).append("\n");
		if (json.has("battleCount")) sb.append("  Battle Count: ").append(json.getInt("battleCount")).append("\n");
		if (json.has("threeCrownWins")) sb.append("  Three Crown Wins: ").append(json.getInt("threeCrownWins")).append("\n");
		sb.append("\n");
		
		// Clan Information
		if (json.has("clan")) {
			JSONObject clan = json.getJSONObject("clan");
			sb.append("CLAN INFO:\n");
			sb.append("-".repeat(60)).append("\n");
			if (clan.has("name")) sb.append("  Clan Name: ").append(clan.getString("name")).append("\n");
			if (clan.has("tag")) sb.append("  Clan Tag: ").append(clan.getString("tag")).append("\n");
			if (clan.has("role")) sb.append("  Role: ").append(clan.getString("role")).append("\n");
			if (clan.has("donations")) sb.append("  Donations: ").append(clan.getInt("donations")).append("\n");
			if (clan.has("donationsReceived")) sb.append("  Donations Received: ").append(clan.getInt("donationsReceived")).append("\n");
			sb.append("\n");
		}
		
		// Arena Information
		if (json.has("arena")) {
			JSONObject arena = json.getJSONObject("arena");
			sb.append("ARENA INFO:\n");
			sb.append("-".repeat(60)).append("\n");
			if (arena.has("name")) sb.append("  Arena: ").append(arena.getString("name")).append("\n");
			if (arena.has("id")) sb.append("  Arena ID: ").append(arena.getInt("id")).append("\n");
			sb.append("\n");
		}
		
		// League Statistics
		if (json.has("leagueStatistics")) {
			JSONObject leagueStats = json.getJSONObject("leagueStatistics");
			sb.append("LEAGUE STATISTICS:\n");
			sb.append("-".repeat(60)).append("\n");
			if (leagueStats.has("currentSeason")) {
				JSONObject currentSeason = leagueStats.getJSONObject("currentSeason");
				sb.append("  Current Season:\n");
				if (currentSeason.has("trophies")) sb.append("    Trophies: ").append(currentSeason.getInt("trophies")).append("\n");
				if (currentSeason.has("bestTrophies")) sb.append("    Best Trophies: ").append(currentSeason.getInt("bestTrophies")).append("\n");
			}
			if (leagueStats.has("previousSeason")) {
				JSONObject previousSeason = leagueStats.getJSONObject("previousSeason");
				sb.append("  Previous Season:\n");
				if (previousSeason.has("trophies")) sb.append("    Trophies: ").append(previousSeason.getInt("trophies")).append("\n");
				if (previousSeason.has("bestTrophies")) sb.append("    Best Trophies: ").append(previousSeason.getInt("bestTrophies")).append("\n");
			}
			if (leagueStats.has("bestSeason")) {
				JSONObject bestSeason = leagueStats.getJSONObject("bestSeason");
				sb.append("  Best Season:\n");
				if (bestSeason.has("trophies")) sb.append("    Trophies: ").append(bestSeason.getInt("trophies")).append("\n");
			}
			sb.append("\n");
		}
		
		// Cards
		if (json.has("cards")) {
			JSONArray cards = json.getJSONArray("cards");
			sb.append("CARDS:\n");
			sb.append("-".repeat(60)).append("\n");
			sb.append("  Total Cards: ").append(cards.length()).append("\n");
			int maxLevel = 0;
			int totalLevel = 0;
			for (int i = 0; i < cards.length(); i++) {
				JSONObject card = cards.getJSONObject(i);
				if (card.has("level")) {
					int level = card.getInt("level");
					totalLevel += level;
					if (level > maxLevel) maxLevel = level;
				}
			}
			if (cards.length() > 0) {
				sb.append("  Average Card Level: ").append(String.format("%.2f", (double)totalLevel / cards.length())).append("\n");
				sb.append("  Highest Card Level: ").append(maxLevel).append("\n");
			}
			sb.append("\n");
		}
		
		// Achievements
		if (json.has("achievements")) {
			JSONArray achievements = json.getJSONArray("achievements");
			sb.append("ACHIEVEMENTS:\n");
			sb.append("-".repeat(60)).append("\n");
			int completed = 0;
			for (int i = 0; i < achievements.length(); i++) {
				JSONObject achievement = achievements.getJSONObject(i);
				if (achievement.has("completionInfo") && !achievement.isNull("completionInfo")) {
					completed++;
				}
			}
			sb.append("  Completed Achievements: ").append(completed).append(" / ").append(achievements.length()).append("\n");
			sb.append("\n");
		}
		
		// Badges
		if (json.has("badges")) {
			JSONArray badges = json.getJSONArray("badges");
			sb.append("BADGES:\n");
			sb.append("-".repeat(60)).append("\n");
			sb.append("  Total Badges: ").append(badges.length()).append("\n");
			if (badges.length() > 0) {
				for (int i = 0; i < Math.min(badges.length(), 10); i++) {
					JSONObject badge = badges.getJSONObject(i);
					if (badge.has("name")) {
						sb.append("  - ").append(badge.getString("name"));
						if (badge.has("level")) sb.append(" (Level ").append(badge.getInt("level")).append(")");
						sb.append("\n");
					}
				}
				if (badges.length() > 10) {
					sb.append("  ... and ").append(badges.length() - 10).append(" more\n");
				}
			}
			sb.append("\n");
		}
		
		sb.append("=".repeat(60)).append("\n");
		sb.append("  End of Player Information\n");
		sb.append("=".repeat(60)).append("\n");
		
		return sb.toString();
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("playerinfo"))
			return;
		event.deferReply().queue();
		String title = "Spielerinformation";

		OptionMapping userOption = event.getOption("user");
		OptionMapping playerOption = event.getOption("player");
		OptionMapping getApiFileOption = event.getOption("getapifile");

		if (userOption == null && playerOption == null) {
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
					"Einer der beiden Parameter ist erforderlich!", MessageUtil.EmbedType.ERROR)).queue();
			return;
		}

		if (userOption != null && playerOption != null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Bitte gib nur einen Parameter an!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}
		
		boolean getApiFile = false;
		if (getApiFileOption != null) {
			String getApiFileValue = getApiFileOption.getAsString();
			if ("true".equalsIgnoreCase(getApiFileValue)) {
				getApiFile = true;
			} else {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Der getapifile Parameter muss entweder \"true\" enthalten oder nicht angegeben sein (false).",
						MessageUtil.EmbedType.ERROR)).queue();
				return;
			}
		}

		final boolean getApiFileFinal = getApiFile;
		
		new Thread(() -> {
			String userid = null;
			String playertag = null;
			ArrayList<Player> linkedaccs = new ArrayList<>();

			Player player = null;

			ConvertionType conv = null;

			if (userOption != null) {
				userid = userOption.getAsMentionable().getId();
				linkedaccs = new User(userid).getAllLinkedAccounts();
				conv = ConvertionType.USERTOACCS;
			}
			if (playerOption != null) {
				playertag = playerOption.getAsString();
				player = new Player(playertag);
				if (player.IsLinked()) {
					userid = player.getUser().getUserID();
					conv = ConvertionType.ACCTOUSER;
				} else {
					event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Dieser Spieler ist nicht verifiziert.", MessageUtil.EmbedType.ERROR)).queue();
					return;
				}
			}

			String desc = "";

			if (conv == ConvertionType.ACCTOUSER) {
				try {
					desc += "## " + MessageUtil.unformat(player.getInfoStringDB()) + "\n";
				} catch (Exception e) {
					e.printStackTrace();
				}
				desc += "Verlinkter Discord Account: <@" + userid + ">\n";
				if (player.getClanDB() != null) {
					desc += "Eingetragen in Clan: " + player.getClanDB().getInfoStringDB();
					if (player.isMarked()) {
						desc += " (✗)";
						String note = player.getNote();
						if (note != null && !note.trim().isEmpty()) {
							desc += " - " + note;
						}
					}
					desc += "\n";
				} else {
					desc += "Eingetragen in Clan: ---\n";
				}
				if (player.getClanAPI() != null) {
					desc += "Ingame in Clan: " + player.getClanAPI().getInfoStringAPI() + "\n";
				} else {
					desc += "Ingame in Clan: ---\n";
				}
				desc += "Aktuelle Anzahl Kickpunkte: " + player.getActiveKickpoints().size() + "\n";
				desc += "Ingesamte Anzahl Kickpunkte: " + player.getTotalKickpoints();

				final String uuid = userid;
				MessageChannelUnion channel = event.getChannel();
				MessageUtil.sendUserPingHidden(channel, uuid);
			}
			if (conv == ConvertionType.USERTOACCS) {
				try {
					desc += "## <@" + userid + "> \n";
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (linkedaccs.isEmpty()) {
					desc += "	Keine verlinkten Accounts.\n";
				} else {
					desc += "Verlinkte Accounts: \n";
					for (Player p : linkedaccs) {
						desc += "   \\- " + MessageUtil.unformat(p.getInfoStringDB()) + "\n";
					}
				}
			}
			
			// Send response with or without API file
			// playertag is only set when playerOption is provided, so API file is only available for player lookups
			if (getApiFileFinal && playertag != null) {
				// Fetch the API JSON for the player
				String apiJson = APIUtil.getPlayerJson(playertag);
				if (apiJson != null) {
					// Format the JSON into readable text
					String formattedText = formatPlayerJsonToText(apiJson);
					ByteArrayInputStream inputStream = new ByteArrayInputStream(formattedText.getBytes(StandardCharsets.UTF_8));
					// Sanitize filename by removing all non-alphanumeric characters except underscore and hyphen
					String sanitizedTag = playertag.replaceAll("[^a-zA-Z0-9_-]", "");
					// Fallback to default name if sanitization results in empty string
					String filename = (sanitizedTag.isEmpty() ? "player" : sanitizedTag) + "_info.txt";
					event.getHook().editOriginal(inputStream, filename)
							.setEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO))
							.queue();
					return;
				}
			}
			// Send response without API file (either not requested or API fetch failed)
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO))
					.queue();
		}).start();

	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("playerinfo"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("player")) {
			List<Command.Choice> choices = DBManager.getPlayerlistAutocomplete(input, DBManager.InClanType.ALL);

			event.replyChoices(choices).queue();
		} else if (focused.equals("getapifile")) {
			List<Command.Choice> choices = new ArrayList<>();
			if ("true".startsWith(input.toLowerCase())) {
				choices.add(new Command.Choice("true", "true"));
			}
			event.replyChoices(choices).queue();
		}
	}

}
