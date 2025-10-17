package commands.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import datautil.DBManager;
import datawrapper.Clan;
import datawrapper.Player;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
			allclantags = DBManager.getAllClans();
			desc = "Hier eine Liste aller Mitglieder von Lost Clans inklusive Warteliste sortiert nach LeagueNumber bzw. Trophies.";
			break;
		case "alllost":
			allclantags = new ArrayList<>();
			ArrayList<String> allclans = DBManager.getAllClans();
			for (String tags : allclans) {
				if (!tags.equals("warteliste")) {
					allclantags.add(tags);
				}
			}
			desc = "Hier eine Liste aller Mitglieder von Lost Clans sortiert nach LeagueNumber bzw. Trophies.";
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
			desc = "Hier eine Liste aller Mitglieder des Clans " + c.getInfoString()
					+ " sortiert nach LeagueNumber bzw. Trophies.";
			break;
		}
		final ArrayList<String> clans = allclantags;

		HashMap<String, String> clantocontentstring = new HashMap<>();
		HashMap<String, Integer> clantocounter = new HashMap<>();

		ArrayList<Player> allplayers = new ArrayList<>();

		final String description = desc;

		Thread thread = new Thread(() -> {
			String content = "";
			for (String clantag : clans) {
				Clan c = new Clan(clantag);
				ArrayList<Player> playerlistclan = c.getPlayersDB();
				for (Player p : playerlistclan) {
					allplayers.add(p);
				}
			}
			allplayers.sort(Comparator.comparingInt((Player p) -> p.getRole().ordinal())
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
								+ "  Aktuelle PathOfLegendSeason-Trophäen: " + p.getPoLTrophies()
								+ System.lineSeparator() + "  Aktuelle Seasonal-Trophy-Road-Trophäen: "
								+ p.getSTRTrophies() + System.lineSeparator() + "  LeagueNumber: "
								+ p.getPoLLeagueNumber() + System.lineSeparator();
					} else {
						clanplayercontent += p.getInfoString() + role + marked + ":" + System.lineSeparator()
								+ "  Aktuelle Seasonal-Trophy-Road-Trophäen: " + p.getSTRTrophies()
								+ System.lineSeparator() + "  LeagueNumber: " + p.getPoLLeagueNumber()
								+ System.lineSeparator();
					}
				} else {
					clanplayercontent += p.getInfoString() + role + marked + ":" + System.lineSeparator()
							+ "  Aktuelle Trophäen: " + p.getTrophies() + System.lineSeparator();
				}
				String clancontent = clantocontentstring.getOrDefault(p.getClanDB().getTag(), "");
				int counter = clantocounter.getOrDefault(p.getClanDB().getTag(), 1);
				clancontent += "#" + counter + " | " + clanplayercontent;
				counter++;
				clantocounter.put(p.getClanDB().getTag(), counter);
				clantocontentstring.put(p.getClanDB().getTag(), clancontent);
			}

			for (String key : clantocontentstring.keySet()) {
				Clan c = new Clan(key);
				content += "### " + c.getInfoString() + ":\n";
				content += "\n";
				content += clantocontentstring.get(key);
				content += "\n";
				content += "---------------------\n";
				content += "\n";
			}

			ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
			event.getHook().editOriginal(inputStream, "Liste.txt")
					.setEmbeds(MessageUtil.buildEmbed(title, description, MessageUtil.EmbedType.INFO)).queue();
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
			choices.add(new Command.Choice("Alle Clans inklusive Warteliste", "all"));
			choices.add(new Command.Choice("Alle Lost-Clans", "alllost"));

			event.replyChoices(choices).queue();
		}
	}

}
