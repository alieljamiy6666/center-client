package net.centerclient.event;

import net.minecraft.client.multiplayer.WorldClient;

/**
 * Sent when a world finishes loading, and also when you leave one
 * (then the world is null).
 */
public final class WorldLoadEvent extends Event {

	private WorldClient world;

	WorldLoadEvent set(WorldClient world) {
		this.world = world;
		return this;
	}

	/** The new world, or null if you just left a world. */
	public WorldClient getWorld() {
		return world;
	}

	public boolean isUnload() {
		return world == null;
	}
}
