package net.centerclient.module.hud;

import net.centerclient.config.BooleanSetting;
import net.centerclient.hud.TextHudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.minecraft.client.multiplayer.ServerData;

/** The server you're on and how many players are online. */
public class ServerInfoModule extends TextHudModule {

	private final BooleanSetting showPlayers = add(new BooleanSetting("Show player count", true));
	private final BooleanSetting showAddress = add(new BooleanSetting("Show address", false));

	private String shownName = null;
	private String shownAddress = "";
	private int shownPlayers = -1;
	private int shownMax = -1;
	private boolean shownWithPlayers;
	private boolean shownWithAddress;

	public ServerInfoModule() {
		super("Server Info", "Shows the server name and how many players are online", Category.HUD, END, START,
				0.995F, 0.07F);
	}

	@Override
	protected void update() {
		String name;
		String address;
		int players;
		int max;
		if (mc.world == null || mc.getConnection() == null) {
			name = "Youth Center SMP"; // example for the HUD editor
			address = "youthcentersmp.eagler.host";
			players = 12;
			max = 20;
		} else if (mc.isSingleplayer()) {
			name = "Singleplayer";
			address = "this browser";
			players = mc.getConnection().getPlayerInfoMap().size();
			max = -1;
		} else {
			ServerData data = mc.getCurrentServerData();
			name = data != null ? data.serverName : "Server";
			address = data != null ? data.serverIP : "";
			players = mc.getConnection().getPlayerInfoMap().size();
			max = mc.getConnection().currentServerMaxPlayers;
		}
		boolean withPlayers = showPlayers.isOn();
		boolean withAddress = showAddress.isOn();
		if (name.equals(shownName) && address.equals(shownAddress) && players == shownPlayers && max == shownMax
				&& withPlayers == shownWithPlayers && withAddress == shownWithAddress) {
			return;
		}
		shownName = name;
		shownAddress = address;
		shownPlayers = players;
		shownMax = max;
		shownWithPlayers = withPlayers;
		shownWithAddress = withAddress;

		setLineCount(1 + (withPlayers ? 1 : 0) + (withAddress ? 1 : 0));
		int i = 0;
		setLine(i++, "Server", name, Theme.TEXT);
		if (withPlayers) {
			setLine(i++, "Players", max > 0 ? players + " / " + max : Integer.toString(players), Theme.TEXT);
		}
		if (withAddress) {
			String shown = address.startsWith("wss://") ? address.substring(6)
					: (address.startsWith("ws://") ? address.substring(5) : address);
			setLine(i, "Address", shown, Theme.TEXT_FADED);
		}
	}
}
