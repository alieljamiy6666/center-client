package net.centerclient.gui.clickgui;

import net.centerclient.config.BooleanSetting;
import net.centerclient.gui.GuiUtil;
import net.centerclient.theme.Theme;
import net.centerclient.util.Animation;
import net.centerclient.util.Easing;
import net.minecraft.client.gui.FontRenderer;

/** A setting name with an on/off switch. Click anywhere on the row to flip it. */
class BooleanRow extends SettingRow {

	private final BooleanSetting setting;
	private final Animation knob;

	BooleanRow(BooleanSetting setting) {
		this.setting = setting;
		this.knob = new Animation(200L, Easing.EASE_OUT_BACK, setting.isOn() ? 1.0F : 0.0F);
	}

	@Override
	boolean isVisible() {
		return setting.isVisible();
	}

	@Override
	int getHeight() {
		return 18;
	}

	@Override
	protected void drawRow(FontRenderer font, int mouseX, int mouseY) {
		knob.animateTo(setting.isOn() ? 1.0F : 0.0F);
		boolean hover = isInside(mouseX, mouseY, x, y, width, 18);
		GuiUtil.text(font, setting.getName(), x, y + 5, hover ? Theme.TEXT : Theme.TEXT_FADED, false);
		GuiUtil.drawSwitch(x + width - 20, y + 4, knob.get());
	}

	@Override
	boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (button == 0 && isInside(mouseX, mouseY, x, y, width, 18)) {
			setting.toggle();
			click();
			return true;
		}
		return false;
	}
}
