package br.com.owfrigo.enchantselector.network;

import br.com.owfrigo.enchantselector.EnchantSelectorMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class OpenSelectorMessage implements IMessage {
    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<OpenSelectorMessage, IMessage> {
        @Override
        public IMessage onMessage(OpenSelectorMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
                    player.openGui(
                            EnchantSelectorMod.INSTANCE,
                            EnchantSelectorMod.GUI_ID,
                            player.world,
                            0, 0, 0
                    )
            );
            return null;
        }
    }
}
