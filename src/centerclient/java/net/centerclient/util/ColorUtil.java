package net.centerclient.util;

/**
 * Small helpers for 0xAARRGGBB colors. Nothing here creates objects, so it's
 * safe to use every frame.
 */
public final class ColorUtil {

	private ColorUtil() {
	}

	public static int alpha(int color) {
		return (color >>> 24) & 255;
	}

	public static int red(int color) {
		return (color >> 16) & 255;
	}

	public static int green(int color) {
		return (color >> 8) & 255;
	}

	public static int blue(int color) {
		return color & 255;
	}

	/** The same color with its see-through amount multiplied by alpha (0 to 1). */
	public static int withAlpha(int color, float alpha) {
		int a = (int) (alpha(color) * clamp(alpha));
		return (a << 24) | (color & 0xFFFFFF);
	}

	/** Blends from color a (t = 0) to color b (t = 1). */
	public static int mix(int a, int b, float t) {
		t = clamp(t);
		int al = (int) (alpha(a) + (alpha(b) - alpha(a)) * t);
		int r = (int) (red(a) + (red(b) - red(a)) * t);
		int g = (int) (green(a) + (green(b) - green(a)) * t);
		int bl = (int) (blue(a) + (blue(b) - blue(a)) * t);
		return (al << 24) | (r << 16) | (g << 8) | bl;
	}

	/** How bright a color looks, from 0 (black) to 1 (white). */
	public static float brightness(int color) {
		return (0.299F * red(color) + 0.587F * green(color) + 0.114F * blue(color)) / 255.0F;
	}

	/** White or dark text, whichever is easier to read on this color. */
	public static int readableTextOn(int color) {
		return brightness(color) > 0.6F ? 0xFF0D0F14 : 0xFFFFFFFF;
	}

	private static float clamp(float t) {
		return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
	}
}
