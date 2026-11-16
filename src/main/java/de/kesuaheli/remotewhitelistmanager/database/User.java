package de.kesuaheli.remotewhitelistmanager.database;

import de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import static de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager.LOGGER;

public class User {
	private static final RandomGenerator RANDOM = new Random();
	public final int ID;
	private String email;
	private String username;
	private String name;
	private String onlineName;
	private List<String> nicknames;
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
			TABLE+".name AS "+TABLE+"_name, "+
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
			user.name = res.getString(TABLE+"_name");
			user.onlineName = res.getString("online_name");
			user.nicknames = Arrays.stream(res.getString("nicknames").split("\\|")).filter(s -> !s.isBlank()).toList();
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
		List<UUID> uuids = new ArrayList<>();
		for (UUID uuid : RemoteWhitelistmanager.usernameCache.keySet()) {
			String username = RemoteWhitelistmanager.usernameCache.get(uuid).toLowerCase();
			if (username.contains(name.toLowerCase())) {
				uuids.add(uuid);
			}
		}
		final String uuidQueryPlaceholders;
		if (uuids.isEmpty()) {
			uuidQueryPlaceholders = "";
		} else {
			final String placeholders = uuids.stream()
					.map(_ ->"?")
					.collect(Collectors.joining(", "));
			uuidQueryPlaceholders = " OR "+WhitelistEntry.TABLE+".uuid IN ("+placeholders+")";
		}

		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement(GET_ALL_SQL+" WHERE lower("+TABLE+".online_name) LIKE lower(?) OR lower("+TABLE+".username) LIKE lower(?) OR lower("+TABLE+".nicknames) LIKE lower(?)" + uuidQueryPlaceholders)) {
			statement.setString(1, "%"+name+"%");
			statement.setString(2, "%"+name+"%");
			statement.setString(3, "%"+name+"%");
			for (int i = 0; i < uuids.size(); i++) {
				statement.setString(i+4, uuids.get(i).toString());
			}
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
				", nicknames=" + nicknames +
				", referenceID=" + referenceID +
				", reference=" + reference +
				", flags=" + flags +
				", discord='" + discord + '\'' +
				", twitch='" + twitch + '\'' +
				", whitelistEntry=" + whitelistEntry +
				'}';
	}

	public MutableComponent format() {
		MutableComponent text = Component.literal("Username: ").withColor(TextColor.GRAY);
		text.append(Component.literal(this.username).withColor(TextColor.GOLD));
		if (!this.onlineName.isEmpty()) {
			text.append("\nName: ");
			text.append(Component.literal(this.onlineName).withColor(TextColor.AQUA));
		}
		if (!this.nicknames.isEmpty()) {
			text.append("\nNicknames: ");
			text.append(Component.literal(String.join(", ", this.nicknames)).withColor(TextColor.DARK_AQUA));
		}
		if (!this.discord.isBlank()) {
			text.append("\nDiscord ID: ");
			text.append(Component.literal(this.discord).withColor(TextColor.WHITE));
		}
		if (this.whitelistEntry != null && this.whitelistEntry.isActive()) {
			text.append("\nMinecraft: ");
			String mcUsername = RemoteWhitelistmanager.getUsername(this.whitelistEntry.uuid);
			text.append(Component.literal(mcUsername).withColor(TextColor.WHITE));
		}
		text.append("\nRef: ");
		if (this.reference == null) {
			try {
				this.reference = getByID(this.referenceID);
			} catch (Exception e) {
				LOGGER.error("Failed to get ref user by ID '{}'", this.referenceID, e);
			}
			if (this.reference == null) {
				text.append(Component.literal("UNKNOWN<"+this.referenceID+">").withColor(TextColor.RED));
				return text;
			}
		}

		text.append(Component.literal(this.reference.username).withStyle(ChatFormatting.ITALIC));
		return text;
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

	public List<String> getNicknames() {
		return nicknames;
	}

	public WhitelistEntry getWhitelistEntry() {
		return whitelistEntry;
	}
}
