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
import sql.Player;
import util.MessageUtil;
import util.Tuple;

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

		Clan c = new Clan(clantag);

		ArrayList<Player> playerlistdb = c.getPlayers();
		
		ArrayList<String> taglistdb = new ArrayList<>();
		playerlistdb.forEach(p -> taglistdb.add(p.getTag()));

		ArrayList<String> taglistapi = new ArrayList<>();
		ArrayList<Tuple<String, String>> tagnamelistapi = api.Clan.getMemberTagNameList(clantag);

		for(Tuple<String, String> t : tagnamelistapi) {
			taglistapi.add(t.getFirst());
		}
		
		ArrayList<String> membernotinclan = new ArrayList<>();
		ArrayList<Tuple<String, String>> inclannotmember = new ArrayList<>();
		
		for(String s : taglistdb) {
			if(!taglistapi.contains(s)) {
				membernotinclan.add(s);
			}
		}
		
		for(Tuple<String, String> t : tagnamelistapi) {
			if(!taglistdb.contains(t.getFirst())) {
				inclannotmember.add(t);
			}
		}
		
		String membernotinclanstr = "";
		
		for(String s : membernotinclan) {
			membernotinclanstr += new sql.Player(s).getInfoString() +  "\n";
		}
		
		String inclannotmemberstr = "";
		
		for(Tuple<String, String> t : inclannotmember) {
			inclannotmemberstr += t.getSecond() + " (" + t.getFirst() + ")" + "\n";
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
			List<Command.Choice> choices = DBManager.getClansAutocomplete(input);

			event.replyChoices(choices).queue();
		}
	}

}
