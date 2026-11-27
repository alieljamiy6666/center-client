package net.centerclient.gui.clickgui;

import net.centerclient.config.NumberSetting;
import net.centerclient.gui.GuiUtil;
import net.centerclient.theme.Theme;
import net.centerclient.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;

/** A setting name, its value, and a slider you can click or drag. */
class NumberRow extends SettingRow {

	private static final int HEIGHT = 26;
	private static final int TRACK_Y = 17;

	private final NumberSetting setting;
	private boolean dragging;

	/** The value as text, only redone when the value changes. */
	private String valueText = "";
	private double valueTextFor = Double.NaN;

	NumberRow(NumberSetting setting) {
		this.setting = setting;
	}

	@Override
	boolean isVisible() {
		return setting.isVisible();
	}

	@Override
	int getHeight() {
		return HEIGHT;
	}

	@Override
	protected void drawRow(FontRenderer font, int mouseX, int mouseY) {
		if (setting.getDouble() != valueTextFor) {
			valueTextFor = setting.getDouble();
			valueText = setting.getDisplayValue();
		}
		boolean hover = dragging || isInside(mouseX, mouseY, x, y, width, HEIGHT);
		GuiUtil.text(font, setting.getName(), x, y + 4, hover ? Theme.TEXT : Theme.TEXT_FADED, false);
		GuiUtil.text(font, valueText, x + width - font.getStringWidth(valueText), y + 4, Theme.accent(), false);

		float percent = (float) setting.getPercent();
		int trackY = y + TRACK_Y;
		RenderUtil.roundedRect(x, trackY, width, 4, 2, Theme.BORDER);
		RenderUtil.roundedRect(x, trackY, Math.max(4, width * percent), 4, 2, Theme.accent());
		float knobX = x + 3 + (width - 6) * percent;
		RenderUtil.circle(knobX, trackY + 2, dragging || hover ? 4.5F : 4.0F, 0xFFFFFFFF);
	}

	@Override
	boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (button == 0 && isInside(mouseX, mouseY, x, y + TRACK_Y - 5, width, 14)) {
			dragging = true;
			updateFromMouse(mouseX);
			return true;
		}
		return false;
	}

	@Override
	void mouseDragged(int mouseX, int mouseY) {
		if (dragging) {
			updateFromMouse(mouseX);
		}
	}

	@Override
	void mouseReleased() {
		dragging = false;
	}

	private void updateFromMouse(int mouseX) {
		double percent = (mouseX - x - 3) / (double) Math.max(1, width - 6);
		setting.setPercent(Math.max(0.0, Math.min(1.0, percent)));
	}
}
