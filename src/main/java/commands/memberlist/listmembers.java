package commands.memberlist;

import java.util.ArrayList;
import java.util.Comparator;
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

public class listmembers extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("listmembers"))
			return;
		event.deferReply().queue();
		String title = "Memberliste";

		OptionMapping clanOption = event.getOption("clan");

		if (clanOption == null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Der Parameter ist erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String clantag = clanOption.getAsString();

		Clan c = new Clan(clantag);

		ArrayList<Player> playerlist = c.getPlayersDB();

		playerlist.sort(Comparator
			    .comparing(Player::isMarked).reversed()
			    .thenComparing((p1, p2) -> {
			        String name1 = p1.getNameDB() != null ? p1.getNameDB() : p1.getNameAPI();
			        String name2 = p2.getNameDB() != null ? p2.getNameDB() : p2.getNameAPI();
			        if (name1 == null && name2 == null) return 0;
			        if (name1 == null) return 1; // nulls last
			        if (name2 == null) return -1;
			        return name1.compareTo(name2);
			    }));

		String adminlist = "";
		String leaderlist = "";
		String coleaderlist = "";
		String elderlist = "";
		String memberlist = "";

		for (Player p : playerlist) {
			if (p.getRole() == Player.RoleType.ADMIN) {
				adminlist += p.getInfoString();
				if(p.isMarked()) {
					adminlist += " (✗)";
				}
				adminlist += "\n";
			}
			if (p.getRole() == Player.RoleType.LEADER) {
				leaderlist += p.getInfoString();
				if(p.isMarked()) {
					leaderlist += " (✗)";
				}
				leaderlist += "\n";
			}
			if (p.getRole() == Player.RoleType.COLEADER) {
				coleaderlist += p.getInfoString();
				if(p.isMarked()) {
					coleaderlist += " (✗)";
				}
				coleaderlist += "\n";
			}
			if (p.getRole() == Player.RoleType.ELDER) {
				elderlist += p.getInfoString();
				if(p.isMarked()) {
					elderlist += " (✗)";
				}
				elderlist += "\n";
			}
			if (p.getRole() == Player.RoleType.MEMBER) {
				memberlist += p.getInfoString();
				if(p.isMarked()) {
					memberlist += " (✗)";
				}
				memberlist += "\n";
			}
		}
		String desc = "## " + c.getInfoStringDB() + "\n";
		if (!clantag.equals("warteliste")) {
			desc += "**Admin:**\n";
			desc += adminlist == "" ? "---\n\n" : MessageUtil.unformat(adminlist) + "\n";
			desc += "**Anführer:**\n";
			desc += leaderlist == "" ? "---\n\n" : MessageUtil.unformat(leaderlist) + "\n";
			desc += "**Vize-Anführer:**\n";
			desc += coleaderlist == "" ? "---\n\n" : MessageUtil.unformat(coleaderlist) + "\n";
			desc += "**Ältester:**\n";
			desc += elderlist == "" ? "---\n\n" : MessageUtil.unformat(elderlist) + "\n";
			desc += "**Mitglied:**\n";
			desc += memberlist == "" ? "---\n\n" : MessageUtil.unformat(memberlist) + "\n";
			desc += "\nInsgesamte Mitglieder des Clans: " + playerlist.size();
		} else {
			desc += "**Wartend:**\n";
			desc += memberlist == "" ? "---\n\n" : MessageUtil.unformat(memberlist) + "\n";
			desc += "\nInsgesamte Spieler auf der Warteliste: " + playerlist.size();
		}

		event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO)).queue();

	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("listmembers"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("clan")) {
			List<Command.Choice> choices = DBManager.getClansAutocomplete(input);

			event.replyChoices(choices).queue();
		}
	}

}
