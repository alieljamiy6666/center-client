package net.centerclient.gui.clickgui;

import java.util.ArrayList;
import java.util.List;

import net.centerclient.config.BooleanSetting;
import net.centerclient.config.ColorSetting;
import net.centerclient.config.KeybindSetting;
import net.centerclient.config.ModeSetting;
import net.centerclient.config.NumberSetting;
import net.centerclient.config.Setting;
import net.centerclient.gui.GuiUtil;
import net.centerclient.gui.PixelIcon;
import net.centerclient.module.Module;
import net.centerclient.theme.Theme;
import net.centerclient.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;

/**
 * The panel that slides out when you click a module's gear: the module's
 * name and description, its toggle key, and a row for each of its settings.
 * Rows that are hidden (see Setting.showWhen) are skipped. It scrolls if the
 * settings don't fit.
 */
class SettingsPanel {

	private static final int HEADER_HEIGHT = 18;
	private static final int ROW_GAP = 4;
	private static final int PADDING = 8;

	private Module module;
	private final List<SettingRow> rows = new ArrayList<SettingRow>();
	private List<String> descriptionLines = new ArrayList<String>();
	private int descriptionWidth = -1;

	private float scroll;
	private float scrollTarget;
	private float maxScroll;

	// where it was last drawn
	private int x;
	private int y;
	private int width;
	private int height;
	private int rowsTop;

	Module getModule() {
		return module;
	}

	/** Fills the panel with a module's settings. */
	void open(Module module) {
		this.module = module;
		rows.clear();
		rows.add(KeybindRow.forModule(module));
		List<Setting<?>> settings = module.getSettings();
		for (int i = 0; i < settings.size(); ++i) {
			Setting<?> s = settings.get(i);
			if (s instanceof BooleanSetting) {
				rows.add(new BooleanRow((BooleanSetting) s));
			} else if (s instanceof NumberSetting) {
				rows.add(new NumberRow((NumberSetting) s));
			} else if (s instanceof ModeSetting) {
				rows.add(new ModeRow((ModeSetting) s));
			} else if (s instanceof ColorSetting) {
				rows.add(new ColorRow((ColorSetting) s));
			} else if (s instanceof KeybindSetting) {
				rows.add(KeybindRow.forSetting((KeybindSetting) s));
			}
		}
		descriptionWidth = -1;
		scroll = scrollTarget = 0.0F;
	}

	boolean isInside(int mouseX, int mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	boolean isOverClose(int mouseX, int mouseY) {
		return mouseX >= x + width - 18 && mouseX < x + width - 4 && mouseY >= y + 4 && mouseY < y + 18;
	}

	boolean isListening() {
		for (int i = 0; i < rows.size(); ++i) {
			if (rows.get(i).isListening()) {
				return true;
			}
		}
		return false;
	}

	void draw(FontRenderer font, int x, int y, int width, int height, int mouseX, int mouseY, float frameSeconds) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		if (module == null) {
			return;
		}
		int inner = width - PADDING * 2;
		if (descriptionWidth != inner) {
			descriptionWidth = inner;
			descriptionLines = font.listFormattedStringToWidth(module.getDescription(), inner);
		}

		RenderUtil.shadow(x, y, width, height, 7, 6, 0x90000000);
		RenderUtil.roundedRect(x, y, width, height, 7, Theme.PANEL);
		RenderUtil.roundedOutline(x, y, width, height, 7, 1, Theme.BORDER);

		// rows (drawn first, then the header is drawn over anything scrolled up under it)
		int descHeight = descriptionLines.size() * 10;
		rowsTop = y + HEADER_HEIGHT + 4 + descHeight + 6;
		int rowsBottom = y + height - PADDING;
		scroll += (scrollTarget - scroll) * Math.min(1.0F, frameSeconds * 14.0F);
		int total = 0;
		for (int i = 0; i < rows.size(); ++i) {
			SettingRow row = rows.get(i);
			if (row.isVisible()) {
				total += row.getHeight() + ROW_GAP;
			}
		}
		maxScroll = Math.max(0, total - (rowsBottom - rowsTop));
		scrollTarget = Math.max(0.0F, Math.min(maxScroll, scrollTarget));
		int rowY = rowsTop - Math.round(scroll);
		int mx = mouseY >= rowsTop && mouseY < rowsBottom ? mouseX : -1000; // no hover outside the visible area
		RenderUtil.beginClip(x, rowsTop - 2, width, rowsBottom - rowsTop + 4);
		try {
			for (int i = 0; i < rows.size(); ++i) {
				SettingRow row = rows.get(i);
				if (!row.isVisible()) {
					continue;
				}
				int h = row.getHeight();
				if (rowY + h > rowsTop - 2 && rowY < rowsBottom + 2) {
					row.draw(font, x + PADDING, rowY, inner, mx, mouseY);
				} else {
					row.setBounds(x + PADDING, rowY, inner); // off screen: just remember where it is
				}
				rowY += h + ROW_GAP;
			}
		} finally {
			RenderUtil.endClip();
		}

		// header: name and close button, then the description
		GuiUtil.text(font, module.getName(), x + PADDING, y + 6, Theme.accent(), true);
		boolean overClose = isOverClose(mouseX, mouseY);
		PixelIcon.CLOSE.draw(x + width - 15, y + 7, 1.0F, overClose ? Theme.DANGER : Theme.TEXT_FADED);
		RenderUtil.rect(x + PADDING, y + HEADER_HEIGHT, inner, 1, Theme.BORDER);
		for (int i = 0; i < descriptionLines.size(); ++i) {
			GuiUtil.text(font, descriptionLines.get(i), x + PADDING, y + HEADER_HEIGHT + 4 + i * 10,
					Theme.TEXT_FADED, false);
		}

		// scrollbar
		if (maxScroll > 0.0F) {
			float viewH = rowsBottom - rowsTop;
			float barH = Math.max(12.0F, viewH * viewH / (viewH + maxScroll));
			float barY = rowsTop + (viewH - barH) * (scroll / maxScroll);
			RenderUtil.roundedRect(x + width - 4, barY, 2, barH, 1, Theme.BORDER);
		}
	}

	/** Returns true if the click was inside the panel (used or not). */
	boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (!isInside(mouseX, mouseY)) {
			stopListening();
			return false;
		}
		boolean inRows = mouseY >= rowsTop - 2 && mouseY < y + height - PADDING + 2;
		for (int i = 0; i < rows.size(); ++i) {
			SettingRow row = rows.get(i);
			if (row.isVisible()) {
				boolean used = inRows && row.mouseClicked(mouseX, mouseY, button);
				if (used) {
					// let the other rows know (so an open dropdown or key button closes)
					for (int j = 0; j < rows.size(); ++j) {
						if (j != i && rows.get(j).isListening()) {
							rows.get(j).mouseClicked(-1000, -1000, button);
						}
					}
					return true;
				}
			}
		}
		return true;
	}

	void mouseDragged(int mouseX, int mouseY) {
		for (int i = 0; i < rows.size(); ++i) {
			rows.get(i).mouseDragged(mouseX, mouseY);
		}
	}

	void mouseReleased() {
		for (int i = 0; i < rows.size(); ++i) {
			rows.get(i).mouseReleased();
		}
	}

	boolean keyTyped(char typedChar, int keyCode) {
		for (int i = 0; i < rows.size(); ++i) {
			if (rows.get(i).keyTyped(typedChar, keyCode)) {
				return true;
			}
		}
		return false;
	}

	void scroll(int direction) {
		scrollTarget += direction * 24.0F;
	}

	private void stopListening() {
		for (int i = 0; i < rows.size(); ++i) {
			if (rows.get(i).isListening()) {
				rows.get(i).mouseClicked(-1000, -1000, 0);
			}
		}
	}
}
