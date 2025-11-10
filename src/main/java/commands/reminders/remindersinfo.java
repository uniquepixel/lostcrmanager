package commands.reminders;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import datautil.Connection;
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

public class remindersinfo extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("remindersinfo"))
			return;
		event.deferReply().queue();
		String title = "Reminder-Informationen";

		OptionMapping clanOption = event.getOption("clan");

		if (clanOption == null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Der Parameter Clan ist erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String clantag = clanOption.getAsString();

		Clan c = new Clan(clantag);

		if (!c.ExistsDB()) {
			event.getHook()
					.editOriginalEmbeds(
							MessageUtil.buildEmbed(title, "Dieser Clan existiert nicht.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		if (clantag.equals("warteliste")) {
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
					"Diesen Befehl kannst du nicht auf die Warteliste ausführen.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		User userexecuted = new User(event.getUser().getId());
		if (!(userexecuted.getClanRoles().get(clantag) == Player.RoleType.ADMIN
				|| userexecuted.getClanRoles().get(clantag) == Player.RoleType.LEADER
				|| userexecuted.getClanRoles().get(clantag) == Player.RoleType.COLEADER)) {
			event.getHook()
					.editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Du musst mindestens Vize-Anführer des Clans sein, um diesen Befehl ausführen zu können.",
							MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		// Get all reminders for this clan
		ArrayList<ReminderInfo> reminders = new ArrayList<>();
		String sql = "SELECT id, channelid, time FROM reminders WHERE clantag = ? ORDER BY time";

		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			pstmt.setString(1, clantag);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					int id = rs.getInt("id");
					String channelId = rs.getString("channelid");
					Time time = rs.getTime("time");
					reminders.add(new ReminderInfo(id, channelId, time));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		String desc = "### Reminder für Clan: " + c.getInfoStringDB() + "\n\n";

		if (reminders.isEmpty()) {
			desc += "Keine Reminder konfiguriert.\n";
		} else {
			desc += "```\n";
			desc += String.format("%-5s %-20s %-10s\n", "ID", "Kanal", "Zeit");
			desc += "---------------------------------------------\n";
			for (ReminderInfo reminder : reminders) {
				desc += String.format("%-5d %-20s %-10s\n", reminder.id, "<#" + reminder.channelId + ">",
						reminder.time.toLocalTime().toString());
			}
			desc += "```\n";
			desc += "\nReminder werden Donnerstag, Freitag, Samstag und Sonntag zur konfigurierten Zeit gesendet.\n";
			desc += "Sie erinnern Spieler, die heute weniger als 4 Decks verwendet haben.";
		}

		event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO)).queue();
	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("remindersinfo"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("clan")) {
			List<Command.Choice> choices = DBManager.getClansAutocompleteNoWaitlist(input);
			event.replyChoices(choices).queue();
		}
	}

	private static class ReminderInfo {
		int id;
		String channelId;
		Time time;

		ReminderInfo(int id, String channelId, Time time) {
			this.id = id;
			this.channelId = channelId;
			this.time = time;
		}
	}

}
