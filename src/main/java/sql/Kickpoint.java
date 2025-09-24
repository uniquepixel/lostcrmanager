package sql;

import java.time.OffsetDateTime;

public class Kickpoint {

	private long id;
	private String description;
	private long amount;
	private Player player;
	private OffsetDateTime kpdate;
	private OffsetDateTime givendate;
	private OffsetDateTime expirationdate;
	private User givenby;

	public Kickpoint(long id) {
		this.id = id;
	}

	// all public getter Methods

	public long getID() {
		return id;
	}

	public String getDescription() {
		return createDescription();
	}

	public long getAmount() {
		return createAmount();
	}

	public Player getPlayer() {
		return createPlayer();
	}

	public OffsetDateTime getDate() {
		return createKPDate();
	}

	public OffsetDateTime getGivenDate() {
		return createGivenDate();
	}

	public OffsetDateTime getExpirationDate() {
		return createExpirationDate();
	}

	public User getUserGivenBy() {
		return createGivenBy();
	}

	// all private Methods to set Attributes correctly

	private String createDescription() {
		description = DBUtil.getValueFromSQL("SELECT description FROM kickpoints WHERE id = ?", String.class, id);
		return description;
	}

	private long createAmount() {
		amount = DBUtil.getValueFromSQL("SELECT amount FROM kickpoints WHERE id = ?", Long.class, id);
		return amount;
	}

	private Player createPlayer() {
		String value = DBUtil.getValueFromSQL("SELECT player_tag FROM kickpoints WHERE id = ?", String.class, id);
		player = value == null ? null : new Player(value);
		return player;
	}

	private OffsetDateTime createKPDate() {
		kpdate = DBUtil.getDateFromSQL("SELECT date FROM kickpoints WHERE id = ?", id);
		return kpdate;
	}

	private OffsetDateTime createGivenDate() {
		givendate = DBUtil.getDateFromSQL("SELECT created_at FROM kickpoints WHERE id = ?", id);
		return givendate;
	}

	private OffsetDateTime createExpirationDate() {
		expirationdate = DBUtil.getDateFromSQL("SELECT expires_at FROM kickpoints WHERE id = ?", id);
		return expirationdate;
	}

	private User createGivenBy() {
		String value = DBUtil.getValueFromSQL("SELECT created_by_discord_id FROM kickpoints WHERE id = ?", String.class,
				id);
		givenby = value == null ? null : new User(value);
		return givenby;
	}

}
