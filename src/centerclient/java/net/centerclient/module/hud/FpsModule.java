package net.centerclient.module.hud;

import net.centerclient.config.BooleanSetting;
import net.centerclient.hud.TextHudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.minecraft.client.Minecraft;

/** Frames per second, with an optional lowest and highest from the last 10 seconds. */
public class FpsModule extends TextHudModule {

	private static final int HISTORY_SECONDS = 10;

	private final BooleanSetting showLowHigh = add(new BooleanSetting("Show low and high", false));

	private final int[] history = new int[HISTORY_SECONDS];
	private int historyCount;
	private int historyIndex;
	private long lastSample;
	private int shownFps = -1;
	private boolean shownLowHigh;

	public FpsModule() {
		super("FPS", "Shows your frames per second", Category.HUD, START, START, 0.005F, 0.07F);
	}

	@Override
	protected void update() {
		int fps = Minecraft.getDebugFPS();
		long now = Minecraft.getSystemTime();
		if (now - lastSample >= 1000L) { // the game measures FPS once a second
			lastSample = now;
			history[historyIndex] = fps;
			historyIndex = (historyIndex + 1) % HISTORY_SECONDS;
			if (historyCount < HISTORY_SECONDS) {
				++historyCount;
			}
		} else if (fps == shownFps && showLowHigh.isOn() == shownLowHigh) {
			return; // nothing changed, keep the old text
		}
		shownFps = fps;
		shownLowHigh = showLowHigh.isOn();
		String value = Integer.toString(fps);
		if (shownLowHigh && historyCount > 0) {
			int low = Integer.MAX_VALUE;
			int high = 0;
			for (int i = 0; i < historyCount; ++i) {
				low = Math.min(low, history[i]);
				high = Math.max(high, history[i]);
			}
			value += "  L " + low + "  H " + high;
		}
		setLine(0, "FPS", value, Theme.TEXT);
	}
}
