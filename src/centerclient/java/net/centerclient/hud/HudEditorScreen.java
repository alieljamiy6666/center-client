package net.centerclient.hud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.centerclient.CenterClient;
import net.centerclient.gui.CenterButton;
import net.centerclient.gui.GuiUtil;
import net.centerclient.module.Module;
import net.centerclient.theme.Theme;
import net.centerclient.util.Animation;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.Easing;
import net.centerclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Keyboard;
import net.lax1dude.eaglercraft.KeyboardConstants;
import net.lax1dude.eaglercraft.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * The HUD editor: drag HUD elements wherever you want them.
 *
 * - Drag an element to move it. It snaps to the screen edges, the middle of
 *   the screen and the edges of other elements, and shows a guide line when
 *   it does. Hold Shift to move freely without snapping.
 * - Scroll over an element, or drag its corner handle, to make it bigger or smaller.
 * - Arrow keys nudge the last element you touched by one pixel.
 * - "Reset layout" puts everything back where it started (click it twice).
 *
 * Only HUD modules that are turned on show up here.
 */
public class HudEditorScreen extends GuiScreen {

	private static final float SNAP_DISTANCE = 4.0F;
	private static final float EDGE_MARGIN = 2.0F;
	private static final int HANDLE_SIZE = 5;
	private static final long RESET_CONFIRM_MS = 3000L;

	private final GuiScreen parent;
	private final List<HudModule> elements = new ArrayList<HudModule>();
	private final CenterButton resetButton;
	private final CenterButton doneButton;
	private final Animation intro = new Animation(250L, Easing.EASE_OUT, 0.0F);
	private final Animation toolbarFade = new Animation(150L, Easing.EASE_OUT, 1.0F);
	private long resetArmedUntil;

	private HudModule dragging;
	private HudModule resizing;
	private HudModule selected;
	private float dragOffsetX;
	private float dragOffsetY;

	// guide lines for the current snap (at most one up-down and one side-to-side)
	private float guideX = -1.0F;
	private float guideY = -1.0F;

	private int toolbarY;

	public HudEditorScreen(GuiScreen parent) {
		this.parent = parent;
		resetButton = new CenterButton("Reset layout", false, this::resetLayout);
		doneButton = new CenterButton("Done", true, () -> this.mc.displayGuiScreen(this.parent));
	}

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		intro.set(0.0F);
		intro.animateTo(1.0F);
		elements.clear();
		List<Module> modules = CenterClient.MODULES.getModules();
		for (int i = 0; i < modules.size(); ++i) {
			Module m = modules.get(i);
			if (m instanceof HudModule && m.isEnabled()) {
				elements.add((HudModule) m);
			}
		}
		toolbarY = this.height - 70; // just above the hotbar, out of the way
		int cx = this.width / 2;
		resetButton.setBounds(cx - 82, toolbarY, 80, 18);
		doneButton.setBounds(cx + 2, toolbarY, 80, 18);
	}

	// ---- Drawing --------------------------------------------------------------------

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		long now = Minecraft.getSystemTime();
		if (resetArmedUntil != 0L && now >= resetArmedUntil) {
			resetArmedUntil = 0L;
			resetButton.setLabel("Reset layout");
		}
		if (this.mc.world != null) {
			RenderUtil.rect(0, 0, this.width, this.height, 0x55000000);
		} else {
			GuiUtil.drawScreenBackground(this.width, this.height);
		}

		// faint center lines, brighter guide lines while snapping
		int faint = ColorUtil.withAlpha(0xFFFFFFFF, 0.08F);
		RenderUtil.rect(this.width / 2.0F, 0, 1, this.height, faint);
		RenderUtil.rect(0, this.height / 2.0F, this.width, 1, faint);
		if (guideX >= 0.0F) {
			RenderUtil.rect(guideX - 0.5F, 0, 1, this.height, Theme.accent());
		}
		if (guideY >= 0.0F) {
			RenderUtil.rect(0, guideY - 0.5F, this.width, 1, Theme.accent());
		}

		HudModule hovered = dragging != null || resizing != null ? null : elementAt(mouseX, mouseY);
		for (int i = 0; i < elements.size(); ++i) {
			HudModule e = elements.get(i);
			e.render(this.width, this.height, true);
			float x = e.getScreenX(this.width);
			float y = e.getScreenY(this.height);
			float w = e.getScaledWidth();
			float h = e.getScaledHeight();
			boolean active = e == dragging || e == resizing;
			int outline = active || e == selected ? Theme.accent()
					: (e == hovered ? ColorUtil.withAlpha(0xFFFFFFFF, 0.7F) : ColorUtil.withAlpha(0xFFFFFFFF, 0.25F));
			RenderUtil.roundedOutline(x - 1, y - 1, w + 2, h + 2, 2, 1, outline);
			if (active || e == hovered || e == selected) {
				RenderUtil.rect(x + w - HANDLE_SIZE + 1, y + h - HANDLE_SIZE + 1, HANDLE_SIZE, HANDLE_SIZE, Theme.accent());
				String label = e.getName() + "  " + Math.round(e.getScale() * 100) + "%";
				float ly = y - 11 >= 0 ? y - 11 : y + h + 3;
				GuiUtil.text(this.fontRendererObj, label, x, ly, active ? Theme.accent() : Theme.TEXT, true);
			}
		}

		// toolbar above the hotbar; it fades in, and almost disappears while you drag
		toolbarFade.animateTo(dragging != null || resizing != null ? 0.15F : 1.0F);
		float e = intro.get() * toolbarFade.get();
		RenderUtil.setGlobalAlpha(e);
		int cx = this.width / 2;
		String hint = "Drag to move  -  Scroll or drag the corner to resize  -  Shift: no snapping";
		int hintW = this.fontRendererObj.getStringWidth(hint);
		RenderUtil.roundedRect(cx - Math.max(90, hintW / 2 + 8), toolbarY - 16, Math.max(180, hintW + 16), 40, 6,
				ColorUtil.withAlpha(Theme.PANEL, 0.85F));
		GuiUtil.textCentered(this.fontRendererObj, hint, cx, toolbarY - 12, Theme.TEXT_FADED, false);
		resetButton.draw(this.mc, mouseX, mouseY);
		doneButton.draw(this.mc, mouseX, mouseY);
		if (elements.isEmpty()) {
			GuiUtil.textCentered(this.fontRendererObj, "No HUD modules are on. Turn some on in the Right Shift menu.",
					cx, this.height / 2 - 4, Theme.TEXT, true);
		}
		RenderUtil.setGlobalAlpha(1.0F);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	/** The top-most element under the mouse, or null. */
	private HudModule elementAt(int mouseX, int mouseY) {
		for (int i = elements.size() - 1; i >= 0; --i) {
			HudModule e = elements.get(i);
			float x = e.getScreenX(this.width);
			float y = e.getScreenY(this.height);
			if (mouseX >= x - 1 && mouseX < x + e.getScaledWidth() + 1 && mouseY >= y - 1
					&& mouseY < y + e.getScaledHeight() + 1) {
				return e;
			}
		}
		return null;
	}

	private boolean isOverHandle(HudModule e, int mouseX, int mouseY) {
		float hx = e.getScreenX(this.width) + e.getScaledWidth() - HANDLE_SIZE + 1;
		float hy = e.getScreenY(this.height) + e.getScaledHeight() - HANDLE_SIZE + 1;
		return mouseX >= hx - 2 && mouseX < hx + HANDLE_SIZE + 2 && mouseY >= hy - 2 && mouseY < hy + HANDLE_SIZE + 2;
	}

	// ---- Input ----------------------------------------------------------------------

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (resetButton.mouseClicked(this.mc, mouseX, mouseY, mouseButton)
				|| doneButton.mouseClicked(this.mc, mouseX, mouseY, mouseButton)) {
			return;
		}
		if (mouseButton != 0) {
			return;
		}
		// check handles first, so a handle on top of another element still works
		for (int i = elements.size() - 1; i >= 0; --i) {
			HudModule e = elements.get(i);
			if ((e == selected || elementAt(mouseX, mouseY) == e) && isOverHandle(e, mouseX, mouseY)) {
				resizing = e;
				selected = e;
				return;
			}
		}
		HudModule e = elementAt(mouseX, mouseY);
		selected = e;
		if (e != null) {
			dragging = e;
			dragOffsetX = mouseX - e.getScreenX(this.width);
			dragOffsetY = mouseY - e.getScreenY(this.height);
			elements.remove(e);
			elements.add(e); // bring it to the front
		}
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if (dragging != null) {
			moveWithSnapping(dragging, mouseX - dragOffsetX, mouseY - dragOffsetY);
		} else if (resizing != null) {
			float x = resizing.getScreenX(this.width);
			float y = resizing.getScreenY(this.height);
			float newScale = (mouseX - x) / Math.max(1.0F, resizing.getWidth());
			newScale = Math.round(newScale * 20.0F) / 20.0F; // steps of 5%
			resizing.setScale(newScale);
			resizing.setScreenPosition(x, y, this.width, this.height); // keep the top-left corner in place
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		dragging = null;
		resizing = null;
		guideX = -1.0F;
		guideY = -1.0F;
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		if (wheel == 0) {
			return;
		}
		int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
		int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
		HudModule e = elementAt(mouseX, mouseY);
		if (e != null) {
			float x = e.getScreenX(this.width);
			float y = e.getScreenY(this.height);
			float cxBefore = x + e.getScaledWidth() / 2.0F;
			float cyBefore = y + e.getScaledHeight() / 2.0F;
			e.setScale(e.getScale() + (wheel > 0 ? 0.05F : -0.05F));
			// grow around the middle, not the corner
			e.setScreenPosition(cxBefore - e.getScaledWidth() / 2.0F, cyBefore - e.getScaledHeight() / 2.0F, this.width,
					this.height);
			selected = e;
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == KeyboardConstants.KEY_ESCAPE) {
			this.mc.displayGuiScreen(parent);
			return;
		}
		if (selected == null) {
			return;
		}
		float dx = 0.0F;
		float dy = 0.0F;
		if (keyCode == KeyboardConstants.KEY_LEFT) {
			dx = -1.0F;
		} else if (keyCode == KeyboardConstants.KEY_RIGHT) {
			dx = 1.0F;
		} else if (keyCode == KeyboardConstants.KEY_UP) {
			dy = -1.0F;
		} else if (keyCode == KeyboardConstants.KEY_DOWN) {
			dy = 1.0F;
		}
		if (dx != 0.0F || dy != 0.0F) {
			selected.setScreenPosition(selected.getScreenX(this.width) + dx, selected.getScreenY(this.height) + dy,
					this.width, this.height);
		}
	}

	// ---- Snapping -------------------------------------------------------------------

	private void moveWithSnapping(HudModule e, float x, float y) {
		float w = e.getScaledWidth();
		float h = e.getScaledHeight();
		guideX = -1.0F;
		guideY = -1.0F;
		if (!GuiScreen.isShiftKeyDown()) {
			float bestX = SNAP_DISTANCE + 1.0F;
			float snapX = 0.0F;
			float bestY = SNAP_DISTANCE + 1.0F;
			float snapY = 0.0F;

			// targets: screen edges (with a small margin) and the middle of the screen
			float[] xs = { EDGE_MARGIN, this.width / 2.0F, this.width - EDGE_MARGIN };
			float[] ys = { EDGE_MARGIN, this.height / 2.0F, this.height - EDGE_MARGIN };
			for (int t = 0; t < 3; ++t) {
				float[] r = snapAxis(x, w, xs[t], bestX);
				if (r != null) {
					bestX = r[0];
					snapX = r[1];
					guideX = xs[t];
				}
				r = snapAxis(y, h, ys[t], bestY);
				if (r != null) {
					bestY = r[0];
					snapY = r[1];
					guideY = ys[t];
				}
			}
			// targets: the edges and middles of the other elements
			for (int i = 0; i < elements.size(); ++i) {
				HudModule o = elements.get(i);
				if (o == e) {
					continue;
				}
				float ox = o.getScreenX(this.width);
				float oy = o.getScreenY(this.height);
				float ow = o.getScaledWidth();
				float oh = o.getScaledHeight();
				float[] oxs = { ox, ox + ow / 2.0F, ox + ow };
				float[] oys = { oy, oy + oh / 2.0F, oy + oh };
				for (int t = 0; t < 3; ++t) {
					float[] r = snapAxis(x, w, oxs[t], bestX);
					if (r != null) {
						bestX = r[0];
						snapX = r[1];
						guideX = oxs[t];
					}
					r = snapAxis(y, h, oys[t], bestY);
					if (r != null) {
						bestY = r[0];
						snapY = r[1];
						guideY = oys[t];
					}
				}
			}
			if (guideX >= 0.0F) {
				x = snapX;
			}
			if (guideY >= 0.0F) {
				y = snapY;
			}
		}
		e.setScreenPosition(x, y, this.width, this.height);
	}

	/**
	 * Checks if the element's start, middle or end is close to a target line.
	 *
	 * @return {distance, new position} if it's closer than best, otherwise null
	 */
	private static float[] snapAxis(float pos, float size, float target, float best) {
		float[] edges = { pos, pos + size / 2.0F, pos + size };
		float[] result = null;
		for (int i = 0; i < 3; ++i) {
			float d = Math.abs(edges[i] - target);
			if (d < best && d <= SNAP_DISTANCE) {
				best = d;
				result = new float[] { d, pos + (target - edges[i]) };
			}
		}
		return result;
	}

	// ---- Actions --------------------------------------------------------------------

	private void resetLayout() {
		long now = Minecraft.getSystemTime();
		if (now < resetArmedUntil) {
			resetArmedUntil = 0L;
			resetButton.setLabel("Reset layout");
			List<Module> modules = CenterClient.MODULES.getModules();
			for (int i = 0; i < modules.size(); ++i) {
				if (modules.get(i) instanceof HudModule) {
					((HudModule) modules.get(i)).resetLayout();
				}
			}
		} else {
			resetArmedUntil = now + RESET_CONFIRM_MS;
			resetButton.setLabel("Click again!");
		}
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
		CenterClient.CONFIG.saveNow();
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
