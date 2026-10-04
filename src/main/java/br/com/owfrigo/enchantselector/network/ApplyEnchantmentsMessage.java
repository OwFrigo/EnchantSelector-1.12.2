package br.com.owfrigo.enchantselector.network;

import br.com.owfrigo.enchantselector.gui.ContainerEnchantSelector;
import io.netty.buffer.ByteBuf;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class ApplyEnchantmentsMessage implements IMessage {
    private boolean unlocked;
    private final Map<ResourceLocation, Integer> requestedLevels = new HashMap<>();

    public ApplyEnchantmentsMessage() {
    }

    public ApplyEnchantmentsMessage(boolean unlocked, Map<ResourceLocation, Integer> requestedLevels) {
        this.unlocked = unlocked;
        this.requestedLevels.putAll(requestedLevels);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        unlocked = buf.readBoolean();
        int size = Math.max(0, Math.min(buf.readInt(), 4096));

        for (int i = 0; i < size; i++) {
            ResourceLocation id = new ResourceLocation(ByteBufUtils.readUTF8String(buf));
            int level = buf.readUnsignedByte();
            requestedLevels.put(id, Math.max(0, Math.min(level, 10)));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(unlocked);
        buf.writeInt(requestedLevels.size());

        for (Map.Entry<ResourceLocation, Integer> entry : requestedLevels.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey().toString());
            buf.writeByte(Math.max(0, Math.min(entry.getValue(), 10)));
        }
    }

    private static boolean allowedInLockedMode(Enchantment enchantment, ItemStack stack) {
        ResourceLocation id = enchantment.getRegistryName();
        return id != null
                && "minecraft".equals(id.getNamespace())
                && enchantment.canApplyAtEnchantingTable(stack);
    }

    public static class Handler implements IMessageHandler<ApplyEnchantmentsMessage, IMessage> {
        @Override
        public IMessage onMessage(ApplyEnchantmentsMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof ContainerEnchantSelector)) {
                    return;
                }

                ContainerEnchantSelector container = (ContainerEnchantSelector) player.openContainer;
                ItemStack stack = container.getEnchantStack();

                if (stack.isEmpty()) {
                    return;
                }

                Map<Enchantment, Integer> result = new HashMap<>();

                for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS.getValuesCollection()) {
                    ResourceLocation id = enchantment.getRegistryName();
                    if (id == null) {
                        continue;
                    }

                    if (!message.unlocked && !allowedInLockedMode(enchantment, stack)) {
                        continue;
                    }

                    int level = message.requestedLevels.containsKey(id)
                            ? message.requestedLevels.get(id)
                            : 0;

                    if (level > 0) {
                        result.put(enchantment, Math.min(level, 10));
                    }
                }

                EnchantmentHelper.setEnchantments(result, stack);
                container.detectAndSendChanges();
            });

            return null;
        }
    }
}
