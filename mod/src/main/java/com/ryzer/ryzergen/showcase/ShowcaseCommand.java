package com.ryzer.ryzergen.showcase;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /ryzergen showcase}: builds the showcase site ({@link Showcase}) north to south from where
 * you stand. Only in a development run, never in the published mod.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID)
public final class ShowcaseCommand {
    private ShowcaseCommand() {}

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.production) {
            return;
        }
        event.getDispatcher().register(Commands.literal("ryzergen").then(Commands.literal("showcase")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    var source = context.getSource();
                    BlockPos at = BlockPos.containing(source.getPosition());
                    for (String line : Showcase.build(source.getLevel(), at)) {
                        source.sendSuccess(() -> Component.literal(line), false);
                    }
                    return 1;
                })));
    }
}
