package net.centerclient.gui;

import net.centerclient.util.RenderUtil;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;

/**
 * Tiny pixel-art icons drawn from little text pictures ("X" = a pixel).
 * They're plain rectangles, so they can be any color and cost almost nothing.
 */
public final class PixelIcon {

	public static final PixelIcon ALL = new PixelIcon(
			"XXX.XXX",
			"XXX.XXX",
			"XXX.XXX",
			".......",
			"XXX.XXX",
			"XXX.XXX",
			"XXX.XXX");

	public static final PixelIcon RENDER = new PixelIcon(
			"..XXX..",
			".X...X.",
			"X..X..X",
			"X.XXX.X",
			"X..X..X",
			".X...X.",
			"..XXX..");

	public static final PixelIcon HUD = new PixelIcon(
			"XXXXXXX",
			"X.....X",
			"X.XX..X",
			"X.....X",
			"XXXXXXX",
			"...X...",
			".XXXXX.");

	public static final PixelIcon MISC = new PixelIcon(
			"...X...",
			"..XXX..",
			".XXXXX.",
			"XXX.XXX",
			".XXXXX.",
			"..XXX..",
			"...X...");

	public static final PixelIcon PVP = new PixelIcon(
			".....XX",
			"....XXX",
			"...XXX.",
			"X.XXX..",
			".XXX...",
			".XX....",
			"X.X....");

	public static final PixelIcon GEAR = new PixelIcon(
			"...X...",
			".X.X.X.",
			"..XXX..",
			"XXX.XXX",
			"..XXX..",
			".X.X.X.",
			"...X...");

	public static final PixelIcon SEARCH = new PixelIcon(
			".XXX...",
			"X...X..",
			"X...X..",
			"X...X..",
			".XXX...",
			".....X.",
			"......X");

	public static final PixelIcon PROFILE = new PixelIcon(
			"..XXX..",
			"..XXX..",
			"..XXX..",
			".......",
			".XXXXX.",
			"XXXXXXX",
			"XXXXXXX");

	public static final PixelIcon EDIT_HUD = new PixelIcon(
			"XX...XX",
			"X.....X",
			"..XXX..",
			"..XXX..",
			"..XXX..",
			"X.....X",
			"XX...XX");

	public static final PixelIcon CLOSE = new PixelIcon(
			"X.....X",
			".X...X.",
			"..X.X..",
			"...X...",
			"..X.X..",
			".X...X.",
			"X.....X");

	public static final PixelIcon ARROW_DOWN = new PixelIcon(
			"XXXXX",
			".XXX.",
			"..X..");

	private final int width;
	private final int height;
	private final boolean[] pixels;

	private PixelIcon(String... rows) {
		this.height = rows.length;
		this.width = rows[0].length();
		this.pixels = new boolean[width * height];
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				pixels[y * width + x] = rows[y].charAt(x) == 'X';
			}
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/** Draws the icon with its top-left corner at x, y. scale 1 = one GUI pixel per dot. */
	public void draw(float x, float y, float scale, int color) {
		WorldRenderer batch = RenderUtil.beginBatch();
		for (int py = 0; py < height; ++py) {
			for (int px = 0; px < width; ++px) {
				if (pixels[py * width + px]) {
					RenderUtil.batchRect(batch, x + px * scale, y + py * scale, scale, scale, color);
				}
			}
		}
		RenderUtil.endBatch();
	}
}
