package net.centerclient.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.centerclient.CenterClient;
import net.centerclient.gui.clickgui.ClickGui;
import net.centerclient.theme.Theme;
import net.centerclient.util.Animation;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.Easing;
import net.centerclient.util.RenderUtil;
import net.centerclient.util.TextUtil;
import net.lax1dude.eaglercraft.EaglercraftRandom;
import net.lax1dude.eaglercraft.IOUtils;
import net.lax1dude.eaglercraft.KeyboardConstants;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;
import net.lax1dude.eaglercraft.profile.EaglerProfile;
import net.lax1dude.eaglercraft.profile.GuiScreenEditProfile;
import net.lax1dude.eaglercraft.profile.SkinModel;
import net.lax1dude.eaglercraft.sp.gui.GuiScreenIntegratedServerStartup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenFirstLoad;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.IResource;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/**
 * The Center Client title screen.
 *
 * - A dark background with slowly drifting pixel "stars" in the accent color
 * - The Center Client logo (in the accent color) with a splash text
 * - Rounded buttons that slide in one after another and glow on hover
 * - Your skin's face and name in the top-right corner (click to edit your profile)
 * - A small "Vanilla menu" button that opens the original title screen
 */
public class CenterMainMenu extends GuiScreen {

	private static final ResourceLocation SPLASH_TEXTS = new ResourceLocation("texts/splashes.txt");
	private static final EaglercraftRandom RANDOM = new EaglercraftRandom();

	private static final int PARTICLE_COUNT = 70;
	private static final int BUTTON_WIDTH = 200;
	private static final int CONTENT_HEIGHT = Branding.LOGO_HEIGHT + 14 + 22 + 6 + 20 + 4 + 20;

	/** Delay between each part sliding in, and how long each slide takes. */
	private static final long INTRO_STAGGER_MS = 70L;
	private static final long INTRO_SLIDE_MS = 380L;

	// Particles: start position, speed, size, sway and color mix. Made once, never per frame.
	private final float[] particleX = new float[PARTICLE_COUNT];
	private final float[] particleY = new float[PARTICLE_COUNT];
	private final float[] particleSpeed = new float[PARTICLE_COUNT];
	private final float[] particleSize = new float[PARTICLE_COUNT];
	private final float[] particlePhase = new float[PARTICLE_COUNT];
	private final float[] particleTint = new float[PARTICLE_COUNT];

	private final CenterButton joinButton;
	private final CenterButton singleplayerButton;
	private final CenterButton multiplayerButton;
	private final CenterButton settingsButton;
	private final CenterButton optionsButton;
	private final CenterButton vanillaButton;
	private final CenterButton[] introButtons;

	private final Animation cardHover = new Animation(160L, Easing.EASE_OUT, 0.0F);

	private String splashText = "";
	private long openTime = -1L;

	// layout
	private int logoY;
	private int cardX;
	private int cardY;
	private int cardWidth;
	private int cardHeight;
	private String playerName = "";
	private int creditsX;
	private int creditsWidth;

	public CenterMainMenu() {
		joinButton = new CenterButton("Join " + CenterClient.SERVER_NAME, true, this::joinServer);
		singleplayerButton = new CenterButton("Singleplayer", false,
				() -> this.mc.displayGuiScreen(new GuiScreenIntegratedServerStartup(this)));
		multiplayerButton = new CenterButton("Multiplayer", false,
				() -> this.mc.displayGuiScreen(new GuiMultiplayer(this)));
		settingsButton = new CenterButton("Center Settings", false,
				() -> this.mc.displayGuiScreen(new CenterSettingsScreen(this)));
		optionsButton = new CenterButton("Options", false,
				() -> this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings)));
		vanillaButton = new CenterButton("Vanilla menu", false,
				() -> this.mc.displayGuiScreen(new VanillaMainMenu()));
		introButtons = new CenterButton[] { joinButton, singleplayerButton, multiplayerButton, settingsButton,
				optionsButton };
		splashText = pickSplash();
	}

	// ---- Setup --------------------------------------------------------------------

	@Override
	public void initGui() {
		if (!this.mc.gameSettings.hasSeenFirstLoad) {
			this.mc.displayGuiScreen(new GuiScreenFirstLoad(this.mc.gameSettings));
			return;
		}
		if (openTime < 0L) {
			openTime = Minecraft.getSystemTime(); // the slide-in plays once per visit
			spawnParticles();
		}

		int centerX = this.width / 2;
		logoY = Math.max(10, (this.height - CONTENT_HEIGHT) / 2 - 10);
		int joinY = logoY + Branding.LOGO_HEIGHT + 14;
		int row1 = joinY + 22 + 6;
		int row2 = row1 + 24;
		int half = (BUTTON_WIDTH - 4) / 2;
		joinButton.setBounds(centerX - BUTTON_WIDTH / 2, joinY, BUTTON_WIDTH, 22);
		singleplayerButton.setBounds(centerX - BUTTON_WIDTH / 2, row1, half, 20);
		multiplayerButton.setBounds(centerX + 2, row1, half, 20);
		settingsButton.setBounds(centerX - BUTTON_WIDTH / 2, row2, half, 20);
		optionsButton.setBounds(centerX + 2, row2, half, 20);
		vanillaButton.setBounds(4, 4, 70, 14);

		playerName = EaglerProfile.getName();
		cardHeight = 26;
		cardWidth = 6 + 18 + 6 + Math.max(this.fontRendererObj.getStringWidth(playerName),
				this.fontRendererObj.getStringWidth("Edit profile")) + 8;
		cardX = this.width - cardWidth - 4;
		cardY = 4;

		creditsWidth = this.fontRendererObj.getStringWidth("Resources copyright Mojang AB");
		creditsX = this.width - creditsWidth - 2;
	}

	private void spawnParticles() {
		for (int i = 0; i < PARTICLE_COUNT; ++i) {
			particleX[i] = RANDOM.nextFloat();
			particleY[i] = RANDOM.nextFloat();
			particleSpeed[i] = 3.0F + RANDOM.nextFloat() * 9.0F; // GUI pixels per second
			particleSize[i] = RANDOM.nextFloat() < 0.25F ? 2.0F : 1.0F;
			particlePhase[i] = RANDOM.nextFloat() * 6.2832F;
			particleTint[i] = RANDOM.nextFloat();
		}
	}

	private String pickSplash() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		int month = calendar.get(Calendar.MONTH) + 1;
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		if (month == 12 && day == 24) {
			return "Merry Christmas from Youth Center SMP!";
		} else if (month == 1 && day == 1) {
			return "Happy New Year from Center Client!";
		} else if (month == 10 && day == 31) {
			return "Spooky season on Youth Center SMP!";
		}
		List<String> lines = new ArrayList<String>();
		IResource resource = null;
		try {
			resource = Minecraft.getMinecraft().getResourceManager().getResource(SPLASH_TEXTS);
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty()) {
					lines.add(line);
				}
			}
		} catch (IOException e) {
			// no splash texts, that's fine
		} finally {
			IOUtils.closeQuietly((java.io.Closeable) resource);
		}
		return lines.isEmpty() ? "" : lines.get(RANDOM.nextInt(lines.size()));
	}

	private void joinServer() {
		ServerData server = new ServerData(CenterClient.SERVER_NAME, CenterClient.SERVER_ADDRESS, false);
		this.mc.displayGuiScreen(new GuiConnecting(this, this.mc, server));
	}

	// ---- Drawing --------------------------------------------------------------------

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		long now = Minecraft.getSystemTime();
		drawBackground(now);

		// logo slides down into place
		float logoIn = introProgress(now, 0);
		if (logoIn > 0.0F) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0.0F, (logoIn - 1.0F) * 12.0F, 0.0F);
			Branding.drawLogo(this.mc, this.width / 2, logoY);
			drawSplash(now);
			GlStateManager.popMatrix();
		}

		// buttons slide up into place, one after another
		for (int i = 0; i < introButtons.length; ++i) {
			float in = introProgress(now, i + 1);
			if (in <= 0.0F) {
				continue;
			}
			GlStateManager.pushMatrix();
			GlStateManager.translate(0.0F, (1.0F - in) * 14.0F, 0.0F);
			introButtons[i].draw(this.mc, mouseX, mouseY);
			GlStateManager.popMatrix();
		}

		vanillaButton.draw(this.mc, mouseX, mouseY);
		drawPlayerCard(mouseX, mouseY);

		// bottom corners
		this.fontRendererObj.drawString("Minecraft " + CenterClient.MC_VERSION, 2, this.height - 20, Theme.TEXT_FADED,
				true);
		this.fontRendererObj.drawString(CenterClient.NAME + " v" + CenterClient.VERSION, 2, this.height - 10,
				Theme.accent(), true);
		String credit = "Built on Eaglercraft 1.12";
		this.fontRendererObj.drawString(credit, this.width - this.fontRendererObj.getStringWidth(credit) - 2,
				this.height - 20, Theme.TEXT_FADED, true);
		this.fontRendererObj.drawString("Resources copyright Mojang AB", creditsX, this.height - 10, Theme.TEXT_FADED,
				true);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	/** 0 before this part's turn, then eases up to 1. */
	private float introProgress(long now, int order) {
		float t = (float) (now - openTime - order * INTRO_STAGGER_MS) / (float) INTRO_SLIDE_MS;
		if (t <= 0.0F) {
			return 0.0F;
		}
		return t >= 1.0F ? 1.0F : Easing.EASE_OUT.apply(t);
	}

	private void drawBackground(long now) {
		int accent = Theme.accent();
		RenderUtil.gradientVertical(0, 0, this.width, this.height, Theme.BACKGROUND,
				ColorUtil.mix(Theme.BACKGROUND, Theme.accentDark(), 0.22F));

		// drifting pixel stars: their positions come straight from the time,
		// so there's nothing to update and nothing new is made each frame
		float seconds = (float) ((now - openTime) % 1000000L) / 1000.0F;
		float wrapHeight = this.height + 4.0F;
		WorldRenderer batch = RenderUtil.beginBatch();
		for (int i = 0; i < PARTICLE_COUNT; ++i) {
			float y = particleY[i] * wrapHeight - particleSpeed[i] * seconds;
			y = ((y % wrapHeight) + wrapHeight) % wrapHeight - 2.0F;
			float x = particleX[i] * this.width + MathHelper.sin(seconds * 0.5F + particlePhase[i]) * 4.0F;
			float twinkle = 0.25F + 0.35F * (0.5F + 0.5F * MathHelper.sin(seconds * 1.7F + particlePhase[i] * 3.0F));
			int color = ColorUtil.withAlpha(ColorUtil.mix(Theme.TEXT, accent, particleTint[i]), twinkle);
			RenderUtil.batchRect(batch, x, y, particleSize[i], particleSize[i], color);
		}
		RenderUtil.endBatch();

		// darken the very top and bottom a little so the logo and corner text stand out
		RenderUtil.gradientVertical(0, 0, this.width, 40, 0x66000000, 0x00000000);
		RenderUtil.gradientVertical(0, this.height - 40, this.width, 40, 0x00000000, 0x66000000);
	}

	private void drawSplash(long now) {
		if (splashText.isEmpty()) {
			return;
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) (this.width / 2 + 104), (float) (logoY + 32), 0.0F); // over the end of "CENTER", clear of "CLIENT"
		GlStateManager.rotate(-20.0F, 0.0F, 0.0F, 1.0F);
		float pulse = 1.8F - MathHelper.abs(MathHelper.sin((now % 1000L) / 1000.0F * 6.2832F) * 0.1F);
		pulse = pulse * 100.0F / (float) (this.fontRendererObj.getStringWidth(splashText) + 32);
		GlStateManager.scale(pulse, pulse, pulse);
		TextUtil.drawCentered(this.fontRendererObj, splashText, 0.0F, -8.0F, 0xFFFFFF00, true);
		GlStateManager.popMatrix();
	}

	private void drawPlayerCard(int mouseX, int mouseY) {
		boolean over = isOverCard(mouseX, mouseY);
		cardHover.animateTo(over ? 1.0F : 0.0F);
		float h = cardHover.get();

		RenderUtil.shadow(cardX, cardY + 1, cardWidth, cardHeight, 5, 3, 0x70000000);
		if (h > 0.02F) {
			RenderUtil.shadow(cardX, cardY, cardWidth, cardHeight, 5, 4, ColorUtil.withAlpha(Theme.accent(), 0.5F * h));
		}
		RenderUtil.roundedRect(cardX, cardY, cardWidth, cardHeight, 5, ColorUtil.mix(Theme.PANEL, Theme.PANEL_HOVER, h));
		RenderUtil.roundedOutline(cardX, cardY, cardWidth, cardHeight, 5, 1, ColorUtil.mix(Theme.BORDER, Theme.accent(), h));

		drawSkinHead(cardX + 5, cardY + 4, 18);
		this.fontRendererObj.drawString(playerName, cardX + 29, cardY + 4, Theme.TEXT, false);
		this.fontRendererObj.drawString("Edit profile", cardX + 29, cardY + 14,
				ColorUtil.mix(Theme.TEXT_FADED, Theme.accent(), h), false);
	}

	/** Draws the face of the player's skin (plus the hat layer) as a square. */
	private void drawSkinHead(int x, int y, int size) {
		SkinModel model = EaglerProfile.getActiveSkinModel();
		RenderUtil.roundedRect(x - 1, y - 1, size + 2, size + 2, 2, 0xFF000000);
		if (model == null || model.highPoly != null) {
			// special 3D skins don't have a normal face: show the accent color instead
			RenderUtil.roundedRect(x, y, size, size, 2, Theme.accent());
			return;
		}
		float texW = model.width;
		float texH = model.height;
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(EaglerProfile.getActiveSkinResourceLocation());
		Gui.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8, size, size, texW, texH); // face
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		Gui.drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8, 8, size, size, texW, texH); // hat layer
		GlStateManager.disableBlend();
	}

	private boolean isOverCard(int mouseX, int mouseY) {
		return mouseX >= cardX && mouseX < cardX + cardWidth && mouseY >= cardY && mouseY < cardY + cardHeight;
	}

	// ---- Input --------------------------------------------------------------------

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		for (int i = 0; i < introButtons.length; ++i) {
			if (introButtons[i].mouseClicked(this.mc, mouseX, mouseY, mouseButton)) {
				return;
			}
		}
		if (vanillaButton.mouseClicked(this.mc, mouseX, mouseY, mouseButton)) {
			return;
		}
		if (mouseButton == 0 && isOverCard(mouseX, mouseY)) {
			playClick();
			this.mc.displayGuiScreen(new GuiScreenEditProfile(this));
			return;
		}
		if (mouseButton == 0 && mouseX >= creditsX && mouseX < creditsX + creditsWidth && mouseY >= this.height - 10
				&& mouseY < this.height) {
			this.mc.displayGuiScreen(new GuiWinGame(false, null));
		}
	}

	private void playClick() {
		this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		// Right Shift opens the Center Client menu here too; other keys are ignored
		// (Escape shouldn't close the title screen)
		if (keyCode == KeyboardConstants.KEY_RSHIFT) {
			this.mc.displayGuiScreen(new ClickGui(this));
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
