package commands.wins;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import datautil.Connection;
import datautil.DBManager;
import datautil.DBUtil;
import datawrapper.Clan;
import datawrapper.Player;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import util.MessageUtil;

public class wins extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("wins"))
			return;
		event.deferReply().queue();
		String title = "Wins-Statistik";

		OptionMapping playerOption = event.getOption("player");
		OptionMapping clanOption = event.getOption("clan");
		OptionMapping monthOption = event.getOption("month");

		// Check that at least one of player or clan is provided
		if (playerOption == null && clanOption == null) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Du musst entweder einen Spieler oder einen Clan angeben.",
							MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		// Check that month is provided
		if (monthOption == null) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Du musst einen Monat angeben.",
							MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String monthValue = monthOption.getAsString();
		int year;
		int month;

		try {
			// Parse month value in format "YYYY-MM"
			String[] parts = monthValue.split("-");
			year = Integer.parseInt(parts[0]);
			month = Integer.parseInt(parts[1]);
		} catch (Exception e) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Ungültiges Monat-Format. Bitte wähle einen Monat aus der Liste.",
							MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		ZoneId zone = ZoneId.of("Europe/Berlin");
		ZonedDateTime now = ZonedDateTime.now(zone);
		int currentYear = now.getYear();
		int currentMonth = now.getMonthValue();
		boolean isCurrentMonth = (year == currentYear && month == currentMonth);

		// Start of the selected month
		ZonedDateTime startOfMonth = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, zone);
		// Start of the next month (end boundary)
		ZonedDateTime startOfNextMonth = startOfMonth.plusMonths(1);

		if (playerOption != null) {
			// Single player mode
			String playerTag = playerOption.getAsString();
			Player player = new Player(playerTag);

			if (!player.IsLinked()) {
				event.getHook()
						.editOriginalEmbeds(MessageUtil.buildEmbed(title,
								"Dieser Spieler ist nicht verlinkt.",
								MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}

			String result = getPlayerWinsForMonth(player, year, month, isCurrentMonth, startOfMonth, startOfNextMonth, zone);
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title, result, MessageUtil.EmbedType.INFO))
					.queue();
		} else {
			// Clan mode
			String clanTag = clanOption.getAsString();
			Clan clan = new Clan(clanTag);

			if (!clan.ExistsDB()) {
				event.getHook()
						.editOriginalEmbeds(MessageUtil.buildEmbed(title,
								"Dieser Clan existiert nicht.",
								MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}

			ArrayList<Player> players = clan.getPlayersDB();
			if (players.isEmpty()) {
				event.getHook()
						.editOriginalEmbeds(MessageUtil.buildEmbed(title,
								"Dieser Clan hat keine Mitglieder.",
								MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}

			StringBuilder sb = new StringBuilder();
			String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.GERMAN);
			sb.append("**Wins für " + clan.getInfoStringDB() + " im " + monthName + " " + year + ":**\n\n");

			for (Player player : players) {
				String playerResult = getPlayerWinsForMonth(player, year, month, isCurrentMonth, startOfMonth, startOfNextMonth, zone);
				sb.append("**" + MessageUtil.unformat(player.getInfoStringDB()) + ":**\n");
				sb.append(playerResult);
				sb.append("\n");
			}

			// Split message if too long
			String fullMessage = sb.toString();
			if (fullMessage.length() > 4000) {
				fullMessage = fullMessage.substring(0, 3997) + "...";
			}

			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title, fullMessage, MessageUtil.EmbedType.INFO))
					.queue();
		}
	}

	private String getPlayerWinsForMonth(Player player, int year, int month, boolean isCurrentMonth,
			ZonedDateTime startOfMonth, ZonedDateTime startOfNextMonth, ZoneId zone) {

		String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.GERMAN);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

		// Check if any data exists for this player, if not save current data first
		if (!hasAnyWinsData(player.getTag())) {
			savePlayerWins(player.getTag());
		}

		if (isCurrentMonth) {
			// Current month: get start of month data and fetch current wins from API
			WinsRecord startRecord = getWinsAtOrAfter(player.getTag(), startOfMonth);

			Integer currentWins = player.getWinsAPI();
			if (currentWins == null) {
				return "Fehler beim Abrufen der aktuellen Wins von der API.\n";
			}

			if (startRecord == null) {
				// No data at start of month - should not happen now since we save on first request
				return "Keine Daten für den Monatsanfang verfügbar. Aktuelle Wins: " + currentWins + "\n";
			}

			int winsThisMonth = currentWins - startRecord.wins;
			String startTimeFormatted = startRecord.recordedAt.atZoneSameInstant(zone).format(formatter);

			if (isStartOfMonth(startRecord.recordedAt, startOfMonth)) {
				return "Wins im " + monthName + " " + year + ": **" + winsThisMonth + "**\n"
						+ "(Von " + startRecord.wins + " am Monatsanfang auf " + currentWins + " aktuell)\n";
			} else {
				return "Wins seit " + startTimeFormatted + ": **" + winsThisMonth + "**\n"
						+ "(Von " + startRecord.wins + " auf " + currentWins + " aktuell)\n"
						+ "⚠️ Daten sind nicht vom Monatsanfang, sondern vom Zeitpunkt der Verlinkung.\n";
			}
		} else {
			// Past month: get data from start of month and start of next month
			WinsRecord startRecord = getWinsAtOrAfter(player.getTag(), startOfMonth);
			WinsRecord endRecord = getWinsAtOrAfter(player.getTag(), startOfNextMonth);

			if (startRecord == null) {
				return "Keine Daten für " + monthName + " " + year + " verfügbar.\n";
			}

			if (endRecord == null) {
				return "Keine Enddaten für " + monthName + " " + year + " verfügbar.\n";
			}

			int winsInMonth = endRecord.wins - startRecord.wins;
			String startTimeFormatted = startRecord.recordedAt.atZoneSameInstant(zone).format(formatter);
			String endTimeFormatted = endRecord.recordedAt.atZoneSameInstant(zone).format(formatter);

			boolean startIsMonthStart = isStartOfMonth(startRecord.recordedAt, startOfMonth);
			boolean endIsMonthStart = isStartOfMonth(endRecord.recordedAt, startOfNextMonth);

			StringBuilder result = new StringBuilder();
			result.append("Wins im " + monthName + " " + year + ": **" + winsInMonth + "**\n");
			result.append("(Von " + startRecord.wins + " auf " + endRecord.wins + ")\n");

			if (!startIsMonthStart) {
				result.append("⚠️ Startdaten vom " + startTimeFormatted + " (nicht Monatsanfang)\n");
			}
			if (!endIsMonthStart) {
				result.append("⚠️ Enddaten vom " + endTimeFormatted + " (nicht Monatsanfang)\n");
			}

			return result.toString();
		}
	}

	private boolean hasAnyWinsData(String playerTag) {
		String sql = "SELECT 1 FROM player_wins WHERE player_tag = ? LIMIT 1";
		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			pstmt.setString(1, playerTag);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			System.err.println("Fehler beim Prüfen der Wins-Daten für Spieler " + playerTag + ": " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	private boolean isStartOfMonth(OffsetDateTime recordedAt, ZonedDateTime expectedStart) {
		// Check if the recorded time is within the first day of the month
		ZonedDateTime recordedZoned = recordedAt.atZoneSameInstant(expectedStart.getZone());
		return recordedZoned.toLocalDate().equals(expectedStart.toLocalDate());
	}

	private WinsRecord getWinsAtOrAfter(String playerTag, ZonedDateTime dateTime) {
		String sql = "SELECT wins, recorded_at FROM player_wins WHERE player_tag = ? AND recorded_at >= ? ORDER BY recorded_at ASC LIMIT 1";

		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			pstmt.setString(1, playerTag);
			pstmt.setObject(2, dateTime.toOffsetDateTime());

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					int wins = rs.getInt("wins");
					OffsetDateTime recordedAt = rs.getObject("recorded_at", OffsetDateTime.class);
					return new WinsRecord(wins, recordedAt);
				}
			}
		} catch (SQLException e) {
			System.err.println("Fehler beim Abrufen der Wins-Daten für Spieler " + playerTag + ": " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("wins"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("player")) {
			List<Command.Choice> choices = DBManager.getPlayerlistAutocomplete(input, DBManager.InClanType.ALL);
			event.replyChoices(choices).queue();
		} else if (focused.equals("clan")) {
			List<Command.Choice> choices = DBManager.getClansAutocomplete(input);
			event.replyChoices(choices).queue();
		} else if (focused.equals("month")) {
			List<Command.Choice> choices = getMonthAutocomplete(input);
			event.replyChoices(choices).queue();
		}
	}

	private List<Command.Choice> getMonthAutocomplete(String input) {
		List<Command.Choice> choices = new ArrayList<>();
		ZoneId zone = ZoneId.of("Europe/Berlin");
		ZonedDateTime now = ZonedDateTime.now(zone);

		// Offer last 12 months
		for (int i = 0; i < 12; i++) {
			ZonedDateTime monthDate = now.minusMonths(i);
			int year = monthDate.getYear();
			int month = monthDate.getMonthValue();

			String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.GERMAN);
			String display = monthName + " " + year;
			String value = year + "-" + String.format("%02d", month);

			if (display.toLowerCase().contains(input.toLowerCase()) || value.contains(input)) {
				choices.add(new Command.Choice(display, value));
				if (choices.size() >= 25) {
					break;
				}
			}
		}
		return choices;
	}

	// Helper class to hold wins record data
	private static class WinsRecord {
		int wins;
		OffsetDateTime recordedAt;

		WinsRecord(int wins, OffsetDateTime recordedAt) {
			this.wins = wins;
			this.recordedAt = recordedAt;
		}
	}

	// Static method to save wins for a player (called from scheduler and link command)
	public static void savePlayerWins(String playerTag) {
		try {
			Player player = new Player(playerTag);
			Integer wins = player.getWinsAPI();
			if (wins != null) {
				OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Berlin"));
				String sql = "INSERT INTO player_wins (player_tag, recorded_at, wins) VALUES (?, ?, ?) "
						+ "ON CONFLICT (player_tag, recorded_at) DO UPDATE SET wins = ?";
				DBUtil.executeUpdate(sql, playerTag, now, wins, wins);
				System.out.println("Wins gespeichert für " + playerTag + ": " + wins);
			}
		} catch (Exception e) {
			System.err.println("Fehler beim Speichern der Wins für " + playerTag + ": " + e.getMessage());
		}
	}

	// Static method to save wins for all linked players (called from scheduler)
	public static void saveAllPlayerWins() {
		String sql = "SELECT cr_tag FROM players";
		ArrayList<String> playerTags = DBUtil.getArrayListFromSQL(sql, String.class);

		for (String tag : playerTags) {
			try {
				savePlayerWins(tag);
				// Delay to avoid API rate limiting (500ms between requests)
				Thread.sleep(500);
			} catch (Exception e) {
				System.err.println("Fehler beim Speichern der Wins für " + tag + ": " + e.getMessage());
			}
		}
	}
}
