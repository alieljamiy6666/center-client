package net.centerclient.module;

import java.util.ArrayList;
import java.util.List;

import net.centerclient.CenterClient;
import net.centerclient.event.Event;
import net.centerclient.event.Listener;
import net.lax1dude.eaglercraft.KeyboardConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * One Center Client feature that can be turned on and off, like Zoom or FPS.
 *
 * To make a module:
 * <pre>
 * public class MyModule extends Module {
 *     public MyModule() {
 *         super("My Module", "What it does, in a few words", Category.MISC);
 *         listen(TickEvent.class, this::onTick); // events it wants while it's on
 *     }
 *     private void onTick(TickEvent event) { ... }
 * }
 * </pre>
 * Then register it in CenterClient.registerModules().
 *
 * Listeners added with {@link #listen} only receive events while the module
 * is on, so a module that's off costs nothing.
 */
public abstract class Module {

	/** No key is bound. */
	public static final int NO_KEY = KeyboardConstants.KEY_NONE;

	protected final Minecraft mc = Minecraft.getMinecraft();

	private final String name;
	private final String description;
	private final Category category;
	private int key;
	private boolean enabled;
	private final List<Subscription<?>> subscriptions = new ArrayList<Subscription<?>>();

	protected Module(String name, String description, Category category) {
		this(name, description, category, NO_KEY);
	}

	protected Module(String name, String description, Category category, int defaultKey) {
		if (category == null || category.isView()) {
			throw new IllegalArgumentException(name + " needs a real category, not " + category);
		}
		this.name = name;
		this.description = description;
		this.category = category;
		this.key = defaultKey;
	}

	// ---- Turning on and off -----------------------------------------------------

	public final boolean isEnabled() {
		return enabled;
	}

	public final void toggle() {
		setEnabled(!enabled);
	}

	public final void setEnabled(boolean enable) {
		if (enable == this.enabled) {
			return;
		}
		this.enabled = enable;
		if (enable) {
			for (int i = 0; i < subscriptions.size(); ++i) {
				subscriptions.get(i).subscribe();
			}
			onEnable();
		} else {
			onDisable();
			for (int i = 0; i < subscriptions.size(); ++i) {
				subscriptions.get(i).unsubscribe();
			}
		}
	}

	/** Runs when the module is turned on. */
	protected void onEnable() {
	}

	/** Runs when the module is turned off. */
	protected void onDisable() {
	}

	// ---- Events -----------------------------------------------------------------

	/** Asks for an event while this module is on. Call it in the constructor. */
	protected final <E extends Event> void listen(Class<E> type, Listener<E> listener) {
		Subscription<E> sub = new Subscription<E>(type, listener);
		subscriptions.add(sub);
		if (enabled) {
			sub.subscribe();
		}
	}

	private static final class Subscription<E extends Event> {
		private final Class<E> type;
		private final Listener<E> listener;

		Subscription(Class<E> type, Listener<E> listener) {
			this.type = type;
			this.listener = listener;
		}

		void subscribe() {
			CenterClient.EVENTS.subscribe(type, listener);
		}

		void unsubscribe() {
			CenterClient.EVENTS.unsubscribe(type, listener);
		}
	}

	// ---- Info -------------------------------------------------------------------

	public final String getName() {
		return name;
	}

	public final String getDescription() {
		return description;
	}

	public final Category getCategory() {
		return category;
	}

	/** The key that toggles this module, or {@link #NO_KEY}. */
	public final int getKey() {
		return key;
	}

	public final void setKey(int key) {
		this.key = key;
	}

	/** The key's name, like "K", or "None". */
	public final String getKeyName() {
		return key == NO_KEY ? "None" : KeyboardConstants.getKeyName(key);
	}

	// ---- Helpers ----------------------------------------------------------------

	/** Shows a message in your chat (only you can see it). */
	protected final void sendClientMessage(String message) {
		if (mc.ingameGUI != null && mc.player != null) {
			mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(TextFormatting.AQUA + "["
					+ CenterClient.NAME + "] " + TextFormatting.RESET + message));
		}
	}
}
