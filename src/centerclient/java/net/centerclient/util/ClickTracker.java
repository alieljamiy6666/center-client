package net.centerclient.util;

/**
 * Counts mouse clicks per second: how many clicks happened in the last 1000
 * milliseconds.
 *
 * Clicks come from the game's own mouse handling (on the game thread), so
 * nothing here ever runs before the game has finished starting up.
 */
public final class ClickTracker {

	public static final int LEFT = 0;
	public static final int RIGHT = 1;

	/** More than any human can click in one second. */
	private static final int CAPACITY = 64;
	private static final long WINDOW_MS = 1000L;

	private static final long[][] times = new long[2][CAPACITY];
	private static final int[] next = new int[2];

	private ClickTracker() {
	}

	/** Called by the game (on its own thread) each time a mouse button goes down while you're playing. */
	public static void onMouseDown(int button) {
		if (button != LEFT && button != RIGHT) {
			return;
		}
		times[button][next[button]] = System.currentTimeMillis();
		next[button] = (next[button] + 1) % CAPACITY;
	}

	/** How many times this button was clicked in the last second. */
	public static int getCps(int button) {
		long cutoff = System.currentTimeMillis() - WINDOW_MS;
		long[] list = times[button];
		int count = 0;
		for (int i = 0; i < CAPACITY; ++i) {
			if (list[i] > cutoff) {
				++count;
			}
		}
		return count;
	}
}
