package net.centerclient.gui;

import net.centerclient.theme.Theme;
import net.centerclient.util.ColorUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

/**
 * The Center Client logo.
 *
 * The logo picture (desktopRuntime/resources/assets/minecraft/textures/centerclient/logo.png)
 * holds two layers so it can take on the player's accent color:
 * <ul>
 * <li>rows 0-127: a gray "base" layer, drawn tinted with the accent color</li>
 * <li>rows 128-255: a white "shine" layer, drawn on top as-is for the light
 * edges and the "C L I E N T" letters</li>
 * </ul>
 * On the title screen a band of light also sweeps across it every few seconds.
 */
public final class Branding {

	/** The logo picture. */
	public static final ResourceLocation LOGO = new ResourceLocation("textures/centerclient/logo.png");

	/** Size of the logo drawing, in GUI pixels. */
	public static final int LOGO_WIDTH = 207;
	public static final int LOGO_HEIGHT = 75;

	/** Size of the whole picture file, and where the shine layer starts. */
	private static final float TEXTURE_SIZE = 256.0F;
	private static final float SHINE_LAYER_V = 128.0F;

	/** The light sweep: how often, how long, how wide. */
	private static final long SWEEP_EVERY_MS = 5000L;
	private static final long SWEEP_MS = 1000L;
	private static final int SWEEP_WIDTH = 16;
	private static final int SWEEP_STRIP_HEIGHT = 3;

	/** The red behind the loading screen, like modern Minecraft. */
	public static final int COLOR_LOADING_BACKGROUND = 0xFFEF323D;

	private Branding() {
	}

	/**
	 * Draws the logo in the current accent color, with its top edge at y,
	 * centered on centerX.
	 */
	public static void drawLogo(Minecraft mc, int centerX, int y) {
		int x = centerX - LOGO_WIDTH / 2;
		int accent = Theme.accent();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		mc.getTextureManager().bindTexture(LOGO);

		// 1. base layer, tinted with the accent color
		GlStateManager.color(ColorUtil.red(accent) / 255.0F, ColorUtil.green(accent) / 255.0F,
				ColorUtil.blue(accent) / 255.0F, 1.0F);
		Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, LOGO_WIDTH, LOGO_HEIGHT, TEXTURE_SIZE,
				TEXTURE_SIZE);

		// 2. shine layer on top, untinted
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, SHINE_LAYER_V, LOGO_WIDTH, LOGO_HEIGHT, TEXTURE_SIZE,
				TEXTURE_SIZE);

		// 3. the light sweep
		drawSweep(x, y);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.disableBlend();
	}

	/**
	 * A slanted band of light that slides across the logo. It draws thin strips
	 * of the gray base layer again with "add light" blending, so only the logo's
	 * own pixels get brighter and the see-through parts stay see-through.
	 */
	private static void drawSweep(int x, int y) {
		long time = Minecraft.getSystemTime() % SWEEP_EVERY_MS;
		if (time >= SWEEP_MS) {
			return;
		}
		float t = (float) time / (float) SWEEP_MS;
		t = t * t * (3.0F - 2.0F * t); // ease in and out
		int lean = LOGO_HEIGHT / 2;
		int travel = LOGO_WIDTH + SWEEP_WIDTH + lean;
		int bandLeft = (int) (t * travel) - SWEEP_WIDTH - lean;

		GlStateManager.blendFunc(770, 1);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 0.55F);
		for (int row = 0; row < LOGO_HEIGHT; row += SWEEP_STRIP_HEIGHT) {
			int height = Math.min(SWEEP_STRIP_HEIGHT, LOGO_HEIGHT - row);
			int left = bandLeft + (LOGO_HEIGHT - row) / 2; // higher strips sit further right
			int right = left + SWEEP_WIDTH;
			if (left < 0) {
				left = 0;
			}
			if (right > LOGO_WIDTH) {
				right = LOGO_WIDTH;
			}
			if (right > left) {
				Gui.drawModalRectWithCustomSizedTexture(x + left, y + row, (float) left, (float) row, right - left,
						height, TEXTURE_SIZE, TEXTURE_SIZE);
			}
		}
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
	}
}
