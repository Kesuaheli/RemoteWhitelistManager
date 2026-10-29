package de.kesuaheli.remotewhitelistmanager;

import de.kesuaheli.remotewhitelistmanager.command.About;
import de.kesuaheli.remotewhitelistmanager.command.RemoteWhitelist;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class RemoteWhitelistmanager implements ModInitializer {
	public static Logger LOGGER = LoggerFactory.getLogger("RemoteWhitelistmanager");

	private static Connection postgresConn;

	public static Config config;

	@Override
	public void onInitialize() {
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

	public static boolean connectSQL() {
		try {
			postgresConn = DriverManager.getConnection(config.SQLConnectionString(), config.SQLCredentials());
		} catch (SQLException e) {
			LOGGER.error("Failed to connect to DB", e);
			return false;
		}
		LOGGER.info("Connected to database");
		return true;
	}
}
