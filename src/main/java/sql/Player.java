package sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;

public class Player {

	public enum RoleType {
		LEADER, COLEADER, ELDER, MEMBER
	};

	private String tag;
	private String name;
	private User user;
	private Clan clan;
	private ArrayList<Kickpoint> kickpoints;
	private int kickpointstotal = -1;
	private RoleType role;

	public Player(String tag) {
		this.tag = tag;
	}

	// all public getter Methods
	
	public String getInfoString() {
		try {
			return getName() + " (" + tag + ")";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getTag() {
		return tag;
	}
	
	public String getName() {
		return createName();
	}

	public User getUser() {
		return createUser();
	}

	public Clan getClan() {
		return createClan();
	}

	public ArrayList<Kickpoint> getActiveKickpoints() {
		return createKickpointList();
	}

	public int getTotalKickpoints() {
		return createTotalKP();
	}

	public RoleType getRole() {
		return createRoleType();
	}

	// all private Methods to set Attributes correctly
	private String createName() {
		name = DBUtil.getValueFromSQL("SELECT name FROM players WHERE cr_tag = ?", String.class, tag);
		return name == null ? api.Player.getPlayerName(tag) : name;
	}
	
	private User createUser() {
		String value = DBUtil.getValueFromSQL("SELECT discord_id FROM players WHERE cr_tag = ?", String.class, tag);
		user = value == null ? null : new User(value);
		return user;
	}

	private Clan createClan() {
		String value = DBUtil.getValueFromSQL("SELECT clan_tag FROM clan_members WHERE player_tag = ?", String.class,
				tag);
		clan = value == null ? null : new Clan(value);
		return clan;
	}

	private ArrayList<Kickpoint> createKickpointList() {
		kickpoints = new ArrayList<>();
		String sql = "SELECT id FROM kickpoints WHERE player_tag = ?";
		for (Integer id : DBUtil.getArrayListFromSQL(sql, Integer.class, tag)) {
			Kickpoint kp = new Kickpoint(id);
			if (kp.getExpirationDate().isAfter(OffsetDateTime.now())) {
				kickpoints.add(kp);
			}
		}
		return kickpoints;
	}

	private int createTotalKP() {
		ArrayList<Kickpoint> a = new ArrayList<>();
		String sql = "SELECT id FROM kickpoints WHERE player_tag = ?";
		for (Integer id : DBUtil.getArrayListFromSQL(sql, Integer.class, tag)) {
			a.add(new Kickpoint(id));
		}
		kickpointstotal = a.size();
		return kickpointstotal;
	}

	private RoleType createRoleType() {
		if (new Player(tag).getClan() == null) {
			return null;
		}
		String sql = "SELECT clan_role FROM clan_members WHERE player_tag = ?";
		try (PreparedStatement pstmt = Connection.getConnection().prepareStatement(sql)) {
			pstmt.setString(1, tag);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					String rolestring = rs.getString("clan_role");
					role = rolestring.equals("leader") ? RoleType.LEADER
							: rolestring.equals("coleader") ? RoleType.COLEADER
									: rolestring.equals("elder") ? RoleType.ELDER
											: rolestring.equals("member") ? RoleType.MEMBER : null;
					return role;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
