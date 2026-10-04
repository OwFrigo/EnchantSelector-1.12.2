package br.com.owfrigo.enchantselector.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerEnchantSelector extends Container {
    private final InventoryBasic enchantInventory = new InventoryBasic("Enchant Selector", false, 1);

    public ContainerEnchantSelector(EntityPlayer player) {
        this.addSlotToContainer(new Slot(enchantInventory, 0, 80, 35) {
            @Override
            public int getSlotStackLimit() {
                return 1;
            }

            @Override
            public boolean isItemValid(ItemStack stack) {
                return !stack.isEmpty();
            }
        });

        InventoryPlayer inv = player.inventory;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    public ItemStack getEnchantStack() {
        return enchantInventory.getStackInSlot(0);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);

        if (!playerIn.world.isRemote) {
            ItemStack stack = enchantInventory.removeStackFromSlot(0);
            if (!stack.isEmpty() && !playerIn.inventory.addItemStackToInventory(stack)) {
                playerIn.dropItem(stack, false);
            }
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        if (index == 0) {
            if (!this.mergeItemStack(stack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.mergeItemStack(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }

        return original;
    }
}
