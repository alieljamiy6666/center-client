package net.centerclient.theme;

import net.centerclient.config.ConfigManager;
import net.centerclient.util.ColorUtil;

/**
 * One place for every Center Client color, so all screens match.
 *
 * Colors are written as 0xAARRGGBB: alpha (see-through amount), red, green, blue.
 * FF alpha means fully solid.
 *
 * Everything is fixed except the accent color, which the player can change.
 * The light and dark versions of the accent are worked out automatically.
 * The player's choice is saved by the ConfigManager.
 */
public final class Theme {

	// ---- The color tokens -----------------------------------------------------

	/** Near-black: behind everything. */
	public static final int BACKGROUND = 0xFF0D0F14;
	/** Slightly lighter: boxes, cards and buttons. */
	public static final int PANEL = 0xFF161A22;
	/** A panel with the mouse over it. */
	public static final int PANEL_HOVER = 0xFF1F2530;
	/** Thin lines around panels. */
	public static final int BORDER = 0xFF2A313D;
	/** Main text. */
	public static final int TEXT = 0xFFE8ECF2;
	/** Less important text, like descriptions. */
	public static final int TEXT_FADED = 0xFF8A93A3;
	/** Good things: "on", "saved". */
	public static final int SUCCESS = 0xFF3DDC84;
	/** Bad things: errors, "delete". */
	public static final int DANGER = 0xFFFF4D5E;
	/** In-between things: "careful", medium ping, half-broken armor. */
	public static final int WARNING = 0xFFFFD23F;

	// ---- The accent color -----------------------------------------------------

	/** Accent colors the player can pick from. */
	public static final int[] ACCENT_PRESETS = { 0xFF33D1FF, 0xFF8B7BFF, 0xFFFF5FA2, 0xFFFF9F43, 0xFF3DDC84,
			0xFFFFD23F, 0xFFFF4D5E, 0xFFF2F4F8 };
	public static final String[] ACCENT_PRESET_NAMES = { "Cyan", "Violet", "Pink", "Orange", "Green", "Gold", "Red",
			"White" };

	private static int accent;
	private static int accentLight;
	private static int accentDark;
	private static int accentPreset;

	static {
		setAccentPreset(0);
	}

	private Theme() {
	}

	/** The accent color: buttons that matter, "on" switches, highlights. */
	public static int accent() {
		return accent;
	}

	/** A lighter accent, for shine and top edges. */
	public static int accentLight() {
		return accentLight;
	}

	/** A darker accent, for shading and bottom edges. */
	public static int accentDark() {
		return accentDark;
	}

	/** Picks one of the preset accent colors by its number (saved automatically). */
	public static void setAccentPreset(int index) {
		int count = ACCENT_PRESETS.length;
		accentPreset = ((index % count) + count) % count; // wraps around: after the last comes the first
		setAccent(ACCENT_PRESETS[accentPreset]);
		ConfigManager.changed();
	}

	/** The number of the current preset accent color. */
	public static int getAccentPreset() {
		return accentPreset;
	}

	/** Switches to the next preset accent color and returns its name. */
	public static String nextAccentPreset() {
		setAccentPreset(accentPreset + 1);
		return accentName();
	}

	/** The name of the current preset accent color. */
	public static String accentName() {
		return ACCENT_PRESET_NAMES[accentPreset];
	}

	/** Sets any accent color. */
	public static void setAccent(int color) {
		accent = 0xFF000000 | color;
		accentLight = ColorUtil.mix(accent, 0xFFFFFFFF, 0.55F);
		accentDark = ColorUtil.mix(accent, 0xFF000000, 0.35F);
	}
}
