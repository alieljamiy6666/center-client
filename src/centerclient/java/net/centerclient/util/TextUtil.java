package net.centerclient.util;

import net.minecraft.client.gui.FontRenderer;

/**
 * Text helpers: centered text, text with a shadow, and text that cuts off
 * with "..." when it's too long for its space.
 */
public final class TextUtil {

	private static final String DOTS = "...";

	private TextUtil() {
	}

	/** Draws text with its middle at centerX. */
	public static void drawCentered(FontRenderer font, String text, float centerX, float y, int color,
			boolean shadow) {
		font.drawString(text, centerX - font.getStringWidth(text) / 2.0F, y, color, shadow);
	}

	/** Draws text with a soft dark shadow behind it. */
	public static void drawShadowed(FontRenderer font, String text, float x, float y, int color) {
		font.drawString(text, x, y, color, true);
	}

	/** A number with a fixed count of decimals, like formatDecimal(4.3219, 2) = "4.32". */
	public static String formatDecimal(double value, int decimals) {
		long factor = 1L;
		for (int i = 0; i < decimals; ++i) {
			factor *= 10L;
		}
		long scaled = Math.round(Math.abs(value) * factor);
		StringBuilder sb = new StringBuilder();
		if (value < 0.0 && scaled != 0L) {
			sb.append('-');
		}
		sb.append(scaled / factor);
		if (decimals > 0) {
			String fraction = Long.toString(scaled % factor);
			sb.append('.');
			for (int i = fraction.length(); i < decimals; ++i) {
				sb.append('0');
			}
			sb.append(fraction);
		}
		return sb.toString();
	}

	/** A number with at least two digits, like 7 = "07" (for clocks). */
	public static String twoDigits(int value) {
		return value < 10 ? "0" + value : Integer.toString(value);
	}

	/**
	 * Returns the text, cut short with "..." if it's wider than maxWidth.
	 * If it already fits, the same text comes back and nothing new is made, so
	 * it's cheap to call. Widgets that call it every frame should still keep the
	 * result and only redo it when the text or width changes.
	 */
	public static String ellipsize(FontRenderer font, String text, int maxWidth) {
		if (font.getStringWidth(text) <= maxWidth) {
			return text;
		}
		int dotsWidth = font.getStringWidth(DOTS);
		if (maxWidth <= dotsWidth) {
			return "";
		}
		return font.trimStringToWidth(text, maxWidth - dotsWidth) + DOTS;
	}
}
