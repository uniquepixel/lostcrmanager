package datawrapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import datautil.APIUtil;
import datautil.Connection;
import datautil.DBUtil;

public class Clan {

	private String clan_tag;
	private String namedb;
	private String nameapi;
	private ArrayList<Player> playerlistdb;
	private ArrayList<Player> playerlistapi;
	private Long max_kickpoints;
	private Integer kickpoints_expire_after_days;
	private ArrayList<KickpointReason> kickpoint_reasons;

	public enum Role {
		LEADER, COLEADER, ELDER, MEMBER
	}

	public Clan(String clantag) {
		clan_tag = clantag;
	}

	public boolean ExistsDB() {
		String sql = "SELECT 1 FROM clans WHERE tag = ?";
		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			pstmt.setString(1, clan_tag);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next(); // true, wenn mindestens eine Zeile existiert
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// all public getter Methods

	public String getRoleID(Role role) {
		switch (role) {
		case LEADER:
			return DBUtil.getValueFromSQL("SELECT leader_roleid FROM clans WHERE tag = ?", String.class, clan_tag);
		case COLEADER:
			return DBUtil.getValueFromSQL("SELECT coleader_roleid FROM clans WHERE tag = ?", String.class, clan_tag);
		case ELDER:
			return DBUtil.getValueFromSQL("SELECT elder_roleid FROM clans WHERE tag = ?", String.class, clan_tag);
		case MEMBER:
			return DBUtil.getValueFromSQL("SELECT member_roleid FROM clans WHERE tag = ?", String.class, clan_tag);
		}
		return null;
	}

	public String getInfoString() {
		if (!clan_tag.equals("warteliste")) {
			return getNameDB() + " (" + clan_tag + ")";
		} else {
			return getNameDB();
		}
	}

	public String getTag() {
		return clan_tag;
	}

	public ArrayList<Player> getPlayersDB() {
		if (playerlistdb == null) {
			String sql = "SELECT player_tag FROM clan_members WHERE clan_tag = ? ";
			ArrayList<String> result = DBUtil.getArrayListFromSQL(sql, String.class, clan_tag);

			playerlistdb = new ArrayList<>();
			for (String tags : result) {
				playerlistdb.add(new Player(tags));
			}
		}
		return playerlistdb;
	}

	public ArrayList<Player> getPlayersAPI() {
		if (playerlistapi == null) {
			playerlistapi = new ArrayList<>();
			JSONObject jsonObject = new JSONObject(APIUtil.getClanJson(clan_tag));

			JSONArray members = jsonObject.getJSONArray("memberList");

			for (int i = 0; i < members.length(); i++) {
				JSONObject member = members.getJSONObject(i);
				if (member.has("tag") && member.has("name")) {
					playerlistapi.add(new Player(member.getString("tag")).setNameAPI(member.getString("name")));
				}
			}
		}
		return playerlistapi;
	}

	public Long getMaxKickpoints() {
		if (max_kickpoints == null) {
			String sql = "SELECT max_kickpoints FROM clan_settings WHERE clan_tag = ?";
			max_kickpoints = DBUtil.getValueFromSQL(sql, Long.class, clan_tag);
		}
		return max_kickpoints;
	}

	public Integer getDaysKickpointsExpireAfter() {
		if (kickpoints_expire_after_days == null) {
			String sql = "SELECT kickpoints_expire_after_days FROM clan_settings WHERE clan_tag = ?";
			kickpoints_expire_after_days = DBUtil.getValueFromSQL(sql, Integer.class, clan_tag);
		}
		return kickpoints_expire_after_days;
	}

	public ArrayList<KickpointReason> getKickpointReasons() {
		if (kickpoint_reasons == null) {
			kickpoint_reasons = new ArrayList<>();

			String sql = "SELECT name, clan_tag FROM kickpoint_reasons WHERE clan_tag = ?";
			try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
				// Parameter setzen
				pstmt.setObject(1, clan_tag);

				try (ResultSet rs = pstmt.executeQuery()) {
					while (rs.next()) {
						kickpoint_reasons.add(new KickpointReason(rs.getString("name"), rs.getString("clan_tag")));
					}
					Statement stmt = rs.getStatement();
					rs.close();
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return kickpoint_reasons;
	}

	public String getNameDB() {
		if (namedb == null) {
			String sql = "SELECT name FROM clans WHERE tag = ?";
			try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
				pstmt.setString(1, clan_tag);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						namedb = rs.getString("name");
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return namedb;
	}

	public String getNameAPI() {
		if (nameapi == null) {
			JSONObject jsonObject = new JSONObject(APIUtil.getClanJson(clan_tag));
			nameapi = jsonObject.getString("name");
		}
		return nameapi;
	}
}
