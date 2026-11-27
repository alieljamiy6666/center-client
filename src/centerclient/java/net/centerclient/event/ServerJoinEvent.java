package net.centerclient.event;

import net.minecraft.client.multiplayer.ServerData;

/**
 * Sent when you join a server (or a singleplayer world, which runs a small
 * server in the background), right after the server says "welcome".
 */
public final class ServerJoinEvent extends Event {

	private ServerData server;
	private boolean singleplayer;

	ServerJoinEvent set(ServerData server, boolean singleplayer) {
		this.server = server;
		this.singleplayer = singleplayer;
		return this;
	}

	/** The server you joined (name and address), or null in singleplayer. */
	public ServerData getServer() {
		return server;
	}

	public boolean isSingleplayer() {
		return singleplayer;
	}
}
