package commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import sql.User;
import util.MessageUtil;

public class restart extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("restart"))
			return;
		event.deferReply().queue();
		String title = "Restart";

		User userexecuted = new User(event.getUser().getId());
		if (!(userexecuted.getPermissions() == User.PermissionType.ADMIN)) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Du musst Admin sein, um diesen Befehl ausführen zu können.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		event.getHook()
				.editOriginalEmbeds(
						MessageUtil.buildEmbed(title, "Der Bot wird neugestartet.", MessageUtil.EmbedType.SUCCESS))
				.queue();
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.exit(0);

	}
}
