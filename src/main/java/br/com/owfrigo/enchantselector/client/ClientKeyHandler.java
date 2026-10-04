package br.com.owfrigo.enchantselector.client;

import br.com.owfrigo.enchantselector.EnchantSelectorMod;
import br.com.owfrigo.enchantselector.gui.GuiEnchantSelector;
import br.com.owfrigo.enchantselector.network.OpenSelectorMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class ClientKeyHandler {
    public static final KeyBinding OPEN_SELECTOR =
            new KeyBinding("key.enchantselector.open", Keyboard.KEY_X, "key.categories.enchantselector");

    public static void registerKey() {
        ClientRegistry.registerKeyBinding(OPEN_SELECTOR);
    }

    @SubscribeEvent
    public void onGuiKeyInput(GuiScreenEvent.KeyboardInputEvent.Post event) {
        if (!Keyboard.getEventKeyState() || Keyboard.getEventKey() != OPEN_SELECTOR.getKeyCode()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc.currentScreen instanceof GuiEnchantSelector) {
            if (mc.player != null) {
                mc.player.closeScreen();
            }
            return;
        }

        if (mc.currentScreen instanceof GuiContainer) {
            EnchantSelectorMod.NETWORK.sendToServer(new OpenSelectorMessage());
        }
    }
}
