package net.centerclient.config;

/** Pick one option from a list, like "Top left / Top right / Bottom left". */
public class ModeSetting extends Setting<String> {

	private final String[] modes;

	public ModeSetting(String name, String defaultMode, String... modes) {
		super(name, defaultMode);
		this.modes = modes;
	}

	public String[] getModes() {
		return modes;
	}

	/** True if the current choice is this one. */
	public boolean is(String mode) {
		return value.equals(mode);
	}

	public int getIndex() {
		for (int i = 0; i < modes.length; ++i) {
			if (modes[i].equals(value)) {
				return i;
			}
		}
		return 0;
	}

	/** Moves to the next choice (or the previous one, with backwards = true). */
	public void cycle(boolean backwards) {
		int i = getIndex() + (backwards ? -1 : 1);
		set(modes[((i % modes.length) + modes.length) % modes.length]);
	}

	@Override
	protected String normalize(String newValue) {
		for (int i = 0; i < modes.length; ++i) {
			if (modes[i].equalsIgnoreCase(newValue)) {
				return modes[i];
			}
		}
		return null; // not one of the choices: ignore it
	}

	@Override
	public Object toJson() {
		return value;
	}

	@Override
	public void fromJson(Object json) {
		if (!(json instanceof String) || normalize((String) json) == null) {
			throw new IllegalArgumentException("not one of the choices: " + json);
		}
		set((String) json);
	}
}
