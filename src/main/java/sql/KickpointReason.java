package sql;

public class KickpointReason {

	private String kpreason;
	private String clan_tag;
	private int amount;

	public KickpointReason(String reason, String clan_tag) {
		kpreason = reason;
		this.clan_tag = clan_tag;
	}

	// all public getter Methods

	public String getReason() {
		return kpreason;
	}

	public String getClanTag() {
		return clan_tag;
	}

	public int getAmount() {
		return createAmount();
	}

	// all private Methods to set Attributes correctly

	private int createAmount() {
		String sql = "SELECT amount FROM kickpoint_reasons WHERE clan_tag = ? AND name = ?";
		amount = DBUtil.getValueFromSQL(sql, Integer.class, clan_tag, kpreason);
		return amount;
	}
}
