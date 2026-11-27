package net.centerclient.config;

/**
 * A number picked with a slider: it always stays between min and max, and
 * snaps to steps (a step of 0.5 allows 1.0, 1.5, 2.0...).
 */
public class NumberSetting extends Setting<Double> {

	private final double min;
	private final double max;
	private final double step;

	public NumberSetting(String name, double defaultValue, double min, double max, double step) {
		super(name, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step > 0.0 ? step : 0.0;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getStep() {
		return step;
	}

	public double getDouble() {
		return value;
	}

	public float getFloat() {
		return (float) (double) value;
	}

	public int getInt() {
		return (int) Math.round(value);
	}

	/** Where the value sits between min and max, from 0 to 1 (for drawing a slider). */
	public double getPercent() {
		return max > min ? (value - min) / (max - min) : 0.0;
	}

	/** Sets the value from a slider position between 0 and 1. */
	public void setPercent(double percent) {
		set(min + (max - min) * percent);
	}

	/** The value as text, with as many decimals as the step needs. */
	public String getDisplayValue() {
		int decimals = 0;
		double s = step;
		while (decimals < 3 && s > 0.0 && Math.abs(s - Math.rint(s)) > 1.0E-6) {
			s *= 10.0;
			++decimals;
		}
		if (decimals == 0) {
			return Long.toString(Math.round(value));
		}
		double factor = Math.pow(10, decimals);
		long scaled = Math.round(Math.abs(value) * factor);
		String digits = Long.toString(scaled);
		while (digits.length() <= decimals) {
			digits = "0" + digits;
		}
		int point = digits.length() - decimals;
		return (value < 0 ? "-" : "") + digits.substring(0, point) + "." + digits.substring(point);
	}

	@Override
	protected Double normalize(Double newValue) {
		if (newValue == null || newValue.isNaN() || newValue.isInfinite()) {
			return null;
		}
		double v = newValue;
		if (step > 0.0) {
			v = min + Math.round((v - min) / step) * step;
		}
		v = Math.max(min, Math.min(max, v));
		v = Math.round(v * 1.0E6) / 1.0E6; // remove tiny floating point leftovers like 0.30000000004
		return v;
	}

	@Override
	public Object toJson() {
		return value;
	}

	@Override
	public void fromJson(Object json) {
		if (!(json instanceof Number)) {
			throw new IllegalArgumentException("not a number: " + json);
		}
		set(((Number) json).doubleValue());
	}
}
