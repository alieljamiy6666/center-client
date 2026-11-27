package net.centerclient.hud;

import net.centerclient.config.BooleanSetting;
import net.centerclient.event.TickEvent;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;

/**
 * A HUD element made of text lines, each with a label (in the accent color)
 * and a value, like "FPS 144" or "Ping 42 ms".
 *
 * Subclasses fill in {@link #update()}, which runs 20 times a second, and call
 * {@link #setLine} there. Text is only rebuilt when a value really changes,
 * and widths are only measured again then, so drawing costs almost nothing.
 */
public abstract class TextHudModule extends HudModule {

	private static final int PAD_X = 5;
	private static final int PAD_Y = 3;
	private static final int LINE_HEIGHT = 10;
	private static final int LABEL_GAP = 4;

	protected final BooleanSetting showLabels = add(new BooleanSetting("Show labels", true));

	private String[] labels = new String[1];
	private String[] values = new String[1];
	private int[] colors = new int[1];
	private int lineCount = 1;
	private int cachedWidth = 20;
	private boolean widthDirty = true;
	private boolean widthHadLabels;

	protected TextHudModule(String name, String description, Category category, int anchorX, int anchorY,
			float defaultX, float defaultY) {
		super(name, description, category, anchorX, anchorY, defaultX, defaultY);
		labels[0] = "";
		values[0] = "";
		colors[0] = Theme.TEXT;
		listen(TickEvent.class, this::onTickUpdate);
	}

	private void onTickUpdate(TickEvent event) {
		update();
	}

	/** Refreshes the text. Runs 20 times a second while the module is on. */
	protected abstract void update();

	@Override
	protected void onEnable() {
		update(); // have something to show straight away
	}

	/** How many lines to show. */
	protected final void setLineCount(int count) {
		if (count == lineCount) {
			return;
		}
		if (count > labels.length) {
			String[] newLabels = new String[count];
			String[] newValues = new String[count];
			int[] newColors = new int[count];
			System.arraycopy(labels, 0, newLabels, 0, labels.length);
			System.arraycopy(values, 0, newValues, 0, values.length);
			System.arraycopy(colors, 0, newColors, 0, colors.length);
			for (int i = labels.length; i < count; ++i) {
				newLabels[i] = "";
				newValues[i] = "";
				newColors[i] = Theme.TEXT;
			}
			labels = newLabels;
			values = newValues;
			colors = newColors;
		}
		lineCount = count;
		widthDirty = true;
	}

	/** Sets one line. Does nothing if it's the same as before. */
	protected final void setLine(int index, String label, String value, int valueColor) {
		if (!label.equals(labels[index]) || !value.equals(values[index])) {
			labels[index] = label;
			values[index] = value;
			widthDirty = true;
		}
		colors[index] = valueColor;
	}

	@Override
	public int getWidth() {
		boolean withLabels = showLabels.isOn();
		if (widthDirty || withLabels != widthHadLabels) {
			int widest = 0;
			for (int i = 0; i < lineCount; ++i) {
				widest = Math.max(widest, lineWidth(i, withLabels));
			}
			cachedWidth = widest + PAD_X * 2;
			widthDirty = false;
			widthHadLabels = withLabels;
		}
		return cachedWidth;
	}

	private int lineWidth(int i, boolean withLabels) {
		int w = mc.fontRendererObj.getStringWidth(values[i]);
		if (withLabels && !labels[i].isEmpty()) {
			w += mc.fontRendererObj.getStringWidth(labels[i]) + LABEL_GAP;
		}
		return w;
	}

	@Override
	public int getHeight() {
		return lineCount * LINE_HEIGHT + PAD_Y * 2 - 1;
	}

	@Override
	protected void draw(boolean editing) {
		boolean withLabels = showLabels.isOn();
		boolean shadow = !background.isOn();
		for (int i = 0; i < lineCount; ++i) {
			int x = PAD_X;
			int y = PAD_Y + 1 + i * LINE_HEIGHT;
			if (withLabels && !labels[i].isEmpty()) {
				mc.fontRendererObj.drawString(labels[i], x, y, Theme.accent(), shadow);
				x += mc.fontRendererObj.getStringWidth(labels[i]) + LABEL_GAP;
			}
			mc.fontRendererObj.drawString(values[i], x, y, colors[i], shadow);
		}
	}
}
