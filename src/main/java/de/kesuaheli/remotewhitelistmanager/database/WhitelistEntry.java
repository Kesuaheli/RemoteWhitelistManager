package de.kesuaheli.remotewhitelistmanager.database;

import de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class WhitelistEntry {
	public final int userID;
	//public User user;
	public final UUID uuid;
	public String name;
	public String[] nicknames;
	private int referenceID;
	public WhitelistEntry reference;
	public String discordID;
	//public Marker[] markers;
	private int flags;

	public static final int FLAG_ACTIVE = 1;
	public static final int FLAG_ADMIN = 2;

	private WhitelistEntry(final int userID, final UUID uuid) {
		this.userID = userID;
		this.uuid = uuid;
	}

	public static @Nullable WhitelistEntry getByID(int id) {
		final WhitelistEntry entry;
		try {
			try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement("SELECT uuid,name,nicknames,reference,discord_id,flags FROM whitelist WHERE id=?")) {
				statement.setInt(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.first()) {
					// no user found with id
					return null;
				}
				// early ignore inactive entries
				int flags = res.getInt(6);
				if ((flags&FLAG_ACTIVE) == 0) {
					return null;
				}

				String uuid = res.getString(1);
				entry = new WhitelistEntry(id, UUID.fromString(uuid));

				entry.name = res.getString(2);
				entry.nicknames = res.getString(3).split("\\|");
				entry.referenceID = res.getInt(4);
				entry.discordID = res.getString(5);
				entry.flags = flags;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return entry;
	}

	public static @NotNull List<WhitelistEntry> getByNameMatch(String name) {
		List<WhitelistEntry> entries = new ArrayList<>();
		try {
			try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement("SELECT id,uuid,name,nicknames,reference,discord_id,flags FROM whitelist WHERE lower(name) LIKE lower(?)")) {
				statement.setString(1, "%"+name+"%");
				ResultSet res = statement.executeQuery();
				while (res.next()) {
					// early ignore inactive entries
					int flags = res.getInt(7);
					if ((flags&FLAG_ACTIVE) == 0) {
						continue;
					}

					int id = res.getInt(1);
					String uuid = res.getString(2);
					final WhitelistEntry entry = new WhitelistEntry(id, UUID.fromString(uuid));

					entry.name = res.getString(3);
					entry.nicknames = res.getString(4).split("\\|");
					entry.referenceID = res.getInt(5);
					entry.discordID = res.getString(6);
					entry.flags = flags;
					entries.add(entry);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return entries;
	}

	public static @NotNull List<WhitelistEntry> getAll() {
		List<WhitelistEntry> entries = new ArrayList<>();
		try {
			try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement("SELECT id,uuid,name,nicknames,reference,discord_id,flags FROM whitelist")) {
				ResultSet res = statement.executeQuery();
				while (res.next()) {
					// early ignore inactive entries
					int flags = res.getInt(7);
					if ((flags&FLAG_ACTIVE) == 0) {
						continue;
					}

					int id = res.getInt(1);
					String uuid = res.getString(2);
					final WhitelistEntry entry = new WhitelistEntry(id, UUID.fromString(uuid));

					entry.name = res.getString(3);
					entry.nicknames = res.getString(4).split("\\|");
					entry.referenceID = res.getInt(5);
					entry.discordID = res.getString(6);
					entry.flags = flags;
					entries.add(entry);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return entries;
	}

	@Override
	public String toString() {
		return "WhitelistEntry{" +
				"userID=" + userID +
				", uuid=" + uuid +
				", name='" + name + '\'' +
				", nicknames=" + Arrays.toString(nicknames) +
				", referenceID=" + referenceID +
				", reference=" + reference +
				", discordID='" + discordID + '\'' +
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

	public boolean isAdmin() {
		return (this.flags&FLAG_ADMIN) != 0;
	}
}
