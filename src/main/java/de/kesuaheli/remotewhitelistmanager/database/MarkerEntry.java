package de.kesuaheli.remotewhitelistmanager.database;

import com.mojang.authlib.GameProfile;
import de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager;
import net.pl3x.map.core.markers.marker.Rectangle;
import net.pl3x.map.core.markers.option.Fill;
import net.pl3x.map.core.markers.option.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MarkerEntry extends Rectangle {
	public final int ID;

	public static final Map<UUID, String> usernameCache = new HashMap<>();

	private final Options.Builder optionsBuilder = Options.builder();

	private final List<WhitelistEntry> users = new ArrayList<>();

	static final Map<Integer, MarkerEntry> CACHE = new HashMap<>();

	public static final String TABLE = "markers";
	public static final String LINK_TABLE = "user_markers";
	private static final String GET_ALL_SQL = "SELECT *, "+
			TABLE+".id AS "+TABLE+"_id, "+
			WhitelistEntry.TABLE+".id AS "+WhitelistEntry.TABLE+"_id, "+
			WhitelistEntry.TABLE+".flags AS "+WhitelistEntry.TABLE+"_flags, "+
			User.TABLE+".id AS "+User.TABLE+"_id, "+
			User.TABLE+".flags AS "+User.TABLE+"_flags FROM "+TABLE+
			" LEFT OUTER JOIN "+LINK_TABLE+" ON "+TABLE+".id = "+LINK_TABLE+".marker_id"+
			" INNER JOIN "+WhitelistEntry.TABLE+" ON "+LINK_TABLE+".whitelist_entry_uuid = "+WhitelistEntry.TABLE+".uuid"+
			" INNER JOIN "+User.TABLE+" ON "+WhitelistEntry.TABLE+".id = "+User.TABLE+".id";

	private MarkerEntry(final int ID, int X1, int Z1, int X2, int Z2) {
		super(""+ID, X1, Z1, X2, Z2);
		this.ID = ID;
	}

	static MarkerEntry parse(ResultSet res) throws SQLException {
		final int id = res.getInt(TABLE+"_id");
		MarkerEntry marker = CACHE.get(id);
		if (marker == null) {
			marker = new MarkerEntry(id,
					res.getInt("point1_x"),
					res.getInt("point1_z"),
					res.getInt("point2_x"),
					res.getInt("point2_z")
			);
			CACHE.put(id, marker);
			marker.setColor(res.getString("color"));
		}

		WhitelistEntry mcAccount = WhitelistEntry.parse(res);
		if (mcAccount != null) {
			marker.users.add(mcAccount);
		}
		return marker;
	}

	public void setColor(String hex) {
		this.setColor(parseColor(hex));
	}

	public void setColor(int color) {
		this.optionsBuilder
			.fillColor(color)
			.fill(true)
			.fillType(Fill.Type.EVENODD)
			.stroke(false)
			.tooltipSticky(true)
		;
	}

	private MarkerEntry build() {
		final int TOOLTIP_IMG_SIZE=21;
		final int POPUP_IMG_SIZE=69;

		StringBuilder tooltip = new StringBuilder("<div style='vertical-align: middle; text-align: center;'>");
		StringBuilder popup = new StringBuilder("<div style='vertical-align: middle; text-align:center;'>");
		for (WhitelistEntry user : this.users) {
			tooltip.append("<img src='/images/skins/2D/");
			tooltip.append(user.uuid);
			tooltip.append(".png' width='");
			tooltip.append(TOOLTIP_IMG_SIZE);
			tooltip.append("' heigth='");
			tooltip.append(TOOLTIP_IMG_SIZE);
			tooltip.append("' style='border-radius:3px;border:1px solid black;'/>");
			tooltip.append(user.getUser().getOnlineName());

			popup.append("<img src='/images/skins/3D/");
			popup.append(user.uuid);
			popup.append(".png' width='");
			popup.append(POPUP_IMG_SIZE);
			popup.append("' heigth='");
			popup.append(POPUP_IMG_SIZE);
			popup.append("'><br><h4>");
			popup.append(getUsername(user.uuid));
			popup.append("</h4>");
		}
		tooltip.append("</div>");
		popup.append("</div>");
		this.optionsBuilder
			.tooltipContent(tooltip.toString())
			.popupContent(popup.toString());

		super.setOptions(this.optionsBuilder.build());
		return this;
	}

	public static @Nullable MarkerEntry getByID(int id) {
		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement(GET_ALL_SQL+" WHERE "+TABLE+".id=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			statement.setInt(1, id);
			ResultSet res = statement.executeQuery();
			if (!res.first()) {
				// no marker found with id
				return null;
			}

			return parse(res).build();
		} catch (SQLException e) {
			throw new RuntimeException("Database getting marker entry by ID", e);
		}
	}

	public static @NotNull List<MarkerEntry> getAll() {
		try (PreparedStatement statement = RemoteWhitelistmanager.getSQL().prepareStatement(GET_ALL_SQL)) {
			ResultSet res = statement.executeQuery();
			List<MarkerEntry> entries = new ArrayList<>();
			while (res.next()) {
				entries.add(parse(res));
			}
			entries.forEach(MarkerEntry::build);
			return entries;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private static int parseColor(final @NotNull String hex) throws NumberFormatException {
		return Integer.parseInt(hex, 16);
	}

	@Override
	public @NonNull String toString() {
		return "Marker" + this.ID + ": " +
			"users=" + this.users +
			", marker=" + super.toString()
			;
	}

	public static String getUsername(UUID uuid) {
		String name = usernameCache.get(uuid);
		if (name != null) return name;

		Optional<String> newName = RemoteWhitelistmanager.SERVER_INSTANCE.services()
			.profileResolver()
			.fetchById(uuid)
			.map(GameProfile::name);
		newName.ifPresent(s -> usernameCache.put(uuid, s));
		return newName.orElse("???");
	}

	public static void clearCache() {
		CACHE.clear();
	}
}
