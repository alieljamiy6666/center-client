package net.centerclient.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.centerclient.event.EventBus;
import net.centerclient.event.KeyEvent;
import net.centerclient.event.Listener;
import net.minecraft.client.Minecraft;

/**
 * Keeps the list of every Center Client module, finds modules by name or
 * category, and toggles modules when their key is pressed.
 */
public final class ModuleManager {

	private final List<Module> modules = new ArrayList<Module>();
	private final List<Module> readOnlyModules = Collections.unmodifiableList(modules);
	private final Listener<KeyEvent> keyListener = this::onKey;

	public ModuleManager(EventBus bus) {
		bus.subscribe(KeyEvent.class, keyListener);
	}

	/** Adds a module. Names must be unique. */
	public <T extends Module> T register(T module) {
		if (getModule(module.getName()) != null) {
			throw new IllegalStateException("Two modules are called " + module.getName());
		}
		modules.add(module);
		return module;
	}

	/** Every module, in the order they were added. */
	public List<Module> getModules() {
		return readOnlyModules;
	}

	/** Finds a module by its name, ignoring capital letters. Returns null if there isn't one. */
	public Module getModule(String name) {
		for (int i = 0; i < modules.size(); ++i) {
			Module m = modules.get(i);
			if (m.getName().equalsIgnoreCase(name)) {
				return m;
			}
		}
		return null;
	}

	/** Finds a module by its class, like getModule(ZoomModule.class). Returns null if there isn't one. */
	@SuppressWarnings("unchecked")
	public <T extends Module> T getModule(Class<T> type) {
		for (int i = 0; i < modules.size(); ++i) {
			Module m = modules.get(i);
			if (m.getClass() == type) {
				return (T) m;
			}
		}
		return null;
	}

	/** The modules under a menu tab. "All" gives every module. Makes a new list, so don't call it every frame. */
	public List<Module> getModules(Category category) {
		List<Module> list = new ArrayList<Module>();
		for (int i = 0; i < modules.size(); ++i) {
			Module m = modules.get(i);
			if (category.contains(m)) {
				list.add(m);
			}
		}
		return list;
	}

	/** Toggles modules when their key is pressed while playing (not while a menu or chat is open). */
	private void onKey(KeyEvent event) {
		if (!event.isPressed() || event.isRepeat() || event.isInGui() || Minecraft.getMinecraft().world == null) {
			return;
		}
		int key = event.getKey();
		if (key == Module.NO_KEY) {
			return;
		}
		for (int i = 0; i < modules.size(); ++i) {
			Module m = modules.get(i);
			if (m.getKey() == key) {
				m.toggle();
			}
		}
	}
}
