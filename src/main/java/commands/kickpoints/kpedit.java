package commands.kickpoints;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import sql.DBUtil;
import sql.Kickpoint;
import sql.User;
import util.MessageUtil;

public class kpedit extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("kpedit"))
			return;
		String title = "Kickpunkte";

		User userexecuted = new User(event.getUser().getId());
		if (!(userexecuted.getPermissions() == User.PermissionType.ADMIN
				|| userexecuted.getPermissions() == User.PermissionType.LEADER
				|| userexecuted.getPermissions() == User.PermissionType.COLEADER)) {
			event.replyEmbeds(MessageUtil.buildEmbed(title,
					"Du musst mindestens Vize-Anführer eines Clans sein, um diesen Befehl ausführen zu können.",
					MessageUtil.EmbedType.ERROR)).queue();
			return;
		}

		OptionMapping idOption = event.getOption("id");

		if (idOption == null) {
			event.replyEmbeds(
					MessageUtil.buildEmbed(title, "Der Parameter id ist erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		int id = event.getOption("id").getAsInt();

		Kickpoint kp = new Kickpoint(id);

		String desc = "";

		if (kp.getDescription() != null) {
			TextInput reasonti = TextInput.create("reason", "Grund", TextInputStyle.SHORT)
					.setPlaceholder("z.B. CW Angriff").setValue(kp.getDescription()).setMinLength(1).build();
			TextInput kpamountti = TextInput.create("amount", "Anzahl Kickpunkte", TextInputStyle.SHORT)
					.setPlaceholder("z.B. 1").setValue(kp.getAmount() + "").setMinLength(1).build();
			TextInput dateti = TextInput.create("date", "Datum", TextInputStyle.SHORT).setPlaceholder("z.B. 31.01.2025")
					.setValue(kp.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).setMinLength(1).build();

			Modal modal = Modal.create("kpedit_" + id, "Kickpunkt bearbeiten")
					.addActionRows(ActionRow.of(reasonti), ActionRow.of(dateti), ActionRow.of(kpamountti)).build();

			event.replyModal(modal).queue();
		} else {
			desc += "Ein Kickpunkt mit dieser ID existiert nicht.";
			event.replyEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.ERROR)).queue();
		}

	}

	@Override
	public void onModalInteraction(ModalInteractionEvent event) {
		if (event.getModalId().startsWith("kpedit")) {
			event.deferReply().queue();
			int id = Integer.valueOf(event.getModalId().split("_")[1]);
			String title = "Kickpunkte";
			String reason = event.getValue("reason").getAsString();
			String amountstr = event.getValue("amount").getAsString();
			int amount = -1;
			try {
				amount = Integer.valueOf(amountstr);
			} catch (Exception ex) {
				event.getHook().editOriginalEmbeds(
						MessageUtil.buildEmbed(title, "Die Anzahl muss eine Zahl sein.", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}
			String date = event.getValue("date").getAsString();

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
			boolean validdate;
			try {
				LocalDate.parse(date, formatter);
				validdate = true;
			} catch (DateTimeParseException e) {
				validdate = false;
			}
			if (!validdate) {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Ungültiges Format für die Datums-Eingabe. Nutze dd.MM.yyyy", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}

			LocalDate localdate = LocalDate.parse(date, formatter);
			LocalDateTime dateTime = localdate.atStartOfDay();
			ZoneId zone = ZoneId.of("Europe/Berlin");
			ZonedDateTime zonedDateTime = dateTime.atZone(zone);
			Timestamp timestampcreated = Timestamp.from(zonedDateTime.toInstant());

			DBUtil.executeUpdate("UPDATE kickpoints SET description = ?, amount = ?, date = ? WHERE id = ?", reason,
					amount, timestampcreated, id);

			String desc = "### Der Kickpunkt wurde bearbeitet.\n";
			desc += "ID: " + id + "\n";
			desc += "Grund: " + reason + "\n";
			desc += "Datum: " + date + "\n";
			desc += "Anzahl: " + amount + "\n";

			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.SUCCESS))
					.queue();
		}
	}

}
