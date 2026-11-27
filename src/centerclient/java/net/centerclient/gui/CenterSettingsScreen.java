package net.centerclient.gui;

import java.io.IOException;
import java.util.List;

import net.centerclient.CenterClient;
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
 * Center Settings: the accent color, profiles, and sharing configs.
 * Everything here saves by itself.
 */
public class CenterSettingsScreen extends GuiScreen {

	private static final int ACCENT_PANEL_HEIGHT = 40;
	private static final int PROFILE_PANEL_HEIGHT = 44;
	private static final int SHARE_PANEL_HEIGHT = 54;
	private static final int GAP = 6;
	private static final int CONTENT_HEIGHT = 26 + ACCENT_PANEL_HEIGHT + GAP + PROFILE_PANEL_HEIGHT + GAP
			+ SHARE_PANEL_HEIGHT + 8 + 20;
	private static final int CIRCLE_RADIUS = 7;
	private static final long STATUS_SHOW_MS = 4000L;
	private static final long RESET_CONFIRM_MS = 3000L;

	private final GuiScreen parent;
	private final Animation intro = new Animation(300L, Easing.EASE_OUT, 0.0F);

	private CenterButton[] profileButtons = new CenterButton[0];
	private final CenterButton copyButton;
	private final CenterButton pasteButton;
	private final CenterButton resetButton;
	private final CenterButton previewButton;
	private final CenterButton doneButton;

	private String status = "";
	private int statusColor = Theme.TEXT_FADED;
	private long statusUntil;
	private long resetArmedUntil;

	// layout
	private int left;
	private int top;
	private int contentWidth;
	private int circlesY;
	private int circleSpacing;
	private int circlesLeft;

	public CenterSettingsScreen(GuiScreen parent) {
		this.parent = parent;
		copyButton = new CenterButton("Copy config", false, this::copyConfig);
		pasteButton = new CenterButton("Paste config", false, this::pasteConfig);
		resetButton = new CenterButton("Reset profile", false, this::resetProfile);
		previewButton = new CenterButton("Theme preview", false,
				() -> this.mc.displayGuiScreen(new ThemeTestScreen(this)));
		doneButton = new CenterButton("Done", true, () -> this.mc.displayGuiScreen(this.parent));
	}

	@Override
	public void initGui() {
		intro.set(0.0F);
		intro.animateTo(1.0F);
		contentWidth = Math.min(this.width - 16, 300);
		left = (this.width - contentWidth) / 2;
		top = Math.max(6, (this.height - CONTENT_HEIGHT) / 2);

		int accentY = top + 26;
		circlesY = accentY + 27;
		circleSpacing = (contentWidth - 24) / Theme.ACCENT_PRESETS.length;
		circlesLeft = left + 12 + circleSpacing / 2;

		int profileY = accentY + ACCENT_PANEL_HEIGHT + GAP;
		List<String> names = CenterClient.CONFIG.getProfiles();
		int count = Math.min(names.size(), 4);
		profileButtons = new CenterButton[count];
		int pw = (contentWidth - 12 - (count - 1) * 4) / Math.max(1, count);
		for (int i = 0; i < count; ++i) {
			final String name = names.get(i);
			CenterButton b = new CenterButton(name, false, () -> switchProfile(name));
			b.setBounds(left + 6 + i * (pw + 4), profileY + 20, pw, 18);
			profileButtons[i] = b;
		}
		refreshProfileButtons();

		int shareY = profileY + PROFILE_PANEL_HEIGHT + GAP;
		int sw = (contentWidth - 12 - 8) / 3;
		copyButton.setBounds(left + 6, shareY + 20, sw, 18);
		pasteButton.setBounds(left + 6 + sw + 4, shareY + 20, sw, 18);
		resetButton.setBounds(left + 6 + (sw + 4) * 2, shareY + 20, sw, 18);

		int bottomY = shareY + SHARE_PANEL_HEIGHT + 8;
		int half = (contentWidth - 6) / 2;
		previewButton.setBounds(left, bottomY, half, 20);
		doneButton.setBounds(left + half + 6, bottomY, half, 20);
	}

	private void refreshProfileButtons() {
		String current = CenterClient.CONFIG.getCurrentProfile();
		List<String> names = CenterClient.CONFIG.getProfiles();
		for (int i = 0; i < profileButtons.length; ++i) {
			profileButtons[i].setPrimary(names.get(i).equals(current));
		}
	}

	// ---- Actions ------------------------------------------------------------------

	private void switchProfile(String name) {
		CenterClient.CONFIG.switchProfile(name);
		refreshProfileButtons();
		showStatus("Switched to " + name + ".", Theme.SUCCESS);
	}

	private void copyConfig() {
		GuiScreen.setClipboardString(CenterClient.CONFIG.exportProfile());
		showStatus("Copied! Paste it anywhere to share it.", Theme.SUCCESS);
	}

	private void pasteConfig() {
		String text = GuiScreen.getClipboardString();
		if (CenterClient.CONFIG.importProfile(text)) {
			showStatus("Loaded the config into " + CenterClient.CONFIG.getCurrentProfile() + ".", Theme.SUCCESS);
		} else {
			showStatus("That doesn't look like a Center Client config.", Theme.DANGER);
		}
	}

	private void resetProfile() {
		long now = Minecraft.getSystemTime();
		if (now < resetArmedUntil) {
			resetArmedUntil = 0L;
			resetButton.setLabel("Reset profile");
			CenterClient.CONFIG.resetCurrentProfile();
			showStatus(CenterClient.CONFIG.getCurrentProfile() + " is back to the defaults.", Theme.SUCCESS);
		} else {
			resetArmedUntil = now + RESET_CONFIRM_MS;
			resetButton.setLabel("Click again!");
			showStatus("Click Reset again to put every module in this profile back to default.", Theme.DANGER);
		}
	}

	private void showStatus(String text, int color) {
		status = text;
		statusColor = color;
		statusUntil = Minecraft.getSystemTime() + STATUS_SHOW_MS;
	}

	// ---- Drawing ------------------------------------------------------------------

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		long now = Minecraft.getSystemTime();
		if (resetArmedUntil != 0L && now >= resetArmedUntil) {
			resetArmedUntil = 0L;
			resetButton.setLabel("Reset profile");
		}

		GuiUtil.drawScreenBackground(this.width, this.height);
		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0F, (1.0F - intro.get()) * 12.0F, 0.0F);

		TextUtil.drawCentered(this.fontRendererObj, "Center Settings", this.width / 2.0F, top, Theme.accent(), true);
		TextUtil.drawCentered(this.fontRendererObj, "Everything saves by itself in this browser", this.width / 2.0F,
				top + 11, Theme.TEXT_FADED, false);

		// accent color
		int accentY = top + 26;
		GuiUtil.drawPanel(this.fontRendererObj, left, accentY, contentWidth, ACCENT_PANEL_HEIGHT,
				"Accent color: " + Theme.accentName());
		for (int i = 0; i < Theme.ACCENT_PRESETS.length; ++i) {
			int cx = circlesLeft + i * circleSpacing;
			boolean selected = i == Theme.getAccentPreset();
			boolean hover = isOverCircle(mouseX, mouseY, i);
			if (selected) {
				RenderUtil.circle(cx, circlesY, CIRCLE_RADIUS + 2, 0xFFFFFFFF);
				RenderUtil.circle(cx, circlesY, CIRCLE_RADIUS + 1, Theme.PANEL);
			}
			RenderUtil.circle(cx, circlesY, hover ? CIRCLE_RADIUS + 0.5F : CIRCLE_RADIUS, Theme.ACCENT_PRESETS[i]);
		}

		// profiles
		int profileY = accentY + ACCENT_PANEL_HEIGHT + GAP;
		GuiUtil.drawPanel(this.fontRendererObj, left, profileY, contentWidth, PROFILE_PANEL_HEIGHT,
				"Profile (each one remembers its own modules)");
		for (int i = 0; i < profileButtons.length; ++i) {
			profileButtons[i].draw(this.mc, mouseX, mouseY);
		}

		// share
		int shareY = profileY + PROFILE_PANEL_HEIGHT + GAP;
		GuiUtil.drawPanel(this.fontRendererObj, left, shareY, contentWidth, SHARE_PANEL_HEIGHT, "Share config");
		copyButton.draw(this.mc, mouseX, mouseY);
		pasteButton.draw(this.mc, mouseX, mouseY);
		resetButton.draw(this.mc, mouseX, mouseY);
		if (now < statusUntil) {
			float fade = Math.min(1.0F, (statusUntil - now) / 400.0F);
			String shown = TextUtil.ellipsize(this.fontRendererObj, status, contentWidth - 12);
			this.fontRendererObj.drawString(shown, left + 6, shareY + 42, ColorUtil.withAlpha(statusColor, fade),
					false);
		}

		previewButton.draw(this.mc, mouseX, mouseY);
		doneButton.draw(this.mc, mouseX, mouseY);

		GlStateManager.popMatrix();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private boolean isOverCircle(int mouseX, int mouseY, int index) {
		int dx = mouseX - (circlesLeft + index * circleSpacing);
		int dy = mouseY - circlesY;
		return dx * dx + dy * dy <= (CIRCLE_RADIUS + 1) * (CIRCLE_RADIUS + 1);
	}

	// ---- Input --------------------------------------------------------------------

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (mouseButton == 0) {
			for (int i = 0; i < Theme.ACCENT_PRESETS.length; ++i) {
				if (isOverCircle(mouseX, mouseY, i)) {
					Theme.setAccentPreset(i);
					this.mc.getSoundHandler()
							.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					return;
				}
			}
		}
		for (int i = 0; i < profileButtons.length; ++i) {
			if (profileButtons[i].mouseClicked(this.mc, mouseX, mouseY, mouseButton)) {
				return;
			}
		}
		if (copyButton.mouseClicked(this.mc, mouseX, mouseY, mouseButton)
				|| pasteButton.mouseClicked(this.mc, mouseX, mouseY, mouseButton)
				|| resetButton.mouseClicked(this.mc, mouseX, mouseY, mouseButton)
				|| previewButton.mouseClicked(this.mc, mouseX, mouseY, mouseButton)
				|| doneButton.mouseClicked(this.mc, mouseX, mouseY, mouseButton)) {
			return;
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 1) { // Escape
			this.mc.displayGuiScreen(parent);
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
