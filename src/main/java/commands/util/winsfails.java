package commands.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import commands.kickpoints.kpadd;
import datautil.Connection;
import datautil.DBManager;
import datawrapper.Clan;
import datawrapper.KickpointReason;
import datawrapper.Player;
import datawrapper.User;
import lostcrmanager.Bot;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import util.MessageUtil;

public class winsfails extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("winsfails"))
			return;
		event.deferReply().queue();
		String title = "Wins-Fails";

		OptionMapping clanOption = event.getOption("clan");
		OptionMapping thresholdOption = event.getOption("threshold");
		OptionMapping monthOption = event.getOption("month");
		OptionMapping kpreasonOption = event.getOption("kpreason");
		OptionMapping minThresholdOption = event.getOption("min_threshold");
		OptionMapping excludeLeadersOption = event.getOption("exclude_leaders");

		if (clanOption == null || thresholdOption == null || monthOption == null) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Die Parameter Clan, Threshold & Month sind verpflichtend!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String clantag = clanOption.getAsString();

		if (clantag.equals("warteliste")) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Diesen Befehl kannst du nicht auf die Warteliste ausführen.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		User userexecuted = new User(event.getUser().getId());
		if (!(userexecuted.getClanRoles().get(clantag) == Player.RoleType.ADMIN
				|| userexecuted.getClanRoles().get(clantag) == Player.RoleType.LEADER
				|| userexecuted.getClanRoles().get(clantag) == Player.RoleType.COLEADER)) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Du musst mindestens Vize-Anführer des Clans sein, um diesen Befehl ausführen zu können.",
							MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		int threshold = thresholdOption.getAsInt();

		Integer minThreshold = null;
		if (minThresholdOption != null) {
			try {
				minThreshold = Integer.parseInt(minThresholdOption.getAsString());
			} catch (NumberFormatException e) {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Der min_threshold Parameter muss eine gültige Zahl sein.", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}
		}

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

		// Parse month parameter
		String monthValue = monthOption.getAsString();
		int year;
		int month;

		if (monthValue.equals("current")) {
			ZoneId zone = ZoneId.of("Europe/Berlin");
			ZonedDateTime now = ZonedDateTime.now(zone);
			year = now.getYear();
			month = now.getMonthValue();
		} else {
			try {
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

		boolean addkp;
		String kpreasonstring;
		KickpointReason kpreason = null;
		if (kpreasonOption != null) {
			kpreasonstring = kpreasonOption.getAsString();
			kpreason = new KickpointReason(kpreasonstring, clantag);
			if (!kpreason.Exists()) {
				event.getHook().editOriginalEmbeds(
						MessageUtil.buildEmbed(title, "Diese Begründung existiert nicht.", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}
			addkp = true;
		} else {
			addkp = false;
		}

		Clan c = new Clan(clantag);

		if (!c.ExistsDB()) {
			event.getHook()
					.editOriginalEmbeds(
							MessageUtil.buildEmbed(title, "Dieser Clan existiert nicht.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		final KickpointReason kpreasontemp = kpreason;
		final Integer minThresholdFinal = minThreshold;
		final boolean excludeLeadersFinal = excludeLeaders;
		final int yearFinal = year;
		final int monthFinal = month;

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

				String monthName = Month.of(monthFinal).getDisplayName(TextStyle.FULL, Locale.GERMAN);
				String desc = "## Eine Liste aller Spieler, welche unter " + threshold + " Wins im " + monthName + " "
						+ yearFinal + " haben.\n";

				if (addkp) {
					desc += "### Da ein Kickpunkt-Grund ausgewählt wurde, wird dieser auf jeden Spieler der Liste angewandt.\n";
				}

				boolean listempty = true;

				ArrayList<Player> clanplayerlist = c.getPlayersDB();

				// Calculate wins for each player
				HashMap<String, Integer> tagToWins = new HashMap<>();
				HashMap<String, Boolean> tagToHasWarning = new HashMap<>();

				for (Player p : clanplayerlist) {
					// Skip leaders/coleaders/admins if exclude_leaders is true
					if (excludeLeadersFinal) {
						Player.RoleType role = p.getRole();
						if (role == Player.RoleType.ADMIN || role == Player.RoleType.LEADER
								|| role == Player.RoleType.COLEADER) {
							continue;
						}
					}

					// Calculate monthly wins for this player
					WinsData winsData = getPlayerMonthlyWins(p.getTag(), yearFinal, monthFinal, isCurrentMonth,
							startOfMonth, startOfNextMonth, zone);
					tagToWins.put(p.getTag(), winsData.wins);
					tagToHasWarning.put(p.getTag(), winsData.hasWarning);
				}

				ArrayList<Player> playerdonewrong = new ArrayList<>();

				for (Player p : clanplayerlist) {
					// Skip leaders/coleaders/admins if exclude_leaders is true
					if (excludeLeadersFinal) {
						Player.RoleType role = p.getRole();
						if (role == Player.RoleType.ADMIN || role == Player.RoleType.LEADER
								|| role == Player.RoleType.COLEADER) {
							continue;
						}
					}

					String playertag = p.getTag();

					if (tagToWins.containsKey(playertag)) {
						int playerWins = tagToWins.get(playertag);

						if (playerWins < threshold) {
							// Apply min_threshold check - only display and add KP if wins are >=
							// min_threshold
							if (minThresholdFinal == null || playerWins >= minThresholdFinal) {
								desc += "**" + p.getInfoStringDB() + "**:\n";
								desc += " - Wins: " + playerWins;
								boolean playerhaswarning = false;
								if (tagToHasWarning.get(playertag)) {
									playerhaswarning = true;
									desc += " ⚠️";
								}
								desc += "\n";
								if (listempty)
									listempty = false;
								if (addkp && !playerhaswarning)
									playerdonewrong.add(p);
							}
						}
					}
				}

				if (listempty) {
					desc += "**Keine Fehler anzuzeigen.**";
				}

				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO))
						.queue();

				if (!listempty) {
					if (addkp) {
						String kpmessagedesc = "### Hinzugefügte Kickpunkte:\n";
						for (Player p : playerdonewrong) {
							int id = kpadd.addKPtoDB(p.getTag(), Timestamp.from(Instant.now()), kpreasontemp,
									Bot.getJda().getSelfUser().getId());
							kpmessagedesc += MessageUtil.unformat(p.getInfoStringDB()) + ":\n";
							kpmessagedesc += " - ID: " + id + "\n";
						}
						event.getChannel()
								.sendMessageEmbeds(
										MessageUtil.buildEmbed(title, kpmessagedesc, MessageUtil.EmbedType.INFO))
								.queue();
					}
				}
			} catch (Exception e) {
				System.err.println("Fehler beim Verarbeiten des Winsfails-Befehls: " + e.getMessage());
				e.printStackTrace();
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Ein Fehler ist aufgetreten: " + e.getMessage(), MessageUtil.EmbedType.ERROR)).queue();
			}
		});
		thread.setName("winsfails-command-" + event.getUser().getId());
		thread.start();
	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("winsfails"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("clan")) {
			List<Command.Choice> choices = DBManager.getClansAutocompleteNoWaitlist(input);
			event.replyChoices(choices).queue();
		} else if (focused.equals("kpreason")) {
			List<Command.Choice> choices = DBManager.getKPReasonsAutocomplete(input,
					event.getOption("clan").getAsString());
			event.replyChoices(choices).queue();
		} else if (focused.equals("exclude_leaders")) {
			List<Command.Choice> choices = new ArrayList<>();
			if ("true".startsWith(input.toLowerCase())) {
				choices.add(new Command.Choice("true", "true"));
			}
			event.replyChoices(choices).queue();
		} else if (focused.equals("month")) {
			List<Command.Choice> choices = getMonthAutocomplete(input);
			event.replyChoices(choices).queue();
		}
	}

	// Helper class to hold wins data
	private static class WinsData {
		int wins;
		boolean hasWarning;

		WinsData(int wins, boolean hasWarning) {
			this.wins = wins;
			this.hasWarning = hasWarning;
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

	// Calculate monthly wins for a player
	private WinsData getPlayerMonthlyWins(String playerTag, int year, int month, boolean isCurrentMonth,
			ZonedDateTime startOfMonth, ZonedDateTime startOfNextMonth, ZoneId zone) {

		// Check if any data exists for this player
		if (!hasAnyWinsData(playerTag)) {
			// Try to save current data
			savePlayerWins(playerTag);
		}

		if (isCurrentMonth) {
			// Current month: get start of month data and fetch current wins from API
			WinsRecord startRecord = getWinsAtOrAfter(playerTag, startOfMonth);

			if (startRecord == null) {
				return new WinsData(0, true);
			}

			Player player = new Player(playerTag);
			Integer currentWins = player.getWinsAPI();
			if (currentWins == null) {
				return new WinsData(0, true);
			}

			int winsThisMonth = currentWins - startRecord.wins;
			boolean hasWarning = !isStartOfMonth(startRecord.recordedAt, startOfMonth);
			return new WinsData(winsThisMonth, hasWarning);
		} else {
			// Past month: get data from start of month and start of next month
			WinsRecord startRecord = getWinsAtOrAfter(playerTag, startOfMonth);
			WinsRecord endRecord = getWinsAtOrAfter(playerTag, startOfNextMonth);

			if (startRecord == null || endRecord == null) {
				return new WinsData(0, true);
			}

			int winsInMonth = endRecord.wins - startRecord.wins;
			boolean startIsMonthStart = isStartOfMonth(startRecord.recordedAt, startOfMonth);
			boolean endIsMonthStart = isStartOfMonth(endRecord.recordedAt, startOfNextMonth);
			boolean hasWarning = !startIsMonthStart || !endIsMonthStart;

			return new WinsData(winsInMonth, hasWarning);
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

	private void savePlayerWins(String playerTag) {
		// Reuse the static method from wins command to maintain consistency
		commands.wins.wins.savePlayerWins(playerTag);
	}

	private List<Command.Choice> getMonthAutocomplete(String input) {
		List<Command.Choice> choices = new ArrayList<>();
		ZoneId zone = ZoneId.of("Europe/Berlin");
		ZonedDateTime now = ZonedDateTime.now(zone);

		// Add "Aktueller Monat" option first
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
}
