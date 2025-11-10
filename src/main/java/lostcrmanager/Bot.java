package lostcrmanager;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

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
import commands.memberlist.togglemark;
import commands.memberlist.transfermember;
import commands.reminders.remindersadd;
import commands.reminders.remindersinfo;
import commands.reminders.remindersremove;
import commands.util.cwfails;
import commands.util.leaguetrophylist;
import datautil.APIUtil;
import datautil.DBUtil;
import datawrapper.Clan;
import datawrapper.Player;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class Bot extends ListenerAdapter {

	private final static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private static JDA jda;
	public static String VERSION;
	public static String guild_id;
	public static String api_key;
	public static String url;
	public static String user;
	public static String password;
	public static String exmemberroleid;
	public static String seasonstringfallback;

	public static void main(String[] args) throws Exception {
		VERSION = "1.2.5";
		guild_id = System.getenv("CR_MANAGER_GUILD_ID");
		api_key = System.getenv("CR_MANAGER_API_KEY");
		url = System.getenv("CR_MANAGER_DB_URL");
		user = System.getenv("CR_MANAGER_DB_USER");
		password = System.getenv("CR_MANAGER_DB_PASSWORD");
		exmemberroleid = System.getenv("CR_MANAGER_EXMEMBER_ROLEID");

		String token = System.getenv("CR_MANAGER_TOKEN");

		if (datautil.Connection.checkDB()) {
			System.out.println("Verbindung zur Datenbank funktioniert.");
		} else {
			System.out.println("Verbindung zur Datenbank fehlgeschlagen.");
		}

		datautil.Connection.tablesExists();
		startNameUpdates();
		startLoadingLists();
		startReminders();

		JDABuilder.createDefault(token).enableIntents(GatewayIntent.GUILD_MEMBERS)
				.setMemberCachePolicy(MemberCachePolicy.ALL).setChunkingFilter(ChunkingFilter.ALL)
				.setActivity(Activity.playing("mit deinen Kickpunkten"))
				.addEventListeners(new Bot(), new link(), new unlink(), new restart(), new addmember(),
						new removemember(), new listmembers(), new editmember(), new playerinfo(), new memberstatus(),
						new kpaddreason(), new kpremovereason(), new kpeditreason(), new kpadd(), new kpmember(),
						new kpremove(), new kpedit(), new kpinfo(), new kpclan(), new clanconfig(),
						new leaguetrophylist(), new transfermember(), new togglemark(), new cwfails(),
						new remindersadd(), new remindersremove(), new remindersinfo())
				.build();
	}

	public static void registerCommands(JDA jda, String guildId) {
		Guild guild = jda.getGuildById(guildId);
		if (guild != null) {
			guild.updateCommands().addCommands(Commands
					.slash("link", "Verlinke einen Clash Royale Account mit einem Discord User oder einer UserID.")
					.addOption(OptionType.STRING, "tag", "Der Tag des Clash Royale Accounts", true)
					.addOption(OptionType.MENTIONABLE, "user", "Der User, mit dem der Account verlinkt werden soll.")
					.addOption(OptionType.STRING, "userid",
							"Die ID des Users, mit dem der Account verlinkt werden soll."),
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
					Commands.slash("togglemark", "Schaltet die Markierung eines Spielers in einem Clan an/aus.")
							.addOptions(new OptionData(OptionType.STRING, "player",
									"Der Spieler, welcher markiert/entmarkiert werden soll", true)
									.setAutoComplete(true)),
					Commands.slash("listmembers", "Liste aller Spieler in einem Clan.")
							.addOptions(new OptionData(OptionType.STRING, "clan",
									"Der Clan, welcher ausgegeben werden soll.", true).setAutoComplete(true)),
					Commands.slash("editmember", "Ändere die Rolle eines Mitglieds.")
							.addOptions(new OptionData(OptionType.STRING, "player",
									"Der Spieler, welcher bearbeitet werden soll.", true).setAutoComplete(true))
							.addOptions(new OptionData(OptionType.STRING, "role",
									"Die Rolle, welcher der Spieler sein soll.", true).setAutoComplete(true)),
					Commands.slash("playerinfo",
							"Info eines Spielers. Bei Eingabe eines Parameters werden Infos über diesen Nutzer aufgelistet.")
							.addOptions(new OptionData(OptionType.MENTIONABLE, "user",
									"Der User, über welchem Informationen über verlinkte Accounts gesucht sind."))
							.addOptions(new OptionData(OptionType.STRING, "player",
									"Der Spieler, über welchem Informationen gesucht sind.").setAutoComplete(true)),
					Commands.slash("memberstatus",
							"Status über einen Clan, welche Spieler keine Mitglieder sind und welche Mitglieder fehlen.")
							.addOptions(new OptionData(OptionType.STRING, "clan",
									"Der Clan, welcher ausgegeben werden soll.", true).setAutoComplete(true)),
					Commands.slash("kpaddreason", "Erstelle einen vorgefertigten Kickpunktgrund.")
							.addOptions(new OptionData(OptionType.STRING, "clan",
									"Der Clan, für welchen dieser erstellt wird.", true).setAutoComplete(true))
							.addOptions(new OptionData(OptionType.STRING, "reason", "Der angezeigte Grund.", true))
							.addOptions(
									new OptionData(OptionType.INTEGER, "amount", "Die Anzahl der Kickpunkte.", true)),
					Commands.slash("kpremovereason", "Lösche einen vorgefertigten Kickpunktgrund.")
							.addOptions(new OptionData(OptionType.STRING, "clan",
									"Der Clan, für welchen dieser erstellt wird.", true).setAutoComplete(true))
							.addOptions(new OptionData(OptionType.STRING, "reason", "Der angezeigte Grund.", true)
									.setAutoComplete(true)),
					Commands.slash("kpeditreason", "Aktualisiere die Anzahl der Kickpunkte für eine Grund-Vorlage.")
							.addOptions(new OptionData(OptionType.STRING, "clan",
									"Der Clan, für welchen dieser erstellt wird.", true).setAutoComplete(true))
							.addOptions(new OptionData(OptionType.STRING, "reason", "Der angezeigte Grund.", true)
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
					Commands.slash("kpremove", "Lösche einen Kickpunkt.")
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
									"Der Clan, welcher bearbeitet werden soll.", true).setAutoComplete(true)),
					Commands.slash("leaguetrophylist", "Sortierte Rangliste.")
							.addOptions(new OptionData(OptionType.STRING, "timestamp",
									"Der Zeitpunkt der gespeicherten Liste", true).setAutoComplete(true)),
					Commands.slash("transfermember", "Transferiere einen Spieler in einen anderen Clan.")
							.addOptions(new OptionData(OptionType.STRING, "player",
									"Der Spieler, welcher transferiert werden soll", true).setAutoComplete(true))
							.addOptions(new OptionData(OptionType.STRING, "clan",
									"Der Clan, zu welchem der Spieler hinzugefügt werden soll", true)
									.setAutoComplete(true)),
					Commands.slash("cwfails",
							"Überprüfe einen Clan auf CW-Punkte mit einer Hürde, die zu überwinden gilt.")
							.addOptions(new OptionData(OptionType.STRING, "clan",
									"Der Clan, welcher überprüft werden soll.", true).setAutoComplete(true))
							.addOptions(new OptionData(OptionType.STRING, "threshold",
									"Die Hürde, welche jeder Spieler überwunden haben sollte", true))
							.addOptions(new OptionData(OptionType.STRING, "kpreason",
									"(Optional) Der Kickpunkt-Grund für jeden Spieler, der die Hürde nicht erreicht hat.")
									.setAutoComplete(true))
							.addOptions(new OptionData(OptionType.STRING, "min_threshold",
									"(Optional) Minimale Punktzahl - Spieler darunter werden nicht angezeigt")
									.setRequired(false))
							.addOptions(new OptionData(OptionType.STRING, "exclude_leaders",
									"(Optional) Wenn 'true', werden Leader, Co-Leader und Admins von der Prüfung ausgeschlossen")
									.setAutoComplete(true)
									.setRequired(false)),
					Commands.slash("remindersadd", "Erstelle einen Reminder für Clan War Beteiligung.")
							.addOptions(new OptionData(OptionType.STRING, "clan",
									"Der Clan, für welchen der Reminder erstellt wird.", true).setAutoComplete(true))
							.addOptions(new OptionData(OptionType.CHANNEL, "channel",
									"Der Kanal, in dem der Reminder gesendet wird.", true))
							.addOptions(new OptionData(OptionType.STRING, "time",
									"Die Uhrzeit für den Reminder (Format: HH:mm, z.B. 14:30)", true)),
					Commands.slash("remindersremove", "Entferne einen Reminder.")
							.addOptions(new OptionData(OptionType.INTEGER, "id",
									"Die ID des Reminders. Ist unter /remindersinfo zu sehen.", true)),
					Commands.slash("remindersinfo", "Zeige alle Reminder für einen Clan an.")
							.addOptions(new OptionData(OptionType.STRING, "clan",
									"Der Clan, für welchen Reminder angezeigt werden sollen.", true).setAutoComplete(true)))
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

	public static void startLoadingLists() {
		System.out.println("Jede Stunde wird nun die Leaguetrophylist erstellt. " + System.currentTimeMillis());
		Runnable task = () -> {
			Thread thread = new Thread(() -> {
				File folder = new File(leaguetrophylist.getRunningJarDirectory(), "CRManager_ListFiles");
				Pattern pattern = Pattern.compile("Liste_(\\d+)\\.txt");

				ZonedDateTime twoMonthsAgo = ZonedDateTime.now().minusMonths(2);
				long twoMonthsAgoMillis = twoMonthsAgo.toInstant().toEpochMilli();

				if (folder.exists() && folder.isDirectory()) {
					File[] files = folder.listFiles();
					if (files != null) {
						for (File file : files) {
							Matcher matcher = pattern.matcher(file.getName());
							if (matcher.matches()) {
								long millis = Long.parseLong(matcher.group(1));
								if (millis < twoMonthsAgoMillis) {
									boolean deleted = file.delete();
									if (deleted) {
										System.out.println("Gelöscht: " + file.getName());
									} else {
										System.err.println("Konnte nicht löschen: " + file.getName());
									}
								}
							}
						}
					}
				} else {
					System.out.println("Der Ordner existiert nicht oder ist kein Verzeichnis");
				}
				leaguetrophylist.saveNewList();

			});
			thread.start();
		};
		scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.HOURS);
	}

	public static void startNameUpdates() {
		System.out.println("Alle 2h werden nun die Namen aktualisiert. " + System.currentTimeMillis());
		Runnable task = () -> {
			Thread thread = new Thread(() -> {

				String sql = "SELECT cr_tag FROM players";
				for (String tag : DBUtil.getArrayListFromSQL(sql, String.class)) {
					try {
						Player p = new Player(tag);
						DBUtil.executeUpdate("UPDATE players SET name = ? WHERE cr_tag = ?", p.getNameAPI(), tag);
					} catch (Exception e) {
						System.out.println(
								"Beim Updaten des Namens von Spieler mit Tag " + tag + " ist ein Fehler aufgetreten.");
					}
				}

			});
			thread.start();
		};
		scheduler.scheduleAtFixedRate(task, 0, 2, TimeUnit.HOURS);
	}

	public static void startReminders() {
		System.out.println("Reminder-Check wird gestartet. Prüfung alle 5 Minuten. " + System.currentTimeMillis());
		Runnable task = () -> {
			Thread thread = new Thread(() -> {
				checkReminders();
			});
			thread.start();
		};
		scheduler.scheduleAtFixedRate(task, 0, 5, TimeUnit.MINUTES);
	}

	private static void checkReminders() {
		// Check if today is Thursday, Friday, Saturday, or Sunday
		DayOfWeek today = ZonedDateTime.now().getDayOfWeek();
		if (today != DayOfWeek.THURSDAY && today != DayOfWeek.FRIDAY && 
		    today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY) {
			return; // Not a reminder day
		}

		LocalTime currentTime = LocalTime.now();
		
		// Get all reminders
		String sql = "SELECT id, clantag, channelid, time FROM reminders";
		try (PreparedStatement pstmt = datautil.Connection.getConnection().prepareStatement(sql)) {
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					int id = rs.getInt("id");
					String clantag = rs.getString("clantag");
					String channelId = rs.getString("channelid");
					Time reminderTime = rs.getTime("time");
					LocalTime reminderLocalTime = reminderTime.toLocalTime();
					
					// Check if reminder should be sent (within 5 minute window)
					long minutesDiff = java.time.Duration.between(reminderLocalTime, currentTime).toMinutes();
					if (minutesDiff >= 0 && minutesDiff < 5) {
						sendReminder(clantag, channelId);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void sendReminder(String clantag, String channelId) {
		try {
			// Get clan info
			Clan clan = new Clan(clantag);
			if (!clan.ExistsDB()) {
				System.out.println("Clan " + clantag + " existiert nicht mehr.");
				return;
			}

			// Get current river race data
			String json = APIUtil.getCurrentRiverRaceJson(clantag);
			if (json == null) {
				System.out.println("Konnte River Race Daten für " + clantag + " nicht abrufen.");
				return;
			}

			JSONObject data = new JSONObject(json);
			JSONObject clanData = data.getJSONObject("clan");
			JSONArray participants = clanData.getJSONArray("participants");

			ArrayList<String> reminderList = new ArrayList<>();
			for (int i = 0; i < participants.length(); i++) {
				JSONObject participant = participants.getJSONObject(i);
				int decksUsedToday = participant.getInt("decksUsedToday");
				if (decksUsedToday < 4) {
					String playerName = participant.getString("name");
					String playerTag = participant.getString("tag");
					reminderList.add(playerName + " (" + playerTag + ") - " + decksUsedToday + "/4 Decks");
				}
			}

			if (reminderList.isEmpty()) {
				System.out.println("Keine Spieler mit weniger als 4 Decks für " + clantag);
				return;
			}

			// Send reminder message
			if (jda != null) {
				Guild guild = jda.getGuildById(guild_id);
				if (guild != null) {
					TextChannel channel = guild.getTextChannelById(channelId);
					if (channel != null) {
						EmbedBuilder embed = new EmbedBuilder();
						embed.setTitle("⚠️ Clan War Reminder - " + clan.getNameDB());
						embed.setColor(0xFF9900);
						
						StringBuilder description = new StringBuilder();
						description.append("Folgende Spieler haben heute weniger als 4 Decks verwendet:\n\n");
						
						for (String playerInfo : reminderList) {
							description.append("• ").append(playerInfo).append("\n");
						}
						
						description.append("\n**Bitte denkt daran, eure verbleibenden Decks heute noch zu spielen!**");
						embed.setDescription(description.toString());
						
						channel.sendMessageEmbeds(embed.build()).queue(
							success -> System.out.println("Reminder erfolgreich gesendet für " + clantag),
							error -> System.err.println("Fehler beim Senden des Reminders: " + error.getMessage())
						);
					} else {
						System.err.println("Kanal " + channelId + " nicht gefunden.");
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Fehler beim Senden des Reminders für " + clantag + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void stopScheduler() {
		scheduler.shutdown();
	}

}
