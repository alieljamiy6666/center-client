package net.centerclient.gui.clickgui;

import java.util.List;

import net.centerclient.gui.GuiUtil;
import net.centerclient.gui.PixelIcon;
import net.centerclient.module.Module;
import net.centerclient.theme.Theme;
import net.centerclient.util.Animation;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.Easing;
import net.centerclient.util.RenderUtil;
import net.centerclient.util.TextUtil;
import net.minecraft.client.gui.FontRenderer;

/**
 * One module's card in the menu grid: its name, a short description, an
 * on/off switch and a gear button for its settings. Click the card to turn
 * the module on or off; click the gear to open its settings.
 */
class ModuleCard {

	static final int HEIGHT = 48;
	private static final int RADIUS = 6;

	final Module module;
	private final Animation hover = new Animation(140L, Easing.EASE_OUT, 0.0F);
	private final Animation gearHover = new Animation(140L, Easing.EASE_OUT, 0.0F);
	private final Animation toggle;

	// position inside the grid (before scrolling)
	float x;
	float y;
	float width;

	/** Text cut to fit, only redone when the card's width changes. */
	private String shownName = "";
	private String line1 = "";
	private String line2 = "";
	private int textWidth = -1;

	ModuleCard(Module module) {
		this.module = module;
		this.toggle = new Animation(220L, Easing.EASE_OUT_BACK, module.isEnabled() ? 1.0F : 0.0F);
	}

	boolean isOver(int mouseX, int mouseY, float scroll) {
		float top = y - scroll;
		return mouseX >= x && mouseX < x + width && mouseY >= top && mouseY < top + HEIGHT;
	}

	boolean isOverGear(int mouseX, int mouseY, float scroll) {
		float gx = x + width - 18;
		float gy = y - scroll + 3;
		return mouseX >= gx && mouseX < gx + 15 && mouseY >= gy && mouseY < gy + 14;
	}

	void draw(FontRenderer font, int mouseX, int mouseY, float scroll, boolean selected) {
		boolean over = isOver(mouseX, mouseY, scroll);
		boolean overGear = over && isOverGear(mouseX, mouseY, scroll);
		hover.animateTo(over ? 1.0F : 0.0F);
		gearHover.animateTo(overGear ? 1.0F : 0.0F);
		toggle.animateTo(module.isEnabled() ? 1.0F : 0.0F);
		float h = hover.get();
		float on = Math.min(1.0F, Math.max(0.0F, toggle.get()));
		float top = y - scroll - h; // lifts up a pixel when hovered

		int inner = (int) width - 16;
		if (inner != textWidth) {
			textWidth = inner;
			shownName = TextUtil.ellipsize(font, module.getName(), inner - 16);
			List<String> lines = font.listFormattedStringToWidth(module.getDescription(), inner - 22);
			line1 = lines.isEmpty() ? "" : lines.get(0);
			line2 = lines.size() < 2 ? "" : (lines.size() == 2 ? lines.get(1)
					: TextUtil.ellipsize(font, lines.get(1) + " " + lines.get(2), inner - 22));
		}

		RenderUtil.shadow(x, top + 1 + h, width, HEIGHT, RADIUS, 3, 0x60000000);
		if (on > 0.02F) {
			RenderUtil.shadow(x, top, width, HEIGHT, RADIUS, 4, ColorUtil.withAlpha(Theme.accent(), 0.35F * on));
		}
		int fillTop = ColorUtil.mix(Theme.PANEL, Theme.PANEL_HOVER, h);
		int fillBottom = ColorUtil.mix(Theme.PANEL, ColorUtil.mix(Theme.PANEL, Theme.accentDark(), 0.25F), on);
		RenderUtil.roundedGradient(x, top, width, HEIGHT, RADIUS, fillTop, fillBottom);
		int border = ColorUtil.mix(ColorUtil.mix(Theme.BORDER, Theme.TEXT_FADED, h * 0.4F), Theme.accent(),
				Math.max(on * 0.8F, selected ? 1.0F : 0.0F));
		RenderUtil.roundedOutline(x, top, width, HEIGHT, RADIUS, 1, border);

		GuiUtil.text(font, shownName, x + 8, top + 7, ColorUtil.mix(Theme.TEXT, Theme.accent(), on), true);
		GuiUtil.text(font, line1, x + 8, top + 20, Theme.TEXT_FADED, false);
		GuiUtil.text(font, line2, x + 8, top + 30, Theme.TEXT_FADED, false);

		int gearColor = ColorUtil.mix(Theme.TEXT_FADED, Theme.accent(), gearHover.get());
		if (gearHover.get() > 0.02F) {
			RenderUtil.roundedRect(x + width - 18, top + 3, 15, 14, 3,
					ColorUtil.withAlpha(Theme.accent(), 0.15F * gearHover.get()));
		}
		PixelIcon.GEAR.draw(x + width - 14, top + 6.5F, 1.0F, gearColor);

		GuiUtil.drawSwitch(x + width - 27, top + HEIGHT - 15, toggle.get());
	}
}
