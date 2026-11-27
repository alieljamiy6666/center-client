package net.centerclient.module.hud;

import net.centerclient.config.BooleanSetting;
import net.centerclient.hud.TextHudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.centerclient.util.ClickTracker;

/** A small display of your left (and right) clicks per second. */
public class CpsModule extends TextHudModule {

	private final BooleanSetting showRight = add(new BooleanSetting("Show right click", true));

	private int shownLeft = -1;
	private int shownRight = -1;
	private boolean shownWithRight;

	public CpsModule() {
		super("CPS Counter", "Shows how many times you click per second", Category.HUD, START, START, 0.005F, 0.25F);
	}

	@Override
	protected void update() {
		int left = ClickTracker.getCps(ClickTracker.LEFT);
		int right = ClickTracker.getCps(ClickTracker.RIGHT);
		boolean withRight = showRight.isOn();
		if (left == shownLeft && right == shownRight && withRight == shownWithRight) {
			return;
		}
		shownLeft = left;
		shownRight = right;
		shownWithRight = withRight;
		setLine(0, "CPS", withRight ? left + " | " + right : Integer.toString(left), Theme.TEXT);
	}
}
