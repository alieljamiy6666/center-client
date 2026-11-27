package net.centerclient.event;

import net.centerclient.CenterClient;
import net.centerclient.util.ClickTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;

/**
 * The bridge between the game and the event bus. The game's own code calls
 * these one-line methods (each call is marked "// Center Client"), and they
 * fill in the reusable event objects and send them out.
 *
 * Keeping this here means each change to the game's code is a single line.
 */
public final class EventHooks {

	private static final TickEvent TICK = new TickEvent();
	private static final Render2DEvent RENDER_2D = new Render2DEvent();
	private static final Render3DEvent RENDER_3D = new Render3DEvent();
	private static final KeyEvent KEY = new KeyEvent();
	private static final MouseEvent MOUSE = new MouseEvent();
	private static final ChatReceiveEvent CHAT = new ChatReceiveEvent();
	private static final WorldLoadEvent WORLD_LOAD = new WorldLoadEvent();
	private static final ServerJoinEvent SERVER_JOIN = new ServerJoinEvent();

	private EventHooks() {
	}

	/** Minecraft.runTick(), at the end of every tick. */
	public static void onTick() {
		if (CenterClient.isInitialized()) {
			CenterClient.EVENTS.post(TICK.set(Minecraft.getMinecraft().world != null));
		}
	}

	/** GuiIngame.renderGameOverlay(), after the HUD is drawn. */
	public static void onRender2D(float partialTicks, int width, int height) {
		if (CenterClient.isInitialized()) {
			CenterClient.EVENTS.post(RENDER_2D.set(partialTicks, width, height));
		}
	}

	/** EntityRenderer.renderWorldPass(), after the world and before the hand. */
	public static void onRender3D(float partialTicks) {
		if (CenterClient.isInitialized()) {
			CenterClient.EVENTS.post(RENDER_3D.set(partialTicks));
		}
	}

	/** Minecraft.runTickKeyboard(), for every key going down or up. */
	public static void onKey(int key, boolean pressed, boolean repeat, boolean inGui) {
		if (CenterClient.isInitialized()) {
			CenterClient.EVENTS.post(KEY.set(key, pressed, repeat, inGui));
		}
	}

	/** Minecraft.runTickMouse(), for every mouse button going down or up while playing. */
	public static void onMouseButton(int button, boolean pressed) {
		if (button < 0 || !CenterClient.isInitialized()) {
			return; // not a button (mouse movement or the wheel)
		}
		if (pressed) {
			ClickTracker.onGameMouseDown(button);
		}
		CenterClient.EVENTS.post(MOUSE.set(button, pressed));
	}

	/**
	 * NetHandlerPlayClient.handleChat(), before a message is shown.
	 *
	 * @return the message to show (maybe changed by a listener), or null if a
	 *         listener cancelled it
	 */
	public static ITextComponent onChatReceive(ChatType type, ITextComponent message) {
		if (!CenterClient.isInitialized()) {
			return message;
		}
		ChatReceiveEvent event = CenterClient.EVENTS.post(CHAT.set(type, message));
		return event.isCancelled() ? null : event.getMessage();
	}

	/** Minecraft.loadWorld(), after a world loads or unloads. */
	public static void onWorldLoad(WorldClient world) {
		if (CenterClient.isInitialized()) {
			CenterClient.EVENTS.post(WORLD_LOAD.set(world));
		}
	}

	/** NetHandlerPlayClient.handleJoinGame(), after joining a server or world. */
	public static void onServerJoin() {
		if (CenterClient.isInitialized()) {
			Minecraft mc = Minecraft.getMinecraft();
			CenterClient.EVENTS.post(SERVER_JOIN.set(mc.getCurrentServerData(), mc.isSingleplayer()));
		}
	}
}
