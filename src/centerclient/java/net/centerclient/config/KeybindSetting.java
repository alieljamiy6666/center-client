package net.centerclient.config;

import net.lax1dude.eaglercraft.KeyboardConstants;

/**
 * A key, like the one you hold to zoom. Uses the key numbers from
 * KeyboardConstants (0 means no key).
 */
public class KeybindSetting extends Setting<Integer> {

	public KeybindSetting(String name, int defaultKey) {
		super(name, defaultKey);
	}

	public int getKey() {
		return value;
	}

	public boolean isBound() {
		return value != KeyboardConstants.KEY_NONE;
	}

	/** The key's name, like "C", or "None". */
	public String getKeyName() {
		return isBound() ? KeyboardConstants.getKeyName(value) : "None";
	}

	@Override
	protected Integer normalize(Integer newValue) {
		if (newValue == null || newValue < 0 || newValue > 255) {
			return null;
		}
		return newValue;
	}

	@Override
	public Object toJson() {
		return value;
	}

	@Override
	public void fromJson(Object json) {
		if (!(json instanceof Number)) {
			throw new IllegalArgumentException("not a key number: " + json);
		}
		set(((Number) json).intValue());
	}
}
