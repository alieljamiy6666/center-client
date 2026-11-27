package net.centerclient.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import net.centerclient.CenterClient;
import net.centerclient.config.ConfigManager;
import net.centerclient.config.Setting;
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
	private final int defaultKey;
	private int key;
	private boolean enabled;
	private final List<Subscription<?>> subscriptions = new ArrayList<Subscription<?>>();
	private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
	private final List<Setting<?>> readOnlySettings = Collections.unmodifiableList(settings);

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
		this.defaultKey = defaultKey;
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
		ConfigManager.changed();
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
		if (key != this.key) {
			this.key = key;
			ConfigManager.changed();
		}
	}

	/** The key's name, like "K", or "None". */
	public final String getKeyName() {
		return key == NO_KEY ? "None" : KeyboardConstants.getKeyName(key);
	}

	// ---- Settings and saving ----------------------------------------------------

	/** Adds a setting to this module. Call it in the constructor. */
	protected final <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	/** This module's settings, in the order they were added. */
	public final List<Setting<?>> getSettings() {
		return readOnlySettings;
	}

	/** Turns the module off and puts its key and every setting back to default. */
	public final void resetToDefaults() {
		setEnabled(false);
		setKey(defaultKey);
		for (int i = 0; i < settings.size(); ++i) {
			settings.get(i).reset();
		}
		resetExtra();
	}

	/** Extra things to save, like a HUD element's position. Override if needed. */
	protected void saveExtra(JSONObject json) {
	}

	/** Reads what {@link #saveExtra} saved. Override if needed. */
	protected void loadExtra(JSONObject json) {
	}

	/** Puts the extra things back to default. Override if needed. */
	protected void resetExtra() {
	}

	/** This module as JSON: on/off, key, settings and extras. */
	public final JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("enabled", enabled);
		json.put("key", key);
		JSONObject values = new JSONObject();
		for (int i = 0; i < settings.size(); ++i) {
			Setting<?> s = settings.get(i);
			values.put(s.getName(), s.toJson());
		}
		json.put("settings", values);
		JSONObject extra = new JSONObject();
		saveExtra(extra);
		if (extra.length() > 0) {
			json.put("extra", extra);
		}
		return json;
	}

	/**
	 * Loads this module from JSON. Each part is read on its own, so one broken
	 * or missing value just stays at its default instead of spoiling the rest.
	 */
	public final void fromJson(JSONObject json) {
		JSONObject values = json.optJSONObject("settings");
		if (values != null) {
			for (int i = 0; i < settings.size(); ++i) {
				Setting<?> s = settings.get(i);
				Object saved = values.opt(s.getName());
				if (saved != null) {
					try {
						s.fromJson(saved);
					} catch (RuntimeException e) {
						s.reset();
					}
				}
			}
		}
		Object savedKey = json.opt("key");
		if (savedKey instanceof Number) {
			int k = ((Number) savedKey).intValue();
			setKey(k >= 0 && k <= 255 ? k : defaultKey);
		}
		JSONObject extra = json.optJSONObject("extra");
		if (extra != null) {
			try {
				loadExtra(extra);
			} catch (RuntimeException e) {
				resetExtra();
			}
		}
		// last, so onEnable() already sees the loaded settings
		setEnabled(json.optBoolean("enabled", false));
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
