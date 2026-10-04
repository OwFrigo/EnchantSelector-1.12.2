package br.com.owfrigo.enchantselector;

import br.com.owfrigo.enchantselector.gui.GuiHandler;
import br.com.owfrigo.enchantselector.network.ApplyEnchantmentsMessage;
import br.com.owfrigo.enchantselector.network.OpenSelectorMessage;
import br.com.owfrigo.enchantselector.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
        modid = EnchantSelectorMod.MODID,
        name = EnchantSelectorMod.NAME,
        version = EnchantSelectorMod.VERSION,
        acceptedMinecraftVersions = "[1.12.2]"
)
public class EnchantSelectorMod {
    public static final String MODID = "enchantselector";
    public static final String NAME = "Enchant Selector";
    public static final String VERSION = "1.0.0";
    public static final int GUI_ID = 1;

    @Mod.Instance(MODID)
    public static EnchantSelectorMod INSTANCE;

    @SidedProxy(
            clientSide = "br.com.owfrigo.enchantselector.proxy.ClientProxy",
            serverSide = "br.com.owfrigo.enchantselector.proxy.CommonProxy"
    )
    public static CommonProxy PROXY;

    public static SimpleNetworkWrapper NETWORK;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        NETWORK.registerMessage(OpenSelectorMessage.Handler.class, OpenSelectorMessage.class, 0, Side.SERVER);
        NETWORK.registerMessage(ApplyEnchantmentsMessage.Handler.class, ApplyEnchantmentsMessage.class, 1, Side.SERVER);

        NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, new GuiHandler());
        PROXY.preInit();
    }
}
