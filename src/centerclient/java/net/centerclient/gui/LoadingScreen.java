package net.centerclient.gui;

import static net.lax1dude.eaglercraft.internal.PlatformOpenGL._wglBindFramebuffer;

import net.lax1dude.eaglercraft.Display;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

/**
 * The Center Client loading screen, in the style of modern Minecraft: a red
 * background, the white Center Client logo, and a thin white bar that fills up.
 * It replaces the white Mojang screen while the game starts.
 *
 * It draws the same 192x192 picture that the browser shows before the game
 * starts (textures/centerclient/loading.png, also baked into
 * EarlyLoadScreen.java), scaled to the window the same way. That way the
 * logo doesn't jump when the game takes over. Then it fills the bar on top.
 *
 * The game calls {@link #setProgress(Minecraft, float)} at a few points while
 * it loads. The bar slides to each new value over a few frames, so it moves
 * smoothly instead of jumping.
 */
public final class LoadingScreen {

	private static final ResourceLocation PICTURE = new ResourceLocation("textures/centerclient/loading.png");

	/** Size of the picture, and where the empty bar sits inside it (in picture pixels). */
	private static final float PICTURE_SIZE = 192.0F;
	private static final int BAR_X = 28;
	private static final int BAR_Y = 156;
	private static final int BAR_WIDTH = 136;
	private static final int BAR_HEIGHT = 7;

	/** How many frames the bar takes to slide to a new value. */
	private static final int SLIDE_FRAMES = 3;

	/** What the bar shows right now, from 0 to 1. */
	private static float shown = 0.0F;
	private static boolean finished = false;

	private LoadingScreen() {
	}

	/**
	 * Moves the bar to a new value (0 = just started, 1 = done) and draws it.
	 */
	public static void setProgress(Minecraft mc, float target) {
		if (finished) {
			return;
		}
		if (target > 1.0F) {
			target = 1.0F;
		}
		if (target < shown) {
			target = shown; // the bar never goes backwards
		}
		float start = shown;
		for (int frame = 1; frame <= SLIDE_FRAMES; ++frame) {
			float t = (float) frame / (float) SLIDE_FRAMES;
			float eased = 1.0F - (1.0F - t) * (1.0F - t); // ease-out: fast at first, then gentle
			shown = start + (target - start) * eased;
			draw(mc, shown);
		}
	}

	/** Fills the bar and stops the loading screen. */
	public static void finish(Minecraft mc) {
		setProgress(mc, 1.0F);
		finished = true;
	}

	private static void draw(Minecraft mc, float progress) {
		int width = mc.displayWidth;
		int height = mc.displayHeight;

		// Draw straight to the screen in real pixels.
		_wglBindFramebuffer(0x8D40, null);
		GlStateManager.viewport(0, 0, width, height);
		GlStateManager.matrixMode(5889);
		GlStateManager.loadIdentity();
		GlStateManager.ortho(0.0D, (double) width, (double) height, 0.0D, 1000.0D, 3000.0D);
		GlStateManager.matrixMode(5888);
		GlStateManager.loadIdentity();
		GlStateManager.translate(0.0F, 0.0F, -2000.0F);
		GlStateManager.disableLighting();
		GlStateManager.disableFog();
		GlStateManager.disableDepth();
		GlStateManager.enableTexture2D();

		// Red background
		Gui.drawRect(0, 0, width, height, Branding.COLOR_LOADING_BACKGROUND);

		// The picture: a square as big as the window's short side, in the middle
		float size = (float) Math.min(width, height);
		float x = (width - size) * 0.5F;
		float y = (height - size) * 0.5F;
		float px = size / PICTURE_SIZE; // screen pixels per picture pixel

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(PICTURE);
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer buffer = tessellator.getBuffer();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
		buffer.pos((double) x, (double) (y + size), 0.0D).tex(0.0D, 1.0D).endVertex();
		buffer.pos((double) (x + size), (double) (y + size), 0.0D).tex(1.0D, 1.0D).endVertex();
		buffer.pos((double) (x + size), (double) y, 0.0D).tex(1.0D, 0.0D).endVertex();
		buffer.pos((double) x, (double) y, 0.0D).tex(0.0D, 0.0D).endVertex();
		tessellator.draw();

		// White fill inside the bar, leaving a gap inside the outline
		int fillLeft = Math.round(x + (BAR_X + 2) * px);
		int fillTop = Math.round(y + (BAR_Y + 2) * px);
		int fillRightFull = Math.round(x + (BAR_X + BAR_WIDTH - 2) * px);
		int fillBottom = Math.round(y + (BAR_Y + BAR_HEIGHT - 2) * px);
		int fillRight = fillLeft + (int) ((fillRightFull - fillLeft) * progress);
		if (fillRight > fillLeft) {
			Gui.drawRect(fillLeft, fillTop, fillRight, fillBottom, 0xFFFFFFFF);
		}

		GlStateManager.enableDepth();
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);
		Display.update();
	}
}
