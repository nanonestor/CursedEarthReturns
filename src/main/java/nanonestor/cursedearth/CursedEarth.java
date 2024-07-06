package nanonestor.cursedearth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.Objects;

@Mod(CursedEarth.MODID)
public class CursedEarth {

    public static final String MODID = "cursedearth";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("cursed_earth", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.nullToEmpty("Cursed Earth"))
            .icon(() -> CursedEarthBlock.cursed_earth_item.getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CursedEarthBlock.cursed_earth_item.getDefaultInstance()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(BlessedEarthBlock.blessed_earth_item.getDefaultInstance());
                output.accept(BlessedFlowerBlock.blessed_flower_item.getDefaultInstance());
            }).build());

    public static final TagKey<EntityType<?>> blacklisted_entities = create(ResourceLocation.fromNamespaceAndPath(MODID, "blacklisted"));
    public static final TagKey<Block> spreadable = BlockTags.create(ResourceLocation.fromNamespaceAndPath(MODID, "spreadable"));


    private static TagKey<EntityType<?>> create(ResourceLocation p_203849_) {
        return TagKey.create(Registries.ENTITY_TYPE, p_203849_);
    }

    public CursedEarth(IEventBus modEventBus, ModContainer modContainer) {

        modContainer.registerConfig(ModConfig.Type.SERVER, CursedEarthConfig.GENERAL_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CursedEarthConfig.CLIENT_SPEC);

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(this::onClientSetup);
        }
        modEventBus.addListener(this::blocks);
        NeoForge.EVENT_BUS.addListener(this::rose);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(CursedEarthBlock.cursed_earth, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlessedEarthBlock.blessed_earth, RenderType.cutout());
    }

    public void blocks(RegisterEvent event) {

        event.register(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "cursed_earth"),() -> CursedEarthBlock.cursed_earth);
        event.register(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID,"cursed_earth"),() -> CursedEarthBlock.cursed_earth_item);
        event.register(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID,"blessed_earth"),() -> BlessedEarthBlock.blessed_earth);
        event.register(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID,"blessed_earth"),() -> BlessedEarthBlock.blessed_earth_item);
        event.register(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID,"blessed_flower"),() -> BlessedFlowerBlock.blessed_flower);
        event.register(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID,"blessed_flower"),() -> BlessedFlowerBlock.blessed_flower_item);
    }

    private void rose(PlayerInteractEvent.RightClickBlock e) {

        // This disables using items to create either earth types if the doItemsMakeEarth config setting is false.
        if (!CursedEarthConfig.GENERAL.doItemsMakeEarth.get()) return;

        Player p = e.getEntity();
        Level w = p.level();
        BlockPos pos = e.getPos();
        boolean isBlockSpreadable = w.getBlockState(pos).is(spreadable);


        if (p.isShiftKeyDown() && !w.isClientSide() && e.getItemStack().getItem() ==
                BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(CursedEarthConfig.GENERAL.cursedItem.get())) && isBlockSpreadable ) {
            // Below if wanting to allow only vanilla WITHER_ROSE item, instead of above
            // Items.WITHER_ROSE && w.getBlockState(pos).getBlock() == Blocks.DIRT) {
            w.setBlockAndUpdate(pos, CursedEarthBlock.cursed_earth.defaultBlockState());
            p.getItemInHand(p.getUsedItemHand()).shrink(1);
            e.setCanceled(true);
        }

        if (p.isShiftKeyDown() && !w.isClientSide() && e.getItemStack().getItem() ==
                BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(CursedEarthConfig.GENERAL.blessedItem.get())) && isBlockSpreadable ) {
            // Below if wanting to only allow mod's blessed_flower_item, instead of above
            // BlessedFlowerBlock.blessed_flower_item && w.getBlockState(pos).getBlock() == Blocks.DIRT) {
            w.setBlockAndUpdate(pos, BlessedEarthBlock.blessed_earth.defaultBlockState());
            p.getItemInHand(p.getUsedItemHand()).shrink(1);
            e.setCanceled(true);
        }
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Colors {
        @SubscribeEvent
        public static void color(FMLClientSetupEvent e) {
            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            BlockColor iBlockColor = (blockState, iEnviromentBlockReader, blockPos, i) -> Integer.decode(CursedEarthConfig.CLIENT.color_cursed_earth.get());
            BlockColor jBlockColor = (blockState, iEnviromentBlockReader, blockPos, i) -> Integer.decode(CursedEarthConfig.CLIENT.color_blessed_earth.get());

            blockColors.register(iBlockColor, CursedEarthBlock.cursed_earth);
            blockColors.register(jBlockColor, BlessedEarthBlock.blessed_earth);

            ItemColors cursed_itemColors = Minecraft.getInstance().getItemColors();
            ItemColors blessed_itemColors = Minecraft.getInstance().getItemColors();

            final ItemColor cursed_itemBlockColor = (stack, tintIndex) -> {
                final BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
                return blockColors.getColor(state, null, null, tintIndex);
            };
            final ItemColor blessed_itemBlockColor = (stack, tintIndex) -> {
                final BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
                return blockColors.getColor(state, null, null, tintIndex);
            };

            cursed_itemColors.register(cursed_itemBlockColor, CursedEarthBlock.cursed_earth);
            blessed_itemColors.register(blessed_itemBlockColor, BlessedEarthBlock.blessed_earth);
        }
    }
}

