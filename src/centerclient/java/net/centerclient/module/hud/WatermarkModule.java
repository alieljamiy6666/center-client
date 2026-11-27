package net.centerclient.module.hud;

import net.centerclient.CenterClient;
import net.centerclient.config.BooleanSetting;
import net.centerclient.hud.HudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;

/** Shows "Center Client v1.0.0" on the HUD, in the accent color. */
public class WatermarkModule extends HudModule {

	private static final String NAME_TEXT = CenterClient.NAME;
	private static final String VERSION_TEXT = " v" + CenterClient.VERSION;

	private final BooleanSetting showVersion = add(new BooleanSetting("Show version", true));

	public WatermarkModule() {
		super("Watermark", "Shows the Center Client name and version on your screen", Category.HUD, START, START,
				0.005F, 0.01F);
	}

	@Override
	public int getWidth() {
		int w = mc.fontRendererObj.getStringWidth(NAME_TEXT);
		if (showVersion.isOn()) {
			w += mc.fontRendererObj.getStringWidth(VERSION_TEXT);
		}
		return w + 12;
	}

	@Override
	public int getHeight() {
		return 15;
	}

	@Override
	protected void draw(boolean editing) {
		mc.fontRendererObj.drawString(NAME_TEXT, 6, 4, Theme.accent(), true);
		if (showVersion.isOn()) {
			mc.fontRendererObj.drawString(VERSION_TEXT, 6 + mc.fontRendererObj.getStringWidth(NAME_TEXT), 4,
					Theme.TEXT_FADED, true);
		}
	}
}
