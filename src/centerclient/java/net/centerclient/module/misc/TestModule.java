package net.centerclient.module.misc;

import net.centerclient.event.ChatReceiveEvent;
import net.centerclient.event.KeyEvent;
import net.centerclient.event.Render2DEvent;
import net.centerclient.event.Render3DEvent;
import net.centerclient.event.ServerJoinEvent;
import net.centerclient.event.TickEvent;
import net.centerclient.event.WorldLoadEvent;
import net.centerclient.module.Category;
import net.centerclient.module.Module;
import net.centerclient.theme.Theme;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.RenderUtil;
import net.lax1dude.eaglercraft.KeyboardConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;

/**
 * Step 6's test module. Press K in a world to turn it on or off. It shows a
 * chat message, plus an "event monitor" box that counts every kind of event,
 * so you can see the event system working without opening the console.
 */
public class TestModule extends Module {

	private static final int LINE_COUNT = 7;

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
	private int panelWidth;

	public TestModule() {
		super("Test Module", "Shows an event monitor so you can see the module system working", Category.MISC,
				KeyboardConstants.KEY_K);
		listen(TickEvent.class, this::onTick);
		listen(Render2DEvent.class, this::onRender2D);
		listen(Render3DEvent.class, this::onRender3D);
		listen(KeyEvent.class, this::onKey);
		listen(ChatReceiveEvent.class, this::onChat);
		listen(WorldLoadEvent.class, this::onWorldLoad);
		listen(ServerJoinEvent.class, this::onServerJoin);
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

	private void onTick(TickEvent event) {
		++ticks;
	}

	private void onRender3D(Render3DEvent event) {
		++frames3d;
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

	private void onRender2D(Render2DEvent event) {
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

		int x = 4;
		int y = 4;
		int lineHeight = 10;
		int height = 16 + LINE_COUNT * lineHeight + 2;
		RenderUtil.shadow(x, y + 1, panelWidth, height, 5, 3, 0x70000000);
		RenderUtil.roundedRect(x, y, panelWidth, height, 5, ColorUtil.withAlpha(Theme.PANEL, 0.9F));
		RenderUtil.roundedOutline(x, y, panelWidth, height, 5, 1, Theme.accent());
		mc.fontRendererObj.drawString("Event monitor", x + 6, y + 5, Theme.accent(), true);
		for (int i = 0; i < LINE_COUNT; ++i) {
			mc.fontRendererObj.drawString(lines[i], x + 6, y + 17 + i * lineHeight, Theme.TEXT, false);
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
