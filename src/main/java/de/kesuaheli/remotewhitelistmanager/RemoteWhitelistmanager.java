package de.kesuaheli.remotewhitelistmanager;

import de.kesuaheli.remotewhitelistmanager.command.About;
import de.kesuaheli.remotewhitelistmanager.command.RemoteWhitelist;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class RemoteWhitelistmanager implements ModInitializer {
	public static Logger LOGGER = LoggerFactory.getLogger("RemoteWhitelistmanager");
	public static MinecraftServer SERVER_INSTANCE;

	private static Connection postgresConn;

	public static Config config;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER_INSTANCE = server);
		CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
			commandDispatcher.register(new About());
			commandDispatcher.register(new RemoteWhitelist());
		});
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			LOGGER.error("PostgreSQL backend disabled — install minecraft-postgresql-jdbc");
			return;
		}
		try {
			config = Config.initConfig();
		} catch (IOException e) {
			LOGGER.error("Failed to init config", e);
			return;
		}
		connectSQL();

		LOGGER.info("Remote Whitelist Manager loaded!");
	}

	public static Connection getSQL() {
		return postgresConn;
	}

	public static SQLConnectionState connectSQL() {
		try {
			if (postgresConn != null && !postgresConn.isClosed()) {
				return SQLConnectionState.ALREADY_CONNECTED;
			}
		} catch (SQLException ignore) {
		}

		try {
			postgresConn = DriverManager.getConnection(config.SQLConnectionString(), config.SQLCredentials());
		} catch (SQLException e) {
			LOGGER.error(SQLConnectionState.FAILED.message, e);
			return SQLConnectionState.FAILED;
		}
		return SQLConnectionState.SUCCESSFULLY_CONNECTED;
	}

	public enum SQLConnectionState {
		ALREADY_CONNECTED("Is already connected"),
		SUCCESSFULLY_CONNECTED("Connected to Database"),
		FAILED("Failed to connect to the database", TextColor.RED);

		public final String message;
		public final TextColor color;

		SQLConnectionState(final String message) {
			this(message, TextColor.GREEN);
		}

		SQLConnectionState(final String message, final TextColor color) {
			this.message = message;
			this.color = color;
		}

		public Component getMessage() {
			return Component.literal(this.message).withColor(this.color);
		}

		public boolean ok() {
			return this.color.equals(TextColor.GREEN);
		}
	}
}
