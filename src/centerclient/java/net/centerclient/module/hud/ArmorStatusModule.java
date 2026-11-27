package net.centerclient.module.hud;

import net.centerclient.config.BooleanSetting;
import net.centerclient.config.ModeSetting;
import net.centerclient.event.TickEvent;
import net.centerclient.hud.HudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.centerclient.util.ColorUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Your armor and the item in your hand, with how much durability each one
 * has left, as numbers, percent, or just the bar.
 */
public class ArmorStatusModule extends HudModule {

	private static final int MAX_ITEMS = 6;
	private static final int PAD = 4;
	private static final int ICON = 16;

	private final ModeSetting durability = add(new ModeSetting("Durability", "Numbers", "Numbers", "Percent", "Bar only"));
	private final ModeSetting layout = add(new ModeSetting("Layout", "Vertical", "Vertical", "Horizontal"));
	private final BooleanSetting showHeld = add(new BooleanSetting("Show held item", true));

	// what to show, refreshed every tick (no new objects unless a number changes)
	private final ItemStack[] items = new ItemStack[MAX_ITEMS];
	private final String[] texts = new String[MAX_ITEMS];
	private final int[] textColors = new int[MAX_ITEMS];
	private final int[] shownDamage = new int[MAX_ITEMS];
	private final String[] shownMode = new String[MAX_ITEMS];
	private final ItemStack[] shownStack = new ItemStack[MAX_ITEMS];
	private int count;
	private int widestText;

	private ItemStack[] preview;

	public ArmorStatusModule() {
		super("Armor Status", "Shows your armor and held item with durability left", Category.HUD, END, CENTER, 0.995F,
				0.40F);
		for (int i = 0; i < MAX_ITEMS; ++i) {
			texts[i] = "";
			shownDamage[i] = -1;
		}
		listen(TickEvent.class, this::onTick);
	}

	@Override
	protected void onEnable() {
		refresh();
	}

	private void onTick(TickEvent event) {
		refresh();
	}

	private void refresh() {
		count = 0;
		if (mc.player == null) {
			ItemStack[] examples = previewItems();
			for (int i = 0; i < examples.length; ++i) {
				addItem(examples[i]);
			}
		} else {
			for (int slot = 3; slot >= 0; --slot) { // helmet first, boots last
				addItem(mc.player.inventory.armorInventory.get(slot));
			}
			if (showHeld.isOn()) {
				addItem(mc.player.getHeldItemMainhand());
				addItem(mc.player.getHeldItemOffhand());
			}
		}
		widestText = 0;
		for (int i = 0; i < count; ++i) {
			widestText = Math.max(widestText, mc.fontRendererObj.getStringWidth(texts[i]));
		}
	}

	private void addItem(ItemStack stack) {
		if (stack == null || stack.func_190926_b() || count >= MAX_ITEMS) {
			return; // empty slot
		}
		int i = count++;
		items[i] = stack;
		String mode = durability.get();
		if (!stack.isItemStackDamageable() || mode.equals("Bar only")) {
			texts[i] = "";
			shownDamage[i] = -1;
			return;
		}
		int damage = stack.getItemDamage();
		if (stack == shownStack[i] && damage == shownDamage[i] && mode.equals(shownMode[i]) && !texts[i].isEmpty()) {
			return; // same item, same damage as last tick
		}
		shownStack[i] = stack;
		shownDamage[i] = damage;
		shownMode[i] = mode;
		int max = stack.getMaxDamage();
		int left = max - damage;
		float fraction = max > 0 ? left / (float) max : 1.0F;
		texts[i] = mode.equals("Percent") ? Math.round(fraction * 100.0F) + "%" : Integer.toString(left);
		textColors[i] = fraction > 0.5F ? ColorUtil.mix(Theme.WARNING, Theme.SUCCESS, (fraction - 0.5F) * 2.0F)
				: ColorUtil.mix(Theme.DANGER, Theme.WARNING, fraction * 2.0F);
	}

	private ItemStack[] previewItems() {
		if (preview == null) {
			Item[] kinds = { Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
					Items.DIAMOND_SWORD };
			preview = new ItemStack[kinds.length];
			for (int i = 0; i < kinds.length; ++i) {
				preview[i] = new ItemStack(kinds[i]);
				preview[i].setItemDamage(preview[i].getMaxDamage() * i / 6); // a bit worn, to show the colors
			}
		}
		return preview;
	}

	@Override
	public boolean isEmpty() {
		return count == 0;
	}

	private boolean vertical() {
		return layout.is("Vertical");
	}

	private boolean hasText() {
		return !durability.is("Bar only") && widestText > 0;
	}

	@Override
	public int getWidth() {
		if (vertical()) {
			return PAD * 2 + ICON + (hasText() ? 4 + widestText : 0);
		}
		int w = PAD * 2;
		for (int i = 0; i < count; ++i) {
			w += columnWidth(i) + (i > 0 ? 4 : 0);
		}
		return Math.max(w, PAD * 2 + ICON);
	}

	private int columnWidth(int i) {
		return Math.max(ICON, hasText() ? mc.fontRendererObj.getStringWidth(texts[i]) : 0);
	}

	@Override
	public int getHeight() {
		if (vertical()) {
			return PAD * 2 + Math.max(1, count) * (ICON + 2) - 2;
		}
		return PAD * 2 + ICON + (hasText() ? 10 : 0);
	}

	@Override
	protected void draw(boolean editing) {
		boolean vertical = vertical();
		boolean withText = hasText();
		boolean shadow = !background.isOn();

		// the item pictures, drawn the same way the hotbar draws them
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		RenderHelper.enableGUIStandardItemLighting();
		int x = PAD;
		for (int i = 0; i < count; ++i) {
			int ix = vertical ? PAD : x + (columnWidth(i) - ICON) / 2;
			int iy = vertical ? PAD + i * (ICON + 2) : PAD;
			mc.getRenderItem().renderItemAndEffectIntoGUI(items[i], ix, iy);
			mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, items[i], ix, iy, null);
			if (!vertical) {
				x += columnWidth(i) + 4;
			}
		}
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableRescaleNormal();

		// durability numbers
		if (withText) {
			x = PAD;
			for (int i = 0; i < count; ++i) {
				if (!texts[i].isEmpty()) {
					if (vertical) {
						mc.fontRendererObj.drawString(texts[i], PAD + ICON + 4, PAD + i * (ICON + 2) + 4, textColors[i],
								shadow);
					} else {
						int tw = mc.fontRendererObj.getStringWidth(texts[i]);
						mc.fontRendererObj.drawString(texts[i], x + (columnWidth(i) - tw) / 2, PAD + ICON + 1,
								textColors[i], shadow);
					}
				}
				if (!vertical) {
					x += columnWidth(i) + 4;
				}
			}
		}
	}
}
