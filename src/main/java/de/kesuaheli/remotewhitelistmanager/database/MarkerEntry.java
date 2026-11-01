package de.kesuaheli.remotewhitelistmanager.database;

import de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager;
import net.pl3x.map.core.markers.marker.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MarkerEntry extends Rectangle {
	private final List<WhitelistEntry> users = new ArrayList<>();
	public int ID;

	private MarkerEntry(final int ID, String key, int X1, int Z1, int X2, int Z2) {
		super(key, X1, Z1, X2, Z2);
		this.ID = ID;
	}

	public static @Nullable MarkerEntry getByID(int id) {
		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement("SELECT point1_x,point1_z,point2_x,point2_z,color FROM markers WHERE id=?")) {
			statement.setInt(1, id);
			ResultSet res = statement.executeQuery();
			if (!res.first()) {
				// no marker found with id
				return null;
			}

			return new MarkerEntry(id,
					""+id,
					res.getInt(1),
					res.getInt(2),
					res.getInt(3),
					res.getInt(4));
		} catch (SQLException e) {
			throw new RuntimeException("Database getting marker entry by ID", e);
		}
	}

	public static @NotNull List<MarkerEntry> getAll() {
		List<MarkerEntry> entries = new ArrayList<>();
		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement("SELECT id,point1_x,point1_z,point2_x,point2_z,color FROM markers")) {
			ResultSet res = statement.executeQuery();
			while (res.next()) {
				int id = res.getInt(1);
				entries.add(new MarkerEntry(id,
					""+id,
					res.getInt(2),
					res.getInt(3),
					res.getInt(4),
					res.getInt(5)
				));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return entries;
	}

	@Override
	public @NonNull String toString() {
		return "Marker" + this.ID + ": " +
			"users=" + this.users +
			", marker=" + super.toString()
			;
	}
}
