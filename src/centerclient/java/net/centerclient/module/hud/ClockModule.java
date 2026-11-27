package net.centerclient.module.hud;

import java.util.Calendar;

import net.centerclient.config.BooleanSetting;
import net.centerclient.config.ModeSetting;
import net.centerclient.hud.TextHudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.centerclient.util.TextUtil;

/** The real-world time, in 12 or 24 hour format. */
public class ClockModule extends TextHudModule {

	private final ModeSetting format = add(new ModeSetting("Format", "12 hour", "12 hour", "24 hour"));
	private final BooleanSetting showSeconds = add(new BooleanSetting("Show seconds", false));

	private long shownSecond = -1L;
	private String shownFormat = "";
	private boolean shownWithSeconds;

	public ClockModule() {
		super("Clock", "Shows the real-world time", Category.HUD, END, START, 0.995F, 0.01F);
	}

	@Override
	protected void update() {
		long second = System.currentTimeMillis() / 1000L;
		if (second == shownSecond && format.get().equals(shownFormat) && showSeconds.isOn() == shownWithSeconds) {
			return; // only redo the text once a second
		}
		shownSecond = second;
		shownFormat = format.get();
		shownWithSeconds = showSeconds.isOn();

		Calendar now = Calendar.getInstance();
		int hour = now.get(Calendar.HOUR_OF_DAY);
		String minutes = TextUtil.twoDigits(now.get(Calendar.MINUTE));
		String seconds = shownWithSeconds ? ":" + TextUtil.twoDigits(now.get(Calendar.SECOND)) : "";
		String text;
		if (shownFormat.equals("24 hour")) {
			text = TextUtil.twoDigits(hour) + ":" + minutes + seconds;
		} else {
			int h12 = hour % 12 == 0 ? 12 : hour % 12;
			text = h12 + ":" + minutes + seconds + (hour < 12 ? " AM" : " PM");
		}
		setLine(0, "Time", text, Theme.TEXT);
	}
}
