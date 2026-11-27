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

	/**
	 * Hue / saturation / brightness (each 0 to 1) to a solid color. This is how
	 * a color picker thinks: hue = which color, saturation = how strong,
	 * brightness = how light.
	 */
	public static int hsvToRgb(float hue, float saturation, float brightness) {
		float h = (hue - (float) Math.floor(hue)) * 6.0F;
		int sector = (int) h;
		float f = h - sector;
		float v = clamp(brightness);
		float s = clamp(saturation);
		float p = v * (1.0F - s);
		float q = v * (1.0F - s * f);
		float t = v * (1.0F - s * (1.0F - f));
		float r;
		float g;
		float b;
		switch (sector) {
		case 0: r = v; g = t; b = p; break;
		case 1: r = q; g = v; b = p; break;
		case 2: r = p; g = v; b = t; break;
		case 3: r = p; g = q; b = v; break;
		case 4: r = t; g = p; b = v; break;
		default: r = v; g = p; b = q; break;
		}
		return 0xFF000000 | (Math.round(r * 255.0F) << 16) | (Math.round(g * 255.0F) << 8) | Math.round(b * 255.0F);
	}

	/** A color to hue / saturation / brightness, written into out[0], out[1], out[2]. */
	public static void rgbToHsv(int color, float[] out) {
		float r = red(color) / 255.0F;
		float g = green(color) / 255.0F;
		float b = blue(color) / 255.0F;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float h = 0.0F;
		if (delta > 0.0F) {
			if (max == r) {
				h = ((g - b) / delta) % 6.0F;
			} else if (max == g) {
				h = (b - r) / delta + 2.0F;
			} else {
				h = (r - g) / delta + 4.0F;
			}
			h /= 6.0F;
			if (h < 0.0F) {
				h += 1.0F;
			}
		}
		out[0] = h;
		out[1] = max > 0.0F ? delta / max : 0.0F;
		out[2] = max;
	}

	private static float clamp(float t) {
		return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
	}
}
