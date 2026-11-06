package de.kesuaheli.remotewhitelistmanager.database;

import de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.random.RandomGenerator;

public class WhitelistEntry {
	private static final RandomGenerator RANDOM = new Random();
	private final int userID;
	public final UUID uuid;
	private int flags;
	private User user;

	static final Map<UUID, WhitelistEntry> CACHE = new HashMap<>();

	public static final String TABLE = "whitelist";
	private static final String GET_ALL_SQL = "SELECT *, "+
			TABLE+".id AS "+TABLE+"_id, "+
			TABLE+".flags AS "+TABLE+"_flags, "+
			User.TABLE+".id AS "+User.TABLE+"_id, "+
			User.TABLE+".flags AS "+User.TABLE+"_flags FROM "+TABLE+" INNER JOIN "+User.TABLE+" on "+TABLE+".id = "+User.TABLE+".id";

	public static final int FLAG_ACTIVE = 1;

	private WhitelistEntry(final int userID, final UUID uuid) {
		this.userID = userID;
		this.uuid = uuid;
	}

	static @Nullable WhitelistEntry parse(ResultSet res) throws SQLException {
		final UUID uuid;
		try {
			uuid = UUID.fromString(res.getString("uuid"));
		} catch (SQLException e) {
			// assume there is no whitelistEntry
			return null;
		}
		WhitelistEntry mcAccount = CACHE.get(uuid);
		if (mcAccount == null) {
			final int id = res.getInt(TABLE + "_id");
			mcAccount = new WhitelistEntry(id, uuid);
			mcAccount.flags = res.getInt(TABLE + "_flags");
			mcAccount.user = User.parse(res);
			CACHE.put(uuid, mcAccount);
		}
		return mcAccount;
	}

	public static @Nullable WhitelistEntry getByID(int id) {
		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement(GET_ALL_SQL+" WHERE "+TABLE+".id=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			statement.setInt(1, id);
			ResultSet res = statement.executeQuery();
			if (!res.first()) {
				// no user found with id
				return null;
			}
			return parse(res);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static @Nullable WhitelistEntry getByUUID(String uuid) {
		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement(GET_ALL_SQL+" WHERE "+TABLE+".uuid=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			statement.setString(1, uuid);
			ResultSet res = statement.executeQuery();
			if (!res.first()) {
				// no user found with id
				return null;
			}
			return parse(res);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static @NotNull List<WhitelistEntry> getAll() {
		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement(GET_ALL_SQL)) {
			ResultSet res = statement.executeQuery();
			List<WhitelistEntry> entries = new ArrayList<>();
			while (res.next()) {
				entries.add(parse(res));
			}
			return entries;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static void clearCache() {
		CACHE.clear();
	}

	@Override
	public String toString() {
		return "WhitelistEntry{" +
				"userID=" + userID +
				", uuid=" + uuid +
				", flags=" + flags +
				'}';
	}

	public boolean isActive() {
		return (this.flags&FLAG_ACTIVE) != 0;
	}

	public void setActive() {
		this.setActive(true);
	}

	public void setActive(boolean state) {
		if (state) {
			this.flags |= FLAG_ACTIVE;
		} else {
			this.flags &= ~FLAG_ACTIVE;
		}
	}

	public User getUser() {
		return user;
	}

	public UUID getUuid() {
		return uuid;
	}
}
