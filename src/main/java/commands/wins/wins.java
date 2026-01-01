package commands.wins;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import datautil.Connection;
import datautil.DBManager;
import datautil.DBUtil;
import datawrapper.Clan;
import datawrapper.Player;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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
		OptionMapping excludeLeadersOption = event.getOption("exclude_leaders");

		// Check that at least one of player or clan is provided
		if (playerOption == null && clanOption == null) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Du musst entweder einen Spieler oder einen Clan angeben.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		// Check that month is provided
		if (monthOption == null) {
			event.getHook()
					.editOriginalEmbeds(
							MessageUtil.buildEmbed(title, "Du musst einen Monat angeben.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String monthValue = monthOption.getAsString();
		int year;
		int month;

		// Handle special "current" value for "Aktueller Monat"
		if (monthValue.equals("current")) {
			ZoneId zone = ZoneId.of("Europe/Berlin");
			ZonedDateTime now = ZonedDateTime.now(zone);
			year = now.getYear();
			month = now.getMonthValue();
		} else {
			try {
				// Parse month value in format "YYYY-MM"
				String[] parts = monthValue.split("-");
				year = Integer.parseInt(parts[0]);
				month = Integer.parseInt(parts[1]);
			} catch (Exception e) {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Ungültiges Monat-Format. Bitte wähle einen Monat aus der Liste.", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}
		}

		// Parse exclude_leaders option
		boolean excludeLeaders = false;
		if (excludeLeadersOption != null) {
			String excludeLeadersValue = excludeLeadersOption.getAsString();
			if ("true".equalsIgnoreCase(excludeLeadersValue)) {
				excludeLeaders = true;
			} else {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Der exclude_leaders Parameter muss entweder \"true\" enthalten oder nicht angegeben sein (false).",
						MessageUtil.EmbedType.ERROR)).queue();
				return;
			}
		}

		// Run heavy processing in a separate thread to not block the main bot instance
		final int yearFinal = year;
		final int monthFinal = month;
		final boolean excludeLeadersFinal = excludeLeaders;
		Thread thread = new Thread(() -> {
			try {
				ZoneId zone = ZoneId.of("Europe/Berlin");
				ZonedDateTime now = ZonedDateTime.now(zone);
				int currentYear = now.getYear();
				int currentMonth = now.getMonthValue();
				boolean isCurrentMonth = (yearFinal == currentYear && monthFinal == currentMonth);

				// Start of the selected month
				ZonedDateTime startOfMonth = ZonedDateTime.of(yearFinal, monthFinal, 1, 0, 0, 0, 0, zone);
				// Start of the next month (end boundary)
				ZonedDateTime startOfNextMonth = startOfMonth.plusMonths(1);

				if (playerOption != null) {
					// Single player mode
					String playerTag = playerOption.getAsString();
					Player player = new Player(playerTag);

					if (!player.IsLinked()) {
						event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
								"Dieser Spieler ist nicht verlinkt.", MessageUtil.EmbedType.ERROR)).queue();
						return;
					}

					String result = getPlayerWinsForMonth(player, yearFinal, monthFinal, isCurrentMonth, startOfMonth,
							startOfNextMonth, zone);

					// Create refresh button with player and month info
					Button refreshButton = Button.secondary("wins_player_" + playerTag + "_" + monthValue, "\u200B")
							.withEmoji(Emoji.fromUnicode("🔁"));

					ZonedDateTime jetzt = ZonedDateTime.now(zone);
					DateTimeFormatter buttonFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm 'Uhr'");
					String formatiert = jetzt.format(buttonFormatter);

					event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, result, MessageUtil.EmbedType.INFO,
							"Zuletzt aktualisiert am " + formatiert)).setActionRow(refreshButton).queue();
				} else {
					// Clan mode
					String clanInput = clanOption.getAsString();
					
					// Parse comma-separated clan tags
					List<String> clansList = parseClanTags(clanInput);
					if (clansList.isEmpty()) {
						event.getHook()
								.editOriginalEmbeds(MessageUtil.buildEmbed(title,
										"Keine gültigen Clans angegeben!", MessageUtil.EmbedType.ERROR))
								.queue();
						return;
					}
					
					// Validate all clans exist
					for (String clanTag : clansList) {
						Clan clan = new Clan(clanTag);
						if (!clan.ExistsDB()) {
							event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, 
									"Clan " + clanTag + " existiert nicht.", MessageUtil.EmbedType.ERROR)).queue();
							return;
						}
					}
					
					// Collect players from all selected clans
					ArrayList<Player> allPlayers = new ArrayList<>();
					for (String clanTag : clansList) {
						Clan clan = new Clan(clanTag);
						ArrayList<Player> clanPlayers = clan.getPlayersDB();
						allPlayers.addAll(clanPlayers);
					}
					
					if (allPlayers.isEmpty()) {
						event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
								"Die angegebenen Clans haben keine Mitglieder.", MessageUtil.EmbedType.ERROR)).queue();
						return;
					}
					
					// Filter out hidden coleaders and optionally exclude leaders/coleaders/admins
					filterPlayersForClanWins(allPlayers, excludeLeadersFinal);

					// Collect wins data for all players with compact format
					int i = 0;
					ArrayList<PlayerWinsResult> results = new ArrayList<>();
					for (Player player : allPlayers) {
						i++;
						PlayerWinsResult result = getPlayerWinsCompact(player, yearFinal, monthFinal, isCurrentMonth,
								startOfMonth, startOfNextMonth, zone);
						results.add(result);
						event.getHook()
								.editOriginalEmbeds(MessageUtil.buildEmbed(title,
										"Wins werden geladen: Spieler " + i + "/" + allPlayers.size(),
										MessageUtil.EmbedType.LOADING))
								.setActionRows().queue();
					}

					// Sort by wins descending
					results.sort(Comparator.comparingInt((PlayerWinsResult r) -> r.wins).reversed());

					StringBuilder sb = new StringBuilder();
					String monthName = Month.of(monthFinal).getDisplayName(TextStyle.FULL, Locale.GERMAN);
					
					// Build clan display for title
					String clanDisplay;
					if (clansList.size() == 1) {
						Clan c = new Clan(clansList.get(0));
						clanDisplay = c.getInfoStringDB();
					} else {
						clanDisplay = clansList.size() + " Clans (" + 
							String.join(", ", clansList.stream()
								.map(tag -> new Clan(tag).getNameDB())
								.toArray(String[]::new)) + ")";
					}
					
					sb.append("**Wins für " + clanDisplay + " im " + monthName + " " + yearFinal + ":**\n\n");

					for (PlayerWinsResult result : results) {
						sb.append(MessageUtil.unformat(result.playerInfo));
						// Add clan name in brackets if multiple clans
						if (clansList.size() > 1) {
							Player p = new Player(result.playerTag);
							if (p.getClanDB() != null) {
								sb.append(" [" + p.getClanDB().getNameDB() + "]");
							}
						}
						sb.append(": **" + result.wins + "**");
						if (result.hasWarning) {
							sb.append(" ⚠️");
						}
						sb.append("\n");
					}

					// Split message if too long
					String fullMessage = sb.toString();
					if (fullMessage.length() > 4000) {
						fullMessage = fullMessage.substring(0, 3997) + "...";
					}

					// Create refresh button - encode multiple clans in button ID
					String clansEncoded = String.join(",", clansList);
					Button refreshButton = Button
							.secondary("wins_clan_" + clansEncoded + "_" + monthValue + "_" + excludeLeadersFinal, "\u200B")
							.withEmoji(Emoji.fromUnicode("🔁"));

					ZonedDateTime jetzt = ZonedDateTime.now(zone);
					DateTimeFormatter buttonFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm 'Uhr'");
					String formatiert = jetzt.format(buttonFormatter);

					event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, fullMessage,
							MessageUtil.EmbedType.INFO, "Zuletzt aktualisiert am " + formatiert))
							.setActionRow(refreshButton).queue();
				}
			} catch (Exception e) {
				System.err.println("Fehler beim Verarbeiten des Wins-Befehls: " + e.getMessage());
				e.printStackTrace();
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Ein Fehler ist aufgetreten: " + e.getMessage(), MessageUtil.EmbedType.ERROR)).queue();
			}
		});
		thread.setName("wins-command-" + event.getUser().getId());
		thread.start();
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
				// No data at start of month - should not happen now since we save on first
				// request
				return "Keine Daten für den Monatsanfang verfügbar. Aktuelle Wins: " + currentWins + "\n";
			}

			int winsThisMonth = currentWins - startRecord.wins;
			String startTimeFormatted = startRecord.recordedAt.atZoneSameInstant(zone).format(formatter);

			if (isStartOfMonth(startRecord.recordedAt, startOfMonth)) {
				return "### " + player.getInfoStringDB() + "\n" + "Wins im " + monthName + " " + year + ": **"
						+ winsThisMonth + "**\n" + "(Von " + startRecord.wins + " am Monatsanfang auf " + currentWins
						+ " aktuell)\n";
			} else {
				return "### " + player.getInfoStringDB() + "\n" + "Wins seit " + startTimeFormatted + ": **"
						+ winsThisMonth + "**\n" + "(Von " + startRecord.wins + " auf " + currentWins + " aktuell)\n"
						+ "⚠️ Daten sind nicht vom Monatsanfang, sondern vom Zeitpunkt der Verlinkung.\n";
			}
		} else {
			// Past month: get data from start of month and start of next month
			WinsRecord startRecord = getWinsAtOrAfter(player.getTag(), startOfMonth);
			WinsRecord endRecord = getWinsAtOrAfter(player.getTag(), startOfNextMonth);

			if (startRecord == null) {
				return "### " + player.getInfoStringDB() + "\n" + "Keine Daten für " + monthName + " " + year
						+ " verfügbar.\n";
			}

			if (endRecord == null) {
				return "### " + player.getInfoStringDB() + "\n" + "Keine Enddaten für " + monthName + " " + year
						+ " verfügbar.\n";
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
			// Handle comma-separated autocomplete similar to statslist (no "Alle Clans")
			List<Command.Choice> choices = getClanAutocomplete(input, event.getUser().getId());
			event.replyChoices(choices).queue();
		} else if (focused.equals("month")) {
			List<Command.Choice> choices = getMonthAutocomplete(input);
			event.replyChoices(choices).queue();
		} else if (focused.equals("exclude_leaders")) {
			List<Command.Choice> choices = new ArrayList<>();
			if ("true".startsWith(input.toLowerCase())) {
				choices.add(new Command.Choice("true", "true"));
			}
			event.replyChoices(choices).queue();
		}
	}

	private List<Command.Choice> getMonthAutocomplete(String input) {
		List<Command.Choice> choices = new ArrayList<>();
		ZoneId zone = ZoneId.of("Europe/Berlin");
		ZonedDateTime now = ZonedDateTime.now(zone);

		// Add "Aktueller Monat" option first - uses special value "current" that gets
		// resolved dynamically
		String currentMonthDisplay = "Aktueller Monat";
		if (currentMonthDisplay.toLowerCase().contains(input.toLowerCase())) {
			choices.add(new Command.Choice(currentMonthDisplay, "current"));
		}

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

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		String id = event.getComponentId();
		if (!id.startsWith("wins_"))
			return;

		event.deferEdit().queue();
		String title = "Wins-Statistik";

		// Parse the button ID: wins_player_<tag>_<year-month> or
		// wins_clan_<tag>_<year-month>_<excludeLeaders>
		
		String type;
		String tag;
		String monthValue;
		boolean excludeLeaders = false;
		
		// Regular parsing for player or clan
		String[] parts = id.split("_", 5);
		if (parts.length < 4) {
			return;
		}

		type = parts[1];
		tag = parts[2];
		monthValue = parts[3];

		// Parse exclude_leaders for clan buttons (5th part if present)
		if (type.equals("clan") && parts.length >= 5) {
			excludeLeaders = "true".equals(parts[4]);
		}

		int year;
		int month;
		// Handle special "current" value for "Aktueller Monat"
		if (monthValue.equals("current")) {
			ZoneId zone = ZoneId.of("Europe/Berlin");
			ZonedDateTime now = ZonedDateTime.now(zone);
			year = now.getYear();
			month = now.getMonthValue();
		} else {
			try {
				String[] monthParts = monthValue.split("-");
				year = Integer.parseInt(monthParts[0]);
				month = Integer.parseInt(monthParts[1]);
			} catch (Exception e) {
				event.getHook()
						.editOriginalEmbeds(
								MessageUtil.buildEmbed(title, "Ungültiges Monat-Format.", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}
		}

		// Run heavy processing in a separate thread to not block the main bot instance
		final int yearFinal = year;
		final int monthFinal = month;
		final boolean excludeLeadersFinal = excludeLeaders;
		Thread thread = new Thread(() -> {
			try {
				ZoneId zone = ZoneId.of("Europe/Berlin");
				ZonedDateTime now = ZonedDateTime.now(zone);
				int currentYear = now.getYear();
				int currentMonth = now.getMonthValue();
				boolean isCurrentMonth = (yearFinal == currentYear && monthFinal == currentMonth);

				// Start of the selected month
				ZonedDateTime startOfMonth = ZonedDateTime.of(yearFinal, monthFinal, 1, 0, 0, 0, 0, zone);
				// Start of the next month (end boundary)
				ZonedDateTime startOfNextMonth = startOfMonth.plusMonths(1);

				if (type.equals("player")) {
					Player player = new Player(tag);

					if (!player.IsLinked()) {
						event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
								"Dieser Spieler ist nicht verlinkt.", MessageUtil.EmbedType.ERROR)).queue();
						return;
					}

					String result = getPlayerWinsForMonth(player, yearFinal, monthFinal, isCurrentMonth, startOfMonth,
							startOfNextMonth, zone);

					// Create refresh button with player and month info
					Button refreshButton = Button.secondary("wins_player_" + tag + "_" + monthValue, "\u200B")
							.withEmoji(Emoji.fromUnicode("🔁"));

					ZonedDateTime jetzt = ZonedDateTime.now(zone);
					DateTimeFormatter buttonFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm 'Uhr'");
					String formatiert = jetzt.format(buttonFormatter);

					event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, result, MessageUtil.EmbedType.INFO,
							"Zuletzt aktualisiert am " + formatiert)).setActionRow(refreshButton).queue();
				} else if (type.equals("clan")) {
					// Parse comma-separated clan tags from button tag parameter
					List<String> clansList = parseClanTags(tag);
					if (clansList.isEmpty()) {
						event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
								"Keine gültigen Clans angegeben!", MessageUtil.EmbedType.ERROR)).queue();
						return;
					}
					
					// Validate all clans exist
					for (String clanTag : clansList) {
						Clan clan = new Clan(clanTag);
						if (!clan.ExistsDB()) {
							event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, 
									"Clan " + clanTag + " existiert nicht.", MessageUtil.EmbedType.ERROR)).queue();
							return;
						}
					}
					
					// Collect players from all selected clans
					ArrayList<Player> allPlayers = new ArrayList<>();
					for (String clanTag : clansList) {
						Clan clan = new Clan(clanTag);
						ArrayList<Player> clanPlayers = clan.getPlayersDB();
						allPlayers.addAll(clanPlayers);
					}
					
					if (allPlayers.isEmpty()) {
						event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
								"Die angegebenen Clans haben keine Mitglieder.", MessageUtil.EmbedType.ERROR)).queue();
						return;
					}
					
					// Filter out hidden coleaders and optionally exclude leaders/coleaders/admins
					filterPlayersForClanWins(allPlayers, excludeLeadersFinal);

					// Collect wins data for all players with compact format
					ArrayList<PlayerWinsResult> results = new ArrayList<>();
					int i = 0;
					for (Player player : allPlayers) {
						i++;
						PlayerWinsResult result = getPlayerWinsCompact(player, yearFinal, monthFinal, isCurrentMonth,
								startOfMonth, startOfNextMonth, zone);
						results.add(result);
						
						event.getHook()
								.editOriginalEmbeds(MessageUtil.buildEmbed(title,
										"Wins werden geladen: Spieler " + i + "/" + allPlayers.size(),
										MessageUtil.EmbedType.LOADING))
								.setActionRows().queue();
					}

					// Sort by wins descending
					results.sort(Comparator.comparingInt((PlayerWinsResult r) -> r.wins).reversed());

					StringBuilder sb = new StringBuilder();
					String monthName = Month.of(monthFinal).getDisplayName(TextStyle.FULL, Locale.GERMAN);
					
					// Build clan display for title
					String clanDisplay;
					if (clansList.size() == 1) {
						Clan c = new Clan(clansList.get(0));
						clanDisplay = c.getInfoStringDB();
					} else {
						clanDisplay = clansList.size() + " Clans (" + 
							String.join(", ", clansList.stream()
								.map(clanTag -> new Clan(clanTag).getNameDB())
								.toArray(String[]::new)) + ")";
					}
					
					sb.append("**Wins für " + clanDisplay + " im " + monthName + " " + yearFinal + ":**\n\n");

					for (PlayerWinsResult result : results) {
						sb.append(MessageUtil.unformat(result.playerInfo));
						// Add clan name in brackets if multiple clans
						if (clansList.size() > 1) {
							Player p = new Player(result.playerTag);
							if (p.getClanDB() != null) {
								sb.append(" [" + p.getClanDB().getNameDB() + "]");
							}
						}
						sb.append(": **" + result.wins + "**");
						if (result.hasWarning) {
							sb.append(" ⚠️");
						}
						sb.append("\n");
					}

					// Split message if too long
					String fullMessage = sb.toString();
					if (fullMessage.length() > 4000) {
						fullMessage = fullMessage.substring(0, 3997) + "...";
					}

					// Create refresh button - encode multiple clans in button ID
					String clansEncoded = String.join(",", clansList);
					Button refreshButton = Button
							.secondary("wins_clan_" + clansEncoded + "_" + monthValue + "_" + excludeLeadersFinal, "\u200B")
							.withEmoji(Emoji.fromUnicode("🔁"));

					ZonedDateTime jetzt = ZonedDateTime.now(zone);
					DateTimeFormatter buttonFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm 'Uhr'");
					String formatiert = jetzt.format(buttonFormatter);

					event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, fullMessage,
							MessageUtil.EmbedType.INFO, "Zuletzt aktualisiert am " + formatiert))
							.setActionRow(refreshButton).queue();
				}
			} catch (Exception e) {
				System.err.println("Fehler beim Verarbeiten des Wins-Refresh-Befehls: " + e.getMessage());
				e.printStackTrace();
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Ein Fehler ist aufgetreten: " + e.getMessage(), MessageUtil.EmbedType.ERROR)).queue();
			}
		});
		thread.setName("wins-refresh-" + event.getUser().getId());
		thread.start();
	}

	// Helper method to filter players for clan wins display
	private void filterPlayersForClanWins(ArrayList<Player> players, boolean excludeLeaders) {
		for (int i = 0; i < players.size(); i++) {
			Player p = players.get(i);
			if (p.isHiddenColeader()) {
				players.remove(i);
				i--;
				continue;
			}
			if (excludeLeaders) {
				Player.RoleType role = p.getRole();
				if (role == Player.RoleType.ADMIN || role == Player.RoleType.LEADER
						|| role == Player.RoleType.COLEADER) {
					players.remove(i);
					i--;
				}
			}
		}
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

	// Helper class to hold compact player wins result for clan display
	private static class PlayerWinsResult {
		String playerInfo;
		String playerTag;
		String clanName;
		int wins;
		boolean hasWarning;

		PlayerWinsResult(String playerInfo, String playerTag, String clanName, int wins, boolean hasWarning) {
			this.playerInfo = playerInfo;
			this.playerTag = playerTag;
			this.clanName = clanName;
			this.wins = wins;
			this.hasWarning = hasWarning;
		}
	}

	// Compact method to get player wins for clan display
	private PlayerWinsResult getPlayerWinsCompact(Player player, int year, int month, boolean isCurrentMonth,
			ZonedDateTime startOfMonth, ZonedDateTime startOfNextMonth, ZoneId zone) {

		String playerInfo = player.getInfoStringDB();
		String playerTag = player.getTag();
		String clanName = player.getClanDB() != null ? player.getClanDB().getNameDB() : "";

		// Check if any data exists for this player, if not save current data first
		if (!hasAnyWinsData(player.getTag())) {
			savePlayerWins(player.getTag());
		}

		if (isCurrentMonth) {
			// Current month: get start of month data and fetch current wins from API
			WinsRecord startRecord = getWinsAtOrAfter(player.getTag(), startOfMonth);

			Integer currentWins = player.getWinsAPI();
			if (currentWins == null || startRecord == null) {
				return new PlayerWinsResult(playerInfo, playerTag, clanName, 0, true);
			}

			int winsThisMonth = currentWins - startRecord.wins;
			boolean hasWarning = !isStartOfMonth(startRecord.recordedAt, startOfMonth);
			return new PlayerWinsResult(playerInfo, playerTag, clanName, winsThisMonth, hasWarning);
		} else {
			// Past month: get data from start of month and start of next month
			WinsRecord startRecord = getWinsAtOrAfter(player.getTag(), startOfMonth);
			WinsRecord endRecord = getWinsAtOrAfter(player.getTag(), startOfNextMonth);

			if (startRecord == null || endRecord == null) {
				return new PlayerWinsResult(playerInfo, playerTag, clanName, 0, true);
			}

			int winsInMonth = endRecord.wins - startRecord.wins;
			boolean startIsMonthStart = isStartOfMonth(startRecord.recordedAt, startOfMonth);
			boolean endIsMonthStart = isStartOfMonth(endRecord.recordedAt, startOfNextMonth);
			boolean hasWarning = !startIsMonthStart || !endIsMonthStart;

			return new PlayerWinsResult(playerInfo, playerTag, clanName, winsInMonth, hasWarning);
		}
	}

	// Static method to save wins for a player (called from scheduler and link
	// command)
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
	
	private List<String> parseClanTags(String input) {
		List<String> clanTags = new ArrayList<>();
		if (input == null || input.trim().isEmpty()) {
			return clanTags;
		}

		String[] parts = input.split(",");
		for (String part : parts) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				clanTags.add(trimmed);
			}
		}

		return clanTags;
	}

	private List<Command.Choice> getClanAutocomplete(String input, String userId) {
		List<Command.Choice> choices = new ArrayList<>();

		// Split by comma and get the last part
		String[] parts = input.split(",");
		String displayPrefix = "";
		String valuePrefix = "";
		String lastPart = "";

		// Process all parts before the last comma
		if (parts.length > 1) {
			StringBuilder displayBuilder = new StringBuilder();
			StringBuilder valueBuilder = new StringBuilder();

			for (int i = 0; i < parts.length - 1; i++) {
				String trimmed = parts[i].trim();
				if (!trimmed.isEmpty()) {
					if (i > 0) {
						displayBuilder.append(", ");
						valueBuilder.append(",");
					}
					// Add the display string as-is
					displayBuilder.append(trimmed);
					// Extract and add just the tag to value
					String extractedTag = extractClanTag(trimmed);
					valueBuilder.append(extractedTag);
				}
			}
			displayPrefix = displayBuilder.toString();
			valuePrefix = valueBuilder.toString();
		}

		// The last part is what user is currently typing
		if (parts.length > 0) {
			lastPart = parts[parts.length - 1].trim();
		}

		// Get already selected tags to avoid duplicates
		List<String> alreadySelectedTags = new ArrayList<>();
		for (int i = 0; i < parts.length - 1; i++) {
			String trimmed = parts[i].trim();
			if (!trimmed.isEmpty()) {
				String extractedTag = extractClanTag(trimmed);
				if (!extractedTag.isEmpty()) {
					alreadySelectedTags.add(extractedTag);
				}
			}
		}

		// Get clans from DBManager and filter
		List<Command.Choice> allClans = DBManager.getClansAutocomplete(lastPart);

		// Build the autocomplete choices
		for (Command.Choice clan : allClans) {
			String clanTag = clan.getAsString(); // Just the tag like #xxx
			String clanDisplay = clan.getName(); // Full display like "LOST (#xxx)"

			// Skip if already selected
			if (alreadySelectedTags.contains(clanTag)) {
				continue;
			}

			// Build the display name (what user sees)
			String choiceName = displayPrefix.isEmpty() ? clanDisplay : displayPrefix + ", " + clanDisplay;

			// Build the value (tags only, for command submission)
			String choiceValue = valuePrefix.isEmpty() ? clanTag : valuePrefix + "," + clanTag;

			choices.add(new Command.Choice(choiceName, choiceValue));

			if (choices.size() >= 25) {
				break;
			}
		}

		return choices;
	}

	// Helper method to extract clan tag from display string
	private String extractClanTag(String displayString) {
		// Format is either "ClanName (#TAG)" or just "#TAG"
		int startBracket = displayString.indexOf('(');
		int endBracket = displayString.indexOf(')');

		if (startBracket != -1 && endBracket != -1 && endBracket > startBracket) {
			// Extract the tag from within parentheses
			return displayString.substring(startBracket + 1, endBracket).trim();
		}

		// If no parentheses, assume the whole thing is the tag
		return displayString.trim();
	}
}
