package de.kesuaheli.remotewhitelistmanager;

import de.kesuaheli.remotewhitelistmanager.command.About;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteWhitelistmanager implements ModInitializer {
	public static Logger LOGGER = LoggerFactory.getLogger("RemoteWhitelistmanager");

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
			commandDispatcher.register(new About());
		});
		LOGGER.info("Remote Whitelist Manager loaded!");
	}
}
