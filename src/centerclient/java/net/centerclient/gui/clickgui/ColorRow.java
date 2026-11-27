package net.centerclient.gui.clickgui;

import net.centerclient.config.ColorSetting;
import net.centerclient.gui.GuiUtil;
import net.centerclient.theme.Theme;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;

/**
 * A setting name and a color swatch. Click the swatch to open a color picker:
 * a square for strength and brightness, a rainbow bar for the color itself,
 * and a bar for how see-through it is.
 */
class ColorRow extends SettingRow {

	private static final int SQUARE_HEIGHT = 40;
	private static final int BAR_HEIGHT = 6;
	private static final int PICKER_HEIGHT = 4 + SQUARE_HEIGHT + 5 + BAR_HEIGHT + 5 + BAR_HEIGHT + 4;
	private static final int[] HUE_STOPS = { 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF,
			0xFFFF0000 };

	private final ColorSetting setting;
	private boolean open;
	/** 0 = nothing, 1 = square, 2 = hue bar, 3 = see-through bar. */
	private int dragging;
	/** Hue, strength, brightness. Kept here so the hue doesn't jump when the color is gray. */
	private final float[] hsv = new float[3];

	ColorRow(ColorSetting setting) {
		this.setting = setting;
	}

	@Override
	boolean isVisible() {
		return setting.isVisible();
	}

	@Override
	int getHeight() {
		return 18 + (open ? PICKER_HEIGHT : 0);
	}

	@Override
	protected void drawRow(FontRenderer font, int mouseX, int mouseY) {
		int sx = x + width - 22;
		boolean hoverSwatch = isInside(mouseX, mouseY, x, y, width, 18);
		GuiUtil.text(font, setting.getName(), x, y + 5, hoverSwatch || open ? Theme.TEXT : Theme.TEXT_FADED, false);
		GuiUtil.drawChecker(sx, y + 3, 22, 12, 3);
		RenderUtil.rect(sx, y + 3, 22, 12, setting.getColor());
		RenderUtil.roundedOutline(sx - 1, y + 2, 24, 14, 2, 1, open ? Theme.accent() : Theme.BORDER);

		if (!open) {
			return;
		}
		int top = y + 18 + 4;
		int hueColor = ColorUtil.hsvToRgb(hsv[0], 1.0F, 1.0F);

		// strength (left to right) and brightness (top to bottom)
		RenderUtil.gradientHorizontal(x, top, width, SQUARE_HEIGHT, 0xFFFFFFFF, hueColor);
		RenderUtil.gradientVertical(x, top, width, SQUARE_HEIGHT, 0x00000000, 0xFF000000);
		float px = x + hsv[1] * width;
		float py = top + (1.0F - hsv[2]) * SQUARE_HEIGHT;
		RenderUtil.roundedOutline(px - 3, py - 3, 6, 6, 3, 1, 0xFFFFFFFF);

		// rainbow bar
		int hueY = top + SQUARE_HEIGHT + 5;
		float segment = width / 6.0F;
		for (int i = 0; i < 6; ++i) {
			RenderUtil.gradientHorizontal(x + i * segment, hueY, segment + 0.5F, BAR_HEIGHT, HUE_STOPS[i],
					HUE_STOPS[i + 1]);
		}
		RenderUtil.rect(x + hsv[0] * width - 1, hueY - 1, 2, BAR_HEIGHT + 2, 0xFFFFFFFF);

		// see-through bar
		int alphaY = hueY + BAR_HEIGHT + 5;
		GuiUtil.drawChecker(x, alphaY, width, BAR_HEIGHT, 3);
		int solid = setting.getColor() | 0xFF000000;
		RenderUtil.gradientHorizontal(x, alphaY, width, BAR_HEIGHT, solid & 0x00FFFFFF, solid);
		RenderUtil.rect(x + (setting.getAlpha() / 255.0F) * width - 1, alphaY - 1, 2, BAR_HEIGHT + 2, 0xFFFFFFFF);
	}

	@Override
	boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0) {
			return false;
		}
		if (isInside(mouseX, mouseY, x, y, width, 18)) {
			open = !open;
			if (open) {
				ColorUtil.rgbToHsv(setting.getColor(), hsv);
			}
			click();
			return true;
		}
		if (!open) {
			return false;
		}
		int top = y + 18 + 4;
		int hueY = top + SQUARE_HEIGHT + 5;
		int alphaY = hueY + BAR_HEIGHT + 5;
		if (isInside(mouseX, mouseY, x, top, width, SQUARE_HEIGHT)) {
			dragging = 1;
		} else if (isInside(mouseX, mouseY, x, hueY - 2, width, BAR_HEIGHT + 4)) {
			dragging = 2;
		} else if (isInside(mouseX, mouseY, x, alphaY - 2, width, BAR_HEIGHT + 4)) {
			dragging = 3;
		} else {
			return false;
		}
		mouseDragged(mouseX, mouseY);
		return true;
	}

	@Override
	void mouseDragged(int mouseX, int mouseY) {
		if (dragging == 0) {
			return;
		}
		float fx = clamp((mouseX - x) / (float) Math.max(1, width));
		int top = y + 18 + 4;
		if (dragging == 1) {
			hsv[1] = fx;
			hsv[2] = 1.0F - clamp((mouseY - top) / (float) SQUARE_HEIGHT);
		} else if (dragging == 2) {
			hsv[0] = Math.min(fx, 0.999F);
		} else {
			setting.setAlpha(Math.round(fx * 255.0F));
			return;
		}
		setting.setRGB(ColorUtil.hsvToRgb(hsv[0], hsv[1], hsv[2]));
	}

	@Override
	void mouseReleased() {
		dragging = 0;
	}

	private static float clamp(float v) {
		return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
	}
}
