package net.centerclient.module.hud;

import net.centerclient.config.BooleanSetting;
import net.centerclient.hud.TextHudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.minecraft.client.network.NetworkPlayerInfo;

/** Your connection delay to the server, green (good), yellow (okay) or red (laggy). */
public class PingModule extends TextHudModule {

	private static final int PREVIEW_PING = 42;

	private final BooleanSetting colorByPing = add(new BooleanSetting("Color by ping", true));

	private int shownPing = Integer.MIN_VALUE;
	private boolean shownColored;

	public PingModule() {
		super("Ping", "Shows your connection delay to the server", Category.HUD, START, START, 0.005F, 0.13F);
	}

	@Override
	protected void update() {
		int ping;
		if (mc.player == null || mc.getConnection() == null) {
			ping = PREVIEW_PING; // no server: show an example in the HUD editor
		} else if (mc.isSingleplayer()) {
			ping = 0;
		} else {
			NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
			ping = info != null ? info.getResponseTime() : -1;
		}
		if (ping == shownPing && colorByPing.isOn() == shownColored) {
			return;
		}
		shownPing = ping;
		shownColored = colorByPing.isOn();
		String value = ping < 0 ? "?" : ping + " ms";
		int color = Theme.TEXT;
		if (shownColored && ping >= 0) {
			color = ping < 80 ? Theme.SUCCESS : (ping < 150 ? Theme.WARNING : Theme.DANGER);
		}
		setLine(0, "Ping", value, color);
	}
}
