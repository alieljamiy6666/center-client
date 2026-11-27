package net.centerclient.gui.clickgui;

import net.centerclient.config.KeybindSetting;
import net.centerclient.gui.GuiUtil;
import net.centerclient.module.Module;
import net.centerclient.theme.Theme;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.RenderUtil;
import net.lax1dude.eaglercraft.KeyboardConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * A key button. Click it, then press the key you want. Escape cancels, and
 * Backspace or Delete clears the key. Right Shift is saved for opening this
 * menu, so it can't be used.
 */
class KeybindRow extends SettingRow {

	/** What the row changes: a module's own key or a keybind setting. */
	interface KeyTarget {
		int get();

		void set(int key);
	}

	private final String label;
	private final KeyTarget target;
	private final KeybindSetting visibility;
	private boolean listening;

	private KeybindRow(String label, KeyTarget target, KeybindSetting visibility) {
		this.label = label;
		this.target = target;
		this.visibility = visibility;
	}

	static KeybindRow forModule(final Module module) {
		return new KeybindRow("Toggle key", new KeyTarget() {
			@Override
			public int get() {
				return module.getKey();
			}

			@Override
			public void set(int key) {
				module.setKey(key);
			}
		}, null);
	}

	static KeybindRow forSetting(final KeybindSetting setting) {
		return new KeybindRow(setting.getName(), new KeyTarget() {
			@Override
			public int get() {
				return setting.getKey();
			}

			@Override
			public void set(int key) {
				setting.set(key);
			}
		}, setting);
	}

	@Override
	boolean isVisible() {
		return visibility == null || visibility.isVisible();
	}

	@Override
	int getHeight() {
		return 18;
	}

	@Override
	protected void drawRow(FontRenderer font, int mouseX, int mouseY) {
		int key = target.get();
		String keyText = listening ? "Press a key..." : (key == Module.NO_KEY ? "None" : KeyboardConstants.getKeyName(key));
		int bw = Math.max(40, font.getStringWidth(keyText) + 10);
		int bx = x + width - bw;
		boolean hover = isInside(mouseX, mouseY, x, y, width, 18);
		GuiUtil.text(font, label, x, y + 5, hover || listening ? Theme.TEXT : Theme.TEXT_FADED, false);
		int border = Theme.BORDER;
		if (listening) {
			float pulse = 0.5F + 0.5F * (float) Math.sin(Minecraft.getSystemTime() / 150.0);
			border = ColorUtil.mix(Theme.BORDER, Theme.accent(), pulse);
		}
		RenderUtil.roundedRect(bx, y + 2, bw, 14, 3, hover ? Theme.PANEL_HOVER : Theme.BACKGROUND);
		RenderUtil.roundedOutline(bx, y + 2, bw, 14, 3, 1, border);
		GuiUtil.textCentered(font, keyText, bx + bw / 2.0F, y + 5, listening ? Theme.accent() : Theme.TEXT, false);
	}

	@Override
	boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (button == 0 && isInside(mouseX, mouseY, x, y, width, 18)) {
			listening = !listening;
			click();
			return true;
		}
		listening = false;
		return false;
	}

	@Override
	boolean keyTyped(char typedChar, int keyCode) {
		if (!listening) {
			return false;
		}
		listening = false;
		if (keyCode == KeyboardConstants.KEY_ESCAPE || keyCode == KeyboardConstants.KEY_RSHIFT) {
			return true; // cancel, keep the old key
		}
		if (keyCode == KeyboardConstants.KEY_BACK || keyCode == KeyboardConstants.KEY_DELETE) {
			target.set(Module.NO_KEY);
		} else if (keyCode > 0) {
			target.set(keyCode);
		}
		return true;
	}

	@Override
	boolean isListening() {
		return listening;
	}
}
