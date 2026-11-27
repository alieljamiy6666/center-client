package net.centerclient.gui;

import java.io.IOException;

import net.centerclient.theme.Theme;
import net.centerclient.util.Animation;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.Easing;
import net.centerclient.util.RenderUtil;
import net.centerclient.util.TextUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;

/**
 * Step 4's test screen: shows every theme color, rounded panels, the shape
 * and text helpers, the easing curves, and animated buttons.
 *
 * Opened from the temporary "Theme Test" button on the title screen.
 */
public class ThemeTestScreen extends GuiScreen {

	private static final String[] TOKEN_NAMES = { "Background", "Panel", "Panel hover", "Text", "Faded text",
			"Accent", "Success", "Danger" };
	private static final String LONG_TEXT = "This sentence is way too long to fit inside this little box";
	private static final Easing[] DEMO_EASINGS = { Easing.LINEAR, Easing.EASE_OUT, Easing.EASE_IN_OUT };
	private static final String[] DEMO_NAMES = { "linear", "ease-out", "ease-in-out" };

	private static final int SWATCH_HEIGHT = 24;
	private static final int PANEL_HEIGHT = 64;
	private static final int CONTENT_HEIGHT = 26 + 2 * (SWATCH_HEIGHT + 5) + 4 + PANEL_HEIGHT + 10 + 20;

	private final GuiScreen parent;
	private final CenterButton accentButton;
	private final CenterButton backButton;
	private final Animation intro = new Animation(350L, Easing.EASE_OUT, 0.0F);
	private final Animation switchMotion = new Animation(220L, Easing.EASE_OUT_BACK, 0.0F);
	private boolean switchOn = false;

	/** Worked out once (and again when the accent changes), not every frame. */
	private final String[] tokenHex = new String[TOKEN_NAMES.length];
	private String longTextShown;

	// layout
	private int left;
	private int top;
	private int cellWidth;
	private int panelWidth;
	private int switchX;
	private int switchY;

	public ThemeTestScreen(GuiScreen parent) {
		this.parent = parent;
		this.accentButton = new CenterButton("", true, this::nextAccent);
		this.backButton = new CenterButton("Back", false, this::goBack);
	}

	@Override
	public void initGui() {
		intro.set(0.0F);
		intro.animateTo(1.0F);
		int contentWidth = Math.min(this.width - 16, 316);
		left = (this.width - contentWidth) / 2;
		top = Math.max(6, (this.height - CONTENT_HEIGHT) / 2);
		cellWidth = (contentWidth - 18) / 4;
		panelWidth = (contentWidth - 6) / 2;
		int buttonsY = panelsY() + PANEL_HEIGHT + 10;
		accentButton.setBounds(left, buttonsY, panelWidth, 20);
		backButton.setBounds(left + panelWidth + 6, buttonsY, panelWidth, 20);
		longTextShown = TextUtil.ellipsize(this.fontRendererObj, LONG_TEXT, panelWidth - 20);
		refreshColors();
	}

	private int panelsY() {
		return top + 26 + 2 * (SWATCH_HEIGHT + 5) + 4;
	}

	private void refreshColors() {
		for (int i = 0; i < TOKEN_NAMES.length; ++i) {
			tokenHex[i] = hex(tokenColor(i));
		}
		accentButton.setLabel("Accent: " + Theme.accentName());
	}

	private void nextAccent() {
		Theme.nextAccentPreset();
		refreshColors();
	}

	private void goBack() {
		this.mc.displayGuiScreen(parent);
	}

	private static int tokenColor(int i) {
		switch (i) {
		case 0: return Theme.BACKGROUND;
		case 1: return Theme.PANEL;
		case 2: return Theme.PANEL_HOVER;
		case 3: return Theme.TEXT;
		case 4: return Theme.TEXT_FADED;
		case 5: return Theme.accent();
		case 6: return Theme.SUCCESS;
		default: return Theme.DANGER;
		}
	}

	private static String hex(int color) {
		String digits = Integer.toHexString(color & 0xFFFFFF).toUpperCase();
		StringBuilder sb = new StringBuilder("#");
		for (int i = digits.length(); i < 6; ++i) {
			sb.append('0');
		}
		return sb.append(digits).toString();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		float e = intro.get();
		RenderUtil.gradientVertical(0, 0, this.width, this.height, Theme.BACKGROUND,
				ColorUtil.mix(Theme.BACKGROUND, Theme.PANEL, 0.7F));

		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0F, (1.0F - e) * 12.0F, 0.0F); // slide up into place when opened

		TextUtil.drawCentered(this.fontRendererObj, "Center Client Theme", this.width / 2.0F, top, Theme.accent(), true);
		TextUtil.drawCentered(this.fontRendererObj, "Step 4 test screen", this.width / 2.0F, top + 11,
				Theme.TEXT_FADED, false);

		drawSwatches(top + 26);
		int py = panelsY();
		drawPanel(left, py, "Shapes & text");
		drawShapes(left + 6, py + 18);
		int rightX = left + panelWidth + 6;
		drawPanel(rightX, py, "Animations");
		drawAnimations(rightX + 6, py + 18);

		accentButton.draw(this.mc, mouseX, mouseY);
		backButton.draw(this.mc, mouseX, mouseY);

		GlStateManager.popMatrix();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private void drawSwatches(int y0) {
		for (int i = 0; i < TOKEN_NAMES.length; ++i) {
			int x = left + (i % 4) * (cellWidth + 6);
			int y = y0 + (i / 4) * (SWATCH_HEIGHT + 5);
			int color = tokenColor(i);
			RenderUtil.shadow(x, y + 1, cellWidth, SWATCH_HEIGHT, 4, 2, 0x60000000);
			RenderUtil.roundedRect(x, y, cellWidth, SWATCH_HEIGHT, 4, color);
			RenderUtil.roundedOutline(x, y, cellWidth, SWATCH_HEIGHT, 4, 1, Theme.BORDER);
			int textColor = ColorUtil.readableTextOn(color);
			this.fontRendererObj.drawString(TOKEN_NAMES[i], x + 5, y + 4, textColor, false);
			this.fontRendererObj.drawString(tokenHex[i], x + 5, y + 14, ColorUtil.withAlpha(textColor, 0.65F), false);
		}
	}

	private void drawPanel(int x, int y, String title) {
		RenderUtil.shadow(x, y + 2, panelWidth, PANEL_HEIGHT, 6, 4, 0x80000000);
		RenderUtil.roundedRect(x, y, panelWidth, PANEL_HEIGHT, 6, Theme.PANEL);
		RenderUtil.roundedOutline(x, y, panelWidth, PANEL_HEIGHT, 6, 1, Theme.BORDER);
		this.fontRendererObj.drawString(title, x + 6, y + 5, Theme.TEXT_FADED, false);
		RenderUtil.rect(x + 6, y + 15, panelWidth - 12, 1, Theme.BORDER);
	}

	private void drawShapes(int x, int y) {
		RenderUtil.circle(x + 5, y + 5, 5, Theme.accent());
		RenderUtil.roundedOutline(x + 14, y, 12, 11, 3, 1, Theme.accent());
		RenderUtil.roundedGradient(x + 31, y, 44, 11, 5, Theme.accentLight(), Theme.accentDark());
		RenderUtil.gradientHorizontal(x + 80, y + 2, 26, 7, Theme.accent(), ColorUtil.withAlpha(Theme.accent(), 0.0F));
		RenderUtil.circle(x + 115, y + 5, 4, Theme.SUCCESS);
		RenderUtil.circle(x + 126, y + 5, 4, Theme.DANGER);

		TextUtil.drawShadowed(this.fontRendererObj, "Text with a shadow", x, y + 16, Theme.TEXT);

		RenderUtil.roundedRect(x, y + 28, panelWidth - 12, 13, 3, Theme.PANEL_HOVER);
		this.fontRendererObj.drawString(longTextShown, x + 4, y + 31, Theme.TEXT_FADED, false);
	}

	private void drawAnimations(int x, int y) {
		int trackLeft = x + 58;
		int trackWidth = panelWidth - 12 - 58 - 4;
		long time = Minecraft.getSystemTime() % 2400L;
		float t = time / 1200.0F;
		float pingPong = t <= 1.0F ? t : 2.0F - t;
		for (int i = 0; i < DEMO_EASINGS.length; ++i) {
			int rowY = y + i * 10;
			this.fontRendererObj.drawString(DEMO_NAMES[i], x, rowY, Theme.TEXT_FADED, false);
			RenderUtil.roundedRect(trackLeft, rowY + 3, trackWidth, 3, 1, Theme.PANEL_HOVER);
			float dotX = trackLeft + 3 + DEMO_EASINGS[i].apply(pingPong) * (trackWidth - 6);
			RenderUtil.circle(dotX, rowY + 4.5F, 3.5F, Theme.accent());
		}

		// a switch that slides with a little bounce when clicked
		int rowY = y + 32;
		this.fontRendererObj.drawString("switch (click)", x, rowY + 1, Theme.TEXT_FADED, false);
		switchX = x + panelWidth - 12 - 24;
		switchY = rowY;
		float s = switchMotion.get();
		int track = ColorUtil.mix(Theme.BORDER, Theme.accent(), Math.max(0.0F, Math.min(1.0F, s)));
		RenderUtil.roundedRect(switchX, switchY, 22, 10, 5, track);
		RenderUtil.circle(switchX + 5 + s * 12, switchY + 5, 4, 0xFFFFFFFF);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (accentButton.mouseClicked(this.mc, mouseX, mouseY, mouseButton)) {
			return;
		}
		if (backButton.mouseClicked(this.mc, mouseX, mouseY, mouseButton)) {
			return;
		}
		if (mouseButton == 0 && mouseX >= switchX && mouseX < switchX + 22 && mouseY >= switchY
				&& mouseY < switchY + 10) {
			switchOn = !switchOn;
			switchMotion.animateTo(switchOn ? 1.0F : 0.0F);
			this.mc.getSoundHandler()
					.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 1) { // Escape
			goBack();
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
