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
import sql.User;
import util.MessageUtil;

public class addmember extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("addmember"))
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

		OptionMapping clanOption = event.getOption("clan");
		OptionMapping playeroption = event.getOption("player");
		OptionMapping roleoption = event.getOption("role");

		if (clanOption == null || playeroption == null || roleoption == null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Alle Parameter sind erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String playertag = playeroption.getAsString();
		String clantag = clanOption.getAsString();
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

		if (!DBManager.ClanExists(clantag)) {
			event.getHook()
					.editOriginalEmbeds(
							MessageUtil.buildEmbed(title, "Dieser Clan existiert nicht.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		if (new sql.Player(playertag).getClan() != null) {
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
					"Dieser Spieler ist bereits in einem Clan.", MessageUtil.EmbedType.ERROR)).queue();
			return;
		}
		sql.Player p = new sql.Player(playertag);
		DBUtil.executeUpdate("INSERT INTO clan_members (player_tag, clan_tag, clan_role) VALUES (?, ?, ?)", playertag,
				clantag, role);
		String rolestring = role.equals("leader") ? "Anführer"
				: role.equals("coleader") ? "Vize-Anführer"
						: role.equals("elder") ? "Ältester" : role.equals("member") ? "Mitglied" : null;
		String desc = null;
		try {
			desc = "Der Spieler " + MessageUtil.unformat(p.getInfoString()) + " wurde erfolgreich dem Clan "
					+ new Clan(clantag).getInfoString() + " als " + rolestring + " hinzugefügt.";
		} catch (Exception e) {
			e.printStackTrace();
		}
		event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.SUCCESS)).queue();

	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("addmember"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("clan")) {
			List<Command.Choice> choices = DBManager.getClansAutocomplete(input);

			event.replyChoices(choices).queue();
		}
		if (focused.equals("player")) {
			List<Command.Choice> choices = DBManager.getPlayerlistAutocomplete(input, DBManager.InClanType.NOTINCLAN);

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
