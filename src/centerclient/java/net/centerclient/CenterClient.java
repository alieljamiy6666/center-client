package net.centerclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.centerclient.config.ConfigManager;
import net.centerclient.event.EventBus;
import net.centerclient.gui.CenterMainMenu;
import net.centerclient.gui.LoadingScreen;
import net.centerclient.gui.VanillaMainMenu;
import net.centerclient.gui.clickgui.ClickGui;
import net.centerclient.module.ModuleManager;
import net.centerclient.module.hud.ArmorStatusModule;
import net.centerclient.module.hud.ClockModule;
import net.centerclient.module.hud.CoordinatesModule;
import net.centerclient.module.hud.CpsModule;
import net.centerclient.module.hud.FpsModule;
import net.centerclient.module.hud.KeystrokesModule;
import net.centerclient.module.hud.PingModule;
import net.centerclient.module.hud.PotionEffectsModule;
import net.centerclient.module.hud.ServerInfoModule;
import net.centerclient.module.hud.SpeedModule;
import net.centerclient.module.hud.WatermarkModule;
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
		ClickGui.registerOpenKey(EVENTS); // Right Shift opens the Center Client menu
		initialized = true;

		LOGGER.info(NAME + " " + VERSION + " loaded with " + MODULES.getModules().size() + " modules");

		// Fill the loading bar to 100% before the first menu opens.
		LoadingScreen.finish(minecraft);
	}

	/** Every module gets added here. */
	private static void registerModules() {
		// HUD
		MODULES.register(new WatermarkModule());
		MODULES.register(new FpsModule());
		MODULES.register(new PingModule());
		MODULES.register(new SpeedModule());
		MODULES.register(new CoordinatesModule());
		MODULES.register(new ClockModule());
		MODULES.register(new ServerInfoModule());
		MODULES.register(new ArmorStatusModule());
		MODULES.register(new PotionEffectsModule());
		MODULES.register(new KeystrokesModule());
		MODULES.register(new CpsModule());
		// Misc
		MODULES.register(new TestModule());
	}

	/**
	 * Swaps the vanilla title screen for the Center Client one. Called every
	 * time the game changes screens. Only a plain GuiMainMenu is swapped, so
	 * {@link VanillaMainMenu} (the "Vanilla menu" button) still gets through.
	 */
	public static GuiScreen replaceScreen(GuiScreen screen) {
		if (screen != null && screen.getClass() == GuiMainMenu.class) {
			screen = new CenterMainMenu();
		}
		// one short line per screen change; helps when reading a crash log (and the automatic start-up test uses it)
		LOGGER.info("Screen: " + (screen == null ? "none (playing)" : screen.getClass().getSimpleName()));
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
