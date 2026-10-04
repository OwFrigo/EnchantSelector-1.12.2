package br.com.owfrigo.enchantselector.proxy;

import br.com.owfrigo.enchantselector.client.ClientKeyHandler;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        ClientKeyHandler.registerKey();
        MinecraftForge.EVENT_BUS.register(new ClientKeyHandler());
    }
}
