package lostcrmanager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import commands.admin.restart;
import commands.kickpoints.clanconfig;
import commands.kickpoints.kpadd;
import commands.kickpoints.kpaddreason;
import commands.kickpoints.kpclan;
import commands.kickpoints.kpedit;
import commands.kickpoints.kpeditreason;
import commands.kickpoints.kpinfo;
import commands.kickpoints.kpmember;
import commands.kickpoints.kpremove;
import commands.kickpoints.kpremovereason;
import commands.links.link;
import commands.links.playerinfo;
import commands.links.unlink;
import commands.memberlist.addmember;
import commands.memberlist.editmember;
import commands.memberlist.listmembers;
import commands.memberlist.memberstatus;
import commands.memberlist.removemember;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import sql.DBUtil;

public class Bot extends ListenerAdapter {

	private final static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private static JDA jda;
	public static String VERSION;
	public static String guild_id;
	public static String api_key;
	public static String url;
	public static String user;
	public static String password;

	public static void main(String[] args) throws Exception {
		VERSION = "1.1";
		guild_id = System.getenv("CR_MANAGER_GUILD_ID");
		api_key = System.getenv("CR_MANAGER_API_KEY");
		url = System.getenv("CR_MANAGER_DB_URL");
		user = System.getenv("CR_MANAGER_DB_USER");
		password = System.getenv("CR_MANAGER_DB_PASSWORD");

		String token = System.getenv("CR_MANAGER_TOKEN");

		if (sql.Connection.checkDB()) {
			System.out.println("Verbindung zur Datenbank funktioniert.");
		} else {
			System.out.println("Verbindung zur Datenbank fehlgeschlagen.");
		}

		sql.Connection.tablesExists();
		startNameUpdates();

		JDABuilder.createDefault(token).enableIntents(GatewayIntent.GUILD_MEMBERS)
				.setMemberCachePolicy(MemberCachePolicy.ALL).setChunkingFilter(ChunkingFilter.ALL)
				.setActivity(Activity.playing("mit deinen Kickpunkten"))
				.addEventListeners(new Bot(), new link(), new unlink(), new restart(), new addmember(),
						new removemember(), new listmembers(), new editmember(), new playerinfo(), new memberstatus(),
						new kpaddreason(), new kpremovereason(), new kpeditreason(), new kpadd(), new kpmember(),
						new kpremove(), new kpedit(), new kpinfo(), new kpclan(), new clanconfig())
				.build();
	}

	public static void registerCommands(JDA jda, String guildId) {
		Guild guild = jda.getGuildById(guildId);
		if (guild != null) {
			guild.updateCommands()
					.addCommands(
							Commands.slash("link", "Verlinke einen Clash Royale Account mit einem Discord User.")
									.addOption(OptionType.STRING, "tag", "Der Tag des Clash Royale Accounts", true)
									.addOption(OptionType.MENTIONABLE, "user",
											"Der User, mit dem der Account verlinkt werden soll.", true),
							Commands.slash("unlink", "Lösche eine Verlinkung eines Clash Royale Accounts.")
									.addOptions(new OptionData(OptionType.STRING, "tag",
											"Der Spieler, wessen Verknüpfung entfernt werden soll", true)
											.setAutoComplete(true)),
							Commands.slash("restart", "Startet den Bot neu."),
							Commands.slash("addmember", "Füge einen Spieler zu einem Clan hinzu.")
									.addOptions(new OptionData(OptionType.STRING, "clan",
											"Der Clan, zu welchem der Spieler hinzugefügt werden soll", true)
											.setAutoComplete(true))
									.addOptions(new OptionData(OptionType.STRING, "player",
											"Der Spieler, welcher hinzugefügt werden soll", true).setAutoComplete(true))
									.addOptions(new OptionData(OptionType.STRING, "role",
											"Die Rolle, welche der Spieler bekommen soll", true).setAutoComplete(true)),
							Commands.slash("removemember", "Entferne einen Spieler aus seinem Clan.")
									.addOptions(new OptionData(OptionType.STRING, "player",
											"Der Spieler, welcher entfernt werden soll", true).setAutoComplete(true)),
							Commands.slash("listmembers", "Liste aller Spieler in einem Clan.")
									.addOptions(new OptionData(OptionType.STRING, "clan",
											"Der Clan, welcher ausgegeben werden soll.", true).setAutoComplete(true)),
							Commands.slash("editmember", "Ändere die Rolle eines Mitglieds.")
									.addOptions(new OptionData(OptionType.STRING, "player",
											"Der Spieler, welcher bearbeitet werden soll.", true).setAutoComplete(true))
									.addOptions(
											new OptionData(
													OptionType.STRING, "role",
													"Die Rolle, welcher der Spieler sein soll.", true)
													.setAutoComplete(true)),
							Commands.slash("playerinfo",
									"Info eines Spielers. Bei Eingabe eines Parameters werden Infos über diesen Nutzer aufgelistet.")
									.addOptions(new OptionData(
											OptionType.MENTIONABLE, "user",
											"Der User, über welchem Informationen über verlinkte Accounts gesucht sind."))
									.addOptions(new OptionData(
											OptionType.STRING, "player",
											"Der Spieler, über welchem Informationen gesucht sind.")
											.setAutoComplete(true)),
							Commands.slash("memberstatus",
									"Status über einen Clan, welche Spieler keine Mitglieder sind und welche Mitglieder fehlen.")
									.addOptions(
											new OptionData(
													OptionType.STRING, "clan",
													"Der Clan, welcher ausgegeben werden soll.", true)
													.setAutoComplete(true)),
							Commands.slash("kpaddreason", "Erstelle einen vorgefertigten Kickpunktgrund.")
									.addOptions(new OptionData(OptionType.STRING, "clan",
											"Der Clan, für welchen dieser erstellt wird.", true).setAutoComplete(true))
									.addOptions(
											new OptionData(OptionType.STRING, "reason", "Der angezeigte Grund.", true))
									.addOptions(new OptionData(OptionType.INTEGER, "amount",
											"Die Anzahl der Kickpunkte.", true)),
							Commands.slash("kpremovereason", "Lösche einen vorgefertigten Kickpunktgrund.")
									.addOptions(new OptionData(OptionType.STRING, "clan",
											"Der Clan, für welchen dieser erstellt wird.", true).setAutoComplete(true))
									.addOptions(new OptionData(
											OptionType.STRING, "reason", "Der angezeigte Grund.", true)
											.setAutoComplete(true)),
							Commands.slash("kpeditreason",
									"Aktualisiere die Anzahl der Kickpunkte für eine Grund-Vorlage.")
									.addOptions(new OptionData(OptionType.STRING, "clan",
											"Der Clan, für welchen dieser erstellt wird.", true).setAutoComplete(true))
									.addOptions(
											new OptionData(OptionType.STRING, "reason", "Der angezeigte Grund.", true)
													.setAutoComplete(true))
									.addOptions(new OptionData(OptionType.INTEGER, "amount", "Die Anzahl.", true)),
							Commands.slash("kpadd", "Gebe einem Spieler Kickpunkte.")
									.addOptions(new OptionData(OptionType.STRING, "player",
											"Der Spieler, welcher die Kickpunkte erhält.", true).setAutoComplete(true))
									.addOptions(new OptionData(OptionType.STRING, "reason", "Die Grund-Vorlage.")
											.setAutoComplete(true)),
							Commands.slash("kpmember", "Zeige alle Kickpunkte eines Spielers an.")
									.addOptions(new OptionData(OptionType.STRING, "player",
											"Der Spieler, welcher angezeigt werden soll.", true).setAutoComplete(true)),
							Commands.slash("kpremove", "Zeige alle Kickpunkte eines Spielers an.")
									.addOptions(new OptionData(OptionType.INTEGER, "id",
											"Die ID des Kickpunkts. Ist unter /kpmember zu sehen.", true)),
							Commands.slash("kpedit", "Editiere einen Kickpunkt.")
									.addOptions(new OptionData(OptionType.INTEGER, "id",
											"Die ID des Kickpunkts. Ist unter /kpmember zu sehen.", true)),
							Commands.slash("kpinfo", "Infos über Kickpunkt-Gründe eines Clans.")
									.addOptions(new OptionData(OptionType.STRING, "clan",
											"Die Clan, welcher angezeigt werden soll.", true).setAutoComplete(true)),
							Commands.slash("kpclan", "Zeige die Kickpunktanzahlen aller Spieler in einem Clan.")
									.addOptions(new OptionData(OptionType.STRING, "clan",
											"Der Clan, welcher angezeigt werden soll.", true).setAutoComplete(true)),
							Commands.slash("clanconfig", "Ändere Einstellungen an einem Clan.")
									.addOptions(new OptionData(OptionType.STRING, "clan",
											"Der Clan, welcher bearbeitet werden soll.", true).setAutoComplete(true)))
					.queue();
		}
	}

	@Override
	public void onReady(ReadyEvent event) {
		setJda(event.getJDA());
		registerCommands(event.getJDA(), guild_id);
	}

	@Override
	public void onShutdown(ShutdownEvent event) {
		stopScheduler();
	}

	public static void setJda(JDA instance) {
		jda = instance;
	}

	public static JDA getJda() {
		return jda;
	}

	public static void startNameUpdates() {
		Runnable task = () -> {
			System.out.println("Alle 2h werden nun die Namen aktualisiert. " + System.currentTimeMillis());

			String sql = "SELECT cr_tag FROM players";
			for (String tag : DBUtil.getArrayListFromSQL(sql, String.class)) {
				try {
					DBUtil.executeUpdate("UPDATE players SET name = ? WHERE cr_tag = ?", api.Player.getPlayerName(tag),
							tag);
				} catch (Exception e) {
					System.out.println(
							"Beim Updaten des Namens von Spieler mit Tag " + tag + " ist ein Fehler aufgetreten.");
				}
			}

		};
		scheduler.scheduleAtFixedRate(task, 0, 2, TimeUnit.HOURS);
	}

	public void stopScheduler() {
		scheduler.shutdown();
	}

}
