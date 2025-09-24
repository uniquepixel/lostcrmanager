package sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Clan {

	private String clan_tag;
	private String name;
	private ArrayList<Player> playerlist;
	private Long max_kickpoints;
	private Integer kickpoints_expire_after_days;
	private ArrayList<KickpointReason> kickpoint_reasons;

	public Clan(String clantag) {
		clan_tag = clantag;
	}

	// all public getter Methods

	public String getInfoString() {
		return getName() + " (" + clan_tag + ")";
	}

	public String getTag() {
		return clan_tag;
	}

	public ArrayList<Player> getPlayers() {
		return createPlayerList();
	}

	public Long getMaxKickpoints() {
		return createMaxKickpoints();
	}

	public Integer getDaysKickpointsExpireAfter() {
		return createKickpointsExpire();
	}

	public ArrayList<KickpointReason> getKickpointReasons() {
		return createKickpointReasons();
	}

	public String getName() {
		return createName();
	}

	// all private Methods to set Attributes correctly

	private ArrayList<Player> createPlayerList() {
		String sql = "SELECT player_tag FROM clan_members WHERE clan_tag = ? ";
		ArrayList<String> result = DBUtil.getArrayListFromSQL(sql, String.class, clan_tag);

		playerlist = new ArrayList<>();
		for (String tags : result) {
			playerlist.add(new Player(tags));
		}

		return playerlist;
	}

	private Long createMaxKickpoints() {
		String sql = "SELECT max_kickpoints FROM clan_settings WHERE clan_tag = ?";
		max_kickpoints = DBUtil.getValueFromSQL(sql, Long.class, clan_tag);
		return max_kickpoints;
	}

	private Integer createKickpointsExpire() {
		String sql = "SELECT kickpoints_expire_after_days FROM clan_settings WHERE clan_tag = ?";
		kickpoints_expire_after_days = DBUtil.getValueFromSQL(sql, Integer.class, clan_tag);
		return kickpoints_expire_after_days;
	}

	private ArrayList<KickpointReason> createKickpointReasons() {
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

		return kickpoint_reasons;
	}

	private String createName() {
		String sql = "SELECT name FROM clans WHERE tag = ?";
		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			pstmt.setString(1, clan_tag);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					name = rs.getString("name");
					return name;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

}
