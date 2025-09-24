package commands.memberlist;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sql.Clan;
import sql.DBManager;
import sql.Player;
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

		ArrayList<Player> playerlist = c.getPlayers();

		String leaderlist = "";
		String coleaderlist = "";
		String elderlist = "";
		String memberlist = "";

		for (Player p : playerlist) {
			if (p.getRole() == Player.RoleType.LEADER) {
				leaderlist += p.getInfoString() + "\n";
			}
			if (p.getRole() == Player.RoleType.COLEADER) {
				coleaderlist += p.getInfoString() + "\n";
			}
			if (p.getRole() == Player.RoleType.ELDER) {
				elderlist += p.getInfoString() + "\n";
			}
			if (p.getRole() == Player.RoleType.MEMBER) {
				memberlist += p.getInfoString() + "\n";
			}
		}
		String desc = "## " + c.getInfoString() + "\n";
		desc += "**Anführer:**\n";
		desc += leaderlist == "" ? "---\n\n" : MessageUtil.unformat(leaderlist) + "\n";
		desc += "**Vize-Anführer:**\n";
		desc += coleaderlist == "" ? "---\n\n" : MessageUtil.unformat(coleaderlist) + "\n";
		desc += "**Ältester:**\n";
		desc += elderlist == "" ? "---\n\n" : MessageUtil.unformat(elderlist) + "\n";
		desc += "**Mitglied:**\n";
		desc += memberlist == "" ? "---\n\n" : MessageUtil.unformat(memberlist) + "\n";
		desc += "\nInsgesamte Mitglieder des Clans: " + playerlist.size();

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
