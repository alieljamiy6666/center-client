package net.centerclient.util;

import net.minecraft.client.Minecraft;

/**
 * Counts mouse clicks per second, using the real time of every click.
 *
 * In the browser, the click time is written down the moment the browser
 * reports the click (see the "// Center Client" line in PlatformInput), so even
 * very fast clicks between two game ticks or two frames are counted with their
 * exact time. CPS = how many clicks happened in the last 1000 milliseconds.
 *
 * Builds without that hook (the desktop test runtime) fall back to the game's
 * own mouse events.
 */
public final class ClickTracker {

	public static final int LEFT = 0;
	public static final int RIGHT = 1;

	/** More than any human can click in one second. */
	private static final int CAPACITY = 64;
	private static final long WINDOW_MS = 1000L;

	private static final long[][] times = new long[2][CAPACITY];
	private static final int[] next = new int[2];
	private static boolean browserHook;

	private ClickTracker() {
	}

	/** Called by the browser's mouse-down handler, the instant a button goes down. */
	public static void onBrowserMouseDown(int button) {
		browserHook = true;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc != null && mc.currentScreen == null) { // only count clicks while playing
			record(button);
		}
	}

	/** Called from the game's mouse loop. Only used when the browser hook isn't there. */
	public static void onGameMouseDown(int button) {
		if (!browserHook) {
			record(button);
		}
	}

	private static void record(int button) {
		if (button != LEFT && button != RIGHT) {
			return;
		}
		times[button][next[button]] = System.currentTimeMillis();
		next[button] = (next[button] + 1) % CAPACITY;
	}

	/** Clicks of this button (LEFT or RIGHT) in the last second. */
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
