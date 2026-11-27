package net.centerclient.util;

/**
 * Easing curves: they turn "how far through the animation are we" (0 to 1)
 * into "how far should the thing have moved" (0 to 1), so motion speeds up
 * and slows down naturally instead of moving like a robot.
 */
public enum Easing {

	/** Same speed the whole way. */
	LINEAR {
		@Override
		public float apply(float t) {
			return t;
		}
	},

	/** Starts fast, then slows down gently. Good for things appearing. */
	EASE_OUT {
		@Override
		public float apply(float t) {
			float u = 1.0F - t;
			return 1.0F - u * u * u;
		}
	},

	/** Starts slow, speeds up, then slows down again. Good for sliding. */
	EASE_IN_OUT {
		@Override
		public float apply(float t) {
			if (t < 0.5F) {
				return 4.0F * t * t * t;
			}
			float u = -2.0F * t + 2.0F;
			return 1.0F - u * u * u / 2.0F;
		}
	},

	/** Goes a tiny bit too far, then settles back. Good for a bouncy "pop". */
	EASE_OUT_BACK {
		@Override
		public float apply(float t) {
			float c1 = 1.70158F;
			float c3 = c1 + 1.0F;
			float u = t - 1.0F;
			return 1.0F + c3 * u * u * u + c1 * u * u;
		}
	};

	/** @param t progress from 0 to 1 */
	public abstract float apply(float t);
}
