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
 * background, a big white "CENTER" with "CLIENT" underneath, and a thin white
 * bar that fills up. When loading is done it fades away into the game.
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
	private static final int BAR_X = 14;
	private static final int BAR_Y = 156;
	private static final int BAR_WIDTH = 164;
	private static final int BAR_HEIGHT = 7;

	/** How many frames the bar takes to slide to a new value. */
	private static final int SLIDE_FRAMES = 3;

	/** How long the fade into the game takes. */
	private static final long FADE_MS = 700L;

	/** What the bar shows right now, from 0 to 1. */
	private static float shown = 0.0F;
	private static boolean finished = false;
	private static boolean fadeDone = false;
	private static long fadeStart = -1L;

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
			mc.updateDisplayMode(); // read the real window size (the game hasn't done it yet this early)
			beginDrawing(mc);
			draw(mc, shown, 1.0F);
			endDrawing();
			Display.update();
		}
	}

	/** Fills the bar. From the next frame on, the screen fades away. */
	public static void finish(Minecraft mc) {
		setProgress(mc, 1.0F);
		finished = true;
	}

	/**
	 * Called at the end of every frame. For a moment after loading, draws the
	 * loading screen on top of the game, getting more see-through each frame.
	 * After that it does nothing.
	 */
	public static void drawFadeOut(Minecraft mc) {
		if (!finished || fadeDone) {
			return;
		}
		long now = Minecraft.getSystemTime();
		if (fadeStart < 0L) {
			fadeStart = now; // start timing on the first real frame
		}
		float t = (float) (now - fadeStart) / (float) FADE_MS;
		if (t >= 1.0F) {
			fadeDone = true;
			mc.getTextureManager().deleteTexture(PICTURE); // free the memory, it's not needed anymore
			return;
		}
		float alpha = 1.0F - t * t * (3.0F - 2.0F * t); // smooth fade
		beginDrawing(mc);
		GlStateManager.disableAlpha();
		draw(mc, 1.0F, alpha);
		endDrawing();
	}

	/** Sets up flat 2D drawing in real screen pixels. */
	private static void beginDrawing(Minecraft mc) {
		_wglBindFramebuffer(0x8D40, null);
		GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
		GlStateManager.matrixMode(5889);
		GlStateManager.loadIdentity();
		GlStateManager.ortho(0.0D, (double) mc.displayWidth, (double) mc.displayHeight, 0.0D, 1000.0D, 3000.0D);
		GlStateManager.matrixMode(5888);
		GlStateManager.loadIdentity();
		GlStateManager.translate(0.0F, 0.0F, -2000.0F);
		GlStateManager.disableLighting();
		GlStateManager.disableFog();
		GlStateManager.disableDepth();
		GlStateManager.enableTexture2D();
	}

	/** Puts back the settings the game expects. */
	private static void endDrawing() {
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableDepth();
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);
	}

	private static void draw(Minecraft mc, float progress, float alpha) {
		int width = mc.displayWidth;
		int height = mc.displayHeight;
		int a = (int) (alpha * 255.0F) << 24;

		// Red background
		Gui.drawRect(0, 0, width, height, a | (Branding.COLOR_LOADING_BACKGROUND & 0xFFFFFF));

		// The picture: a square as big as the window's short side, in the middle
		float size = (float) Math.min(width, height);
		float x = (width - size) * 0.5F;
		float y = (height - size) * 0.5F;
		float px = size / PICTURE_SIZE; // screen pixels per picture pixel

		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
		mc.getTextureManager().bindTexture(PICTURE);
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer buffer = tessellator.getBuffer();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
		buffer.pos((double) x, (double) (y + size), 0.0D).tex(0.0D, 1.0D).endVertex();
		buffer.pos((double) (x + size), (double) (y + size), 0.0D).tex(1.0D, 1.0D).endVertex();
		buffer.pos((double) (x + size), (double) y, 0.0D).tex(1.0D, 0.0D).endVertex();
		buffer.pos((double) x, (double) y, 0.0D).tex(0.0D, 0.0D).endVertex();
		tessellator.draw();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		// White fill inside the bar, leaving a gap inside the outline
		int fillLeft = Math.round(x + (BAR_X + 2) * px);
		int fillTop = Math.round(y + (BAR_Y + 2) * px);
		int fillRightFull = Math.round(x + (BAR_X + BAR_WIDTH - 2) * px);
		int fillBottom = Math.round(y + (BAR_Y + BAR_HEIGHT - 2) * px);
		int fillRight = fillLeft + (int) ((fillRightFull - fillLeft) * progress);
		if (fillRight > fillLeft) {
			Gui.drawRect(fillLeft, fillTop, fillRight, fillBottom, a | 0xFFFFFF);
		}
	}
}
