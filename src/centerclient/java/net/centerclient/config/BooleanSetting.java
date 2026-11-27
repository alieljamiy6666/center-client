package net.centerclient.config;

/** An on/off switch. */
public class BooleanSetting extends Setting<Boolean> {

	public BooleanSetting(String name, boolean defaultValue) {
		super(name, defaultValue);
	}

	public boolean isOn() {
		return value;
	}

	public void toggle() {
		set(!value);
	}

	@Override
	public Object toJson() {
		return value;
	}

	@Override
	public void fromJson(Object json) {
		if (!(json instanceof Boolean)) {
			throw new IllegalArgumentException("not true/false: " + json);
		}
		set((Boolean) json);
	}
}
