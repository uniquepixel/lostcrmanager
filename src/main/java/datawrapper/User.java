package datawrapper;

import java.util.ArrayList;
import java.util.HashMap;

import datautil.DBManager;
import datautil.DBUtil;

public class User {

	public enum PermissionType {
		ADMIN, LEADER, COLEADER, ELDER, MEMBER, NOTHING
	}

	private HashMap<String, Player.RoleType> clanroles;
	private String userid;
	private ArrayList<Player> linkedaccounts;
	private Boolean isadmin;

	public User(String userid) {
		this.userid = userid;
	}

	public User refreshData() {
		clanroles = null;
		linkedaccounts = null;
		isadmin = null;
		return this;
	}

	// all public getter Methods
	public String getUserID() {
		return userid;
	}

	public boolean isAdmin() {
		if (isadmin == null) {
			if (DBUtil.getValueFromSQL("SELECT is_admin FROM users WHERE discord_id = ?", Boolean.class,
					userid) == null) {
				DBUtil.executeUpdate("INSERT INTO users (discord_id, is_admin) VALUES (?, ?)", userid, false);
			}
			isadmin = DBUtil.getValueFromSQL("SELECT is_admin FROM users WHERE discord_id = ?", Boolean.class, userid);
		}
		return isadmin;
	}

	public ArrayList<Player> getAllLinkedAccounts() {
		if (linkedaccounts == null) {
			linkedaccounts = new ArrayList<>();
			String sql = "SELECT cr_tag FROM players WHERE discord_id = ?";
			for (String tag : DBUtil.getArrayListFromSQL(sql, String.class, userid)) {
				linkedaccounts.add(new Player(tag));
			}
		}
		return linkedaccounts;
	}

	public HashMap<String, Player.RoleType> getClanRoles() {
		if (clanroles == null) {
			clanroles = new HashMap<>();
			boolean admin = false;
			if (DBUtil.getValueFromSQL("SELECT is_admin FROM users WHERE discord_id = ?", Boolean.class,
					userid) == null) {
				DBUtil.executeUpdate("INSERT INTO users (discord_id, is_admin) VALUES (?, ?)", userid, false);
			}
			if (DBUtil.getValueFromSQL("SELECT is_admin FROM users WHERE discord_id = ?", Boolean.class, userid)) {
				admin = true;
			}

			ArrayList<Player> linkedaccs = getAllLinkedAccounts();
			ArrayList<String> allclans = DBManager.getAllClans();
			if (admin) {
				for (String clantag : allclans) {
					clanroles.put(clantag, Player.RoleType.ADMIN);
				}
			} else {
				for (Player p : linkedaccs) {
					System.out.println(p.getRole());
					clanroles.put(p.getClanDB().getTag(), p.getRole());
				}
			}
		}
		return clanroles;
	}

}
