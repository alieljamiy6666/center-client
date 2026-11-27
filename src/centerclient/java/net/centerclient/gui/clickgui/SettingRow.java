package net.centerclient.gui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.init.SoundEvents;

/**
 * One line in the settings panel (a switch, a slider, a dropdown...).
 *
 * Rows remember where they were last drawn, so clicks can be checked against
 * that spot even though the panel scrolls.
 */
abstract class SettingRow {

	protected int x;
	protected int y;
	protected int width;

	/** False if the row's setting is hidden right now (see Setting.showWhen). */
	abstract boolean isVisible();

	/** Height in GUI pixels (can change, for example when a dropdown opens). */
	abstract int getHeight();

	/** Draws the row and remembers where it was drawn. */
	final void draw(FontRenderer font, int x, int y, int width, int mouseX, int mouseY) {
		setBounds(x, y, width);
		drawRow(font, mouseX, mouseY);
	}

	/** Remembers where the row is without drawing it (when it's scrolled out of view). */
	final void setBounds(int x, int y, int width) {
		this.x = x;
		this.y = y;
		this.width = width;
	}

	protected abstract void drawRow(FontRenderer font, int mouseX, int mouseY);

	/** Returns true if the click was used. */
	boolean mouseClicked(int mouseX, int mouseY, int button) {
		return false;
	}

	void mouseDragged(int mouseX, int mouseY) {
	}

	void mouseReleased() {
	}

	/** Returns true if the key was used. */
	boolean keyTyped(char typedChar, int keyCode) {
		return false;
	}

	/** True while the row wants the next key press (a keybind button waiting for a key). */
	boolean isListening() {
		return false;
	}

	protected boolean isInside(int mouseX, int mouseY, int rx, int ry, int rw, int rh) {
		return mouseX >= rx && mouseX < rx + rw && mouseY >= ry && mouseY < ry + rh;
	}

	protected static void click() {
		Minecraft.getMinecraft().getSoundHandler()
				.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}
}
