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
        this.xSize = 360;
        this.ySize = 184;
    }

    @Override
    public void initGui() {
        super.initGui();

        int left = this.guiLeft;
        int top = this.guiTop;

        this.buttonList.clear();
        lockButton = new GuiButton(100, left + 14, top + 64, 90, 20, "");
        applyButton = new GuiButton(101, left + 14, top + 84, 90, 20, "Apply");
        previousButton = new GuiButton(102, left + 183, top + 158, 36, 20, "<");
        nextButton = new GuiButton(103, left + 314, top + 158, 36, 20, ">");

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
            int y = guiTop + 25 + row * 18;

            Enchantment enchantment = visibleEnchantments.get(index);
            ResourceLocation id = enchantment.getRegistryName();

            if (id == null) {
                continue;
            }

            int current = levels.containsKey(id) ? levels.get(id) : 0;

            if (mouseX >= guiLeft + 198 && mouseX < guiLeft + 216
                    && mouseY >= y && mouseY < y + 16) {
                levels.put(id, Math.max(0, current - 1));
                return;
            }

            if (mouseX >= guiLeft + 329 && mouseX < guiLeft + 347
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

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF202020);
        drawRect(guiLeft + 5, guiTop + 5, guiLeft + 170, guiTop + ySize - 5, 0xFF303030);
        drawRect(guiLeft + 175, guiTop + 5, guiLeft + xSize - 5, guiTop + ySize - 5, 0xFF303030);

        drawRect(guiLeft + 44, guiTop + 37, guiLeft + 64, guiTop + 57, 0xFF8B8B8B);
        drawRect(guiLeft + 45, guiTop + 38, guiLeft + 63, guiTop + 56, 0xFF161616);

        int start = page * ROWS_PER_PAGE;
        int end = Math.min(start + ROWS_PER_PAGE, visibleEnchantments.size());

        for (int index = start; index < end; index++) {
            int row = index - start;
            int y = guiTop + 25 + row * 18;
            drawRect(guiLeft + 183, y, guiLeft + 350, y + 16, 0xFF444444);
            drawRect(guiLeft + 198, y, guiLeft + 216, y + 16, 0xFF5A5A5A);
            drawRect(guiLeft + 329, y, guiLeft + 347, y + 16, 0xFF5A5A5A);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString("Enchant Selector", 10, 10, 0xFFFFFF);
        this.fontRenderer.drawString("Item", 14, 42, 0xDDDDDD);
        this.fontRenderer.drawString("Enchantments", 183, 10, 0xFFFFFF);

        if (selector.getEnchantStack().isEmpty()) {
            this.fontRenderer.drawString("Place an item in the slot", 14, 25, 0xAAAAAA);
        }

        int start = page * ROWS_PER_PAGE;
        int end = Math.min(start + ROWS_PER_PAGE, visibleEnchantments.size());

        for (int index = start; index < end; index++) {
            int row = index - start;
            int y = 29 + row * 18;

            Enchantment enchantment = visibleEnchantments.get(index);
            ResourceLocation id = enchantment.getRegistryName();

            if (id == null) {
                continue;
            }

            int level = levels.containsKey(id) ? levels.get(id) : 0;
            String name = displayName(enchantment);

            if (name.length() > 20) {
                name = name.substring(0, 20) + "...";
            }

            this.fontRenderer.drawString("-", 204, y, 0xFFFFFF);
            this.fontRenderer.drawString(name, 221, y, level > 0 ? 0xFFFF80 : 0xFFFFFF);
            this.fontRenderer.drawString(Integer.toString(level), 312, y, 0xFFFFFF);
            this.fontRenderer.drawString("+", 335, y, 0xFFFFFF);
        }

        this.fontRenderer.drawString("Page " + (page + 1) + "/" + (maxPage() + 1), 250, 164, 0xBBBBBB);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
