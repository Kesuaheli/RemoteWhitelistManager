package de.kesuaheli.remotewhitelistmanager.integration;

import net.fabricmc.loader.api.FabricLoader;

public abstract class Integration {
	private final String modID;
	private boolean fetched = false;
	private boolean present = false;

	Integration(final String modID) {
		this.modID = modID;
	}

	private boolean fetch() {
		this.present = FabricLoader.getInstance().isModLoaded(this.modID);
		this.fetched = true;
		return this.present;
	}

	public boolean isPresent() {
		if (this.fetched) return this.present;
		return this.fetch();
	}
}
