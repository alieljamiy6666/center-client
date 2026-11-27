package net.centerclient.config;

/**
 * A color, including how see-through it is. Stored as 0xAARRGGBB and saved
 * in configs as text like "#CC33D1FF" (alpha, red, green, blue).
 */
public class ColorSetting extends Setting<Integer> {

	public ColorSetting(String name, int defaultColor) {
		super(name, defaultColor);
	}

	public int getColor() {
		return value;
	}

	public int getAlpha() {
		return (value >>> 24) & 255;
	}

	/** Changes only the see-through amount (0 = invisible, 255 = solid). */
	public void setAlpha(int alpha) {
		alpha = Math.max(0, Math.min(255, alpha));
		set((alpha << 24) | (value & 0xFFFFFF));
	}

	/** Changes only the color, keeping the see-through amount. */
	public void setRGB(int rgb) {
		set((value & 0xFF000000) | (rgb & 0xFFFFFF));
	}

	public String toHex() {
		String digits = Integer.toHexString(value).toUpperCase();
		while (digits.length() < 8) {
			digits = "0" + digits;
		}
		return "#" + digits;
	}

	@Override
	public Object toJson() {
		return toHex();
	}

	@Override
	public void fromJson(Object json) {
		if (json instanceof Number) {
			set(((Number) json).intValue());
			return;
		}
		if (json instanceof String) {
			String s = ((String) json).trim();
			if (s.startsWith("#")) {
				s = s.substring(1);
			}
			if (s.length() == 6) {
				s = "FF" + s; // no alpha given: solid
			}
			if (s.length() == 8) {
				try {
					set((int) Long.parseLong(s, 16));
					return;
				} catch (NumberFormatException e) {
					// falls through to the error below
				}
			}
		}
		throw new IllegalArgumentException("not a color: " + json);
	}
}
