package de.kesuaheli.remotewhitelistmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager;
import de.kesuaheli.remotewhitelistmanager.database.MarkerEntry;
import de.kesuaheli.remotewhitelistmanager.database.WhitelistEntry;
import de.kesuaheli.remotewhitelistmanager.integration.Pl3xmapIntegration;
import de.kesuaheli.remotewhitelistmanager.pl3xmap.Pl3xmapMarker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.players.*;
import net.pl3x.map.core.markers.layer.SimpleLayer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager.LOGGER;

public class RemoteWhitelist extends LiteralArgumentBuilder<CommandSourceStack> {

	public RemoteWhitelist() {
		super("remotewhitelist");

		then(Commands.literal("update")
				.requires(Commands.hasPermission(Commands.LEVEL_MODERATORS))
				.executes(this::executeUpdate)
		);
		then(Commands.literal("connect")
				.requires(Commands.hasPermission(Commands.LEVEL_MODERATORS))
				.executes(this::connectDB)
		);
		then(Commands.literal("reconnect")
				.requires(Commands.hasPermission(Commands.LEVEL_MODERATORS))
				.executes(this::reconnectDB)
		);
		if (Pl3xmapIntegration.INSTANCE.isPresent()) {
			LOGGER.info("plexmap is present, registering command");
			then(Commands.literal("marker")
					.requires(Commands.hasPermission(Commands.LEVEL_MODERATORS))
					.then(Commands.literal("update")
							.executes(this::markerUpdate)
					)
			);
		} else {
			LOGGER.info("plexmap is not present, not registering command");
		}
	}

	private int connectDB(CommandContext<CommandSourceStack> ctx) {
		Connection sql = RemoteWhitelistmanager.getSQL();
		try {
			if (!sql.isClosed()) {
				ctx.getSource().sendSystemMessage(Component.literal("Is already connected").withColor(TextColor.GREEN));
				return 0;
			}
		} catch (SQLException ignore) {
		}

		if (RemoteWhitelistmanager.connectSQL()) {
			ctx.getSource().sendSystemMessage(Component.literal("Connected to Database").withColor(TextColor.GREEN));
		} else {
			ctx.getSource().sendSystemMessage(Component.literal("Failed to connect to the database").withColor(TextColor.RED));
		}
		return 0;
	}

	private int reconnectDB(CommandContext<CommandSourceStack> ctx) {
		Connection sql = RemoteWhitelistmanager.getSQL();
		try {
			if (!sql.isClosed()) {
				sql.close();
			}
		} catch (SQLException ignore) {
		}

		return this.connectDB(ctx);
	}

	private int executeUpdate(CommandContext<CommandSourceStack> ctx) {
		LOGGER.info("Updating Whitelist from remote...");
		List<WhitelistEntry> entries = WhitelistEntry.getAll();

		int removedOPs = 0, removedUsers = 0, addedOPs = 0, addedUsers = 0;

		// remove non-admins
		PlayerList playerList = ctx.getSource().getServer().getPlayerList();
		ServerOpList opList = playerList.getOps();
		for (ServerOpListEntry opEntry : opList.getEntries()) {
			assert opEntry.getUser() != null;
			UUID uuid = opEntry.getUser().id();
			Optional<WhitelistEntry> e = entries.stream().filter(whitelistEntry->whitelistEntry.uuid.equals(uuid)).findFirst();
			if (e.isEmpty() || !e.get().isAdmin()) {
				playerList.deop(opEntry.getUser());
				removedOPs++;
			}
		}

		// remove non-active users
		UserWhiteList whiteList = playerList.getWhiteList();
		for (UserWhiteListEntry whiteListEntryEntry : whiteList.getEntries()) {
			assert whiteListEntryEntry.getUser() != null;
			UUID uuid = whiteListEntryEntry.getUser().id();
			Optional<WhitelistEntry> e = entries.stream().filter(whitelistEntry->whitelistEntry.uuid.equals(uuid)).findFirst();
			if (e.isEmpty()) {
				whiteList.remove(whiteListEntryEntry.getUser());
				removedUsers++;
			}
		}

		// add new users and admins
		for (WhitelistEntry entry : entries) {
			NameAndId nameAndId = new NameAndId(entry.uuid, entry.nicknames[0]);
			if (!whiteList.isWhiteListed(nameAndId)) {
				whiteList.add(new UserWhiteListEntry(nameAndId));
				addedUsers++;
			}
			if (entry.isAdmin() && !playerList.isOp(nameAndId)) {
				playerList.op(nameAndId);
				addedOPs++;
			}
		}

		if (removedOPs == 0 && removedUsers == 0 && addedOPs == 0 && addedUsers == 0) {
			ctx.getSource().sendSystemMessage(Component.literal("Whitelist is already up to date!").withColor(TextColor.GREEN));
		} else if (removedOPs == 0 && removedUsers == 0 && addedOPs == 0) {
			ctx.getSource().sendSystemMessage(
					Component.literal("Added ")
							.append(Component.literal(String.valueOf(addedUsers)).withColor(TextColor.GREEN))
							.append(" new user(s).")
			);
		} else if (removedOPs == 0 && addedOPs == 0 && addedUsers == 0) {
			ctx.getSource().sendSystemMessage(
					Component.literal("Removed ")
							.append(Component.literal(String.valueOf(removedUsers)).withColor(TextColor.GREEN))
							.append(" user(s).")
			);
		} else if (removedOPs == 0 && removedUsers == 0 && addedUsers == 0) {
			ctx.getSource().sendSystemMessage(
					Component.literal("Added ")
							.append(Component.literal(String.valueOf(addedOPs)).withColor(TextColor.GREEN))
							.append(" new op(s).")
			);
		} else if (removedUsers == 0 && addedOPs == 0 && addedUsers == 0) {
			ctx.getSource().sendSystemMessage(
					Component.literal("Removed ")
							.append(Component.literal(String.valueOf(removedOPs)).withColor(TextColor.GREEN))
							.append(" op(s).")
			);
		} else {
			ctx.getSource().sendSystemMessage(
					Component.literal("Updated whitelist with ")
							.append(String.valueOf(removedOPs + removedUsers + addedOPs + addedUsers)).withColor(TextColor.GOLD)
							.append(" changes in total.")
			);
		}

		return 0;
	}

	public int markerUpdate(CommandContext<CommandSourceStack> ctx) {
		SimpleLayer bases = Pl3xmapMarker.getBases();
		bases.clearMarkers();

		List<MarkerEntry> markers = MarkerEntry.getAll();
		markers.forEach(bases::addMarker);

		ctx.getSource().sendSystemMessage(Component.literal("Bases: "+bases.getMarkers().size()).withColor(TextColor.GREEN));

		return 0;
	}
}
