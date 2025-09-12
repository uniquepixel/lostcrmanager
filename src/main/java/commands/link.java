package commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sql.DBManager;
import sql.DBUtil;
import sql.Player;
import sql.User;
import util.MessageUtil;

public class link extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("link"))
			return;
		event.deferReply().queue();
		String title = "User-Link";

		User userexecuted = new User(event.getUser().getId());
		if (!(userexecuted.getPermissions() == User.PermissionType.ADMIN
				|| userexecuted.getPermissions() == User.PermissionType.LEADER
				|| userexecuted.getPermissions() == User.PermissionType.COLEADER)) {
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
					"Du musst mindestens Vize-Anführer eines Clans sein, um diesen Befehl ausführen zu können.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		OptionMapping tagOption = event.getOption("tag");
		OptionMapping useroption = event.getOption("user");

		if (tagOption == null || useroption == null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Beide Parameter sind erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String tag = tagOption.getAsString();
		String userid = useroption.getAsMentionable().getId();

		if (api.Player.AccExists(tag)) {
			String playername = null;
			try {
				playername = api.Player.getPlayerName(tag);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!DBManager.PlayerTagIsLinked(tag)) {
				DBUtil.executeUpdate("INSERT INTO players (cr_tag, discord_id, name) VALUES (?, ?, ?)", tag, userid, playername);
				String desc = "Der Spieler " + playername + " mit dem Tag " + tag + " wurde erfolgreich mit dem User <@" + userid
						+ "> verknüpft.";
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.SUCCESS)).queue();
			} else {
				Player player = new Player(tag);
				String linkeduserid = player.getUser().getUserID();
				String desc = "Der Spieler " + playername + " mit dem Tag " + tag + " ist bereits mit <@" + linkeduserid
						+ "> verknüpft. Bitte verwende zuerst ``/unlink``.";
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.ERROR)).queue();
			}
		} else {
			String desc = "Der Spieler mit dem Tag " + tag + " existiert nicht oder es ist ein API-Fehler aufgetreten.";
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.ERROR)).queue();
		}

	}

}
