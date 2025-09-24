package commands.kickpoints;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

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
import sql.Clan;
import sql.DBManager;
import sql.DBUtil;
import sql.Kickpoint;
import sql.KickpointReason;
import sql.Player;
import sql.User;
import util.MessageUtil;

public class kpadd extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("kpadd"))
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

		OptionMapping playeroption = event.getOption("player");
		OptionMapping reasonoption = event.getOption("reason");

		if (playeroption == null) {
			event.replyEmbeds(MessageUtil.buildEmbed(title, "Der Paramenter Player sind verpflichtend!",
					MessageUtil.EmbedType.ERROR)).queue();
			return;
		}

		String reason = null;
		if (reasonoption != null) {
			reason = reasonoption.getAsString();
		}
		String playertag = playeroption.getAsString();
		Player p = new Player(playertag);

		if (p.getClan() == null) {
			event.replyEmbeds(MessageUtil.buildEmbed(title, "Dieser Spieler existiert nicht oder ist in keinem Clan.",
					MessageUtil.EmbedType.ERROR)).queue();
			return;
		}
		if (p.getClan().getDaysKickpointsExpireAfter() == null || p.getClan().getMaxKickpoints() == null) {
			event.replyEmbeds(MessageUtil.buildEmbed(title,
					"Es müssen zuerst die Clanconfigs eingestellt werden. Nutze /clanconfig.",
					MessageUtil.EmbedType.ERROR)).queue();
			return;
		}

		String clantag = p.getClan().getTag();

		KickpointReason kpreason = null;

		if (DBManager.KickpointReasonExists(reason)) {
			kpreason = new KickpointReason(reason, clantag);
		}

		TextInput reasonti;
		TextInput kpamountti;
		if (reason != null) {
			reasonti = TextInput.create("reason", "Grund", TextInputStyle.SHORT).setPlaceholder("z.B. CW vergessen")
					.setValue(reason).setMinLength(1).build();
			if (kpreason != null) {
				kpamountti = TextInput.create("amount", "Anzahl Kickpunkte", TextInputStyle.SHORT)
						.setPlaceholder("z.B. 1").setValue(kpreason.getAmount() + "").setMinLength(1).build();
			} else {
				kpamountti = TextInput.create("amount", "Anzahl Kickpunkte", TextInputStyle.SHORT)
						.setPlaceholder("z.B. 1").setValue("z.B. 1").setMinLength(1).build();
			}
		} else {
			reasonti = TextInput.create("reason", "Grund", TextInputStyle.SHORT).setPlaceholder("z.B. CW vergessen")
					.setMinLength(1).build();
			kpamountti = TextInput.create("amount", "Anzahl Kickpunkte", TextInputStyle.SHORT).setPlaceholder("z.B. 1")
					.setMinLength(1).build();
		}
		TextInput dateti = TextInput.create("date", "Datum", TextInputStyle.SHORT).setPlaceholder("z.B. 31.01.2025")
				.setMinLength(1).build();

		TextInput playertagti = TextInput.create("tag", "Spieler-Tag", TextInputStyle.SHORT)
				.setPlaceholder("z.B. #Y0RYLP0Q").setValue(playertag).setMinLength(1).build();

		Modal modal = Modal.create("kpadd", "Kickpunkt hinzufügen").addActionRows(ActionRow.of(reasonti),
				ActionRow.of(kpamountti), ActionRow.of(dateti), ActionRow.of(playertagti)).build();

		event.replyModal(modal).queue();

	}

	@Override
	public void onModalInteraction(ModalInteractionEvent event) {
		if (event.getModalId().equals("kpadd")) {
			event.deferReply().queue();
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
			String playertag = event.getValue("tag").getAsString();

			Player p = new Player(playertag);
			Clan c = p.getClan();
			if (c == null) {
				event.getHook()
						.editOriginalEmbeds(MessageUtil.buildEmbed(title,
								"Dieser Spieler existiert nicht oder ist in keinem Clan.", MessageUtil.EmbedType.ERROR))
						.queue();
				return;
			}

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
			Timestamp timestampexpires = Timestamp.valueOf(dateTime.plusDays(c.getDaysKickpointsExpireAfter()));
			Timestamp timestampnow = Timestamp.from(Instant.now());
			String userid = event.getUser().getId();
			int id = DBManager.getAvailableKPID();

			DBUtil.executeUpdate(
					"INSERT INTO kickpoints (id, player_tag, date, amount, description, created_by_discord_id, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
					id, playertag, timestampcreated, amount, reason, userid, timestampnow, timestampexpires);

			String desc = "### Der Kickpunkt wurde hinzugefügt.\n";
			desc += "Spieler: " + MessageUtil.unformat(p.getInfoString()) + "\n";
			desc += "Clan: " + c.getInfoString() + "\n";
			desc += "Anzahl: " + amount + "\n";
			desc += "Grund: " + reason + "\n";

			if (timestampexpires.before(Timestamp.from(Instant.now()))) {
				desc += "### Achtung: Der Kickpunkt ist bereits abgelaufen.\n";
			}

			int kptotal = 0;
			for (Kickpoint kp : p.getActiveKickpoints()) {
				kptotal += kp.getAmount();
			}
			if (kptotal >= c.getMaxKickpoints()) {
				desc += "### Achtung: Der Spieler hat die maximale Anzahl der Kickpunkte erreicht.\n";
			}

			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.SUCCESS))
					.queue();
		}
	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("kpadd"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("player")) {
			List<Command.Choice> choices = DBManager.getPlayerlistAutocomplete(input, DBManager.InClanType.INCLAN);

			event.replyChoices(choices).queue();
		}
		if (focused.equals("reason")) {
			String playertag = event.getOption("player").getAsString();
			Player p = new Player(playertag);
			Clan c = p.getClan();
			if (c == null) {
				return;
			}
			String clantag = c.getTag();
			List<Command.Choice> choices = DBManager.getKPReasonsAutocomplete(input, clantag);

			event.replyChoices(choices).queue();
		}
	}

}
