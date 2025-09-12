package commands;

import java.util.List;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sql.DBManager;
import sql.DBUtil;
import sql.User;
import util.MessageUtil;

public class unlink extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("unlink"))
			return;
		event.deferReply().queue();
		String title = "User-Link";

		User userexecuted = new User(event.getUser().getId());
		if (!(userexecuted.getPermissions() == User.PermissionType.ADMIN
				|| userexecuted.getPermissions() == User.PermissionType.LEADER
				|| userexecuted.getPermissions() == User.PermissionType.COLEADER)) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Du musst mindestens Vize-Anführer eines Clans sein, um diesen Befehl ausführen zu können.",
							MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		OptionMapping tagOption = event.getOption("tag");

		if (tagOption == null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Der Parameter ist erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String tag = tagOption.getAsString();

		if (DBManager.PlayerTagIsLinked(tag)) {
			DBUtil.executeUpdate("DELETE FROM players WHERE cr_tag = ?", tag);
			String desc = "Die Verknüpfung des Spielers mit dem Tag " + tag + " wurde erfolgreich gelöscht.";
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.SUCCESS))
					.queue();
		} else {
			String desc = "Der Spieler mit dem Tag " + tag + " ist bereits nicht mehr verknüpft.";
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.ERROR))
					.queue();
		}

	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("unlink"))
			return;

		
		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();
		
		if (focused.equals("tag")) {
			List<Command.Choice> choices = DBManager.getPlayerlistAutocomplete(input, DBManager.InClanType.ALL);

			event.replyChoices(choices).queue();
		}
	}

}
