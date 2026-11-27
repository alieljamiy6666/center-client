package net.centerclient.gui.clickgui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.centerclient.CenterClient;
import net.centerclient.event.EventBus;
import net.centerclient.event.KeyEvent;
import net.centerclient.event.Listener;
import net.centerclient.gui.CenterSettingsScreen;
import net.centerclient.gui.GuiUtil;
import net.centerclient.gui.PixelIcon;
import net.centerclient.module.Category;
import net.centerclient.module.Module;
import net.centerclient.theme.Theme;
import net.centerclient.util.Animation;
import net.centerclient.util.ColorUtil;
import net.centerclient.util.Easing;
import net.centerclient.util.RenderUtil;
import net.lax1dude.eaglercraft.Keyboard;
import net.lax1dude.eaglercraft.KeyboardConstants;
import net.lax1dude.eaglercraft.Mouse;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ChatAllowedCharacters;

/**
 * The main Center Client menu, opened with Right Shift.
 *
 * Left: a sidebar with the category tabs (All, Render, HUD, Misc, PvP) and
 * buttons for profiles, client settings and the HUD editor.
 * Right: a search box and a grid of module cards. Click a card to turn the
 * module on or off; click its gear (or right-click the card) to slide out its
 * settings. Everything is usable with just the mouse.
 *
 * It fades and grows in when it opens, darkens the game behind it, and
 * shrinks and fades out when it closes.
 */
public class ClickGui extends GuiScreen {

	private static final int RADIUS = 8;
	private static final int SIDEBAR_WIDTH = 92;
	private static final int SIDEBAR_COMPACT = 28;
	private static final int GAP = 6;
	private static final int PADDING = 8;
	private static final int SEARCH_HEIGHT = 16;
	private static final int TAB_HEIGHT = 18;
	private static final int MAX_SEARCH = 32;
	private static final int PROFILE_ROW = 14;

	private static final Category[] TABS = Category.values();
	private static final PixelIcon[] TAB_ICONS = { PixelIcon.ALL, PixelIcon.RENDER, PixelIcon.HUD, PixelIcon.MISC,
			PixelIcon.PVP };
	private static final String[] BOTTOM_LABELS = { "Profiles", "Settings", "HUD Editor" };
	private static final PixelIcon[] BOTTOM_ICONS = { PixelIcon.PROFILE, PixelIcon.GEAR, PixelIcon.EDIT_HUD };
	private static final int BOTTOM_PROFILES = 0;
	private static final int BOTTOM_SETTINGS = 1;
	private static final int BOTTOM_HUD = 2;

	// ---- Opening with Right Shift ---------------------------------------------------

	private static final Listener<KeyEvent> OPEN_KEY = ClickGui::onOpenKey;

	/** Makes Right Shift open the menu while playing. Called once at startup. */
	public static void registerOpenKey(EventBus bus) {
		bus.subscribe(KeyEvent.class, OPEN_KEY);
	}

	private static void onOpenKey(KeyEvent event) {
		if (event.isPressed() && !event.isRepeat() && !event.isInGui()
				&& event.getKey() == KeyboardConstants.KEY_RSHIFT) {
			Minecraft mc = Minecraft.getMinecraft();
			if (mc.world != null && mc.currentScreen == null) {
				mc.displayGuiScreen(new ClickGui(null));
			}
		}
	}

	// ---- State ----------------------------------------------------------------------

	private final GuiScreen parent;
	private final long openedAt;
	private final Animation open = new Animation(220L, Easing.EASE_OUT, 0.0F);
	private final Animation panelSlide = new Animation(220L, Easing.EASE_OUT, 0.0F);
	private final Animation contentFade = new Animation(200L, Easing.EASE_OUT, 1.0F);
	private final Animation selector = new Animation(200L, Easing.EASE_OUT, 0.0F);
	private final Animation[] tabHover = new Animation[TABS.length];
	private final Animation[] bottomHover = new Animation[BOTTOM_LABELS.length];
	private boolean closing;

	private Category category = Category.ALL;
	private final List<ModuleCard> allCards = new ArrayList<ModuleCard>();
	private final List<ModuleCard> shownCards = new ArrayList<ModuleCard>();
	private String search = "";
	private String searchShown = "";
	private String headerTitle = "";
	private String headerCount = "";
	private String emptyText = "";

	private float scroll;
	private float scrollTarget;
	private float maxScroll;
	private long lastFrame;
	private float frameSeconds;

	private final SettingsPanel panel = new SettingsPanel();
	private boolean panelOpen;
	private boolean profilesOpen;

	// layout
	private int winX;
	private int winY;
	private int winW;
	private int winH;
	private int sideW;
	private boolean compact;
	private int contentX;
	private int contentW;
	private int headerY;
	private int gridTop;
	private int gridBottom;
	private int cols;
	private int searchX;
	private int searchY;
	private int searchW;
	private int tabsTop;
	private int bottomTop;

	public ClickGui(GuiScreen parent) {
		this.parent = parent;
		this.openedAt = Minecraft.getSystemTime();
		List<Module> modules = CenterClient.MODULES.getModules();
		for (int i = 0; i < modules.size(); ++i) {
			allCards.add(new ModuleCard(modules.get(i)));
		}
		for (int i = 0; i < tabHover.length; ++i) {
			tabHover[i] = new Animation(120L, Easing.EASE_OUT, 0.0F);
		}
		for (int i = 0; i < bottomHover.length; ++i) {
			bottomHover[i] = new Animation(120L, Easing.EASE_OUT, 0.0F);
		}
		open.animateTo(1.0F);
	}

	// ---- Layout ---------------------------------------------------------------------

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true); // so holding Backspace keeps deleting
		if (parent != null) {
			parent.setWorldAndResolution(this.mc, this.width, this.height);
		}
		winW = Math.min(this.width - 16, 440);
		winH = Math.min(this.height - 16, 270);
		winX = (this.width - winW) / 2;
		winY = (this.height - winH) / 2;
		compact = winW < 340;
		sideW = compact ? SIDEBAR_COMPACT : SIDEBAR_WIDTH;
		contentX = winX + sideW + PADDING;
		contentW = winX + winW - PADDING - contentX;
		headerY = winY + PADDING;
		if (contentW < 230) {
			// narrow: search box goes under the title
			searchX = contentX;
			searchW = contentW;
			searchY = headerY + 13;
			gridTop = searchY + SEARCH_HEIGHT + 6;
		} else {
			searchW = Math.min(140, contentW / 2);
			searchX = contentX + contentW - searchW;
			searchY = headerY;
			gridTop = headerY + SEARCH_HEIGHT + 8;
		}
		gridBottom = winY + winH - PADDING;
		cols = contentW >= 260 ? 2 : 1;
		tabsTop = winY + (compact ? 10 : 32);
		bottomTop = winY + winH - PADDING - BOTTOM_LABELS.length * (TAB_HEIGHT + 2) + 2;
		selector.set(tabY(categoryIndex()));
		refreshCards();
	}

	private int tabY(int index) {
		return tabsTop + index * (TAB_HEIGHT + 2);
	}

	private int bottomY(int index) {
		return bottomTop + index * (TAB_HEIGHT + 2);
	}

	private int categoryIndex() {
		for (int i = 0; i < TABS.length; ++i) {
			if (TABS[i] == category) {
				return i;
			}
		}
		return 0;
	}

	/** Works out which cards to show (category or search) and where they go. */
	private void refreshCards() {
		shownCards.clear();
		String query = search.trim().toLowerCase();
		for (int i = 0; i < allCards.size(); ++i) {
			ModuleCard card = allCards.get(i);
			Module m = card.module;
			boolean show = query.isEmpty() ? category.contains(m)
					: m.getName().toLowerCase().contains(query) || m.getDescription().toLowerCase().contains(query);
			if (show) {
				shownCards.add(card);
			}
		}
		int n = shownCards.size();
		headerTitle = query.isEmpty() ? category.getDisplayName() : "Search";
		headerCount = n + (n == 1 ? " module" : " modules");
		emptyText = query.isEmpty() ? "No modules here yet" : "No modules match \"" + search.trim() + "\"";
		searchShown = this.fontRendererObj == null ? search
				: this.fontRendererObj.trimStringToWidth(search, searchW - 22, true);

		int cardW = (contentW - (cols - 1) * GAP) / Math.max(1, cols);
		for (int i = 0; i < n; ++i) {
			ModuleCard card = shownCards.get(i);
			card.x = contentX + (i % cols) * (cardW + GAP);
			card.y = gridTop + (i / cols) * (ModuleCard.HEIGHT + GAP);
			card.width = cardW;
		}
		int rows = (n + cols - 1) / Math.max(1, cols);
		int total = rows * (ModuleCard.HEIGHT + GAP) - GAP;
		maxScroll = Math.max(0, total - (gridBottom - gridTop));
		scrollTarget = Math.max(0.0F, Math.min(maxScroll, scrollTarget));
		scroll = Math.max(0.0F, Math.min(maxScroll, scroll));
	}

	// ---- Drawing --------------------------------------------------------------------

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		long now = Minecraft.getSystemTime();
		frameSeconds = lastFrame == 0L ? 0.016F : Math.min(0.1F, (now - lastFrame) / 1000.0F);
		lastFrame = now;
		if (closing && open.isDone()) {
			this.mc.displayGuiScreen(parent);
			return;
		}
		if (parent != null) {
			parent.drawScreen(-1000, -1000, partialTicks); // the menu we came from, behind us
		}

		float o = open.get();
		RenderUtil.rect(0, 0, this.width, this.height, ColorUtil.withAlpha(0xFF000000, 0.5F * o));

		float scale = 0.92F + 0.08F * o;
		float cx = this.width / 2.0F;
		float cy = this.height / 2.0F;
		GlStateManager.pushMatrix();
		GlStateManager.translate(cx, cy, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		GlStateManager.translate(-cx, -cy, 0.0F);
		RenderUtil.setGlobalAlpha(o);
		RenderUtil.setClipScale(cx, cy, scale);
		try {
			drawWindow(mouseX, mouseY, o, now);
		} finally {
			RenderUtil.endClip();
			RenderUtil.setClipScale(0.0F, 0.0F, 1.0F);
			RenderUtil.setGlobalAlpha(1.0F);
			GlStateManager.popMatrix();
		}
	}

	private void drawWindow(int mouseX, int mouseY, float o, long now) {
		RenderUtil.shadow(winX, winY + 3, winW, winH, RADIUS, 10, 0xA0000000);
		RenderUtil.roundedRect(winX, winY, winW, winH, RADIUS, Theme.BACKGROUND);
		RenderUtil.roundedRect(winX, winY, sideW, winH, RADIUS, Theme.PANEL);
		RenderUtil.rect(winX + sideW - RADIUS, winY, RADIUS, winH, Theme.PANEL);
		RenderUtil.rect(winX + sideW, winY + 1, 1, winH - 2, Theme.BORDER);
		RenderUtil.roundedOutline(winX, winY, winW, winH, RADIUS, 1, Theme.BORDER);

		boolean blocked = profilesOpen; // a popup is open: nothing else reacts to the mouse
		int mx = blocked ? -1000 : mouseX;
		drawSidebar(mx, mouseY);
		drawHeader(mx, mouseY, now);

		boolean overPanel = panelOpen && panel.isInside(mouseX, mouseY);
		float cf = contentFade.get();
		RenderUtil.setGlobalAlpha(o * cf);
		drawGrid(blocked || overPanel ? -1000 : mouseX, mouseY, cf);
		RenderUtil.setGlobalAlpha(o);

		float p = panelSlide.get();
		if (p > 0.01F && panel.getModule() != null) {
			int pw = compact ? contentW + PADDING : Math.min(contentW, Math.max(180, (int) (contentW * 0.62F)));
			int px = winX + winW - PADDING - pw;
			RenderUtil.setGlobalAlpha(o * p);
			panel.draw(this.fontRendererObj, (int) (px + (1.0F - p) * 30.0F), winY + 6, pw, winH - 12,
					blocked ? -1000 : mouseX, mouseY, frameSeconds);
			RenderUtil.setGlobalAlpha(o);
		}

		if (profilesOpen) {
			drawProfiles(mouseX, mouseY);
		}
	}

	private void drawSidebar(int mouseX, int mouseY) {
		if (compact) {
			GuiUtil.textCentered(this.fontRendererObj, "C", winX + sideW / 2.0F, winY + 4, Theme.accent(), true);
		} else {
			GuiUtil.text(this.fontRendererObj, "Center", winX + 10, winY + 10, Theme.accent(), true);
			GuiUtil.text(this.fontRendererObj, "Client", winX + 10 + this.fontRendererObj.getStringWidth("Center "),
					winY + 10, Theme.TEXT_FADED, false);
			RenderUtil.rect(winX + 8, winY + 24, sideW - 16, 1, Theme.BORDER);
		}

		// the highlight glides to the selected tab
		selector.animateTo(tabY(categoryIndex()));
		float selY = selector.get();
		RenderUtil.roundedRect(winX + 4, selY, sideW - 8, TAB_HEIGHT, 4, ColorUtil.withAlpha(Theme.accent(), 0.18F));
		RenderUtil.roundedRect(winX + 4, selY + 4, 2, TAB_HEIGHT - 8, 1, Theme.accent());

		for (int i = 0; i < TABS.length; ++i) {
			int y = tabY(i);
			boolean selected = TABS[i] == category;
			boolean over = isInside(mouseX, mouseY, winX + 4, y, sideW - 8, TAB_HEIGHT);
			tabHover[i].animateTo(over ? 1.0F : 0.0F);
			float h = tabHover[i].get();
			if (h > 0.02F && !selected) {
				RenderUtil.roundedRect(winX + 4, y, sideW - 8, TAB_HEIGHT, 4, ColorUtil.withAlpha(Theme.PANEL_HOVER, h));
			}
			int color = selected ? Theme.accent() : ColorUtil.mix(Theme.TEXT_FADED, Theme.TEXT, h);
			drawSidebarItem(TAB_ICONS[i], TABS[i].getDisplayName(), y, color, h);
		}

		for (int i = 0; i < BOTTOM_LABELS.length; ++i) {
			int y = bottomY(i);
			boolean disabled = i == BOTTOM_HUD; // arrives in Step 9
			boolean over = !disabled && isInside(mouseX, mouseY, winX + 4, y, sideW - 8, TAB_HEIGHT);
			bottomHover[i].animateTo(over || (i == BOTTOM_PROFILES && profilesOpen) ? 1.0F : 0.0F);
			float h = bottomHover[i].get();
			if (h > 0.02F) {
				RenderUtil.roundedRect(winX + 4, y, sideW - 8, TAB_HEIGHT, 4, ColorUtil.withAlpha(Theme.PANEL_HOVER, h));
			}
			int color = disabled ? ColorUtil.withAlpha(Theme.TEXT_FADED, 0.5F)
					: ColorUtil.mix(Theme.TEXT_FADED, Theme.TEXT, h);
			drawSidebarItem(BOTTOM_ICONS[i], BOTTOM_LABELS[i], y, color, h);
			if (disabled && !compact) {
				GuiUtil.text(this.fontRendererObj, "soon", winX + sideW - 8 - this.fontRendererObj.getStringWidth("soon"),
						y + 5, ColorUtil.withAlpha(Theme.TEXT_FADED, 0.5F), false);
			}
		}
	}

	private void drawSidebarItem(PixelIcon icon, String label, int y, int color, float hover) {
		if (compact) {
			icon.draw(winX + (sideW - icon.getWidth()) / 2.0F, y + (TAB_HEIGHT - icon.getHeight()) / 2.0F, 1.0F, color);
		} else {
			float nudge = hover * 2.0F; // slides right a little on hover
			icon.draw(winX + 11 + nudge, y + (TAB_HEIGHT - icon.getHeight()) / 2.0F, 1.0F, color);
			GuiUtil.text(this.fontRendererObj, label, winX + 23 + nudge, y + 5, color, false);
		}
	}

	private void drawHeader(int mouseX, int mouseY, long now) {
		GuiUtil.text(this.fontRendererObj, headerTitle, contentX, headerY + 4, Theme.TEXT, true);
		int titleW = this.fontRendererObj.getStringWidth(headerTitle);
		if (searchY > headerY || contentX + titleW + 8 + this.fontRendererObj.getStringWidth(headerCount) < searchX - 6) {
			GuiUtil.text(this.fontRendererObj, headerCount, contentX + titleW + 6, headerY + 4, Theme.TEXT_FADED, false);
		}

		boolean over = isInside(mouseX, mouseY, searchX, searchY, searchW, SEARCH_HEIGHT);
		RenderUtil.roundedRect(searchX, searchY, searchW, SEARCH_HEIGHT, 4, over ? Theme.PANEL_HOVER : Theme.PANEL);
		RenderUtil.roundedOutline(searchX, searchY, searchW, SEARCH_HEIGHT, 4, 1,
				search.isEmpty() ? Theme.BORDER : Theme.accent());
		PixelIcon.SEARCH.draw(searchX + 5, searchY + 4.5F, 1.0F, search.isEmpty() ? Theme.TEXT_FADED : Theme.accent());
		if (search.isEmpty()) {
			GuiUtil.text(this.fontRendererObj, "Search modules...", searchX + 16, searchY + 4,
					ColorUtil.withAlpha(Theme.TEXT_FADED, 0.8F), false);
		} else {
			GuiUtil.text(this.fontRendererObj, searchShown, searchX + 16, searchY + 4, Theme.TEXT, false);
		}
		if ((now / 500L) % 2L == 0L && !panel.isListening()) {
			int caretX = searchX + 16 + (search.isEmpty() ? 0 : this.fontRendererObj.getStringWidth(searchShown));
			RenderUtil.rect(caretX, searchY + 4, 1, 8, Theme.accent());
		}
	}

	private void drawGrid(int mouseX, int mouseY, float fade) {
		scroll += (scrollTarget - scroll) * Math.min(1.0F, frameSeconds * 14.0F);
		float slide = (1.0F - fade) * 10.0F;
		int mx = mouseY >= gridTop && mouseY < gridBottom ? mouseX : -1000;
		RenderUtil.beginClip(contentX - 6, gridTop - 3, contentW + 12, gridBottom - gridTop + 6);
		try {
			if (shownCards.isEmpty()) {
				GuiUtil.textCentered(this.fontRendererObj, emptyText, contentX + contentW / 2.0F, gridTop + 30,
						Theme.TEXT_FADED, false);
			}
			Module selected = panelOpen ? panel.getModule() : null;
			for (int i = 0; i < shownCards.size(); ++i) {
				ModuleCard card = shownCards.get(i);
				float top = card.y - scroll;
				if (top + ModuleCard.HEIGHT < gridTop - 6 || top > gridBottom + 6) {
					continue; // out of view
				}
				card.draw(this.fontRendererObj, mx, mouseY, scroll - slide, card.module == selected);
			}
		} finally {
			RenderUtil.endClip();
		}
		if (maxScroll > 0.0F) {
			float viewH = gridBottom - gridTop;
			float barH = Math.max(14.0F, viewH * viewH / (viewH + maxScroll));
			float barY = gridTop + (viewH - barH) * (scroll / maxScroll);
			RenderUtil.roundedRect(winX + winW - 5, barY, 2, barH, 1, Theme.BORDER);
		}
	}

	private void drawProfiles(int mouseX, int mouseY) {
		List<String> names = CenterClient.CONFIG.getProfiles();
		String current = CenterClient.CONFIG.getCurrentProfile();
		int w = 110;
		int h = names.size() * PROFILE_ROW + 20;
		int x = winX + sideW + 4;
		int y = bottomY(BOTTOM_PROFILES) + TAB_HEIGHT - h;
		RenderUtil.shadow(x, y + 2, w, h, 5, 5, 0x90000000);
		RenderUtil.roundedRect(x, y, w, h, 5, Theme.PANEL);
		RenderUtil.roundedOutline(x, y, w, h, 5, 1, Theme.accent());
		GuiUtil.text(this.fontRendererObj, "Profile", x + 6, y + 5, Theme.TEXT_FADED, false);
		for (int i = 0; i < names.size(); ++i) {
			int ry = y + 16 + i * PROFILE_ROW;
			boolean over = isInside(mouseX, mouseY, x + 2, ry, w - 4, PROFILE_ROW);
			boolean isCurrent = names.get(i).equals(current);
			if (over) {
				RenderUtil.roundedRect(x + 2, ry, w - 4, PROFILE_ROW, 3, ColorUtil.withAlpha(Theme.accent(), 0.2F));
			}
			if (isCurrent) {
				RenderUtil.circle(x + 9, ry + 7, 2.5F, Theme.accent());
			}
			GuiUtil.text(this.fontRendererObj, names.get(i), x + 16, ry + 3, isCurrent ? Theme.accent() : Theme.TEXT, false);
		}
	}

	// ---- Input ----------------------------------------------------------------------

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (closing) {
			return;
		}
		if (profilesOpen) {
			clickProfiles(mouseX, mouseY);
			profilesOpen = false;
			return;
		}
		if (panelOpen) {
			if (panel.isOverClose(mouseX, mouseY)) {
				closePanel();
				playClick();
				return;
			}
			if (panel.mouseClicked(mouseX, mouseY, mouseButton)) {
				return;
			}
			closePanel(); // clicked outside the panel
			if (!isInside(mouseX, mouseY, winX, winY, winW, winH)) {
				return;
			}
		}
		if (!isInside(mouseX, mouseY, winX, winY, winW, winH)) {
			requestClose(); // clicked outside the menu
			return;
		}
		for (int i = 0; i < TABS.length; ++i) {
			if (isInside(mouseX, mouseY, winX + 4, tabY(i), sideW - 8, TAB_HEIGHT)) {
				selectCategory(TABS[i]);
				playClick();
				return;
			}
		}
		if (isInside(mouseX, mouseY, winX + 4, bottomY(BOTTOM_PROFILES), sideW - 8, TAB_HEIGHT)) {
			profilesOpen = true;
			playClick();
			return;
		}
		if (isInside(mouseX, mouseY, winX + 4, bottomY(BOTTOM_SETTINGS), sideW - 8, TAB_HEIGHT)) {
			playClick();
			this.mc.displayGuiScreen(new CenterSettingsScreen(this));
			return;
		}
		if (mouseY >= gridTop && mouseY < gridBottom) {
			for (int i = 0; i < shownCards.size(); ++i) {
				ModuleCard card = shownCards.get(i);
				if (!card.isOver(mouseX, mouseY, scroll)) {
					continue;
				}
				if (mouseButton == 1 || card.isOverGear(mouseX, mouseY, scroll)) {
					openPanel(card.module);
				} else if (mouseButton == 0) {
					card.module.toggle();
				}
				playClick();
				return;
			}
		}
	}

	private void clickProfiles(int mouseX, int mouseY) {
		List<String> names = CenterClient.CONFIG.getProfiles();
		int w = 110;
		int h = names.size() * PROFILE_ROW + 20;
		int x = winX + sideW + 4;
		int y = bottomY(BOTTOM_PROFILES) + TAB_HEIGHT - h;
		for (int i = 0; i < names.size(); ++i) {
			if (isInside(mouseX, mouseY, x + 2, y + 16 + i * PROFILE_ROW, w - 4, PROFILE_ROW)) {
				CenterClient.CONFIG.switchProfile(names.get(i));
				playClick();
				return;
			}
		}
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if (panelOpen) {
			panel.mouseDragged(mouseX, mouseY);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		panel.mouseReleased();
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		if (wheel == 0 || closing) {
			return;
		}
		int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
		int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
		int direction = wheel > 0 ? -1 : 1;
		if (panelOpen && panel.isInside(mouseX, mouseY)) {
			panel.scroll(direction);
		} else {
			scrollTarget = Math.max(0.0F, Math.min(maxScroll, scrollTarget + direction * 30.0F));
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (closing) {
			return;
		}
		if (panelOpen && panel.isListening()) {
			panel.keyTyped(typedChar, keyCode);
			return;
		}
		if (keyCode == KeyboardConstants.KEY_RSHIFT) {
			// ignore the same Right Shift press that opened the menu, and held-down repeats
			if (!Keyboard.isRepeatEvent() && Minecraft.getSystemTime() - openedAt > 150L) {
				requestClose();
			}
			return;
		}
		if (keyCode == KeyboardConstants.KEY_ESCAPE) {
			if (profilesOpen) {
				profilesOpen = false;
			} else if (panelOpen) {
				closePanel();
			} else {
				requestClose();
			}
			return;
		}
		if (keyCode == KeyboardConstants.KEY_BACK) {
			if (!search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
				scrollTarget = 0.0F;
				refreshCards();
			}
			return;
		}
		if (ChatAllowedCharacters.isAllowedCharacter(typedChar) && search.length() < MAX_SEARCH) {
			search += typedChar;
			scrollTarget = 0.0F;
			refreshCards();
		}
	}

	// ---- Actions --------------------------------------------------------------------

	private void selectCategory(Category newCategory) {
		category = newCategory;
		search = "";
		scroll = scrollTarget = 0.0F;
		contentFade.set(0.0F);
		contentFade.animateTo(1.0F);
		refreshCards();
	}

	private void openPanel(Module module) {
		if (panel.getModule() != module) {
			panel.open(module);
		}
		panelOpen = true;
		panelSlide.animateTo(1.0F);
	}

	private void closePanel() {
		panelOpen = false;
		panelSlide.animateTo(0.0F);
	}

	private void requestClose() {
		if (!closing) {
			closing = true;
			open.animateTo(0.0F);
			CenterClient.CONFIG.saveNow();
		}
	}

	private void playClick() {
		this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	private static boolean isInside(int mouseX, int mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
