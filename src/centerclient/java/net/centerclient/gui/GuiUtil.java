package net.centerclient.gui;

import net.centerclient.theme.Theme;
import net.centerclient.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;

/** Small drawing pieces shared by Center Client screens. */
public final class GuiUtil {

	private GuiUtil() {
	}

	/** A rounded panel with a soft shadow, a title and a thin line under the title. */
	public static void drawPanel(FontRenderer font, int x, int y, int width, int height, String title) {
		RenderUtil.shadow(x, y + 2, width, height, 6, 4, 0x80000000);
		RenderUtil.roundedRect(x, y, width, height, 6, Theme.PANEL);
		RenderUtil.roundedOutline(x, y, width, height, 6, 1, Theme.BORDER);
		if (title != null) {
			font.drawString(title, x + 6, y + 5, Theme.TEXT_FADED, false);
			RenderUtil.rect(x + 6, y + 15, width - 12, 1, Theme.BORDER);
		}
	}

	/** A dark background with a gentle fade, for full-screen Center Client menus. */
	public static void drawScreenBackground(int width, int height) {
		RenderUtil.gradientVertical(0, 0, width, height, Theme.BACKGROUND,
				net.centerclient.util.ColorUtil.mix(Theme.BACKGROUND, Theme.PANEL, 0.7F));
	}
}
