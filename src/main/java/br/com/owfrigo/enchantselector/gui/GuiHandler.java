package br.com.owfrigo.enchantselector.gui;

import br.com.owfrigo.enchantselector.EnchantSelectorMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == EnchantSelectorMod.GUI_ID) {
            return new ContainerEnchantSelector(player);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == EnchantSelectorMod.GUI_ID) {
            return new GuiEnchantSelector(new ContainerEnchantSelector(player));
        }
        return null;
    }
}
