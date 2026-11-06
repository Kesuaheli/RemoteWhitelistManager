package de.kesuaheli.remotewhitelistmanager.database;

import de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.random.RandomGenerator;

public class User {
	private static final RandomGenerator RANDOM = new Random();
	public final int ID;
	private String email;
	private String username;
	private String name;
	private String onlineName;
	private String[] nicknames;
	private int referenceID;
	private User reference;
	private int flags;
	private String discord;
	private String twitch;
	private WhitelistEntry whitelistEntry;

	static final Map<Integer, User> CACHE = new HashMap<>();

	public static final String TABLE = "users";
	private static final String GET_ALL_SQL = "SELECT *, "+
			WhitelistEntry.TABLE+".id AS "+WhitelistEntry.TABLE+"_id, "+
			WhitelistEntry.TABLE+".flags AS "+WhitelistEntry.TABLE+"_flags, "+
			TABLE+".id AS "+TABLE+"_id, "+
			TABLE+".flags AS "+TABLE+"_flags FROM "+TABLE+" LEFT OUTER JOIN "+WhitelistEntry.TABLE+" on "+TABLE+".id = "+WhitelistEntry.TABLE+".id";

	public static final int FLAG_ADMIN = 1;

	private User(final int id) {
		this.ID = id;
	}

	static User parse(ResultSet res) throws SQLException {
		int id = res.getInt(TABLE+"_id");
		User user = CACHE.get(id);
		if (user == null) {
			user = new User(id);
			user.email = res.getString("email");
			user.username = res.getString("username");
			user.name = res.getString("name");
			user.onlineName = res.getString("online_name");
			user.nicknames = res.getString("nicknames").split("\\|");
			user.referenceID = res.getInt("reference_id");
			user.flags = res.getInt(TABLE + "_flags");
			user.discord = res.getString("discord");
			user.twitch = res.getString("twitch");
			CACHE.put(id, user);
		}
		return user;
	}

	public static @Nullable User getByID(int id) {
		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement(GET_ALL_SQL+" WHERE "+TABLE+".id=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			statement.setInt(1, id);
			ResultSet res = statement.executeQuery();
			if (!res.first()) {
				// no user found with id
				return null;
			}
			User user = parse(res);
			user.whitelistEntry = WhitelistEntry.parse(res);
			return user;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static @NotNull List<User> getByNameMatch(String name) {
		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement(GET_ALL_SQL+" WHERE lower("+TABLE+".online_name) LIKE lower(?)")) {
			statement.setString(1, "%"+name+"%");
			ResultSet res = statement.executeQuery();
			List<User> entries = new ArrayList<>();
			while (res.next()) {
				User user = parse(res);
				user.whitelistEntry = WhitelistEntry.parse(res);
				entries.add(user);
			}
			return entries;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static @NotNull List<User> getAll() {
		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement(GET_ALL_SQL)) {
			ResultSet res = statement.executeQuery();
			List<User> entries = new ArrayList<>();
			while (res.next()) {
				final User user = parse(res);
				user.whitelistEntry = WhitelistEntry.parse(res);
				entries.add(user);
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
		return "User{" +
				"ID=" + ID +
				", email='" + email + '\'' +
				", username='" + username + '\'' +
				", name='" + name + '\'' +
				", onlineName='" + onlineName + '\'' +
				", nicknames=" + Arrays.toString(nicknames) +
				", referenceID=" + referenceID +
				", reference=" + reference +
				", flags=" + flags +
				", discord='" + discord + '\'' +
				", twitch='" + twitch + '\'' +
				", whitelistEntry=" + whitelistEntry +
				'}';
	}

	public boolean isAdmin() {
		return (this.flags& FLAG_ADMIN) != 0;
	}

	public String getEmail() {
		return email;
	}

	public String getUsername() {
		return username;
	}

	public String getOnlineName() {
		return onlineName;
	}

	public String[] getNicknames() {
		return nicknames;
	}

	public WhitelistEntry getWhitelistEntry() {
		return whitelistEntry;
	}
}
