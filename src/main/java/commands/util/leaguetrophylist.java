package commands.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import datautil.DBManager;
import datawrapper.Clan;
import datawrapper.Player;
import datawrapper.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import util.MessageUtil;

public class leaguetrophylist extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("leaguetrophylist"))
			return;
		event.deferReply().queue();
		String title = "League-Trophy Liste";

		OptionMapping clanOption = event.getOption("clan");

		if (clanOption == null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Der Parameter Clan ist erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String clantagoption = clanOption.getAsString();

		String desc = "";
		ArrayList<String> allclantags = null;

		switch (clantagoption) {
		case "all":
			boolean b = false;
			User userexecuted = new User(event.getUser().getId());
			for (String clantag : DBManager.getAllClans()) {
				if (userexecuted.getClanRoles().get(clantag) == Player.RoleType.ADMIN
						|| userexecuted.getClanRoles().get(clantag) == Player.RoleType.LEADER
						|| userexecuted.getClanRoles().get(clantag) == Player.RoleType.COLEADER) {
					b = true;
					break;
				}
			}
			if (!b) {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Du musst mindestens Vize-Anführer eines Clans sein, um diesen Befehl mit diesem Parameterausführen zu können.",
						MessageUtil.EmbedType.ERROR)).queue();
				return;
			}
			allclantags = DBManager.getAllClans();
			desc = "Hier eine Liste aller Mitglieder von Lost Clans inklusive Warteliste sortiert nach LeagueNumber bzw. Trophies.";
			break;
		default:
			allclantags = new ArrayList<>();
			Clan c = new Clan(clantagoption);
			if (c.ExistsDB()) {
				allclantags.add(clantagoption);
			} else {
				event.getHook().editOriginalEmbeds(
						MessageUtil.buildEmbed(title, "Diese Auswahl funktioniert nicht.", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}
			desc = "Hier eine Liste aller Mitglieder des Clans " + c.getInfoStringDB()
					+ " sortiert nach LeagueNumber bzw. Trophies.";
			break;
		}
		final ArrayList<String> clans = allclantags;

		final String description = desc;

		Thread thread = new Thread(() -> {
			ArrayList<Player> allplayers = new ArrayList<>();
			String content = "";
			for (int a = 0; a < clans.size(); a++) {
				String clantag = clans.get(a);
				Clan c = new Clan(clantag);
				ArrayList<Player> playerlistclan = c.getPlayersDB();
				for (int i = 0; i < playerlistclan.size(); i++) {
					event.getHook()
							.editOriginalEmbeds(MessageUtil.buildEmbed(title,
									"Lade Spieler " + (i + 1) + " / " + playerlistclan.size()
											+ (clans.size() > 1 ? " aus Clan " + (a + 1) + " / " + clans.size() : "")
											+ " von Datenbank in den Cache...",
									MessageUtil.EmbedType.LOADING))
							.queue();
					allplayers.add(playerlistclan.get(i));
				}
			}

			if (clantagoption.equals("all")) {

				allplayers = sortPlayers(allplayers, event.getHook());

				HashMap<String, Integer> clantagtomembercount = new HashMap<>();

				HashMap<String, ArrayList<String>> clantagtoadminstrings = new HashMap<>();
				HashMap<String, ArrayList<String>> clantagtoleaderstrings = new HashMap<>();
				HashMap<String, ArrayList<String>> clantagtocoleaderstrings = new HashMap<>();

				HashMap<String, ArrayList<String>> clantagtomarkedstrings = new HashMap<>();

				ArrayList<String> playerstringssorted = new ArrayList<>();

				for (int i = 0; i < allplayers.size(); i++) {
					event.getHook()
							.editOriginalEmbeds(MessageUtil.buildEmbed(title,
									"Lade Spieler " + (i + 1) + " / " + allplayers.size() + " in die Listen...",
									MessageUtil.EmbedType.LOADING))
							.queue();
					Player p = allplayers.get(i);
					String clantag = p.getClanDB().getTag();

					if (p.getRole() == Player.RoleType.ADMIN) {
						ArrayList<String> adminstrings = clantagtoadminstrings.getOrDefault(clantag, new ArrayList<>());
						adminstrings.add(formatPlayerLine(p));
						clantagtoadminstrings.put(clantag, adminstrings);
						clantagtomembercount.put(clantag, clantagtomembercount.getOrDefault(clantag, 0) + 1);
					} else if (p.getRole() == Player.RoleType.LEADER) {
						ArrayList<String> leaderstrings = clantagtoleaderstrings.getOrDefault(clantag,
								new ArrayList<>());
						leaderstrings.add(formatPlayerLine(p));
						clantagtoleaderstrings.put(clantag, leaderstrings);
						clantagtomembercount.put(clantag, clantagtomembercount.getOrDefault(clantag, 0) + 1);
					} else if (p.getRole() == Player.RoleType.COLEADER) {
						ArrayList<String> coleaderstrings = clantagtocoleaderstrings
								.getOrDefault(p.getClanDB().getTag(), new ArrayList<>());
						coleaderstrings.add(formatPlayerLine(p));
						clantagtocoleaderstrings.put(clantag, coleaderstrings);
						clantagtomembercount.put(clantag, clantagtomembercount.getOrDefault(clantag, 0) + 1);
					} else if (p.isMarked()) {
						ArrayList<String> markedstrings = clantagtomarkedstrings.getOrDefault(clantag,
								new ArrayList<>());
						markedstrings.add(formatPlayerLine(p));
						clantagtomarkedstrings.put(clantag, markedstrings);
						clantagtomembercount.put(clantag, clantagtomembercount.getOrDefault(clantag, 0) + 1);
					} else {
						playerstringssorted.add(formatPlayerLine(p));
					}
				}

				int counter = 1;

				for (String tags : clans) {
					Clan c = new Clan(tags);
					event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Konstruire Clan " + c.getInfoStringDB() + " in der Datei...", MessageUtil.EmbedType.LOADING))
							.queue();
					content += c.getInfoStringDB() + "\n\n";

					boolean coleadersexist = false;

					for (String s : clantagtoadminstrings.getOrDefault(tags, new ArrayList<>())) {
						if (!coleadersexist)
							coleadersexist = true;
						content += "#" + counter + " ";
						counter++;
						content += s;
					}

					for (String s : clantagtoleaderstrings.getOrDefault(tags, new ArrayList<>())) {
						if (!coleadersexist)
							coleadersexist = true;
						content += "#" + counter + " ";
						counter++;
						content += s;
					}

					for (String s : clantagtocoleaderstrings.getOrDefault(tags, new ArrayList<>())) {
						if (!coleadersexist)
							coleadersexist = true;
						content += "#" + counter + " ";
						counter++;
						content += s;
					}

					if (coleadersexist) {
						content += "----------------------------\n\n";
					}

					boolean markedsexist = false;

					for (String s : clantagtomarkedstrings.getOrDefault(tags, new ArrayList<>())) {
						if (!markedsexist)
							markedsexist = true;
						content += "#" + counter + " ";
						counter++;
						content += s;
					}

					if (markedsexist) {
						content += "----------------------------\n\n";
					}

					int fillup;

					if (!tags.equalsIgnoreCase("warteliste")) {
						fillup = 50 - clantagtomembercount.getOrDefault(tags, 0);
					} else {
						fillup = playerstringssorted.size() - 1;
					}

					for (int j = 0; j < fillup && playerstringssorted.size() > 0; j++) {
						content += "#" + counter + " ";
						counter++;
						content += playerstringssorted.get(0);
						playerstringssorted.remove(0);
					}

					content += "-----------------------------------------------------------\n\n";
				}

				ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
				event.getHook().editOriginal(inputStream, "Liste.txt")
						.setEmbeds(MessageUtil.buildEmbed(title, description, MessageUtil.EmbedType.INFO)).queue();
			} else {
				HashMap<String, String> clantocontentstring = new HashMap<>();
				HashMap<String, Integer> clantocounter = new HashMap<>();
				event.getHook()
						.editOriginalEmbeds(
								MessageUtil.buildEmbed(title, "Lade Daten der API...", MessageUtil.EmbedType.LOADING))
						.queue();
				allplayers.sort(Comparator
						.comparingInt((Player p) -> p.getRole() == Player.RoleType.ELDER
								|| p.getRole() == Player.RoleType.MEMBER ? Player.RoleType.ELDER.ordinal()
										: p.getRole().ordinal())
						.thenComparing(Comparator.comparing(Player::isMarked).reversed()) // true zuerst
						.thenComparing(Comparator.comparingInt((Player p) -> p.getPoLLeagueNumber() != null
								? (p.getPoLLeagueNumber() != 1 ? p.getPoLLeagueNumber() : Integer.MIN_VALUE)
								: Integer.MIN_VALUE).reversed())
						.thenComparing(Comparator.comparingInt((Player p) -> p.getPoLTrophies() != null
								? (p.getPoLTrophies() != 0 ? p.getPoLTrophies() : Integer.MIN_VALUE)
								: Integer.MIN_VALUE).reversed())
						.thenComparing(Comparator.comparingInt((Player p) -> p.getSTRTrophies() != null
								? (p.getSTRTrophies() != 10000 ? p.getSTRTrophies() : Integer.MIN_VALUE)
								: Integer.MIN_VALUE).reversed())
						.thenComparing(Comparator.comparingInt(Player::getTrophies).reversed()));

				for (int i = 1; i <= allplayers.size(); i++) {
					event.getHook()
							.editOriginalEmbeds(MessageUtil.buildEmbed(title,
									"Lade Spieler " + i + " / " + allplayers.size() + " in die Datei...",
									MessageUtil.EmbedType.LOADING))
							.queue();
					Player p = allplayers.get(i - 1);
					String clanplayercontent = "";
					String marked = p.isMarked() ? " (✗)" : "";
					String role = p.getRole() == Player.RoleType.ADMIN ? " [Admin]"
							: p.getRole() == Player.RoleType.LEADER ? " [Leader]"
									: p.getRole() == Player.RoleType.COLEADER ? " [Co-Leader]"
											: p.getRole() == Player.RoleType.ELDER ? " [Elder]"
													: p.getRole() == Player.RoleType.MEMBER ? " [Member]" : "";
					if (p.getTrophies() == 10000) {
						if (p.getPoLLeagueNumber() == 7) {
							clanplayercontent += p.getInfoString() + role + marked + ":" + System.lineSeparator()
									+ "  LeagueNumber: " + p.getPoLLeagueNumber() + System.lineSeparator()
									+ "  Aktuelle PathOfLegendSeason-Trophäen: " + p.getPoLTrophies()
									+ System.lineSeparator() + "  Aktuelle Seasonal-Trophy-Road-Trophäen: "
									+ p.getSTRTrophies() + System.lineSeparator() + System.lineSeparator();
						} else {
							clanplayercontent += p.getInfoString() + role + marked + ":" + System.lineSeparator()
									+ "  LeagueNumber: " + p.getPoLLeagueNumber() + System.lineSeparator()
									+ "  Aktuelle Seasonal-Trophy-Road-Trophäen: " + p.getSTRTrophies()
									+ System.lineSeparator() + System.lineSeparator();
						}
					} else {
						clanplayercontent += p.getInfoString() + role + marked + ":" + System.lineSeparator()
								+ "  Aktuelle Trophäen: " + p.getTrophies() + System.lineSeparator()
								+ System.lineSeparator();
					}
					String clancontent = clantocontentstring.getOrDefault(p.getClanDB().getTag(), "");
					int counter = clantocounter.getOrDefault(p.getClanDB().getTag(), 1);
					clancontent += "#" + counter + " | " + clanplayercontent;
					counter++;
					clantocounter.put(p.getClanDB().getTag(), counter);
					clantocontentstring.put(p.getClanDB().getTag(), clancontent);
				}

				for (String key : clans) {
					if (clantocontentstring.containsKey(key)) {
						Clan c = new Clan(key);
						content += "### " + c.getInfoStringDB() + ":\n";
						content += "\n";
						content += clantocontentstring.get(key);
						content += "\n";
						content += "---------------------\n";
						content += "\n";
					}
				}
				ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
				event.getHook().editOriginal(inputStream, "Liste.txt")
						.setEmbeds(MessageUtil.buildEmbed(title, description, MessageUtil.EmbedType.INFO)).queue();
			}
		});
		thread.start();

	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("leaguetrophylist"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("clan")) {
			List<Command.Choice> choices = DBManager.getClansAutocomplete(input);

			boolean b = false;
			User userexecuted = new User(event.getUser().getId());
			for (String clantag : DBManager.getAllClans()) {
				if (userexecuted.getClanRoles().get(clantag) == Player.RoleType.ADMIN
						|| userexecuted.getClanRoles().get(clantag) == Player.RoleType.LEADER
						|| userexecuted.getClanRoles().get(clantag) == Player.RoleType.COLEADER) {
					b = true;
					break;
				}
			}
			if (b) {
				choices.add(new Command.Choice("Alle Clans inklusive Warteliste", "all"));
			}

			event.replyChoices(choices).queue();
		}
	}

	private String formatPlayerLine(Player p) {
		boolean isMarked = p.isMarked();
		// Status bestimmen
		String status;
		if (p.getClanDB() == null) {
			status = "[Warteschlange]";
		} else {
			String role = "";
			if (p.getRole() == Player.RoleType.ADMIN) {
				role = "Admin";
			} else if (p.getRole() == Player.RoleType.LEADER) {
				role = "Anführer";
			} else if (p.getRole() == Player.RoleType.COLEADER) {
				role = "Vize-Anführer";
			} else if (p.getRole() == Player.RoleType.ELDER) {
				role = "Ältester";
			} else if (p.getRole() == Player.RoleType.MEMBER) {
				role = "Mitglied";
			}
			status = "[" + role + " " + p.getClanDB().getNameDB() + "]";
		}
		if (isMarked) {
			status += " will nicht aufsteigen";
		}

		// Trophäen- und Liga-Infos
		int poLTrophies = p.getPoLTrophies() != null ? p.getPoLTrophies() : 0;
		int strTrophies = p.getSTRTrophies() != null ? p.getSTRTrophies() : 0;
		int trophies = p.getTrophies() != null ? p.getTrophies() : 0;
		int leagueNumber = p.getPoLLeagueNumber() != null ? p.getPoLLeagueNumber() : 0;

		// Wenn trophies == 10.000: STR-Trophies ausgeben
		int displayTrophies = trophies == 10000 ? strTrophies : trophies;

		// Ausgabeformat wie im Beispiel
		return String.format(
				"%s (%s) %s\n LeagueNumber: %d\n Aktuelle PathOfLegendSeason-Trophäen: %d\n Aktuelle Seasonal-Trophy-Road-Trophäen: %d\n\n",
				p.getNameDB(), p.getTag(), status, leagueNumber, poLTrophies, displayTrophies);
	}

	public static ArrayList<Player> sortPlayers(ArrayList<Player> players, InteractionHook hook) {
		String title = "League-Trophy Liste";
		List<Player> sortedList = players.stream().sorted(new Comparator<Player>() {
			int i = 1;

			@Override
			public int compare(Player p1, Player p2) {

				hook.editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Lade Spieler von der API und sortiere sie in die Liste... (Sortiervorgang #" + i + ")",
						MessageUtil.EmbedType.LOADING)).queue();
				i++;
				int league1 = p1.getPoLLeagueNumber() != null ? p1.getPoLLeagueNumber() : 0;
				int league2 = p2.getPoLLeagueNumber() != null ? p2.getPoLLeagueNumber() : 0;

				// Primär: LeagueNumber absteigend
				if (league1 != league2) {
					return Integer.compare(league2, league1);
				}

				// In Liga 7: nur nach Medaillen sortieren
				if (league1 == 7 && league2 == 7) {
					int medals1 = p1.getPoLTrophies() != null ? p1.getPoLTrophies() : 0;
					int medals2 = p2.getPoLTrophies() != null ? p2.getPoLTrophies() : 0;
					return Integer.compare(medals2, medals1);
				}

				// Sonst: Trophäen (bei 10.000 STR-Trophäen)
				int trophies1 = (p1.getTrophies() != null && p1.getTrophies() == 10000)
						? (p1.getSTRTrophies() != null ? p1.getSTRTrophies() : 0)
						: (p1.getTrophies() != null ? p1.getTrophies() : 0);
				int trophies2 = (p2.getTrophies() != null && p2.getTrophies() == 10000)
						? (p2.getSTRTrophies() != null ? p2.getSTRTrophies() : 0)
						: (p2.getTrophies() != null ? p2.getTrophies() : 0);

				return Integer.compare(trophies2, trophies1);
			}
		}).collect(Collectors.toList());

		return new ArrayList<>(sortedList);
	}

}
