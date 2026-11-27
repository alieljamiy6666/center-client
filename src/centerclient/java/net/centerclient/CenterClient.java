package net.centerclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.centerclient.gui.CenterMainMenu;
import net.centerclient.gui.LoadingScreen;
import net.centerclient.gui.VanillaMainMenu;
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
 * Later steps will start the theme, modules, settings, HUD and menus from
 * inside {@link #init(Minecraft)}.
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
		initialized = true;
		mc = minecraft;

		LOGGER.info(NAME + " " + VERSION + " loaded");

		// Fill the loading bar to 100% before the first menu opens.
		LoadingScreen.finish(minecraft);
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
