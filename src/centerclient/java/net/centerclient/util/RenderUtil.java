package net.centerclient.util;

import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Drawing helpers for the Center Client look: rectangles, gradients, rounded
 * pixel-art corners, outlines, soft shadows and circles.
 *
 * How it stays fast in WebGL: every shape is built from plain flat rectangles
 * (no textures, no shaders) and each call sends them all to the graphics card
 * in one batch. Rounded corners are "stair steps" one GUI pixel high, which is
 * what gives them the crisp pixel-art look. The corner shapes are worked out
 * once when the game starts, not every frame.
 *
 * All positions are in GUI pixels and colors are 0xAARRGGBB.
 */
public final class RenderUtil {

	/** The biggest corner radius we support. */
	public static final int MAX_RADIUS = 16;

	/** CORNER_INSETS[r][row] = how far row "row" of a radius-r corner is pushed in. */
	private static final int[][] CORNER_INSETS = new int[MAX_RADIUS + 1][];

	static {
		for (int r = 0; r <= MAX_RADIUS; ++r) {
			CORNER_INSETS[r] = new int[r];
			for (int row = 0; row < r; ++row) {
				float dy = r - row - 0.5F;
				float dx = (float) Math.sqrt(r * r - dy * dy);
				CORNER_INSETS[r][row] = Math.round(r - dx);
			}
		}
	}

	private RenderUtil() {
	}

	// ---- Plain shapes -----------------------------------------------------------

	/** A filled rectangle. */
	public static void rect(float x, float y, float width, float height, int color) {
		WorldRenderer buf = begin();
		quad(buf, x, y, x + width, y + height, color, color);
		end();
	}

	/** A rectangle that fades from one color at the top to another at the bottom. */
	public static void gradientVertical(float x, float y, float width, float height, int top, int bottom) {
		WorldRenderer buf = begin();
		quad(buf, x, y, x + width, y + height, top, bottom);
		end();
	}

	/** A rectangle that fades from one color on the left to another on the right. */
	public static void gradientHorizontal(float x, float y, float width, float height, int left, int right) {
		WorldRenderer buf = begin();
		vertex(buf, x, y + height, left);
		vertex(buf, x + width, y + height, right);
		vertex(buf, x + width, y, right);
		vertex(buf, x, y, left);
		end();
	}

	// ---- Rounded shapes ---------------------------------------------------------

	/** A filled rectangle with rounded pixel-art corners. */
	public static void roundedRect(float x, float y, float width, float height, int radius, int color) {
		roundedGradient(x, y, width, height, radius, color, color);
	}

	/** A rounded rectangle that fades from top to bottom. */
	public static void roundedGradient(float x, float y, float width, float height, int radius, int top,
			int bottom) {
		int rows = (int) height;
		if (rows <= 0 || width <= 0.0F) {
			return;
		}
		int r = clampRadius(radius, width, rows);
		WorldRenderer buf = begin();
		// stair-step rows at the top and bottom corners
		for (int row = 0; row < r; ++row) {
			int inset = CORNER_INSETS[r][row];
			int topColor = ColorUtil.mix(top, bottom, (float) row / rows);
			int nextColor = ColorUtil.mix(top, bottom, (float) (row + 1) / rows);
			quad(buf, x + inset, y + row, x + width - inset, y + row + 1, topColor, nextColor);
			int brow = rows - 1 - row;
			int bColor = ColorUtil.mix(top, bottom, (float) brow / rows);
			int bNext = ColorUtil.mix(top, bottom, (float) (brow + 1) / rows);
			quad(buf, x + inset, y + brow, x + width - inset, y + brow + 1, bColor, bNext);
		}
		// the straight middle part
		if (rows - 2 * r > 0) {
			quad(buf, x, y + r, x + width, y + rows - r, ColorUtil.mix(top, bottom, (float) r / rows),
					ColorUtil.mix(top, bottom, (float) (rows - r) / rows));
		}
		end();
	}

	/** A rounded outline (a frame with nothing inside). */
	public static void roundedOutline(float x, float y, float width, float height, int radius, int thickness,
			int color) {
		int rows = (int) height;
		if (rows <= 0 || width <= 0.0F || thickness <= 0) {
			return;
		}
		int r = clampRadius(radius, width, rows);
		int innerRows = rows - 2 * thickness;
		float innerWidth = width - 2 * thickness;
		int ir = innerRows > 0 && innerWidth > 0 ? clampRadius(r - thickness, innerWidth, innerRows) : 0;
		WorldRenderer buf = begin();
		for (int row = 0; row < rows; ++row) {
			int outer = inset(r, row, rows);
			int innerRow = row - thickness;
			if (innerRows <= 0 || innerWidth <= 0 || innerRow < 0 || innerRow >= innerRows) {
				quad(buf, x + outer, y + row, x + width - outer, y + row + 1, color, color); // whole row is frame
			} else {
				int inner = thickness + inset(ir, innerRow, innerRows);
				quad(buf, x + outer, y + row, x + inner, y + row + 1, color, color);
				quad(buf, x + width - inner, y + row, x + width - outer, y + row + 1, color, color);
			}
		}
		end();
	}

	/**
	 * A soft shadow or glow around a rounded box. It stacks a few see-through
	 * rounded boxes, each one a bit bigger, so the edge fades out smoothly.
	 * Draw it first, then the (solid) box on top.
	 *
	 * @param size  how many GUI pixels the shadow spreads out
	 * @param color the shadow color at its darkest (try black with some alpha, or the accent for a glow)
	 */
	public static void shadow(float x, float y, float width, float height, int radius, int size, int color) {
		if (size <= 0) {
			return;
		}
		int layerColor = ColorUtil.withAlpha(color, 1.5F / size);
		for (int ring = size; ring >= 1; --ring) {
			roundedRect(x - ring, y - ring, width + ring * 2, height + ring * 2, Math.min(MAX_RADIUS, radius + ring),
					layerColor);
		}
	}

	/** A filled pixel-art circle. */
	public static void circle(float centerX, float centerY, float radius, int color) {
		int rows = Math.round(radius * 2.0F);
		if (rows <= 0) {
			return;
		}
		float top = centerY - rows / 2.0F;
		WorldRenderer buf = begin();
		for (int row = 0; row < rows; ++row) {
			float dy = row + 0.5F - rows / 2.0F;
			float half = (float) Math.sqrt(Math.max(0.0F, radius * radius - dy * dy));
			half = Math.round(half);
			if (half > 0.0F) {
				quad(buf, centerX - half, top + row, centerX + half, top + row + 1, color, color);
			}
		}
		end();
	}

	// ---- Batches: many small rectangles in one go ---------------------------------

	/**
	 * Starts a batch. Add rectangles with {@link #batchRect}, then call
	 * {@link #endBatch()}. Use this for lots of small shapes, like particles:
	 * they all go to the graphics card in a single trip.
	 */
	public static WorldRenderer beginBatch() {
		return begin();
	}

	public static void batchRect(WorldRenderer batch, float x, float y, float width, float height, int color) {
		quad(batch, x, y, x + width, y + height, color, color);
	}

	public static void endBatch() {
		end();
	}

	// ---- Internals --------------------------------------------------------------

	private static int clampRadius(int radius, float width, int rows) {
		int r = Math.min(radius, MAX_RADIUS);
		r = Math.min(r, (int) (width / 2.0F));
		r = Math.min(r, rows / 2);
		return Math.max(0, r);
	}

	/** How far a row is pushed in by the corners, for a shape that is "rows" tall. */
	private static int inset(int r, int row, int rows) {
		if (row < r) {
			return CORNER_INSETS[r][row];
		}
		if (row >= rows - r) {
			return CORNER_INSETS[r][rows - 1 - row];
		}
		return 0;
	}

	private static WorldRenderer begin() {
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.shadeModel(7425);
		WorldRenderer buf = Tessellator.getInstance().getBuffer();
		buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
		return buf;
	}

	private static void end() {
		Tessellator.getInstance().draw();
		GlStateManager.shadeModel(7424);
		GlStateManager.enableAlpha();
		GlStateManager.disableBlend();
		GlStateManager.enableTexture2D();
	}

	private static void quad(WorldRenderer buf, float x1, float y1, float x2, float y2, int topColor,
			int bottomColor) {
		vertex(buf, x1, y2, bottomColor);
		vertex(buf, x2, y2, bottomColor);
		vertex(buf, x2, y1, topColor);
		vertex(buf, x1, y1, topColor);
	}

	private static void vertex(WorldRenderer buf, float x, float y, int color) {
		buf.pos((double) x, (double) y, 0.0D).color(ColorUtil.red(color), ColorUtil.green(color),
				ColorUtil.blue(color), ColorUtil.alpha(color)).endVertex();
	}
}
