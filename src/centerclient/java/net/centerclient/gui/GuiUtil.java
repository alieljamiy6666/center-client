package net.centerclient.gui;

import net.centerclient.theme.Theme;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.RenderUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;
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
			text(font, title, x + 6, y + 5, Theme.TEXT_FADED, false);
			RenderUtil.rect(x + 6, y + 15, width - 12, 1, Theme.BORDER);
		}
	}

	/** A dark background with a gentle fade, for full-screen Center Client menus. */
	public static void drawScreenBackground(int width, int height) {
		RenderUtil.gradientVertical(0, 0, width, height, Theme.BACKGROUND,
				ColorUtil.mix(Theme.BACKGROUND, Theme.PANEL, 0.7F));
	}

	/**
	 * Draws text that fades along with {@link RenderUtil#setGlobalAlpha}. Text
	 * that's almost invisible is skipped (the game would draw it fully solid).
	 */
	public static void text(FontRenderer font, String text, float x, float y, int color, boolean shadow) {
		int alpha = (int) (ColorUtil.alpha(color) * RenderUtil.getGlobalAlpha());
		if (alpha < 10) {
			return;
		}
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		font.drawString(text, x, y, (alpha << 24) | (color & 0xFFFFFF), shadow);
	}

	/** Same as {@link #text}, centered on centerX. */
	public static void textCentered(FontRenderer font, String text, float centerX, float y, int color,
			boolean shadow) {
		text(font, text, centerX - font.getStringWidth(text) / 2.0F, y, color, shadow);
	}

	/** An on/off switch, 20x10. progress goes from 0 (off) to 1 (on) and can be animated. */
	public static void drawSwitch(float x, float y, float progress) {
		float p = progress < 0.0F ? 0.0F : (progress > 1.2F ? 1.2F : progress);
		RenderUtil.roundedRect(x, y, 20, 10, 5, ColorUtil.mix(Theme.BORDER, Theme.accent(), Math.min(1.0F, p)));
		RenderUtil.circle(x + 5 + p * 10, y + 5, 4, 0xFFFFFFFF);
	}

	/** A checkerboard, used behind see-through colors so you can see how see-through they are. */
	public static void drawChecker(float x, float y, float width, float height, int cell) {
		WorldRenderer batch = RenderUtil.beginBatch();
		for (int cy = 0; cy * cell < height; ++cy) {
			for (int cx = 0; cx * cell < width; ++cx) {
				float w = Math.min(cell, width - cx * cell);
				float h = Math.min(cell, height - cy * cell);
				int color = ((cx + cy) & 1) == 0 ? 0xFFC8C8C8 : 0xFF8A8A8A;
				RenderUtil.batchRect(batch, x + cx * cell, y + cy * cell, w, h, color);
			}
		}
		RenderUtil.endBatch();
	}
}
