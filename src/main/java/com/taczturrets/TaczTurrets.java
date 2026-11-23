package com.taczturrets;

import com.taczturrets.block.CbcTurretBlock;
import com.taczturrets.block.FixedTurretBlock;
import com.taczturrets.block.SeatTurretBlock;
import com.taczturrets.blockentity.CbcTurretBlockEntity;
import com.taczturrets.blockentity.FixedTurretBlockEntity;
import com.taczturrets.blockentity.SeatTurretBlockEntity;
import com.taczturrets.entity.TurretShooterEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(TaczTurrets.MODID)
public class TaczTurrets {
    public static final String MODID = "taczturrets";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    private static BlockBehaviour.Properties turretProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(4.0F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    public static final DeferredBlock<FixedTurretBlock> FIXED_TURRET =
            BLOCKS.registerBlock("fixed_turret", FixedTurretBlock::new, turretProperties());
    public static final DeferredBlock<SeatTurretBlock> SEAT_TURRET =
            BLOCKS.registerBlock("seat_turret", SeatTurretBlock::new, turretProperties());
    public static final DeferredBlock<CbcTurretBlock> CBC_TURRET =
            BLOCKS.registerBlock("cbc_turret", CbcTurretBlock::new, turretProperties());

    public static final DeferredItem<com.taczturrets.item.TurretBlockItem> FIXED_TURRET_ITEM =
            ITEMS.register("fixed_turret", () -> new com.taczturrets.item.TurretBlockItem(FIXED_TURRET.get(), new net.minecraft.world.item.Item.Properties()));
    public static final DeferredItem<com.taczturrets.item.TurretBlockItem> SEAT_TURRET_ITEM =
            ITEMS.register("seat_turret", () -> new com.taczturrets.item.TurretBlockItem(SEAT_TURRET.get(), new net.minecraft.world.item.Item.Properties()));
    public static final DeferredItem<com.taczturrets.item.TurretBlockItem> CBC_TURRET_ITEM =
            ITEMS.register("cbc_turret", () -> new com.taczturrets.item.TurretBlockItem(CBC_TURRET.get(), new net.minecraft.world.item.Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FixedTurretBlockEntity>> FIXED_TURRET_BE =
            BLOCK_ENTITIES.register("fixed_turret",
                    () -> BlockEntityType.Builder.of(FixedTurretBlockEntity::new, FIXED_TURRET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeatTurretBlockEntity>> SEAT_TURRET_BE =
            BLOCK_ENTITIES.register("seat_turret",
                    () -> BlockEntityType.Builder.of(SeatTurretBlockEntity::new, SEAT_TURRET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CbcTurretBlockEntity>> CBC_TURRET_BE =
            BLOCK_ENTITIES.register("cbc_turret",
                    () -> BlockEntityType.Builder.of(CbcTurretBlockEntity::new, CBC_TURRET.get()).build(null));

    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.taczturrets.menu.TurretMenu>> TURRET_MENU =
            MENUS.register("turret", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension
                    .create(com.taczturrets.menu.TurretMenu::new));

    public static final DeferredHolder<EntityType<?>, EntityType<TurretShooterEntity>> TURRET_SHOOTER =
            ENTITY_TYPES.register("turret_shooter", () -> EntityType.Builder
                    .of(TurretShooterEntity::new, MobCategory.MISC)
                    .sized(0.5F, 1.0F)
                    .eyeHeight(0.5F)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(MODID + ":turret_shooter"));

    public static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<net.minecraft.world.item.CreativeModeTab, net.minecraft.world.item.CreativeModeTab> MAIN_TAB =
            CREATIVE_TABS.register("main", () -> net.minecraft.world.item.CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.taczturrets"))
                    .icon(() -> new net.minecraft.world.item.ItemStack(FIXED_TURRET_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(FIXED_TURRET_ITEM);
                        output.accept(SEAT_TURRET_ITEM);
                        // The third turret is a TACZ gun mounted directly on a Create: Big Cannons cannon mount,
                        // so there is deliberately no extra placeable block for it.
                    })
                    .build());

    public TaczTurrets(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(TaczTurrets::registerCapabilities);
        modEventBus.addListener(TaczTurrets::registerEntityAttributes);
        // Create's arm interaction point registry is a plain (synced) registry created lazily by Create. Touch it now,
        // during construction, so it exists when NeoForge later fires RegisterEvent for it - otherwise our extra arm
        // interaction point type (which is what lets a mechanical arm fill the CBC cannon mount) would never register.
        com.taczturrets.compat.ArmInteractionPoints.ensureRegistryClassLoaded();
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            com.taczturrets.client.ClientConfigScreens.register(modContainer);
        }
    }

    /**
     * The turret shooter is a plain LivingEntity, so it needs the default living attributes: without a registered
     * supplier {@code LivingEntity}'s constructor ends up with zero max health, which makes TACZ abort every shot
     * (it checks {@code isDeadOrDying()}) and prevents health/absorption bookkeeping from ever running.
     */
    private static void registerEntityAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(TURRET_SHOOTER.get(), net.minecraft.world.entity.LivingEntity.createLivingAttributes().build());
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FIXED_TURRET_BE.get(), (be, side) -> be);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SEAT_TURRET_BE.get(), (be, side) -> be);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CBC_TURRET_BE.get(), (be, side) -> be);
        // Extra provider for CBC's cannon mount: CBC's own provider returns null while no cannon contraption is
        // mounted, so this one supplies the TACZ ammo inventory of the gun installed on the mount (Create arms).
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                rbasamoyai.createbigcannons.index.CBCBlockEntities.CANNON_MOUNT.get(),
                com.taczturrets.mount.MountedGunHandler::getItemHandler);
        event.registerEntity(Capabilities.ItemHandler.ENTITY, TURRET_SHOOTER.get(), (entity, context) -> entity);
    }
}

