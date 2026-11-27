package net.centerclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.centerclient.config.ConfigManager;
import net.centerclient.event.EventBus;
import net.centerclient.gui.CenterMainMenu;
import net.centerclient.gui.LoadingScreen;
import net.centerclient.gui.VanillaMainMenu;
import net.centerclient.module.ModuleManager;
import net.centerclient.module.misc.TestModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;

/**
 * The heart of Center Client.
 *
 * Everything Center Client adds to the game starts here. The game calls
 * {@link #init(Minecraft)} once at startup, after the window, textures, fonts
 * and sounds are ready, and just before the first menu opens.
 *
 * It holds the event bus ({@link #EVENTS}) and the module list
 * ({@link #MODULES}). New modules are added in {@link #registerModules()}.
 */
public final class CenterClient {

	/** The client's name, shown on screens and in the browser tab. */
	public static final String NAME = "Center Client";

	/**
	 * The client's version. Change it for every release: 1.0.1 for small fixes,
	 * 1.1.0 for new features, 2.0.0 for big changes.
	 */
	public static final String VERSION = "1.0.0";

	/** The Minecraft version Center Client is built on. */
	public static final String MC_VERSION = "1.12.2";

	/** Youth Center SMP, used by the Join button. */
	public static final String SERVER_NAME = "Youth Center SMP";
	public static final String SERVER_ADDRESS = "wss://youthcentersmp.eagler.host";

	private static final Logger LOGGER = LogManager.getLogger(NAME);

	/** Delivers game events (ticks, frames, keys, chat...) to modules. */
	public static final EventBus EVENTS = new EventBus();

	/** Every Center Client module. Ready once {@link #init(Minecraft)} has run. */
	public static ModuleManager MODULES;

	/** Saves and loads settings and profiles. Ready once {@link #init(Minecraft)} has run. */
	public static ConfigManager CONFIG;

	private static Minecraft mc;
	private static boolean initialized = false;

	private CenterClient() {
		// Nobody makes a CenterClient object: everything here is static.
	}

	/**
	 * Starts Center Client. Called once from Minecraft.startGame().
	 *
	 * @param minecraft the running game
	 */
	public static void init(Minecraft minecraft) {
		if (initialized) {
			return; // Only start once, even if something calls this twice.
		}
		mc = minecraft;

		MODULES = new ModuleManager(EVENTS);
		registerModules();
		CONFIG = new ConfigManager(MODULES, EVENTS);
		CONFIG.load(); // brings back saved settings, profiles and the accent color
		initialized = true;

		LOGGER.info(NAME + " " + VERSION + " loaded with " + MODULES.getModules().size() + " modules");

		// Fill the loading bar to 100% before the first menu opens.
		LoadingScreen.finish(minecraft);
	}

	/** Every module gets added here. */
	private static void registerModules() {
		MODULES.register(new TestModule());
	}

	/**
	 * Swaps the vanilla title screen for the Center Client one. Called every
	 * time the game changes screens. Only a plain GuiMainMenu is swapped, so
	 * {@link VanillaMainMenu} (the "Vanilla menu" button) still gets through.
	 */
	public static GuiScreen replaceScreen(GuiScreen screen) {
		if (screen != null && screen.getClass() == GuiMainMenu.class) {
			return new CenterMainMenu();
		}
		return screen;
	}

	/** @return the running game, or null before {@link #init(Minecraft)} has run */
	public static Minecraft getMinecraft() {
		return mc;
	}

	/** @return true once Center Client has started */
	public static boolean isInitialized() {
		return initialized;
	}
}
