package net.centerclient.module;

/**
 * The tabs in the Center Client menu. Every module belongs to one real
 * category. "All" isn't a real category: it's a view that shows every module.
 */
public enum Category {

	ALL("All"),
	RENDER("Render"),
	HUD("HUD"),
	MISC("Misc"),
	PVP("PvP");

	private final String displayName;

	Category(String displayName) {
		this.displayName = displayName;
	}

	/** The name shown in the menu. */
	public String getDisplayName() {
		return displayName;
	}

	/** True for "All", which shows every module instead of being a real category. */
	public boolean isView() {
		return this == ALL;
	}

	/** True if a module should be listed under this tab. */
	public boolean contains(Module module) {
		return this == ALL || module.getCategory() == this;
	}
}
