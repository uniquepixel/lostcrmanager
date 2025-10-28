package commands.kickpoints;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import datautil.DBManager;
import datawrapper.Clan;
import datawrapper.Kickpoint;
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

public class kpclan extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("kpclan"))
			return;
		event.deferReply().queue();
		String title = "Aktive Kickpunkte des Clans";

		OptionMapping clanOption = event.getOption("clan");

		if (clanOption == null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Der Parameter Clan ist erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String desc = "";

		String clantag = clanOption.getAsString();

		if (clantag.equals("warteliste")) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Diesen Befehl kannst du nicht auf die Warteliste ausführen.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		ArrayList<Player> playerlist = new ArrayList<>();

		if (clantag.equals("all")) {
			for (String clantags : DBManager.getAllClans()) {
				if (!clantags.equals("warteliste"))
					playerlist.addAll(new Clan(clantags).getPlayersDB());
			}
			desc = "### Kickpunkte aller Spieler aller Clans:\n";
		} else {
			Clan c = new Clan(clantag);

			if (!c.ExistsDB()) {
				event.getHook().editOriginalEmbeds(
						MessageUtil.buildEmbed(title, "Dieser Clan existiert nicht.", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}
			playerlist.addAll(c.getPlayersDB());
			desc = "### Kickpunkte aller Spieler des Clans " + c.getInfoString() + ":\n";
		}

		HashMap<String, Integer> kpamounts = new HashMap<>();

		for (Player p : playerlist) {
			ArrayList<Kickpoint> activekps = p.getActiveKickpoints();

			int totalkps = 0;
			for (Kickpoint kpi : activekps) {
				totalkps += kpi.getAmount();
			}
			if (totalkps > 0) {
				kpamounts.put(p.getInfoString(), totalkps);
			}
		}

		LinkedHashMap<String, Integer> sorted = kpamounts.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		// Ausgabe sortiert
		for (String key : sorted.keySet()) {
			String kp = sorted.get(key) == 1 ? "Kickpunkt" : "Kickpunkte";
			desc += key + ": " + sorted.get(key) + " " + kp + "\n\n";
		}

		LocalDateTime jetzt = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm 'Uhr'");
		String formatiert = jetzt.atZone(ZoneId.of("Europe/Berlin")).format(formatter);

		event.getHook()
				.editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO,
						"Zuletzt aktualisiert am " + formatiert))
				.setActionRow(Button.secondary("kpclan_" + clantag, "\u200B").withEmoji(Emoji.fromUnicode("🔁")))
				.queue();

	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("kpclan"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("clan")) {
			List<Command.Choice> choices = DBManager.getClansAutocompleteNoWaitlist(input);

			event.replyChoices(choices).queue();
		}
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		String id = event.getComponentId();
		if (!id.startsWith("kpclan_"))
			return;

		event.deferEdit().queue();

		String clantag = id.substring("kpclan_".length());
		String title = "Aktive Kickpunkte des Clans";

		event.getInteraction().getHook()
				.editOriginalEmbeds(MessageUtil.buildEmbed(title, "Wird geladen...", MessageUtil.EmbedType.LOADING))
				.queue();
		String descr;
		ArrayList<Player> playerlist = new ArrayList<>();
		
		if (clantag.equals("all")) {
			for (String clantags : DBManager.getAllClans()) {
				if (!clantags.equals("warteliste"))
					playerlist.addAll(new Clan(clantags).getPlayersDB());
			}
			descr = "### Kickpunkte aller Spieler aller Clans:\n";
		} else {
			Clan c = new Clan(clantag);

			if (!c.ExistsDB()) {
				event.getHook().editOriginalEmbeds(
						MessageUtil.buildEmbed(title, "Dieser Clan existiert nicht.", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}
			playerlist.addAll(c.getPlayersDB());
			descr = "### Kickpunkte aller Spieler des Clans " + c.getInfoString() + ":\n";
		}

		
		HashMap<String, Integer> kpamounts = new HashMap<>();

		new Thread(new Runnable() {
			String desc = descr;

			@Override
			public void run() {

				for (Player p : playerlist) {
					ArrayList<Kickpoint> activekps = p.getActiveKickpoints();

					int totalkps = 0;
					for (Kickpoint kpi : activekps) {
						totalkps += kpi.getAmount();
					}
					if (totalkps > 0) {
						kpamounts.put(p.getInfoString(), totalkps);
					}
				}

				LinkedHashMap<String, Integer> sorted = kpamounts.entrySet().stream()
						.sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).collect(Collectors
								.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

				// Ausgabe sortiert
				for (String key : sorted.keySet()) {
					String kp = sorted.get(key) == 1 ? "Kickpunkt" : "Kickpunkte";
					desc += key + ": " + sorted.get(key) + " " + kp + "\n\n";
				}

				LocalDateTime jetzt = LocalDateTime.now();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm 'Uhr'");
				String formatiert = jetzt.atZone(ZoneId.of("Europe/Berlin")).format(formatter);

				event.getInteraction().getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc,
						MessageUtil.EmbedType.INFO, "Zuletzt aktualisiert am " + formatiert)).queue();

			}
		}).start();
	}

}
