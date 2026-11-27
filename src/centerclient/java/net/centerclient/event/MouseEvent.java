package net.centerclient.event;

/**
 * Sent when a mouse button goes down or comes back up while playing
 * (not while a menu is open). Buttons: 0 = left, 1 = right, 2 = middle.
 */
public final class MouseEvent extends Event {

	private int button;
	private boolean pressed;

	MouseEvent set(int button, boolean pressed) {
		this.button = button;
		this.pressed = pressed;
		return this;
	}

	public int getButton() {
		return button;
	}

	/** True when the button went down, false when it came back up. */
	public boolean isPressed() {
		return pressed;
	}
}
