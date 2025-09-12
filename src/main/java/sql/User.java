package sql;

import java.util.ArrayList;

import lostcrmanager.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class User {

	public enum PermissionType {
		ADMIN, LEADER, COLEADER, ELDER, MEMBER, NOTHING
	}

	private String userid;
	private ArrayList<Player> linkedaccounts;
	private PermissionType permissions;

	public User(String userid) {
		this.userid = userid;
	}

	// all public getter Methods
	public String getUserID() {
		return userid;
	}

	public ArrayList<Player> getAllLinkedAccounts() {
		return createAllLinkedAccountsList();
	}

	public PermissionType getPermissions() {
		return createPermissions();
	}

	// all private Methods to set Attributes correctly
	private ArrayList<Player> createAllLinkedAccountsList() {
		linkedaccounts = new ArrayList<>();
		String sql = "SELECT cr_tag FROM players WHERE discord_id = ?";
		for (String tag : DBUtil.getArrayListFromSQL(sql, String.class, userid)) {
			linkedaccounts.add(new Player(tag));
		}
		return linkedaccounts;
	}

	private PermissionType createPermissions() {
		if(DBUtil.getValueFromSQL("SELECT is_admin FROM users WHERE discord_id = ?", Boolean.class, userid) == null) {
			DBUtil.executeUpdate("INSERT INTO users (discord_id, is_admin) VALUES (?, ?)", userid, false);
		}
		if (DBUtil.getValueFromSQL("SELECT is_admin FROM users WHERE discord_id = ?", Boolean.class, userid)) {
			permissions = PermissionType.ADMIN;
			return permissions;
		}
		Guild guild = Bot.getJda().getGuildById(Bot.guild_id);
		Member member = guild.getMemberById(userid);

		for (String leader_role_ids : DBUtil.getArrayListFromSQL("SELECT leader_roleid FROM clans", String.class)) {
			Role role = guild.getRoleById(leader_role_ids);
			if (member.getRoles().contains(role)) {
				permissions = PermissionType.LEADER;
				return permissions;
			}
		}
		for (String leader_role_ids : DBUtil.getArrayListFromSQL("SELECT coleader_roleid FROM clans", String.class)) {
			Role role = guild.getRoleById(leader_role_ids);
			if (member.getRoles().contains(role)) {
				permissions = PermissionType.COLEADER;
				return permissions;
			}
		}
		for (String leader_role_ids : DBUtil.getArrayListFromSQL("SELECT elder_roleid FROM clans", String.class)) {
			Role role = guild.getRoleById(leader_role_ids);
			if (member.getRoles().contains(role)) {
				permissions = PermissionType.ELDER;
				return permissions;
			}
		}
		for (String leader_role_ids : DBUtil.getArrayListFromSQL("SELECT member_roleid FROM clans", String.class)) {
			Role role = guild.getRoleById(leader_role_ids);
			if (member.getRoles().contains(role)) {
				permissions = PermissionType.MEMBER;
				return permissions;
			}
		}
		permissions = PermissionType.NOTHING;
		return permissions;
	}

}
