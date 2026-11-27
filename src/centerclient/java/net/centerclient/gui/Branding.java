package net.centerclient.gui;

import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

/**
 * The Center Client logo and brand colors.
 *
 * The logo is a pixel-art picture stored in
 * desktopRuntime/resources/assets/minecraft/textures/centerclient/logo.png:
 * a big 3D "CENTER" with a spaced-out "C L I E N T" underneath. On the title
 * screen a band of light sweeps across it every few seconds.
 *
 * The colors match the logo. Step 4 moves them into the theme system so the
 * player can change the accent color.
 */
public final class Branding {

	/** The logo picture. */
	public static final ResourceLocation LOGO = new ResourceLocation("textures/centerclient/logo.png");

	/** Size of the logo drawing inside the picture, in GUI pixels. */
	public static final int LOGO_WIDTH = 207;
	public static final int LOGO_HEIGHT = 75;

	/** Size of the whole picture file. */
	private static final float LOGO_TEXTURE_WIDTH = 256.0F;
	private static final float LOGO_TEXTURE_HEIGHT = 128.0F;

	/** The shine: how often it sweeps, how long a sweep takes, and how wide the band is. */
	private static final long SHINE_EVERY_MS = 5000L;
	private static final long SHINE_SWEEP_MS = 1000L;
	private static final int SHINE_WIDTH = 16;
	private static final int SHINE_STRIP_HEIGHT = 3;

	/** Brand colors, written as 0xAARRGGBB. */
	public static final int COLOR_BACKGROUND = 0xFF0D0F14;
	public static final int COLOR_PANEL = 0xFF161A22;
	public static final int COLOR_BORDER = 0xFF252B36;
	public static final int COLOR_TEXT = 0xFFE8ECF2;
	public static final int COLOR_FADED_TEXT = 0xFF8A93A3;
	public static final int COLOR_ACCENT = 0xFF33D1FF;
	public static final int COLOR_ACCENT_LIGHT = 0xFFB8F1FF;

	/** The red behind the loading screen, like modern Minecraft. */
	public static final int COLOR_LOADING_BACKGROUND = 0xFFEF323D;

	private Branding() {
	}

	/**
	 * Draws the logo with its top edge at y, centered on centerX, with the
	 * light sweep on top.
	 */
	public static void drawLogo(Minecraft mc, int centerX, int y) {
		int x = centerX - LOGO_WIDTH / 2;
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(LOGO);
		Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, LOGO_WIDTH, LOGO_HEIGHT, LOGO_TEXTURE_WIDTH,
				LOGO_TEXTURE_HEIGHT);
		drawShine(x, y);
		GlStateManager.disableBlend();
	}

	/**
	 * A slanted band of light that slides across the logo. It works by drawing
	 * thin strips of the logo again with "add light" blending, so only the logo's
	 * own pixels get brighter and the see-through parts stay see-through.
	 */
	private static void drawShine(int x, int y) {
		long time = Minecraft.getSystemTime() % SHINE_EVERY_MS;
		if (time >= SHINE_SWEEP_MS) {
			return;
		}
		float t = (float) time / (float) SHINE_SWEEP_MS;
		t = t * t * (3.0F - 2.0F * t); // ease in and out
		int lean = LOGO_HEIGHT / 2;
		int travel = LOGO_WIDTH + SHINE_WIDTH + lean;
		int bandLeft = (int) (t * travel) - SHINE_WIDTH - lean;

		GlStateManager.blendFunc(770, 1);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 0.65F);
		for (int row = 0; row < LOGO_HEIGHT; row += SHINE_STRIP_HEIGHT) {
			int height = Math.min(SHINE_STRIP_HEIGHT, LOGO_HEIGHT - row);
			int left = bandLeft + (LOGO_HEIGHT - row) / 2; // higher strips sit further right
			int right = left + SHINE_WIDTH;
			if (left < 0) {
				left = 0;
			}
			if (right > LOGO_WIDTH) {
				right = LOGO_WIDTH;
			}
			if (right > left) {
				Gui.drawModalRectWithCustomSizedTexture(x + left, y + row, (float) left, (float) row, right - left,
						height, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);
			}
		}
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
