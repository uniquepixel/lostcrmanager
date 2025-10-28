package commands.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import commands.kickpoints.kpadd;
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

public class cwfails extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("cwfails"))
			return;
		event.deferReply().queue();
		String title = "CW-Fails";

		OptionMapping clanOption = event.getOption("clan");
		OptionMapping thresholdOption = event.getOption("threshold");
		OptionMapping kpreasonOption = event.getOption("kpreason");

		if (clanOption == null || thresholdOption == null) {
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
					"Die Parameter Clan & Threshold sind verpflichtend!", MessageUtil.EmbedType.ERROR)).queue();
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
			event.replyEmbeds(MessageUtil.buildEmbed(title,
					"Du musst mindestens Vize-Anführer des Clans sein, um diesen Befehl ausführen zu können.",
					MessageUtil.EmbedType.ERROR)).queue();
			return;
		}

		int threshold = thresholdOption.getAsInt();

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

		Thread thread = new Thread(() -> {

			ArrayList<Player> cwfameplayerlist = c.getCWFamePlayerList();

			String desc = "## Eine Liste aller Spieler, welche unter " + threshold + " Punkte im CW haben.\n";

			if (addkp) {
				desc += "### Da ein Kickpunkt-Grund ausgewählt wurde, wird dieser auf jeden Spieler der Liste angewandt.\n";
			}

			boolean listempty = true;

			ArrayList<Player> clanplayerlist = c.getPlayersDB();

			HashMap<String, Integer> tagtocwfame = new HashMap<>();
			HashMap<String, String> tagtoclantagcwdone = new HashMap<>();

			for (Player p : cwfameplayerlist) {
				tagtocwfame.put(p.getTag(), p.getCWFame());
				tagtoclantagcwdone.put(p.getTag(), p.getClantagCWDone());
			}

			ArrayList<Player> playerdonewrong = new ArrayList<>();

			for (Player p : clanplayerlist) {
				String playertag = p.getTag();
				if (tagtocwfame.containsKey(playertag)) {
					if (tagtoclantagcwdone.get(playertag).equals(p.getClanDB().getTag())) {
						if (tagtocwfame.get(playertag) < threshold) {
							if (tagtoclantagcwdone.get(playertag).equals(p.getClanDB().getTag())) {
								desc += "**" + p.getInfoString() + "**:\n";
								desc += " - Punkte: " + p.getCWFame() + ".\n";
								if (listempty)
									listempty = false;
								if (addkp)
									playerdonewrong.add(p);
							}
						}
					} else {
						desc += "**" + p.getInfoString() + "**:\n";
						desc += " - Nicht im Clan gemacht.\n";
						if (listempty)
							listempty = false;
						if (addkp)
							playerdonewrong.add(p);
					}
				} else {
					desc += "**" + p.getInfoString() + "**:\n";
					desc += " - Nicht im Clan gemacht.\n";
					if (listempty)
						listempty = false;
					if (addkp)
						playerdonewrong.add(p);
				}
			}

			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO)).queue();

			if (!listempty) {
				if (addkp) {
					String kpmessagedesc = "### Hinzugefügte Kickpunkte:\n";
					for (Player p : playerdonewrong) {
						int id = kpadd.addKPtoDB(p.getTag(), Timestamp.from(Instant.now()), kpreasontemp,
								Bot.getJda().getSelfUser().getId());
						kpmessagedesc += MessageUtil.unformat(p.getInfoString()) + ":\n";
						kpmessagedesc += " - ID: " + id + "\n";
					}
					event.getChannel()
							.sendMessageEmbeds(MessageUtil.buildEmbed(title, kpmessagedesc, MessageUtil.EmbedType.INFO))
							.queue();
				}
			} else {
				desc += "**Keine Fehler anzuzeigen.**";
			}

		});
		thread.start();

	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("cwfails"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("clan")) {
			List<Command.Choice> choices = DBManager.getClansAutocompleteNoWaitlist(input);

			event.replyChoices(choices).queue();
		}
		if (focused.equals("kpreason")) {
			List<Command.Choice> choices = DBManager.getKPReasonsAutocomplete(input,
					event.getOption("clan").getAsString());

			event.replyChoices(choices).queue();
		}
	}

}
