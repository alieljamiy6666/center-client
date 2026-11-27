package net.centerclient.module.hud;

import net.centerclient.config.BooleanSetting;
import net.centerclient.config.ModeSetting;
import net.centerclient.hud.TextHudModule;
import net.centerclient.module.Category;
import net.centerclient.theme.Theme;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/** Your X, Y and Z, which way you're facing, and the biome you're in. */
public class CoordinatesModule extends TextHudModule {

	private static final String[] DIRECTIONS = { "South (+Z)", "West (-X)", "North (-Z)", "East (+X)" };

	private final ModeSetting layout = add(new ModeSetting("Layout", "Lines", "Lines", "One line"));
	private final BooleanSetting showDirection = add(new BooleanSetting("Show direction", true));
	private final BooleanSetting showBiome = add(new BooleanSetting("Show biome", true));

	// what's on screen now, so the text is only rebuilt when something changes
	private int shownX = Integer.MIN_VALUE;
	private int shownY;
	private int shownZ;
	private int shownDirection = -1;
	private String shownBiome = "";
	private String shownLayout = "";
	private boolean shownWithDirection;
	private boolean shownWithBiome;

	public CoordinatesModule() {
		super("Coordinates", "Shows your position, the direction you're facing and the biome", Category.HUD, START,
				START, 0.005F, 0.31F);
	}

	@Override
	protected void update() {
		int x;
		int y;
		int z;
		int direction;
		String biome = shownBiome;
		if (mc.player == null || mc.world == null) {
			x = 0; // example values for the HUD editor
			y = 64;
			z = 0;
			direction = 2;
			biome = "Plains";
		} else {
			x = MathHelper.floor(mc.player.posX);
			y = MathHelper.floor(mc.player.posY);
			z = MathHelper.floor(mc.player.posZ);
			direction = MathHelper.floor(mc.player.rotationYaw * 4.0F / 360.0F + 0.5) & 3;
			if (x != shownX || y != shownY || z != shownZ || shownBiome.isEmpty()) {
				biome = mc.world.getBiome(new BlockPos(x, y, z)).getBiomeName();
			}
		}
		boolean withDirection = showDirection.isOn();
		boolean withBiome = showBiome.isOn();
		String mode = layout.get();
		if (x == shownX && y == shownY && z == shownZ && direction == shownDirection && biome.equals(shownBiome)
				&& mode.equals(shownLayout) && withDirection == shownWithDirection && withBiome == shownWithBiome) {
			return;
		}
		shownX = x;
		shownY = y;
		shownZ = z;
		shownDirection = direction;
		shownBiome = biome;
		shownLayout = mode;
		shownWithDirection = withDirection;
		shownWithBiome = withBiome;

		boolean oneLine = mode.equals("One line");
		int lines = (oneLine ? 1 : 3) + (withDirection ? 1 : 0) + (withBiome ? 1 : 0);
		setLineCount(lines);
		int i = 0;
		if (oneLine) {
			setLine(i++, "XYZ", x + ", " + y + ", " + z, Theme.TEXT);
		} else {
			setLine(i++, "X", Integer.toString(x), Theme.TEXT);
			setLine(i++, "Y", Integer.toString(y), Theme.TEXT);
			setLine(i++, "Z", Integer.toString(z), Theme.TEXT);
		}
		if (withDirection) {
			setLine(i++, "Facing", DIRECTIONS[direction], Theme.TEXT);
		}
		if (withBiome) {
			setLine(i, "Biome", biome, Theme.TEXT);
		}
	}
}
