package commands.links;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import datautil.APIUtil;
import datautil.DBManager;
import datawrapper.Player;
import datawrapper.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import util.MessageUtil;

public class playerinfo extends ListenerAdapter {

	enum ConvertionType {
		USERTOACCS, ACCTOUSER
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("playerinfo"))
			return;
		event.deferReply().queue();
		String title = "Spielerinformation";

		OptionMapping userOption = event.getOption("user");
		OptionMapping playerOption = event.getOption("player");
		OptionMapping getApiFileOption = event.getOption("getapifile");

		if (userOption == null && playerOption == null) {
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
					"Einer der beiden Parameter ist erforderlich!", MessageUtil.EmbedType.ERROR)).queue();
			return;
		}

		if (userOption != null && playerOption != null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Bitte gib nur einen Parameter an!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}
		
		boolean getApiFile = false;
		if (getApiFileOption != null) {
			String getApiFileValue = getApiFileOption.getAsString();
			if ("true".equalsIgnoreCase(getApiFileValue)) {
				getApiFile = true;
			} else {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Der getapifile Parameter muss entweder \"true\" enthalten oder nicht angegeben sein (false).",
						MessageUtil.EmbedType.ERROR)).queue();
				return;
			}
		}

		final boolean getApiFileFinal = getApiFile;
		
		new Thread(() -> {
			String userid = null;
			String playertag = null;
			ArrayList<Player> linkedaccs = new ArrayList<>();

			Player player = null;

			ConvertionType conv = null;

			if (userOption != null) {
				userid = userOption.getAsMentionable().getId();
				linkedaccs = new User(userid).getAllLinkedAccounts();
				conv = ConvertionType.USERTOACCS;
			}
			if (playerOption != null) {
				playertag = playerOption.getAsString();
				player = new Player(playertag);
				if (player.IsLinked()) {
					userid = player.getUser().getUserID();
					conv = ConvertionType.ACCTOUSER;
				} else {
					event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
							"Dieser Spieler ist nicht verifiziert.", MessageUtil.EmbedType.ERROR)).queue();
					return;
				}
			}

			String desc = "";

			if (conv == ConvertionType.ACCTOUSER) {
				try {
					desc += "## " + MessageUtil.unformat(player.getInfoStringDB()) + "\n";
				} catch (Exception e) {
					e.printStackTrace();
				}
				desc += "Verlinkter Discord Account: <@" + userid + ">\n";
				if (player.getClanDB() != null) {
					desc += "Eingetragen in Clan: " + player.getClanDB().getInfoStringDB();
					if (player.isMarked()) {
						desc += " (✗)";
						String note = player.getNote();
						if (note != null && !note.trim().isEmpty()) {
							desc += " - " + note;
						}
					}
					desc += "\n";
				} else {
					desc += "Eingetragen in Clan: ---\n";
				}
				if (player.getClanAPI() != null) {
					desc += "Ingame in Clan: " + player.getClanAPI().getInfoStringAPI() + "\n";
				} else {
					desc += "Ingame in Clan: ---\n";
				}
				desc += "Aktuelle Anzahl Kickpunkte: " + player.getActiveKickpoints().size() + "\n";
				desc += "Ingesamte Anzahl Kickpunkte: " + player.getTotalKickpoints();

				final String uuid = userid;
				MessageChannelUnion channel = event.getChannel();
				MessageUtil.sendUserPingHidden(channel, uuid);
			}
			if (conv == ConvertionType.USERTOACCS) {
				try {
					desc += "## <@" + userid + "> \n";
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (linkedaccs.isEmpty()) {
					desc += "	Keine verlinkten Accounts.\n";
				} else {
					desc += "Verlinkte Accounts: \n";
					for (Player p : linkedaccs) {
						desc += "   \\- " + MessageUtil.unformat(p.getInfoStringDB()) + "\n";
					}
				}
			}
			
			// Send response with or without API file
			if (getApiFileFinal && player != null && playertag != null) {
				// Fetch the API JSON for the player
				String apiJson = APIUtil.getPlayerJson(playertag);
				if (apiJson != null) {
					ByteArrayInputStream inputStream = new ByteArrayInputStream(apiJson.getBytes(StandardCharsets.UTF_8));
					String filename = playertag.replace("#", "") + "_api.json";
					event.getHook().editOriginal(inputStream, filename)
							.setEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO))
							.queue();
				} else {
					event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO))
							.queue();
				}
			} else {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO))
						.queue();
			}
		}).start();

	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("playerinfo"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("player")) {
			List<Command.Choice> choices = DBManager.getPlayerlistAutocomplete(input, DBManager.InClanType.ALL);

			event.replyChoices(choices).queue();
		}
		if (focused.equals("getapifile")) {
			List<Command.Choice> choices = new ArrayList<>();
			if ("true".startsWith(input.toLowerCase())) {
				choices.add(new Command.Choice("true", "true"));
			}
			event.replyChoices(choices).queue();
		}
	}

}
