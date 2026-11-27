package net.centerclient.event;

/**
 * Sent every frame after the in-game HUD (hotbar, chat...) is drawn. This is
 * where HUD modules draw on the screen. Positions are in GUI pixels.
 * Not sent when the HUD is hidden with F1.
 */
public final class Render2DEvent extends Event {

	private float partialTicks;
	private int width;
	private int height;

	Render2DEvent set(float partialTicks, int width, int height) {
		this.partialTicks = partialTicks;
		this.width = width;
		this.height = height;
		return this;
	}

	/** How far we are between two ticks (0 to 1), for smooth movement. */
	public float getPartialTicks() {
		return partialTicks;
	}

	/** Screen width in GUI pixels. */
	public int getWidth() {
		return width;
	}

	/** Screen height in GUI pixels. */
	public int getHeight() {
		return height;
	}
}
