package br.com.owfrigo.enchantselector.gui;

import br.com.owfrigo.enchantselector.EnchantSelectorMod;
import br.com.owfrigo.enchantselector.client.ClientKeyHandler;
import br.com.owfrigo.enchantselector.network.ApplyEnchantmentsMessage;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiEnchantSelector extends GuiContainer {
    private static final int ROWS_PER_PAGE = 7;
    private static final int LEFT_PANEL_WIDTH = 176;
    private static final int RIGHT_PANEL_X = 176;

    // Vanilla-style GUI palette.
    private static final int GUI_BG = 0xFFC6C6C6;
    private static final int GUI_LIGHT = 0xFFFFFFFF;
    private static final int GUI_MID = 0xFF8B8B8B;
    private static final int GUI_DARK = 0xFF555555;
    private static final int GUI_SLOT = 0xFF8B8B8B;
    private static final int GUI_TEXT = 0xFF404040;
    private static final int GUI_DISABLED = 0xFF7A7A7A;
    private static final int GUI_SELECTED = 0xFFFFFF55;

    private final ContainerEnchantSelector selector;
    private final List<Enchantment> visibleEnchantments = new ArrayList<>();
    private final Map<ResourceLocation, Integer> levels = new HashMap<>();

    private boolean unlocked = false;
    private int page = 0;
    private String lastStackSignature = "";

    private GuiButton lockButton;
    private GuiButton applyButton;
    private GuiButton previousButton;
    private GuiButton nextButton;

    public GuiEnchantSelector(ContainerEnchantSelector container) {
        super(container);
        this.selector = container;
        this.xSize = 352;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();

        int left = this.guiLeft;
        int top = this.guiTop;

        this.buttonList.clear();

        lockButton = new GuiButton(100, left + 20, top + 55, 66, 20, "");
        applyButton = new GuiButton(101, left + 90, top + 55, 66, 20, "Apply");

        previousButton = new GuiButton(102, left + RIGHT_PANEL_X + 8, top + 140, 28, 20, "<");
        nextButton = new GuiButton(103, left + RIGHT_PANEL_X + 140, top + 140, 28, 20, ">");

        this.buttonList.add(lockButton);
        this.buttonList.add(applyButton);
        this.buttonList.add(previousButton);
        this.buttonList.add(nextButton);

        refreshFromItem(true);
        updateButtons();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        refreshFromItem(false);
        updateButtons();
    }

    private String stackSignature(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }

        return String.valueOf(stack.getItem().getRegistryName())
                + "|" + stack.getMetadata()
                + "|" + (stack.hasTagCompound() ? stack.getTagCompound().toString() : "");
    }

    private void refreshFromItem(boolean force) {
        ItemStack stack = selector.getEnchantStack();
        String signature = stackSignature(stack);

        if (!force && signature.equals(lastStackSignature)) {
            return;
        }

        lastStackSignature = signature;
        page = 0;
        visibleEnchantments.clear();
        levels.clear();

        if (stack.isEmpty()) {
            return;
        }

        Map<Enchantment, Integer> current = EnchantmentHelper.getEnchantments(stack);
        for (Map.Entry<Enchantment, Integer> entry : current.entrySet()) {
            ResourceLocation id = entry.getKey().getRegistryName();
            if (id != null) {
                levels.put(id, Math.min(10, Math.max(0, entry.getValue())));
            }
        }

        rebuildVisibleEnchantments();
    }

    private boolean allowedLocked(Enchantment enchantment, ItemStack stack) {
        ResourceLocation id = enchantment.getRegistryName();
        return id != null
                && "minecraft".equals(id.getNamespace())
                && enchantment.canApplyAtEnchantingTable(stack);
    }

    private void rebuildVisibleEnchantments() {
        visibleEnchantments.clear();
        ItemStack stack = selector.getEnchantStack();

        if (stack.isEmpty()) {
            return;
        }

        for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS.getValuesCollection()) {
            if (unlocked || allowedLocked(enchantment, stack)) {
                visibleEnchantments.add(enchantment);
            }
        }

        Collections.sort(
                visibleEnchantments,
                Comparator.comparing(this::displayName, String.CASE_INSENSITIVE_ORDER)
        );

        if (page > maxPage()) {
            page = maxPage();
        }
    }

    private String displayName(Enchantment enchantment) {
        return net.minecraft.client.resources.I18n.format(enchantment.getName());
    }

    private int maxPage() {
        return visibleEnchantments.isEmpty() ? 0 : (visibleEnchantments.size() - 1) / ROWS_PER_PAGE;
    }

    private void updateButtons() {
        if (lockButton == null) {
            return;
        }

        lockButton.displayString = unlocked ? "UNLOCKED" : "LOCKED";
        applyButton.enabled = !selector.getEnchantStack().isEmpty();
        previousButton.enabled = page > 0;
        nextButton.enabled = page < maxPage();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 100) {
            unlocked = !unlocked;
            page = 0;
            rebuildVisibleEnchantments();
            updateButtons();
            return;
        }

        if (button.id == 101) {
            EnchantSelectorMod.NETWORK.sendToServer(new ApplyEnchantmentsMessage(unlocked, levels));
            return;
        }

        if (button.id == 102 && page > 0) {
            page--;
            updateButtons();
            return;
        }

        if (button.id == 103 && page < maxPage()) {
            page++;
            updateButtons();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int start = page * ROWS_PER_PAGE;
        int end = Math.min(start + ROWS_PER_PAGE, visibleEnchantments.size());

        for (int index = start; index < end; index++) {
            int row = index - start;
            int y = guiTop + 18 + row * 17;

            Enchantment enchantment = visibleEnchantments.get(index);
            ResourceLocation id = enchantment.getRegistryName();

            if (id == null) {
                continue;
            }

            int current = levels.containsKey(id) ? levels.get(id) : 0;

            if (mouseX >= guiLeft + RIGHT_PANEL_X + 7 && mouseX < guiLeft + RIGHT_PANEL_X + 24
                    && mouseY >= y && mouseY < y + 16) {
                levels.put(id, Math.max(0, current - 1));
                return;
            }

            if (mouseX >= guiLeft + RIGHT_PANEL_X + 152 && mouseX < guiLeft + RIGHT_PANEL_X + 169
                    && mouseY >= y && mouseY < y + 16) {
                levels.put(id, Math.min(10, current + 1));
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == ClientKeyHandler.OPEN_SELECTOR.getKeyCode()) {
            if (this.mc.player != null) {
                this.mc.player.closeScreen();
            }
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void drawVanillaPanel(int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + height, GUI_BG);
        drawRect(x, y, x + width, y + 1, GUI_LIGHT);
        drawRect(x, y, x + 1, y + height, GUI_LIGHT);
        drawRect(x, y + height - 1, x + width, y + height, GUI_DARK);
        drawRect(x + width - 1, y, x + width, y + height, GUI_DARK);
    }

    private void drawVanillaSlot(int x, int y) {
        drawRect(x, y, x + 18, y + 18, GUI_DARK);
        drawRect(x + 1, y + 1, x + 18, y + 18, GUI_LIGHT);
        drawRect(x + 1, y + 1, x + 17, y + 17, GUI_SLOT);
        drawRect(x + 2, y + 2, x + 17, y + 17, 0xFF373737);
    }

    private void drawInsetRow(int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + height, GUI_DARK);
        drawRect(x + 1, y + 1, x + width, y + height, GUI_LIGHT);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, GUI_MID);
        drawRect(x + 2, y + 2, x + width - 1, y + height - 1, GUI_BG);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawVanillaPanel(guiLeft, guiTop, LEFT_PANEL_WIDTH, ySize);
        drawVanillaPanel(guiLeft + RIGHT_PANEL_X, guiTop, 176, ySize);

        // Separation between the inventory and enchantment list.
        drawRect(guiLeft + RIGHT_PANEL_X - 1, guiTop + 4,
                guiLeft + RIGHT_PANEL_X + 1, guiTop + ySize - 4, GUI_DARK);

        // Dedicated item slot.
        drawVanillaSlot(guiLeft + 79, guiTop + 34);

        // Player inventory slots.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawVanillaSlot(guiLeft + 7 + col * 18, guiTop + 83 + row * 18);
            }
        }

        for (int col = 0; col < 9; col++) {
            drawVanillaSlot(guiLeft + 7 + col * 18, guiTop + 141);
        }

        // Right-side enchantment entries.
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(start + ROWS_PER_PAGE, visibleEnchantments.size());

        for (int index = start; index < end; index++) {
            int row = index - start;
            int y = guiTop + 18 + row * 17;

            drawInsetRow(guiLeft + RIGHT_PANEL_X + 5, y, 166, 16);
            drawVanillaSlot(guiLeft + RIGHT_PANEL_X + 6, y - 1);
            drawVanillaSlot(guiLeft + RIGHT_PANEL_X + 151, y - 1);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString("Enchant Selector", 8, 6, GUI_TEXT);
        this.fontRenderer.drawString("Inventory", 8, 74, GUI_TEXT);
        this.fontRenderer.drawString("Enchantments", RIGHT_PANEL_X + 8, 6, GUI_TEXT);

        ItemStack stack = selector.getEnchantStack();
        if (stack.isEmpty()) {
            this.fontRenderer.drawString("Item", 79, 22, GUI_DISABLED);
        } else {
            String itemName = stack.getDisplayName();
            int maxWidth = 150;
            while (this.fontRenderer.getStringWidth(itemName) > maxWidth && itemName.length() > 4) {
                itemName = itemName.substring(0, itemName.length() - 4) + "...";
            }
            this.fontRenderer.drawString(itemName, 8, 22, GUI_TEXT);
        }

        int start = page * ROWS_PER_PAGE;
        int end = Math.min(start + ROWS_PER_PAGE, visibleEnchantments.size());

        for (int index = start; index < end; index++) {
            int row = index - start;
            int y = 22 + row * 17;

            Enchantment enchantment = visibleEnchantments.get(index);
            ResourceLocation id = enchantment.getRegistryName();

            if (id == null) {
                continue;
            }

            int level = levels.containsKey(id) ? levels.get(id) : 0;
            String name = displayName(enchantment);

            while (this.fontRenderer.getStringWidth(name) > 99 && name.length() > 4) {
                name = name.substring(0, name.length() - 4) + "...";
            }

            this.fontRenderer.drawString("-", RIGHT_PANEL_X + 12, y, GUI_TEXT);
            this.fontRenderer.drawString(name, RIGHT_PANEL_X + 28, y,
                    level > 0 ? GUI_SELECTED : GUI_TEXT);
            this.fontRenderer.drawString(Integer.toString(level), RIGHT_PANEL_X + 136, y, GUI_TEXT);
            this.fontRenderer.drawString("+", RIGHT_PANEL_X + 157, y, GUI_TEXT);
        }

        String pageText = (page + 1) + "/" + (maxPage() + 1);
        int pageWidth = this.fontRenderer.getStringWidth(pageText);
        this.fontRenderer.drawString(pageText, RIGHT_PANEL_X + 88 - pageWidth / 2, 146, GUI_TEXT);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
