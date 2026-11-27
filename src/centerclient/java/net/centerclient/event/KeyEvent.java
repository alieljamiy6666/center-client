package net.centerclient.event;

/**
 * Sent when a key goes down or comes back up. Key numbers are the ones in
 * net.lax1dude.eaglercraft.KeyboardConstants (for example KEY_K).
 */
public final class KeyEvent extends Event {

	private int key;
	private boolean pressed;
	private boolean repeat;
	private boolean inGui;

	KeyEvent set(int key, boolean pressed, boolean repeat, boolean inGui) {
		this.key = key;
		this.pressed = pressed;
		this.repeat = repeat;
		this.inGui = inGui;
		return this;
	}

	public int getKey() {
		return key;
	}

	/** True when the key went down, false when it came back up. */
	public boolean isPressed() {
		return pressed;
	}

	/** True for the extra "still held down" events a key sends. */
	public boolean isRepeat() {
		return repeat;
	}

	/** True when a menu or chat box was open (so it's probably typing, not playing). */
	public boolean isInGui() {
		return inGui;
	}
}
