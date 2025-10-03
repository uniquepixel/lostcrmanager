package datautil;

import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import lostcrmanager.Bot;

public class Connection {

	public static String url;
	public static String user;
	public static String password;

	private static java.sql.Connection connection;

	public static boolean checkDB() {

		url = Bot.url;
		user = Bot.user;
		password = Bot.password;

		try (java.sql.Connection conn = DriverManager.getConnection(url, user, password)) {
			if (conn != null) {
				connection = conn;
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			System.out.println("Verbindungsfehler: " + e.getMessage());
			return false;
		}
	}

	public static void tablesExists() {
		ArrayList<String> tableNames = new ArrayList<>();
		tableNames.add("clans");
		tableNames.add("users");
		tableNames.add("players");
		tableNames.add("clan_members");
		tableNames.add("clan_settings");
		tableNames.add("kickpoint_reasons");
		tableNames.add("kickpoints");
		try (java.sql.Connection conn = DriverManager.getConnection(url, user, password)) {
			DatabaseMetaData dbm = conn.getMetaData();

			for (String tableName : tableNames) {
				try (ResultSet tables = dbm.getTables(null, null, tableName, null)) {
					if (tables.next()) {
						System.out.println("Tabelle '" + tableName + "' existiert schon.");
					} else {
						System.out.println("Tabelle '" + tableName + "' existiert nicht. Erstelle sie jetzt...");
						String createTableSQL = null;
						switch (tableName) {
						case "clans":
							createTableSQL = "CREATE TABLE " + tableName + " (tag TEXT PRIMARY KEY," + "name TEXT,"
									+ "index BIGINT," + "guild_id CHARACTER VARYING(19),"
									+ "leader_roleid CHARACTER VARYING(19)," + "coleader_roleid CHARACTER VARYING(19),"
									+ "elder_roleid CHARACTER VARYING(19)," + "member_roleid CHARACTER VARYING(19))";
							break;
						case "users":
							createTableSQL = "CREATE TABLE " + tableName
									+ " (discord_id CHARACTER VARYING(19) PRIMARY KEY," + "is_admin BOOLEAN)";
							break;
						case "players":
							createTableSQL = "CREATE TABLE " + tableName + " (cr_tag TEXT PRIMARY KEY,"
									+ "discord_id CHARACTER VARYING(19), name TEXT)";
							break;
						case "clan_members":
							createTableSQL = "CREATE TABLE " + tableName + " (player_tag TEXT PRIMARY KEY,"
									+ "clan_tag TEXT," + "clan_role TEXT)";
							break;
						case "clan_settings":
							createTableSQL = "CREATE TABLE " + tableName + " (clan_tag TEXT PRIMARY KEY,"
									+ "max_kickpoints BIGINT," + "kickpoints_expire_after_days SMALLINT)";
							break;
						case "kickpoint_reasons":
							createTableSQL = "CREATE TABLE " + tableName + " (name TEXT," + "clan_tag text,"
									+ "amount SMALLINT," + "PRIMARY KEY (name, clan_tag))";
							break;
						case "kickpoints":
							createTableSQL = "CREATE TABLE " + tableName + " (id BIGINT PRIMARY KEY,"
									+ "player_tag CHARACTER VARYING(19)," + "date TIMESTAMPTZ," + "amount BIGINT,"
									+ "description CHARACTER VARYING(100),"
									+ "created_by_discord_id CHARACTER VARYING(19)," + "created_at TIMESTAMPTZ,"
									+ "expires_at TIMESTAMPTZ)";
							break;
						}

						try (Statement stmt = conn.createStatement()) {
							stmt.executeUpdate(createTableSQL);
							System.out.println("Tabelle '" + tableName + "' wurde erstellt.");
						}
					}

				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public static java.sql.Connection getConnection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			connection = DriverManager.getConnection(url, user, password);
		}
		return connection;
	}

}
