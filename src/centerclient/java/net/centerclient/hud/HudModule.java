package net.centerclient.hud;

import org.json.JSONObject;

import net.centerclient.config.BooleanSetting;
import net.centerclient.config.ConfigManager;
import net.centerclient.config.NumberSetting;
import net.centerclient.event.Render2DEvent;
import net.centerclient.module.Category;
import net.centerclient.module.Module;
import net.centerclient.theme.Theme;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.RenderUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;

/**
 * A module that shows something on the HUD, like FPS or coordinates, and can
 * be moved and resized in the HUD editor.
 *
 * How the position is saved: as fractions of the screen size (0 = left/top
 * edge, 1 = right/bottom edge), plus an "anchor" that says which side of the
 * element the fraction points at. Elements near the right edge are anchored by
 * their right side, so they stay stuck to that edge when the window changes
 * size or their text gets longer. Elements in the middle stay in the middle.
 *
 * To make one, give it a size and draw it at 0,0:
 * <pre>
 * public int getWidth()  { return 60; }
 * public int getHeight() { return 15; }
 * protected void draw(boolean editing) { ... }
 * </pre>
 * The base class handles position, scale and the background.
 */
public abstract class HudModule extends Module {

	/** Anchors: 0 = left/top, 1 = center/middle, 2 = right/bottom. */
	public static final int START = 0;
	public static final int CENTER = 1;
	public static final int END = 2;

	public static final float MIN_SCALE = 0.5F;
	public static final float MAX_SCALE = 2.0F;

	protected final BooleanSetting background = add(new BooleanSetting("Background", true));
	protected final NumberSetting scale = add(new NumberSetting("Scale", 1.0, MIN_SCALE, MAX_SCALE, 0.05));

	private final int defaultAnchorX;
	private final int defaultAnchorY;
	private final float defaultX;
	private final float defaultY;
	private int anchorX;
	private int anchorY;
	private float relX;
	private float relY;

	/**
	 * @param anchorX  START, CENTER or END (left, middle, right)
	 * @param anchorY  START, CENTER or END (top, middle, bottom)
	 * @param defaultX where the anchor point starts, as a fraction of the screen width
	 * @param defaultY where the anchor point starts, as a fraction of the screen height
	 */
	protected HudModule(String name, String description, Category category, int anchorX, int anchorY,
			float defaultX, float defaultY) {
		this(name, description, category, NO_KEY, anchorX, anchorY, defaultX, defaultY);
	}

	/** Same as above, with a default toggle key. */
	protected HudModule(String name, String description, Category category, int defaultKey, int anchorX,
			int anchorY, float defaultX, float defaultY) {
		super(name, description, category, defaultKey);
		this.defaultAnchorX = anchorX;
		this.defaultAnchorY = anchorY;
		this.defaultX = defaultX;
		this.defaultY = defaultY;
		resetPosition();
		listen(Render2DEvent.class, this::onRender2D);
	}

	/** Width in GUI pixels at scale 1. */
	public abstract int getWidth();

	/** Height in GUI pixels at scale 1. */
	public abstract int getHeight();

	/**
	 * Draws the element with its top-left corner at 0,0 (position and scale are
	 * already applied). editing is true inside the HUD editor, where an element
	 * can show example values if it has nothing real to show.
	 */
	protected abstract void draw(boolean editing);

	/**
	 * True when there's nothing to show right now (for example, no potion
	 * effects). Empty elements aren't drawn at all, except in the HUD editor.
	 */
	public boolean isEmpty() {
		return false;
	}

	/** The background color. Override to change it. */
	protected int getBackgroundColor() {
		return ColorUtil.withAlpha(Theme.PANEL, 0.75F);
	}

	private void onRender2D(Render2DEvent event) {
		if (mc.currentScreen instanceof HudEditorScreen) {
			return; // the editor draws the elements itself
		}
		render(event.getWidth(), event.getHeight(), false);
	}

	/** Draws the element where it belongs on a screen of this size. */
	public final void render(int screenWidth, int screenHeight, boolean editing) {
		if (!editing && isEmpty()) {
			return;
		}
		float s = getScale();
		GlStateManager.pushMatrix();
		GlStateManager.translate(getScreenX(screenWidth), getScreenY(screenHeight), 0.0F);
		GlStateManager.scale(s, s, 1.0F);
		if (background.isOn()) {
			RenderUtil.roundedRect(0, 0, getWidth(), getHeight(), 4, getBackgroundColor());
		}
		draw(editing);
		GlStateManager.popMatrix();
	}

	// ---- Position and size ------------------------------------------------------

	public float getScale() {
		return scale.getFloat();
	}

	public void setScale(float newScale) {
		scale.set((double) Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale)));
	}

	public float getScaledWidth() {
		return getWidth() * getScale();
	}

	public float getScaledHeight() {
		return getHeight() * getScale();
	}

	/** Left edge on a screen this wide, always fully on screen. */
	public float getScreenX(int screenWidth) {
		float w = getScaledWidth();
		float x = relX * screenWidth - w * anchorX * 0.5F;
		return Math.max(0.0F, Math.min(screenWidth - w, x));
	}

	/** Top edge on a screen this tall, always fully on screen. */
	public float getScreenY(int screenHeight) {
		float h = getScaledHeight();
		float y = relY * screenHeight - h * anchorY * 0.5F;
		return Math.max(0.0F, Math.min(screenHeight - h, y));
	}

	/**
	 * Moves the element so its top-left corner is at x, y. The anchor is picked
	 * from where it ends up: the left, middle or right third of the screen.
	 */
	public void setScreenPosition(float x, float y, int screenWidth, int screenHeight) {
		float w = getScaledWidth();
		float h = getScaledHeight();
		x = Math.max(0.0F, Math.min(screenWidth - w, x));
		y = Math.max(0.0F, Math.min(screenHeight - h, y));
		anchorX = pickAnchor(x + w / 2.0F, screenWidth);
		anchorY = pickAnchor(y + h / 2.0F, screenHeight);
		relX = (x + w * anchorX * 0.5F) / Math.max(1, screenWidth);
		relY = (y + h * anchorY * 0.5F) / Math.max(1, screenHeight);
		ConfigManager.changed();
	}

	private static int pickAnchor(float middle, int size) {
		if (middle < size / 3.0F) {
			return START;
		}
		return middle > size * 2.0F / 3.0F ? END : CENTER;
	}

	/** Puts the element back where it started, at normal size. */
	public void resetLayout() {
		resetPosition();
		scale.reset();
		ConfigManager.changed();
	}

	private void resetPosition() {
		anchorX = defaultAnchorX;
		anchorY = defaultAnchorY;
		relX = defaultX;
		relY = defaultY;
	}

	// ---- Saving -----------------------------------------------------------------

	@Override
	protected void saveExtra(JSONObject json) {
		json.put("x", (double) relX);
		json.put("y", (double) relY);
		json.put("anchorX", anchorX);
		json.put("anchorY", anchorY);
	}

	@Override
	protected void loadExtra(JSONObject json) {
		double x = json.optDouble("x", Double.NaN);
		double y = json.optDouble("y", Double.NaN);
		int ax = json.optInt("anchorX", -1);
		int ay = json.optInt("anchorY", -1);
		if (x >= 0.0 && x <= 1.0 && y >= 0.0 && y <= 1.0 && ax >= START && ax <= END && ay >= START && ay <= END) {
			relX = (float) x;
			relY = (float) y;
			anchorX = ax;
			anchorY = ay;
		} else {
			resetPosition(); // broken or missing: use the default spot
		}
	}

	@Override
	protected void resetExtra() {
		resetPosition();
	}

	/** True for HUD modules: used by the menu and editor. */
	public static boolean isHud(Module module) {
		return module instanceof HudModule;
	}

	/** Helper for subclasses that don't need a different category. */
	protected static Category hud() {
		return Category.HUD;
	}
}
