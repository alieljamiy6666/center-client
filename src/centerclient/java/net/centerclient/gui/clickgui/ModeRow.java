package net.centerclient.gui.clickgui;

import net.centerclient.config.ModeSetting;
import net.centerclient.gui.GuiUtil;
import net.centerclient.gui.PixelIcon;
import net.centerclient.theme.Theme;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;

/** A setting name and a dropdown. Click it to open the list, then click a choice. */
class ModeRow extends SettingRow {

	private static final int OPTION_HEIGHT = 13;

	private final ModeSetting setting;
	private boolean open;

	ModeRow(ModeSetting setting) {
		this.setting = setting;
	}

	@Override
	boolean isVisible() {
		return setting.isVisible();
	}

	@Override
	int getHeight() {
		return 18 + (open ? setting.getModes().length * OPTION_HEIGHT + 4 : 0);
	}

	private int buttonWidth(FontRenderer font) {
		int widest = 0;
		String[] modes = setting.getModes();
		for (int i = 0; i < modes.length; ++i) {
			widest = Math.max(widest, font.getStringWidth(modes[i]));
		}
		return Math.min(width / 2 + 10, widest + 20);
	}

	@Override
	protected void drawRow(FontRenderer font, int mouseX, int mouseY) {
		int bw = buttonWidth(font);
		int bx = x + width - bw;
		boolean hoverButton = isInside(mouseX, mouseY, bx, y + 2, bw, 14);
		GuiUtil.text(font, setting.getName(), x, y + 5,
				hoverButton || open ? Theme.TEXT : Theme.TEXT_FADED, false);

		RenderUtil.roundedRect(bx, y + 2, bw, 14, 3, hoverButton ? Theme.PANEL_HOVER : Theme.BACKGROUND);
		RenderUtil.roundedOutline(bx, y + 2, bw, 14, 3, 1, open ? Theme.accent() : Theme.BORDER);
		GuiUtil.text(font, setting.get(), bx + 5, y + 5, Theme.TEXT, false);
		PixelIcon.ARROW_DOWN.draw(bx + bw - 9, y + 8, 1.0F, Theme.TEXT_FADED);

		if (open) {
			String[] modes = setting.getModes();
			int listY = y + 18;
			RenderUtil.roundedRect(bx, listY, bw, modes.length * OPTION_HEIGHT + 2, 3, Theme.BACKGROUND);
			RenderUtil.roundedOutline(bx, listY, bw, modes.length * OPTION_HEIGHT + 2, 3, 1, Theme.BORDER);
			for (int i = 0; i < modes.length; ++i) {
				int oy = listY + 1 + i * OPTION_HEIGHT;
				boolean hover = isInside(mouseX, mouseY, bx, oy, bw, OPTION_HEIGHT);
				boolean chosen = setting.is(modes[i]);
				if (hover) {
					RenderUtil.rect(bx + 1, oy, bw - 2, OPTION_HEIGHT, ColorUtil.withAlpha(Theme.accent(), 0.2F));
				}
				GuiUtil.text(font, modes[i], bx + 5, oy + 3, chosen ? Theme.accent() : Theme.TEXT, false);
			}
		}
	}

	@Override
	boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0) {
			return false;
		}
		FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRendererObj;
		int bw = buttonWidth(font);
		int bx = x + width - bw;
		if (open) {
			String[] modes = setting.getModes();
			int listY = y + 18;
			for (int i = 0; i < modes.length; ++i) {
				if (isInside(mouseX, mouseY, bx, listY + 1 + i * OPTION_HEIGHT, bw, OPTION_HEIGHT)) {
					setting.set(modes[i]);
					open = false;
					click();
					return true;
				}
			}
		}
		if (isInside(mouseX, mouseY, x, y, width, 18)) {
			open = !open;
			click();
			return true;
		}
		if (open) {
			open = false; // clicked somewhere else: close the list
		}
		return false;
	}
}
