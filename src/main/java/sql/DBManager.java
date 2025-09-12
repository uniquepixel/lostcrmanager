package sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.interactions.commands.Command;

public class DBManager {

	public enum InClanType {
		INCLAN, NOTINCLAN, ALL
	}

	public static boolean PlayerTagIsLinked(String crTag) {
		String sql = "SELECT 1 FROM players WHERE cr_tag = ?";
		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			pstmt.setString(1, crTag);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next(); // true, wenn mindestens eine Zeile existiert
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean ClanExists(String clanTag) {
		String sql = "SELECT 1 FROM clans WHERE tag = ?";
		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			pstmt.setString(1, clanTag);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next(); // true, wenn mindestens eine Zeile existiert
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static String getClanName(String clanTag) {
		String sql = "SELECT name FROM clans WHERE tag = ?";
		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			pstmt.setString(1, clanTag);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return rs.getString("name");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<Command.Choice> getClansAutocomplete(String input) {
		List<Command.Choice> choices = new ArrayList<>();

		String sql = "SELECT name, tag FROM clans ORDER BY index ASC";

		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String tag = rs.getString("tag");
					String name = rs.getString("name");

					String display = name + " (" + tag + ")";

					if (display.toLowerCase().contains(input.toLowerCase())
							|| tag.toLowerCase().startsWith(input.toLowerCase())) {
						choices.add(new Command.Choice(display, tag));
						if (choices.size() == 25) {
							break;
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return choices;
	}

	public static List<Command.Choice> getPlayerlistAutocomplete(String input, InClanType inclantype) {
		List<Command.Choice> choices = new ArrayList<>();

		String sql = "SELECT players.cr_tag AS tag, players.name AS player_name, clans.name AS clan_name "
				+ "FROM players " + "LEFT JOIN clan_members ON clan_members.player_tag = players.cr_tag "
				+ "LEFT JOIN clans ON clans.tag = clan_members.clan_tag";

		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String tag = rs.getString("tag");
					String clanName = rs.getString("clan_name");

					String display = new Player(tag).getInfoString();
					if (inclantype == InClanType.NOTINCLAN) {
						if (clanName == null || clanName.isEmpty()) {
							if (display.toLowerCase().contains(input.toLowerCase())
									|| tag.toLowerCase().startsWith(input.toLowerCase())) {
								choices.add(new Command.Choice(display, tag));
								if (choices.size() == 25) {
									break;
								}
							}
						}
					} else if (inclantype == InClanType.INCLAN) {
						if (clanName != null && !clanName.isEmpty()) {
							display += " - " + clanName;
							if (display.toLowerCase().contains(input.toLowerCase())
									|| tag.toLowerCase().startsWith(input.toLowerCase())) {
								choices.add(new Command.Choice(display, tag));
								if (choices.size() == 25) {
									break; // Max 25 Vorschläge
								}
							}
						}
					} else if (inclantype == InClanType.ALL) {
						if (clanName != null && !clanName.isEmpty()) {
							display += " - " + clanName;
						}

						// Filter mit Eingabe (input ist String mit aktuell eingegebenem Text)
						if (display.toLowerCase().contains(input.toLowerCase())
								|| tag.toLowerCase().startsWith(input.toLowerCase())) {
							choices.add(new Command.Choice(display, tag));
							if (choices.size() == 25) {
								break; // Max 25 Vorschläge
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return choices;
	}

}
