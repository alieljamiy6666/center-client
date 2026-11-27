package net.centerclient.module.misc;

import net.centerclient.config.BooleanSetting;
import net.centerclient.config.ColorSetting;
import net.centerclient.config.NumberSetting;
import net.centerclient.event.ChatReceiveEvent;
import net.centerclient.event.KeyEvent;
import net.centerclient.event.Render2DEvent;
import net.centerclient.event.Render3DEvent;
import net.centerclient.event.ServerJoinEvent;
import net.centerclient.event.TickEvent;
import net.centerclient.event.WorldLoadEvent;
import net.centerclient.hud.HudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.RenderUtil;
import net.lax1dude.eaglercraft.KeyboardConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;

/**
 * The test module from Step 6. Press K in a world to turn it on or off. It
 * shows a chat message, plus an "event monitor" box that counts every kind of
 * event. Since Step 9 the box is a HUD element you can move in the HUD editor.
 */
public class TestModule extends HudModule {

	private static final int LINE_COUNT = 7;
	private static final int LINE_HEIGHT = 10;

	private int ticks;
	private int frames2d;
	private int frames3d;
	private int keys;
	private int chats;
	private int worldLoads;
	private int serverJoins;

	private int ticksLastSecond;
	private int frames2dLastSecond;
	private int frames3dLastSecond;
	private long secondStart;

	/** The text lines, rebuilt once a second or when a count changes, never every frame. */
	private final String[] lines = new String[LINE_COUNT];
	private int panelWidth = 80;

	private final BooleanSetting showTitle = add(new BooleanSetting("Show title", true));
	private final NumberSetting backgroundOpacity = add(
			new NumberSetting("Background opacity", 0.9, 0.2, 1.0, 0.05).showWhen(background));
	private final BooleanSetting customBorder = add(new BooleanSetting("Custom border color", false));
	private final ColorSetting borderColor = add(new ColorSetting("Border color", 0xFFFFD23F).showWhen(customBorder));

	public TestModule() {
		super("Test Module", "Shows an event monitor so you can see the module system working", Category.MISC,
				KeyboardConstants.KEY_K, END, START, 0.995F, 0.01F);
		listen(TickEvent.class, this::onTick);
		listen(Render2DEvent.class, this::onFrame);
		listen(Render3DEvent.class, this::onRender3D);
		listen(KeyEvent.class, this::onKey);
		listen(ChatReceiveEvent.class, this::onChat);
		listen(WorldLoadEvent.class, this::onWorldLoad);
		listen(ServerJoinEvent.class, this::onServerJoin);
		rebuildLines();
	}

	@Override
	protected void onEnable() {
		ticks = frames2d = frames3d = keys = chats = worldLoads = serverJoins = 0;
		ticksLastSecond = frames2dLastSecond = frames3dLastSecond = 0;
		secondStart = Minecraft.getSystemTime();
		rebuildLines();
		sendClientMessage(getName() + " " + TextFormatting.GREEN + "on" + TextFormatting.RESET + ". Press "
				+ getKeyName() + " again to turn it off.");
	}

	@Override
	protected void onDisable() {
		sendClientMessage(getName() + " " + TextFormatting.RED + "off");
	}

	// ---- Counting events ----------------------------------------------------------

	private void onTick(TickEvent event) {
		++ticks;
	}

	private void onRender3D(Render3DEvent event) {
		++frames3d;
	}

	private void onFrame(Render2DEvent event) {
		++frames2d;
		long now = Minecraft.getSystemTime();
		if (now - secondStart >= 1000L) {
			ticksLastSecond = ticks;
			frames2dLastSecond = frames2d;
			frames3dLastSecond = frames3d;
			ticks = frames2d = frames3d = 0;
			secondStart = now;
			rebuildLines();
		}
	}

	private void onKey(KeyEvent event) {
		if (event.isPressed() && !event.isRepeat()) {
			++keys;
			rebuildLines();
		}
	}

	private void onChat(ChatReceiveEvent event) {
		++chats;
		rebuildLines();
	}

	private void onWorldLoad(WorldLoadEvent event) {
		++worldLoads;
		rebuildLines();
		if (!event.isUnload()) {
			sendClientMessage("World loaded.");
		}
	}

	private void onServerJoin(ServerJoinEvent event) {
		++serverJoins;
		rebuildLines();
		if (event.isSingleplayer()) {
			sendClientMessage("Joined a singleplayer world.");
		} else if (event.getServer() != null) {
			sendClientMessage("Joined " + event.getServer().serverName + ".");
		}
	}

	// ---- The HUD element ----------------------------------------------------------

	@Override
	public int getWidth() {
		return panelWidth;
	}

	@Override
	public int getHeight() {
		return (showTitle.isOn() ? 16 : 5) + LINE_COUNT * LINE_HEIGHT + 2;
	}

	@Override
	protected int getBackgroundColor() {
		return ColorUtil.withAlpha(Theme.PANEL, backgroundOpacity.getFloat());
	}

	@Override
	protected void draw(boolean editing) {
		int top = showTitle.isOn() ? 16 : 5;
		int border = customBorder.isOn() ? borderColor.getColor() : Theme.accent();
		RenderUtil.roundedOutline(0, 0, panelWidth, getHeight(), 4, 1, border);
		if (showTitle.isOn()) {
			mc.fontRendererObj.drawString("Event monitor", 6, 5, border, true);
		}
		for (int i = 0; i < LINE_COUNT; ++i) {
			mc.fontRendererObj.drawString(lines[i], 6, top + 1 + i * LINE_HEIGHT, Theme.TEXT, !background.isOn());
		}
	}

	private void rebuildLines() {
		lines[0] = "Ticks/s: " + ticksLastSecond;
		lines[1] = "HUD frames/s: " + frames2dLastSecond;
		lines[2] = "World frames/s: " + frames3dLastSecond;
		lines[3] = "Keys pressed: " + keys;
		lines[4] = "Chat messages: " + chats;
		lines[5] = "World loads: " + worldLoads;
		lines[6] = "Server joins: " + serverJoins;
		int widest = mc.fontRendererObj.getStringWidth("Event monitor");
		for (int i = 0; i < LINE_COUNT; ++i) {
			widest = Math.max(widest, mc.fontRendererObj.getStringWidth(lines[i]));
		}
		panelWidth = widest + 12;
	}
}
