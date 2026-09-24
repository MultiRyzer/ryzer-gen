package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.battery.HomeBatteryBlockEntity;
import com.ryzer.ryzergen.cable.EnergyCableBlockEntity;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterBlockEntity;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterBlockEntity;
import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RyzerGen.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER =
            BLOCK_ENTITIES.register("alloy_smelter",
                    () -> BlockEntityType.Builder.of(AlloySmelterBlockEntity::new, ModBlocks.ALLOY_SMELTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricAlloySmelterBlockEntity>> ELECTRIC_ALLOY_SMELTER =
            BLOCK_ENTITIES.register("electric_alloy_smelter",
                    () -> BlockEntityType.Builder.of(ElectricAlloySmelterBlockEntity::new, ModBlocks.ELECTRIC_ALLOY_SMELTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCableBlockEntity>> ENERGY_CABLE =
            BLOCK_ENTITIES.register("energy_cable",
                    () -> BlockEntityType.Builder.of(EnergyCableBlockEntity::new, ModBlocks.ENERGY_CABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HomeBatteryBlockEntity>> HOME_BATTERY =
            BLOCK_ENTITIES.register("home_battery",
                    () -> BlockEntityType.Builder.of(HomeBatteryBlockEntity::new, ModBlocks.HOME_BATTERY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorHeartBlockEntity>> REACTOR_HEART =
            BLOCK_ENTITIES.register("reactor_heart",
                    () -> BlockEntityType.Builder.of(ReactorHeartBlockEntity::new, ModBlocks.REACTOR_HEART.get()).build(null));

    private ModBlockEntities() {}
}
