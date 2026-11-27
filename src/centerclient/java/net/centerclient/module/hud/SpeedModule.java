package net.centerclient.module.hud;

import net.centerclient.config.BooleanSetting;
import net.centerclient.hud.TextHudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.centerclient.util.TextUtil;

/**
 * How fast you're moving, in blocks per second. It's averaged over the last
 * quarter of a second so the number doesn't flicker.
 */
public class SpeedModule extends TextHudModule {

	private static final int AVERAGE_TICKS = 5;

	private final BooleanSetting includeVertical = add(new BooleanSetting("Count up and down", false));

	private final double[] samples = new double[AVERAGE_TICKS];
	private int sampleIndex;
	private int shownHundredths = -1;

	public SpeedModule() {
		super("Speed", "Shows how fast you're moving in blocks per second", Category.HUD, START, START, 0.005F, 0.19F);
	}

	@Override
	protected void update() {
		double speed;
		if (mc.player == null) {
			speed = 4.32; // example for the HUD editor
		} else {
			double dx = mc.player.posX - mc.player.prevPosX;
			double dy = includeVertical.isOn() ? mc.player.posY - mc.player.prevPosY : 0.0;
			double dz = mc.player.posZ - mc.player.prevPosZ;
			samples[sampleIndex] = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0; // 20 ticks per second
			sampleIndex = (sampleIndex + 1) % AVERAGE_TICKS;
			double total = 0.0;
			for (int i = 0; i < AVERAGE_TICKS; ++i) {
				total += samples[i];
			}
			speed = total / AVERAGE_TICKS;
		}
		int hundredths = (int) Math.round(speed * 100.0);
		if (hundredths == shownHundredths) {
			return;
		}
		shownHundredths = hundredths;
		setLine(0, "Speed", TextUtil.formatDecimal(hundredths / 100.0, 2) + " b/s", Theme.TEXT);
	}
}
