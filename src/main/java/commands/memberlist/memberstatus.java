package commands.memberlist;

import java.util.ArrayList;
import java.util.List;

import datautil.DBManager;
import datawrapper.Clan;
import datawrapper.Player;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import util.MessageUtil;

public class memberstatus  extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("memberstatus"))
			return;
		event.deferReply().queue();
		String title = "Memberstatus";

		OptionMapping clanOption = event.getOption("clan");

		if (clanOption == null) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Der Parameter ist erforderlich!", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}

		String clantag = clanOption.getAsString();

		if(clantag.equals("wartelist")) {
			event.getHook().editOriginalEmbeds(
					MessageUtil.buildEmbed(title, "Diesen Befehl kannst du nicht auf die Warteliste ausführen.", MessageUtil.EmbedType.ERROR))
					.queue();
			return;
		}
		
		Clan c = new Clan(clantag);

		ArrayList<Player> playerlistdb = c.getPlayersDB();

		ArrayList<String> taglistdb = new ArrayList<>();
		playerlistdb.forEach(p -> taglistdb.add(p.getTag()));

		ArrayList<Player> playerlistapi = c.getPlayersAPI();

		ArrayList<String> taglistapi = new ArrayList<>();
		playerlistapi.forEach(p -> taglistapi.add(p.getTag()));

		ArrayList<Player> membernotinclan = new ArrayList<>();
		ArrayList<Player> inclannotmember = new ArrayList<>();

		for (String s : taglistdb) {
			if (!taglistapi.contains(s)) {
				membernotinclan.add(new Player(s));
			}
		}

		for (String s : taglistapi) {
			if (!taglistdb.contains(s)) {
				inclannotmember.add(new Player(s));
			}
		}

		String membernotinclanstr = "";

		for (Player p : membernotinclan) {
			membernotinclanstr += p.getInfoString() + "\n";
		}

		String inclannotmemberstr = "";

		for (Player p : inclannotmember) {
			inclannotmemberstr += p.getInfoString() + "\n";
		}
		

		String desc = "## " + c.getInfoString() + "\n";
		
		desc += "**Mitglied, ingame nicht im Clan:**\n\n";
		desc += membernotinclanstr == "" ? "---\n\n" : MessageUtil.unformat(membernotinclanstr) + "\n";
		desc += "**Kein Mitglied, ingame im Clan:**\n\n";
		desc += inclannotmemberstr == "" ? "---\n\n" : MessageUtil.unformat(inclannotmemberstr) + "\n";

		event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, desc, MessageUtil.EmbedType.INFO)).queue();

	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("memberstatus"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("clan")) {
			List<Command.Choice> choices = DBManager.getClansAutocompleteNoWaitlist(input);

			event.replyChoices(choices).queue();
		}
	}

}
