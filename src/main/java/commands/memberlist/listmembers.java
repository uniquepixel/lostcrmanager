package commands.memberlist;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import datautil.DBManager;
import datawrapper.Clan;
import datawrapper.Player;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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

		new Thread(() -> {
			ArrayList<Player> playerlist = c.getPlayersDB();

			playerlist.sort(Comparator.comparing(Player::isMarked).reversed().thenComparing((p1, p2) -> {
				String name1 = p1.getNameDB() != null ? p1.getNameDB() : p1.getNameAPI();
				String name2 = p2.getNameDB() != null ? p2.getNameDB() : p2.getNameAPI();
				if (name1 == null && name2 == null)
					return 0;
				if (name1 == null)
					return 1; // nulls last
				if (name2 == null)
					return -1;
				return name1.compareTo(name2);
			}));

			String adminlist = "";
			String leaderlist = "";
			String coleaderlist = "";
			String elderlist = "";
			String memberlist = "";
			int clanSizeCount = 0;

			for (Player p : playerlist) {
				boolean isHidden = p.isHiddenColeader();
				if (!isHidden) {
					clanSizeCount++;
				}

				if (p.getRole() == Player.RoleType.ADMIN) {
					adminlist += p.getInfoStringDB();
					if (p.isMarked()) {
						adminlist += " (✗)";
					}
					adminlist += "\n";
				}
				if (p.getRole() == Player.RoleType.LEADER) {
					leaderlist += p.getInfoStringDB();
					if (p.isMarked()) {
						leaderlist += " (✗)";
					}
					leaderlist += "\n";
				}
				if (p.getRole() == Player.RoleType.COLEADER) {
					coleaderlist += p.getInfoStringDB();
					if (isHidden) {
						coleaderlist += " (versteckt)";
					}
					if (p.isMarked()) {
						coleaderlist += " (✗)";
					}
					coleaderlist += "\n";
				}
				if (p.getRole() == Player.RoleType.ELDER) {
					elderlist += p.getInfoStringDB();
					if (p.isMarked()) {
						elderlist += " (✗)";
					}
					elderlist += "\n";
				}
				if (p.getRole() == Player.RoleType.MEMBER) {
					memberlist += p.getInfoStringDB();
					if (p.isMarked()) {
						memberlist += " (✗)";
					}
					memberlist += "\n";
				}
			}
			String desc = "## " + c.getInfoStringDB() + "\n";
			if (!clantag.equals("warteliste")) {
				desc += "**Admin:**\n";
				desc += adminlist == "" ? "---\n\n" : MessageUtil.unformat(adminlist) + "\n";
				desc += "**Anführer:**\n";
				desc += leaderlist == "" ? "---\n\n" : MessageUtil.unformat(leaderlist) + "\n";
				desc += "**Vize-Anführer:**\n";
				desc += coleaderlist == "" ? "---\n\n" : MessageUtil.unformat(coleaderlist) + "\n";
				desc += "**Ältester:**\n";
				desc += elderlist == "" ? "---\n\n" : MessageUtil.unformat(elderlist) + "\n";
				desc += "**Mitglied:**\n";
				desc += memberlist == "" ? "---\n\n" : MessageUtil.unformat(memberlist) + "\n";
				desc += "\nInsgesamte Mitglieder des Clans: " + clanSizeCount;
			} else {
				desc += "**Wartend:**\n";
				desc += memberlist == "" ? "---\n\n" : MessageUtil.unformat(memberlist) + "\n";
				desc += "\nInsgesamte Spieler auf der Warteliste: " + playerlist.size();
			}

			Button refreshButton = Button.secondary("listmembers_" + clantag, "\u200B")
					.withEmoji(Emoji.fromUnicode("🔁"));

			ZonedDateTime jetzt = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm 'Uhr'");
			String formatiert = jetzt.format(formatter);

			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO,
					"Zuletzt aktualisiert am " + formatiert)).setActionRow(refreshButton).queue();
		}).start();

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

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		String id = event.getComponentId();
		if (!id.startsWith("listmembers_"))
			return;

		event.deferEdit().queue();

		String title = "Memberliste";
		String clantag = id.substring("listmembers_".length());
		Clan c = new Clan(clantag);

		new Thread(() -> {
			ArrayList<Player> playerlist = c.getPlayersDB();

			playerlist.sort(Comparator.comparing(Player::isMarked).reversed().thenComparing((p1, p2) -> {
				String name1 = p1.getNameDB() != null ? p1.getNameDB() : p1.getNameAPI();
				String name2 = p2.getNameDB() != null ? p2.getNameDB() : p2.getNameAPI();
				if (name1 == null && name2 == null)
					return 0;
				if (name1 == null)
					return 1; // nulls last
				if (name2 == null)
					return -1;
				return name1.compareTo(name2);
			}));

			String adminlist = "";
			String leaderlist = "";
			String coleaderlist = "";
			String elderlist = "";
			String memberlist = "";
			int clanSizeCount = 0;

			for (Player p : playerlist) {
				boolean isHidden = p.isHiddenColeader();
				if (!isHidden) {
					clanSizeCount++;
				}

				if (p.getRole() == Player.RoleType.ADMIN) {
					adminlist += p.getInfoStringDB();
					if (p.isMarked()) {
						adminlist += " (✗)";

					}
					adminlist += "\n";
				}
				if (p.getRole() == Player.RoleType.LEADER) {
					leaderlist += p.getInfoStringDB();
					if (p.isMarked()) {
						leaderlist += " (✗)";

					}
					leaderlist += "\n";
				}
				if (p.getRole() == Player.RoleType.COLEADER) {
					coleaderlist += p.getInfoStringDB();
					if (isHidden) {
						coleaderlist += " (versteckt)";
					}
					if (p.isMarked()) {
						coleaderlist += " (✗)";

					}
					coleaderlist += "\n";
				}
				if (p.getRole() == Player.RoleType.ELDER) {
					elderlist += p.getInfoStringDB();
					if (p.isMarked()) {
						elderlist += " (✗)";

					}
					elderlist += "\n";
				}
				if (p.getRole() == Player.RoleType.MEMBER) {
					memberlist += p.getInfoStringDB();
					if (p.isMarked()) {
						memberlist += " (✗)";

					}
					memberlist += "\n";
				}
			}
			String desc = "## " + c.getInfoStringDB() + "\n";
			if (!clantag.equals("warteliste")) {
				desc += "**Admin:**\n";
				desc += adminlist == "" ? "---\n\n" : MessageUtil.unformat(adminlist) + "\n";
				desc += "**Anführer:**\n";
				desc += leaderlist == "" ? "---\n\n" : MessageUtil.unformat(leaderlist) + "\n";
				desc += "**Vize-Anführer:**\n";
				desc += coleaderlist == "" ? "---\n\n" : MessageUtil.unformat(coleaderlist) + "\n";
				desc += "**Ältester:**\n";
				desc += elderlist == "" ? "---\n\n" : MessageUtil.unformat(elderlist) + "\n";
				desc += "**Mitglied:**\n";
				desc += memberlist == "" ? "---\n\n" : MessageUtil.unformat(memberlist) + "\n";
				desc += "\nInsgesamte Mitglieder des Clans: " + clanSizeCount;
			} else {
				desc += "**Wartend:**\n";
				desc += memberlist == "" ? "---\n\n" : MessageUtil.unformat(memberlist) + "\n";
				desc += "\nInsgesamte Spieler auf der Warteliste: " + playerlist.size();
			}

			Button refreshButton = Button.secondary("listmembers_" + clantag, "\u200B")
					.withEmoji(Emoji.fromUnicode("🔁"));

			ZonedDateTime jetzt = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm 'Uhr'");
			String formatiert = jetzt.format(formatter);

			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO,
					"Zuletzt aktualisiert am " + formatiert)).setActionRow(refreshButton).queue();
		}).start();
	}

}
