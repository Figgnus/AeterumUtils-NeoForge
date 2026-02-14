package me.figgnus.aeterumutils;

import me.figgnus.aeterumutils.afk.AfkConfig;
import me.figgnus.aeterumutils.afk.AfkHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Aeterumutils.MODID)
public class Aeterumutils {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "aeterumutils";

    public Aeterumutils(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, AfkConfig.SPEC, "aeterumutils/afk-server.toml");
        NeoForge.EVENT_BUS.addListener(AfkHandler::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(AfkHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(AfkHandler::onPlayerLogout);
    }
}
