package net.centerclient.config;

/**
 * One option on a module, like "Show background" or "Scale".
 *
 * Every setting has a name, a default value and a current value. Changing it
 * tells the ConfigManager, which saves it a moment later.
 *
 * A setting can be hidden unless a switch (a {@link BooleanSetting}) is on:
 * <pre>
 *   background = add(new BooleanSetting("Background", true));
 *   opacity = add(new NumberSetting("Background opacity", 0.8, 0, 1, 0.05).showWhen(background));
 * </pre>
 */
public abstract class Setting<T> {

	private final String name;
	private final T defaultValue;
	protected T value;
	private BooleanSetting parent;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public final String getName() {
		return name;
	}

	public final T get() {
		return value;
	}

	public final T getDefault() {
		return defaultValue;
	}

	/** Changes the value (after fixing it up, for example keeping a number inside its range). */
	public final void set(T newValue) {
		T fixed = normalize(newValue);
		if (fixed == null || fixed.equals(value)) {
			return;
		}
		value = fixed;
		ConfigManager.changed();
	}

	/** Goes back to the default value. */
	public final void reset() {
		set(defaultValue);
	}

	/** Makes a value valid. Returns null if it can't be used at all. */
	protected T normalize(T newValue) {
		return newValue;
	}

	/** Only show this setting while the given switch is on. */
	@SuppressWarnings("unchecked")
	public final <S extends Setting<T>> S showWhen(BooleanSetting parentSwitch) {
		this.parent = parentSwitch;
		return (S) this;
	}

	/** True if this setting should be shown in the menu right now. */
	public final boolean isVisible() {
		return parent == null || (parent.get() && parent.isVisible());
	}

	/** The value in a form that can go into a JSON config. */
	public abstract Object toJson();

	/**
	 * Reads the value from a JSON config.
	 *
	 * @throws IllegalArgumentException if the saved value is the wrong kind
	 *                                  (the caller then falls back to the default)
	 */
	public abstract void fromJson(Object json);
}
