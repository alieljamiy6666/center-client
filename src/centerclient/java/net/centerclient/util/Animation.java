package net.centerclient.util;

import net.minecraft.client.Minecraft;

/**
 * A number that glides smoothly to new values, using real time so it looks
 * the same at any frame rate.
 *
 * Make one when a screen opens (not every frame), then:
 * <pre>
 *   hover.animateTo(mouseOver ? 1 : 0);
 *   float h = hover.get(); // somewhere between 0 and 1
 * </pre>
 */
public final class Animation {

	private final long durationMs;
	private final Easing easing;
	private float from;
	private float to;
	private long startTime;

	public Animation(long durationMs, Easing easing, float startValue) {
		this.durationMs = Math.max(1L, durationMs);
		this.easing = easing;
		this.from = startValue;
		this.to = startValue;
		this.startTime = 0L;
	}

	/** Starts gliding from the current value to a new one. Does nothing if already heading there. */
	public void animateTo(float target) {
		if (target == this.to) {
			return;
		}
		this.from = get();
		this.to = target;
		this.startTime = Minecraft.getSystemTime();
	}

	/** Jumps straight to a value, with no animation. */
	public void set(float value) {
		this.from = value;
		this.to = value;
		this.startTime = 0L;
	}

	/** The current value. */
	public float get() {
		float t = progress();
		return this.from + (this.to - this.from) * this.easing.apply(t);
	}

	/** Where the animation is heading. */
	public float getTarget() {
		return this.to;
	}

	/** True once the value has arrived. */
	public boolean isDone() {
		return progress() >= 1.0F;
	}

	private float progress() {
		if (this.from == this.to) {
			return 1.0F;
		}
		float t = (float) (Minecraft.getSystemTime() - this.startTime) / (float) this.durationMs;
		return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
	}
}
