package net.centerclient.gui;

import net.centerclient.theme.Theme;
import net.centerclient.util.Animation;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.Easing;
import net.centerclient.util.RenderUtil;
import net.centerclient.util.TextUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

/**
 * A rounded Center Client button. When the mouse is over it, it grows a
 * little and glows in the accent color. When clicked, it squishes for a moment.
 *
 * "Primary" buttons are filled with the accent color; normal ones use the
 * panel color. Make buttons when a screen opens, not every frame.
 */
public class CenterButton {

	private static final int RADIUS = 4;

	private float x;
	private float y;
	private float width;
	private float height;
	private String label;
	private boolean primary;
	private final Runnable onClick;

	private final Animation hover = new Animation(160L, Easing.EASE_OUT, 0.0F);
	private final Animation press = new Animation(90L, Easing.EASE_OUT, 0.0F);

	/** The label after cutting it to fit, kept so it isn't re-cut every frame. */
	private String shownLabel;
	private int shownLabelWidth = -1;

	public CenterButton(String label, boolean primary, Runnable onClick) {
		this.label = label;
		this.primary = primary;
		this.onClick = onClick;
	}

	public void setBounds(float x, float y, float width, float height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void setLabel(String label) {
		if (!label.equals(this.label)) {
			this.label = label;
			this.shownLabelWidth = -1;
		}
	}

	/** Primary buttons are filled with the accent color (for example, the selected profile). */
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public boolean isMouseOver(int mouseX, int mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	public void draw(Minecraft mc, int mouseX, int mouseY) {
		hover.animateTo(isMouseOver(mouseX, mouseY) ? 1.0F : 0.0F);
		if (press.getTarget() == 1.0F && press.isDone()) {
			press.animateTo(0.0F); // squish, then spring back
		}
		float h = hover.get();
		float p = press.get();

		// grow when hovered, shrink a little when pressed, around the button's middle
		float scale = 1.0F + 0.06F * h - 0.05F * p;
		float centerX = x + width / 2.0F;
		float centerY = y + height / 2.0F;
		GlStateManager.pushMatrix();
		GlStateManager.translate(centerX, centerY, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		GlStateManager.translate(-centerX, -centerY, 0.0F);

		// a soft dark shadow, plus an accent glow that fades in on hover
		RenderUtil.shadow(x, y + 1.0F, width, height, RADIUS, 3, 0x70000000);
		if (h > 0.02F) {
			RenderUtil.shadow(x, y, width, height, RADIUS, 5, ColorUtil.withAlpha(Theme.accent(), 0.55F * h));
		}

		int top;
		int bottom;
		int border;
		int textColor;
		if (primary) {
			top = ColorUtil.mix(Theme.accent(), Theme.accentLight(), 0.1F + 0.3F * h);
			bottom = ColorUtil.mix(Theme.accent(), Theme.accentDark(), 0.45F - 0.25F * h);
			border = ColorUtil.mix(Theme.accentDark(), Theme.accentLight(), h);
			textColor = ColorUtil.readableTextOn(Theme.accent());
		} else {
			top = ColorUtil.mix(Theme.PANEL_HOVER, ColorUtil.mix(Theme.PANEL_HOVER, Theme.accent(), 0.18F), h);
			bottom = ColorUtil.mix(Theme.PANEL, Theme.PANEL_HOVER, h);
			border = ColorUtil.mix(Theme.BORDER, Theme.accent(), h);
			textColor = Theme.TEXT;
		}
		RenderUtil.roundedGradient(x, y, width, height, RADIUS, top, bottom);
		RenderUtil.roundedOutline(x, y, width, height, RADIUS, 1, border);

		int maxTextWidth = (int) width - 8;
		if (shownLabelWidth != maxTextWidth) {
			shownLabel = TextUtil.ellipsize(mc.fontRendererObj, label, maxTextWidth);
			shownLabelWidth = maxTextWidth;
		}
		boolean darkText = ColorUtil.brightness(textColor) < 0.5F;
		TextUtil.drawCentered(mc.fontRendererObj, shownLabel, centerX, centerY - 4.0F, textColor, !darkText);

		GlStateManager.popMatrix();
	}

	/** Call from the screen's mouseClicked. Returns true if this button was clicked. */
	public boolean mouseClicked(Minecraft mc, int mouseX, int mouseY, int mouseButton) {
		if (mouseButton != 0 || !isMouseOver(mouseX, mouseY)) {
			return false;
		}
		press.set(0.0F);
		press.animateTo(1.0F);
		mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		if (onClick != null) {
			onClick.run();
		}
		return true;
	}
}
