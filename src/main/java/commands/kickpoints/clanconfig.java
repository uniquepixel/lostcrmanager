package commands.kickpoints;

import java.util.List;

import datautil.DBManager;
import datautil.DBUtil;
import datawrapper.Clan;
import datawrapper.Player;
import datawrapper.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import util.MessageUtil;

public class clanconfig extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("clanconfig"))
			return;
		String title = "Clanconfig";

		OptionMapping clanOption = event.getOption("clan");

		if (clanOption == null) {
			event.replyEmbeds(
					MessageUtil.buildEmbed(title, "Der Parameter ist erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String clantag = clanOption.getAsString();

		User userexecuted = new User(event.getUser().getId());
		if (!(userexecuted.getClanRoles().get(clantag) == Player.RoleType.ADMIN
				|| userexecuted.getClanRoles().get(clantag) == Player.RoleType.LEADER
				|| userexecuted.getClanRoles().get(clantag) == Player.RoleType.COLEADER)) {
			event.replyEmbeds(MessageUtil.buildEmbed(title,
					"Du musst mindestens Vize-Anführer des Clans sein, um diesen Befehl ausführen zu können.",
					MessageUtil.EmbedType.ERROR)).queue();
			return;
		}
		
		Clan c = new Clan(clantag);

		if (!c.ExistsDB()) {
			event.replyEmbeds(MessageUtil.buildEmbed(title, "Gib einen gültigen Clan an!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}
		
		if(clantag.equals("warteliste")) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Diesen Befehl kannst du nicht auf die Warteliste ausführen.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		TextInput kpdays;
		TextInput kpmax;
		if (c.getDaysKickpointsExpireAfter() != null) {
			kpdays = TextInput.create("days", "Gültigkeitsdauer von Kickpunkten", TextInputStyle.SHORT)
					.setPlaceholder("z.B. 60").setMinLength(1).setValue(c.getDaysKickpointsExpireAfter() + "").build();
		} else {
			kpdays = TextInput.create("days", "Gültigkeitsdauer von Kickpunkten", TextInputStyle.SHORT)
					.setPlaceholder("z.B. 60").setMinLength(1).build();
		}

		if (c.getMaxKickpoints() != null) {
			kpmax = TextInput.create("max", "Maximale Anzahl an Kickpunkten", TextInputStyle.SHORT)
					.setPlaceholder("z.B. 9").setMinLength(1).setValue(c.getMaxKickpoints() + "").build();
		} else {
			kpmax = TextInput.create("max", "Maximale Anzahl an Kickpunkten", TextInputStyle.SHORT)
					.setPlaceholder("z.B. 9").setMinLength(1).build();
		}

		Modal modal = Modal.create("clanconfig_" + c.getTag(), "Clanconfig bearbeiten")
				.addActionRows(ActionRow.of(kpdays), ActionRow.of(kpmax)).build();

		event.replyModal(modal).queue();

	}

	@Override
	public void onModalInteraction(ModalInteractionEvent event) {
		if (event.getModalId().startsWith("clanconfig")) {
			event.deferReply().queue();
			String title = "Clanconfig";
			String daysstr = event.getValue("days").getAsString();
			String maxstr = event.getValue("max").getAsString();
			int days = -1;
			int max = -1;
			try {
				days = Integer.valueOf(daysstr);
				max = Integer.valueOf(maxstr);
			} catch (Exception ex) {
				event.getHook()
						.editOriginalEmbeds(
								MessageUtil.buildEmbed(title, "Es müssen Zahlen sein.", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}

			String clantag = event.getModalId().split("_")[1];

			Clan c = new Clan(clantag);

			if (c.getDaysKickpointsExpireAfter() == null) {
				DBUtil.executeUpdate(
						"INSERT INTO clan_settings (clan_tag, max_kickpoints, kickpoints_expire_after_days) VALUES (?, ?, ?)",
						clantag, max, days);
			} else {
				DBUtil.executeUpdate(
						"UPDATE clan_settings SET max_kickpoints = ?, kickpoints_expire_after_days = ? WHERE clan_tag = ?",
						max, days, clantag);
			}

			String desc = "### Die Clan-Settings wurden bearbeitet.\n";
			desc += "Clan: " + c.getInfoStringDB() + "\n";
			desc += "Gültigkeitsdauer von Kickpunkten: " + c.getDaysKickpointsExpireAfter() + " Tage\n";
			desc += "Maximale Anzahl an Kickpunkten: " + c.getMaxKickpoints() + "\n";

			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.SUCCESS))
					.queue();
		}
	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("clanconfig"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("clan")) {
			List<Command.Choice> choices = DBManager.getClansAutocompleteNoWaitlist(input);

			event.replyChoices(choices).queue();
		}
	}

}
