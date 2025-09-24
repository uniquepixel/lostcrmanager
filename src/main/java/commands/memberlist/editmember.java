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
import sql.DBUtil;
import sql.Player;
import sql.User;
import util.MessageUtil;

public class editmember extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("editmember"))
			return;
		event.deferReply().queue();
		String title = "Memberverwaltung";

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

		OptionMapping playeroption = event.getOption("player");
		OptionMapping roleoption = event.getOption("role");

		if (playeroption == null || roleoption == null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Alle Parameter sind erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String playertag = playeroption.getAsString();
		String role = roleoption.getAsString();

		if (!(role.equals("leader") || role.equals("coleader") || role.equals("elder") || role.equals("member"))) {
			event.getHook()
					.editOriginalEmbeds(
							MessageUtil.buildEmbed(title, "Gib eine gültige Rolle an.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}
		if (role.equals("leader") && userexecuted.getPermissions() != User.PermissionType.ADMIN) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Um jemanden als Leader hinzuzufügen, musst du Admin sein.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}
		if (role.equals("coleader") && !(userexecuted.getPermissions() == User.PermissionType.ADMIN
				|| userexecuted.getPermissions() == User.PermissionType.LEADER)) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Um jemanden als Vize-Anführer hinzuzufügen, musst du Admin oder Anführer sein.",
							MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		if (!DBManager.PlayerTagIsLinked(playertag)) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Dieser Spieler ist nicht verlinkt.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}
		Player p = new Player(playertag);
		Clan c = p.getClan();

		if (c == null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Der Spieler ist in keinem Clan.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		DBUtil.executeUpdate("UPDATE clan_members SET clan_role = ? WHERE player_tag = ?", role, playertag);
		String rolestring = role.equals("leader") ? "Anführer"
				: role.equals("coleader") ? "Vize-Anführer"
						: role.equals("elder") ? "Ältester" : role.equals("member") ? "Mitglied" : null;
		String desc = null;
		try {
			desc = "Der Spieler " + MessageUtil.unformat(p.getInfoString()) + " im Clan " + c.getInfoString()
					+ " ist nun " + rolestring + ".";
		} catch (Exception e) {
			e.printStackTrace();
		}
		event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.SUCCESS)).queue();

	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("editmember"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("player")) {
			List<Command.Choice> choices = DBManager.getPlayerlistAutocomplete(input, DBManager.InClanType.INCLAN);

			event.replyChoices(choices).queue();
		}
		if (focused.equals("role")) {
			List<Command.Choice> choices = new ArrayList<>();
			choices.add(new Command.Choice("Anführer", "leader"));
			choices.add(new Command.Choice("Vize-Anführer", "coleader"));
			choices.add(new Command.Choice("Ältester", "elder"));
			choices.add(new Command.Choice("Mitglied", "member"));
			event.replyChoices(choices).queue();
		}
	}

}
