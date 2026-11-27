package net.centerclient.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import net.centerclient.event.EventBus;
import net.centerclient.event.Listener;
import net.centerclient.event.TickEvent;
import net.centerclient.module.Module;
import net.centerclient.module.ModuleManager;
import net.centerclient.theme.Theme;
import net.lax1dude.eaglercraft.Base64;
import net.lax1dude.eaglercraft.EagRuntime;
import net.minecraft.client.Minecraft;

/**
 * Saves and loads everything as JSON in the browser's storage (the same
 * localStorage that Eaglercraft uses for its own options).
 *
 * What's saved:
 * <ul>
 * <li>"centerclient.meta": the client settings (accent color), the list of
 * profiles and which one is active</li>
 * <li>"centerclient.profile.NAME": one per profile, with every module's
 * on/off, key, settings and extras (like HUD positions)</li>
 * </ul>
 *
 * Saving happens by itself: when something changes, it waits about a second
 * (so dragging a slider doesn't save 60 times a second), then saves.
 *
 * If a saved config is broken or from an older version, whatever can't be
 * read just goes back to its default, and a broken file is kept under
 * "...broken" in case it's needed.
 */
public final class ConfigManager {

	private static final Logger LOGGER = LogManager.getLogger("Center Client Config");

	public static final int FORMAT_VERSION = 1;
	public static final String[] DEFAULT_PROFILES = { "Default", "PvP", "Building", "Low-end PC" };

	private static final String META_KEY = "centerclient.meta";
	private static final String PROFILE_KEY_PREFIX = "centerclient.profile.";
	private static final String EXPORT_PREFIX = "CenterClient1:";
	private static final long SAVE_DELAY_MS = 1000L;
	private static final int MAX_PROFILE_NAME = 24;

	private static ConfigManager instance;

	private final ModuleManager modules;
	private final List<String> profiles = new ArrayList<String>();
	private final List<String> readOnlyProfiles = Collections.unmodifiableList(profiles);
	private final Listener<TickEvent> tickListener = this::onTick;
	private String currentProfile = DEFAULT_PROFILES[0];
	private boolean dirty;
	private long dirtySince;
	private boolean applying;

	public ConfigManager(ModuleManager modules, EventBus bus) {
		this.modules = modules;
		instance = this;
		bus.subscribe(TickEvent.class, tickListener);
	}

	// ---- "Something changed" ----------------------------------------------------

	/** Settings, modules and the theme call this when they change. Saves a moment later. */
	public static void changed() {
		ConfigManager config = instance;
		if (config != null && !config.applying) {
			config.dirty = true;
			config.dirtySince = Minecraft.getSystemTime();
		}
	}

	private void onTick(TickEvent event) {
		if (dirty && Minecraft.getSystemTime() - dirtySince >= SAVE_DELAY_MS) {
			saveNow();
		}
	}

	// ---- Loading and saving -----------------------------------------------------

	/** Loads everything at startup. Never throws: bad data just means defaults. */
	public void load() {
		JSONObject meta = readJson(META_KEY);
		profiles.clear();
		if (meta != null) {
			JSONArray list = meta.optJSONArray("profiles");
			if (list != null) {
				for (int i = 0; i < list.length(); ++i) {
					String name = cleanName(list.optString(i, null));
					if (name != null && !profiles.contains(name)) {
						profiles.add(name);
					}
				}
			}
			JSONObject client = meta.optJSONObject("client");
			if (client != null) {
				loadClientSettings(client);
			}
		}
		if (profiles.isEmpty()) {
			for (int i = 0; i < DEFAULT_PROFILES.length; ++i) {
				profiles.add(DEFAULT_PROFILES[i]);
			}
		}
		String saved = meta != null ? meta.optString("profile", null) : null;
		currentProfile = saved != null && profiles.contains(saved) ? saved : profiles.get(0);
		applyProfile(readJson(profileKey(currentProfile)));
		dirty = false;
		LOGGER.info("Loaded profile \"" + currentProfile + "\"");
	}

	/** Saves right now (normally saving happens by itself). */
	public void saveNow() {
		dirty = false;
		writeJson(META_KEY, metaToJson());
		writeJson(profileKey(currentProfile), profileToJson());
	}

	private JSONObject metaToJson() {
		JSONObject meta = new JSONObject();
		meta.put("version", FORMAT_VERSION);
		meta.put("profile", currentProfile);
		JSONArray list = new JSONArray();
		for (int i = 0; i < profiles.size(); ++i) {
			list.put(profiles.get(i));
		}
		meta.put("profiles", list);
		JSONObject client = new JSONObject();
		client.put("accentPreset", Theme.getAccentPreset());
		meta.put("client", client);
		return meta;
	}

	private void loadClientSettings(JSONObject client) {
		Object preset = client.opt("accentPreset");
		if (preset instanceof Number) {
			int index = ((Number) preset).intValue();
			if (index >= 0 && index < Theme.ACCENT_PRESETS.length) {
				applying = true;
				try {
					Theme.setAccentPreset(index);
				} finally {
					applying = false;
				}
			}
		}
	}

	private JSONObject profileToJson() {
		JSONObject root = new JSONObject();
		root.put("version", FORMAT_VERSION);
		JSONObject mods = new JSONObject();
		List<Module> list = modules.getModules();
		for (int i = 0; i < list.size(); ++i) {
			Module m = list.get(i);
			mods.put(m.getName(), m.toJson());
		}
		root.put("modules", mods);
		return root;
	}

	/** Puts every module back to default, then applies whatever the JSON has. */
	private void applyProfile(JSONObject root) {
		applying = true;
		try {
			JSONObject mods = root != null ? root.optJSONObject("modules") : null;
			List<Module> list = modules.getModules();
			for (int i = 0; i < list.size(); ++i) {
				Module m = list.get(i);
				try {
					m.resetToDefaults();
					JSONObject saved = mods != null ? mods.optJSONObject(m.getName()) : null;
					if (saved != null) {
						m.fromJson(saved);
					}
				} catch (RuntimeException e) {
					LOGGER.warn("Couldn't load " + m.getName() + ", using its defaults");
					try {
						m.resetToDefaults();
					} catch (RuntimeException ignored) {
						// nothing more we can do
					}
				}
			}
		} finally {
			applying = false;
		}
	}

	// ---- Profiles ---------------------------------------------------------------

	public List<String> getProfiles() {
		return readOnlyProfiles;
	}

	public String getCurrentProfile() {
		return currentProfile;
	}

	/** Saves the current profile, then loads another one. */
	public void switchProfile(String name) {
		if (!profiles.contains(name) || name.equals(currentProfile)) {
			return;
		}
		saveNow();
		currentProfile = name;
		applyProfile(readJson(profileKey(name)));
		saveNow();
	}

	/** Makes a new profile that starts as a copy of the current one, and switches to it. */
	public boolean createProfile(String name) {
		name = cleanName(name);
		if (name == null || profiles.contains(name)) {
			return false;
		}
		saveNow();
		profiles.add(name);
		currentProfile = name;
		saveNow(); // same module settings, saved under the new name
		return true;
	}

	/** Deletes a profile. The last profile can't be deleted. */
	public boolean deleteProfile(String name) {
		if (!profiles.contains(name) || profiles.size() <= 1) {
			return false;
		}
		if (name.equals(currentProfile)) {
			int index = profiles.indexOf(name);
			currentProfile = profiles.get(index == 0 ? 1 : 0);
			applyProfile(readJson(profileKey(currentProfile)));
		}
		profiles.remove(name);
		EagRuntime.setStorage(profileKey(name), null);
		saveNow();
		return true;
	}

	/** Puts every module in the current profile back to default. */
	public void resetCurrentProfile() {
		applyProfile(null);
		saveNow();
	}

	// ---- Import and export ------------------------------------------------------

	/** The current profile as one line of text that can be shared. */
	public String exportProfile() {
		byte[] bytes = profileToJson().toString().getBytes(StandardCharsets.UTF_8);
		return EXPORT_PREFIX + Base64.encodeBase64String(bytes);
	}

	/**
	 * Loads shared text into the current profile.
	 *
	 * @return true if it worked, false if the text isn't a Center Client config
	 */
	public boolean importProfile(String text) {
		if (text == null) {
			return false;
		}
		text = text.trim();
		JSONObject root;
		try {
			String json;
			if (text.startsWith(EXPORT_PREFIX)) {
				json = new String(Base64.decodeBase64(text.substring(EXPORT_PREFIX.length())), StandardCharsets.UTF_8);
			} else {
				json = text; // plain JSON works too
			}
			root = new JSONObject(json);
		} catch (RuntimeException e) {
			return false;
		}
		if (root.optJSONObject("modules") == null) {
			return false;
		}
		applyProfile(root);
		saveNow();
		return true;
	}

	// ---- Storage helpers --------------------------------------------------------

	private static String profileKey(String name) {
		// make the name safe for a storage key: letters and numbers stay, anything else becomes _XX
		StringBuilder sb = new StringBuilder(PROFILE_KEY_PREFIX);
		for (int i = 0; i < name.length(); ++i) {
			char c = name.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
				sb.append(c);
			} else {
				sb.append('_').append(Integer.toHexString(c));
			}
		}
		return sb.toString();
	}

	private static String cleanName(String name) {
		if (name == null) {
			return null;
		}
		name = name.trim();
		if (name.isEmpty()) {
			return null;
		}
		return name.length() > MAX_PROFILE_NAME ? name.substring(0, MAX_PROFILE_NAME) : name;
	}

	private static JSONObject readJson(String key) {
		byte[] data = EagRuntime.getStorage(key);
		if (data == null) {
			return null;
		}
		try {
			return new JSONObject(new String(data, StandardCharsets.UTF_8));
		} catch (RuntimeException e) {
			LOGGER.warn("Saved config \"" + key + "\" is broken, using defaults (a copy was kept as " + key
					+ ".broken)");
			EagRuntime.setStorage(key + ".broken", data);
			return null;
		}
	}

	private static void writeJson(String key, JSONObject json) {
		try {
			EagRuntime.setStorage(key, json.toString().getBytes(StandardCharsets.UTF_8));
		} catch (RuntimeException e) {
			LOGGER.error("Couldn't save \"" + key + "\"");
			LOGGER.error(e);
		}
	}
}
