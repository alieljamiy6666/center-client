package net.centerclient.module.hud;

import net.centerclient.config.BooleanSetting;
import net.centerclient.config.ColorSetting;
import net.centerclient.config.ModeSetting;
import net.centerclient.config.NumberSetting;
import net.centerclient.hud.HudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.centerclient.util.Animation;
import net.centerclient.util.ClickTracker;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.Easing;
import net.centerclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Keyboard;
import net.lax1dude.eaglercraft.KeyboardConstants;
import net.lax1dude.eaglercraft.Mouse;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.settings.KeyBinding;

/**
 * Shows W, A, S, D, the mouse buttons and the space bar. Keys light up in the
 * accent color the instant you press them and fade out when you let go. The
 * mouse buttons can show your clicks per second.
 *
 * It reads your real movement keys, so if you've rebound W to something
 * else, that key's name shows instead.
 */
public class KeystrokesModule extends HudModule {

	private static final int FORWARD = 0;
	private static final int LEFT = 1;
	private static final int BACK = 2;
	private static final int RIGHT = 3;
	private static final int ATTACK = 4;
	private static final int USE = 5;
	private static final int JUMP = 6;
	private static final int KEY_COUNT = 7;

	private final ModeSetting shape = add(new ModeSetting("Shape", "Rounded", "Rounded", "Square"));
	private final NumberSetting keySize = add(new NumberSetting("Key size", 20, 14, 30, 1));
	private final NumberSetting gap = add(new NumberSetting("Gap", 2, 0, 6, 1));
	private final BooleanSetting showMouse = add(new BooleanSetting("Show mouse buttons", true));
	private final BooleanSetting showCps = add(new BooleanSetting("Show CPS", true).showWhen(showMouse));
	private final BooleanSetting showSpace = add(new BooleanSetting("Show space bar", true));
	private final ColorSetting keyColor = add(new ColorSetting("Key color", 0xBF161A22));
	private final ColorSetting textColor = add(new ColorSetting("Text color", 0xFFE8ECF2));
	private final BooleanSetting customPressed = add(new BooleanSetting("Custom pressed color", false));
	private final ColorSetting pressedColor = add(new ColorSetting("Pressed color", 0xFFFFFFFF).showWhen(customPressed));

	private final Animation[] glow = new Animation[KEY_COUNT];
	private final String[] labels = new String[KEY_COUNT];
	private final int[] labelCodes = new int[KEY_COUNT];
	private String leftCpsText = "0 CPS";
	private String rightCpsText = "0 CPS";
	private int shownLeftCps;
	private int shownRightCps;

	public KeystrokesModule() {
		super("Keystrokes", "Shows your movement keys and mouse buttons lighting up as you press them", Category.HUD,
				END, END, 0.995F, 0.97F);
		for (int i = 0; i < KEY_COUNT; ++i) {
			glow[i] = new Animation(160L, Easing.EASE_OUT, 0.0F);
			labels[i] = "";
			labelCodes[i] = Integer.MIN_VALUE;
		}
	}

	@Override
	protected boolean backgroundByDefault() {
		return false; // each key has its own background
	}

	private KeyBinding binding(int key) {
		switch (key) {
		case FORWARD: return mc.gameSettings.keyBindForward;
		case LEFT: return mc.gameSettings.keyBindLeft;
		case BACK: return mc.gameSettings.keyBindBack;
		case RIGHT: return mc.gameSettings.keyBindRight;
		case ATTACK: return mc.gameSettings.keyBindAttack;
		case USE: return mc.gameSettings.keyBindUseItem;
		default: return mc.gameSettings.keyBindJump;
		}
	}

	/** Reads the real key or mouse state right now (not once per tick), so it reacts instantly. */
	private static boolean isDown(KeyBinding binding) {
		int code = binding.getKeyCode();
		if (code < 0) {
			return Mouse.isButtonDown(code + 100);
		}
		return code != KeyboardConstants.KEY_NONE && Keyboard.isKeyDown(code);
	}

	private static String nameOf(int code) {
		switch (code) {
		case -100: return "LMB";
		case -99: return "RMB";
		case -98: return "MMB";
		default: return code < 0 ? "M" + (code + 101) : KeyboardConstants.getKeyName(code);
		}
	}

	// ---- Size -----------------------------------------------------------------------

	private int size() {
		return keySize.getInt();
	}

	private int spacing() {
		return gap.getInt();
	}

	private int mouseHeight() {
		return showCps.isOn() ? Math.max(size(), 21) : size();
	}

	private int spaceHeight() {
		return Math.max(8, size() / 2);
	}

	@Override
	public int getWidth() {
		return size() * 3 + spacing() * 2;
	}

	@Override
	public int getHeight() {
		int h = size() * 2 + spacing();
		if (showMouse.isOn()) {
			h += spacing() + mouseHeight();
		}
		if (showSpace.isOn()) {
			h += spacing() + spaceHeight();
		}
		return h;
	}

	// ---- Drawing --------------------------------------------------------------------

	@Override
	protected void draw(boolean editing) {
		refreshText();
		boolean playing = mc.currentScreen == null; // don't light up while typing in chat or a menu
		for (int i = 0; i < KEY_COUNT; ++i) {
			if (playing && isDown(binding(i))) {
				glow[i].set(1.0F); // light up instantly
			} else {
				glow[i].animateTo(0.0F); // then fade out
			}
		}

		int s = size();
		int g = spacing();
		int full = s * 3 + g * 2;
		drawKey(s + g, 0, s, s, labels[FORWARD], null, glow[FORWARD].get());
		drawKey(0, s + g, s, s, labels[LEFT], null, glow[LEFT].get());
		drawKey(s + g, s + g, s, s, labels[BACK], null, glow[BACK].get());
		drawKey((s + g) * 2, s + g, s, s, labels[RIGHT], null, glow[RIGHT].get());
		int y = (s + g) * 2;
		if (showMouse.isOn()) {
			int mw = (full - g) / 2;
			boolean cps = showCps.isOn();
			drawKey(0, y, mw, mouseHeight(), labels[ATTACK], cps ? leftCpsText : null, glow[ATTACK].get());
			drawKey(mw + g, y, full - mw - g, mouseHeight(), labels[USE], cps ? rightCpsText : null, glow[USE].get());
			y += mouseHeight() + g;
		}
		if (showSpace.isOn()) {
			drawSpace(0, y, full, spaceHeight(), glow[JUMP].get());
		}
	}

	private int pressedColor() {
		return customPressed.isOn() ? pressedColor.getColor() : Theme.accent();
	}

	private void drawKey(int x, int y, int w, int h, String label, String subLabel, float lit) {
		int pressed = pressedColor();
		int radius = shape.is("Rounded") ? Math.min(4, w / 4) : 0;
		RenderUtil.roundedRect(x, y, w, h, radius, ColorUtil.mix(keyColor.getColor(), pressed, lit));
		int color = ColorUtil.mix(textColor.getColor(), ColorUtil.readableTextOn(pressed), lit);
		if (subLabel == null) {
			int tw = mc.fontRendererObj.getStringWidth(label);
			mc.fontRendererObj.drawString(label, x + (w - tw) / 2.0F, y + (h - 8) / 2.0F, color, false);
		} else {
			int tw = mc.fontRendererObj.getStringWidth(label);
			mc.fontRendererObj.drawString(label, x + (w - tw) / 2.0F, y + 2, color, false);
			// the CPS text is drawn smaller so it fits under the label
			int sw = mc.fontRendererObj.getStringWidth(subLabel);
			GlStateManager.pushMatrix();
			GlStateManager.translate(x + w / 2.0F, y + h - 8.0F, 0.0F);
			GlStateManager.scale(0.75F, 0.75F, 1.0F);
			mc.fontRendererObj.drawString(subLabel, -sw / 2.0F, 0.0F, ColorUtil.withAlpha(color, 0.85F), false);
			GlStateManager.popMatrix();
		}
	}

	private void drawSpace(int x, int y, int w, int h, float lit) {
		int pressed = pressedColor();
		int radius = shape.is("Rounded") ? Math.min(4, h / 2) : 0;
		RenderUtil.roundedRect(x, y, w, h, radius, ColorUtil.mix(keyColor.getColor(), pressed, lit));
		int color = ColorUtil.mix(textColor.getColor(), ColorUtil.readableTextOn(pressed), lit);
		RenderUtil.rect(x + w / 3.0F, y + h / 2.0F - 0.5F, w / 3.0F, 1.0F, color); // a little bar for "space"
	}

	/** Updates key names (if you rebind keys) and CPS numbers, only when they change. */
	private void refreshText() {
		for (int i = 0; i < KEY_COUNT; ++i) {
			int code = binding(i).getKeyCode();
			if (code != labelCodes[i]) {
				labelCodes[i] = code;
				labels[i] = nameOf(code);
			}
		}
		int left = ClickTracker.getCps(ClickTracker.LEFT);
		if (left != shownLeftCps) {
			shownLeftCps = left;
			leftCpsText = left + " CPS";
		}
		int right = ClickTracker.getCps(ClickTracker.RIGHT);
		if (right != shownRightCps) {
			shownRightCps = right;
			rightCpsText = right + " CPS";
		}
	}
}
