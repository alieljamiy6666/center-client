package net.centerclient.event;

/**
 * Sent 20 times a second, at the end of each game tick. Good for logic that
 * doesn't need to run every frame, like counting or checking things.
 */
public final class TickEvent extends Event {

	private boolean inWorld;

	TickEvent set(boolean inWorld) {
		this.inWorld = inWorld;
		return this;
	}

	/** True when a world is loaded (you're playing, not on a menu). */
	public boolean isInWorld() {
		return inWorld;
	}
}
