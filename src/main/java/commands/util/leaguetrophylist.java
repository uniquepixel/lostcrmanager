package commands.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import datautil.DBManager;
import datawrapper.Clan;
import datawrapper.Player;
import datawrapper.User;
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
		if (b == false) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Du musst mindestens Vize-Anführer eines Clans sein, um diesen Befehl ausführen zu können.",
							MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

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
			allplayers.sort(Comparator
					.comparingInt(
							(Player p) -> p.getPoLLeagueNumber() != null ? p.getPoLLeagueNumber() : Integer.MIN_VALUE)
					.thenComparingInt(p -> p.getPoLTrophies() != null ? p.getPoLTrophies() : Integer.MIN_VALUE)
					.thenComparingInt(Player::getTrophies));
			Collections.reverse(allplayers);

			for (Player p : allplayers) {
				content += p.getInfoString() + ":" + System.lineSeparator() + "  LeagueNumber: "
						+ p.getPoLLeagueNumber() + System.lineSeparator() + "  PathOfLegendTrophies: "
						+ p.getPoLTrophies() + System.lineSeparator() + "  Trophies: " + p.getTrophies()
						+ System.lineSeparator();
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
