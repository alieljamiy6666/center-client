package net.centerclient.event;

/**
 * Sent every frame after the world is drawn and before your hand is drawn.
 * Things drawn here appear inside the 3D world, like block outlines.
 */
public final class Render3DEvent extends Event {

	private float partialTicks;

	Render3DEvent set(float partialTicks) {
		this.partialTicks = partialTicks;
		return this;
	}

	/** How far we are between two ticks (0 to 1), for smooth movement. */
	public float getPartialTicks() {
		return partialTicks;
	}
}
