package net.centerclient.gui;

import net.minecraft.client.gui.GuiMainMenu;

/**
 * The original title screen, opened by the "Vanilla menu" button on the
 * Center Client menu, just in case. It's its own class only so that
 * CenterClient.replaceScreen() can tell it apart and lets it through.
 */
public class VanillaMainMenu extends GuiMainMenu {
}
