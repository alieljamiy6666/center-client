package net.centerclient.gui;

import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

/**
 * The Center Client logo and brand colors.
 *
 * The logo is a pixel-art picture stored in
 * desktopRuntime/resources/assets/minecraft/textures/centerclient/logo.png.
 * The colors match the logo. Step 4 moves them into the theme system so the
 * player can change the accent color.
 */
public final class Branding {

	/** The logo picture. */
	public static final ResourceLocation LOGO = new ResourceLocation("textures/centerclient/logo.png");

	/** Size of the logo drawing inside the picture, in GUI pixels. */
	public static final int LOGO_WIDTH = 274;
	public static final int LOGO_HEIGHT = 52;

	/** Size of the whole picture file. */
	private static final float LOGO_TEXTURE_WIDTH = 512.0F;
	private static final float LOGO_TEXTURE_HEIGHT = 64.0F;

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
	 * Draws the logo with its top edge at y, centered on centerX.
	 */
	public static void drawLogo(Minecraft mc, int centerX, int y) {
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(LOGO);
		Gui.drawModalRectWithCustomSizedTexture(centerX - LOGO_WIDTH / 2, y, 0.0F, 0.0F, LOGO_WIDTH, LOGO_HEIGHT,
				LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);
		GlStateManager.disableBlend();
	}
}
