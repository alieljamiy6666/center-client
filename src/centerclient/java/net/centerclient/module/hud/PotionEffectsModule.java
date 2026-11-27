package net.centerclient.module.hud;

import java.util.Collection;
import java.util.Iterator;

import net.centerclient.config.BooleanSetting;
import net.centerclient.event.TickEvent;
import net.centerclient.hud.HudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.centerclient.util.ColorUtil;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * Your active potion effects with their icons and time left. The time blinks
 * red in the last 10 seconds. Can hide the game's own potion icons in the
 * top-right corner so they don't show twice.
 */
public class PotionEffectsModule extends HudModule {

	private static final int MAX_EFFECTS = 16;
	private static final int ROW = 20;
	private static final int PAD = 3;

	private static PotionEffectsModule instance;

	private final BooleanSetting showNames = add(new BooleanSetting("Show names", true));
	private final BooleanSetting hideVanilla = add(new BooleanSetting("Hide game's potion icons", true));

	private final PotionEffect[] effects = new PotionEffect[MAX_EFFECTS];
	private final String[] names = new String[MAX_EFFECTS];
	private final String[] times = new String[MAX_EFFECTS];
	private final int[] shownSeconds = new int[MAX_EFFECTS];
	private final Potion[] shownPotion = new Potion[MAX_EFFECTS];
	private final int[] shownAmplifier = new int[MAX_EFFECTS];
	private int count;
	private int widestText;
	private PotionEffect[] preview;

	public PotionEffectsModule() {
		super("Potion Effects", "Shows your active potion effects and their time left", Category.HUD, END, CENTER,
				0.995F, 0.62F);
		instance = this;
		listen(TickEvent.class, this::onTick);
	}

	/** True when the game's own potion icons should be hidden (checked by GuiIngame). */
	public static boolean hidesVanillaIcons() {
		return instance != null && instance.isEnabled() && instance.hideVanilla.isOn();
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
			PotionEffect[] examples = previewEffects();
			for (int i = 0; i < examples.length; ++i) {
				addEffect(examples[i]);
			}
		} else {
			Collection<PotionEffect> active = mc.player.getActivePotionEffects();
			if (!active.isEmpty()) {
				Iterator<PotionEffect> it = active.iterator();
				while (it.hasNext()) {
					addEffect(it.next());
				}
			}
		}
		widestText = 0;
		for (int i = 0; i < count; ++i) {
			widestText = Math.max(widestText, mc.fontRendererObj.getStringWidth(names[i]));
			widestText = Math.max(widestText, mc.fontRendererObj.getStringWidth(times[i]));
		}
	}

	private void addEffect(PotionEffect effect) {
		if (count >= MAX_EFFECTS) {
			return;
		}
		int i = count++;
		effects[i] = effect;
		Potion potion = effect.getPotion();
		int amplifier = effect.getAmplifier();
		if (potion != shownPotion[i] || amplifier != shownAmplifier[i] || names[i] == null) {
			shownPotion[i] = potion;
			shownAmplifier[i] = amplifier;
			String name = I18n.format(effect.getEffectName());
			if (amplifier > 0 && amplifier < 9) {
				name += " " + I18n.format("enchantment.level." + (amplifier + 1));
			}
			names[i] = name;
			shownSeconds[i] = -2; // force the time to be redone too
		}
		int seconds = effect.getIsPotionDurationMax() ? -1 : effect.getDuration() / 20;
		if (seconds != shownSeconds[i]) {
			shownSeconds[i] = seconds;
			times[i] = Potion.getPotionDurationString(effect, 1.0F);
		}
	}

	private PotionEffect[] previewEffects() {
		if (preview == null) {
			preview = new PotionEffect[] { new PotionEffect(MobEffects.SPEED, 20 * 83, 1),
					new PotionEffect(MobEffects.STRENGTH, 20 * 45, 0) };
		}
		return preview;
	}

	@Override
	public boolean isEmpty() {
		return count == 0;
	}

	@Override
	public int getWidth() {
		return PAD * 2 + 18 + 4 + widestText;
	}

	@Override
	public int getHeight() {
		return PAD * 2 + Math.max(1, count) * ROW - 2;
	}

	@Override
	protected void draw(boolean editing) {
		boolean shadow = !background.isOn();
		boolean withNames = showNames.isOn();
		mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
		GlStateManager.enableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		for (int i = 0; i < count; ++i) {
			Potion potion = effects[i].getPotion();
			if (potion.hasStatusIcon()) {
				int index = potion.getStatusIconIndex();
				Gui.drawModalRectWithCustomSizedTexture(PAD, PAD + i * ROW, (index % 8) * 18, 198 + (index / 8) * 18, 18,
						18, 256.0F, 256.0F);
			}
		}
		long now = Minecraft.getSystemTime();
		for (int i = 0; i < count; ++i) {
			int y = PAD + i * ROW;
			int tx = PAD + 18 + 4;
			int timeColor = Theme.TEXT_FADED;
			int seconds = shownSeconds[i];
			if (seconds >= 0 && seconds < 10) {
				float blink = 0.5F + 0.5F * (float) Math.sin(now / 120.0);
				timeColor = ColorUtil.mix(Theme.TEXT_FADED, Theme.DANGER, blink);
			}
			if (withNames) {
				int nameColor = effects[i].getPotion().isBadEffect() ? Theme.DANGER : Theme.TEXT;
				mc.fontRendererObj.drawString(names[i], tx, y + 1, nameColor, shadow);
				mc.fontRendererObj.drawString(times[i], tx, y + 10, timeColor, shadow);
			} else {
				mc.fontRendererObj.drawString(times[i], tx, y + 5, timeColor, shadow);
			}
		}
	}
}
