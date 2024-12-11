package nanonestor.cursedearth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.level.block.SoundType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.Environment;

import java.util.Objects;
import java.util.Optional;

@Mod(CursedEarth.MODID)
public class CursedEarth {

    public static final String MODID = "cursedearth";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("cursed_earth", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.nullToEmpty("Cursed Earth"))
            .icon(() -> CursedEarth.cursed_earth_item.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CursedEarth.cursed_earth_item.get().getDefaultInstance()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(CursedEarth.blessed_earth_item.get().getDefaultInstance());
                output.accept(CursedEarth.blessed_flower_item.get().getDefaultInstance());
            }).build());

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredBlock<Block> cursed_earth = BLOCKS.registerBlock("cursed_earth", CursedEarthBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK));
    public static final DeferredBlock<Block> blessed_earth = BLOCKS.registerBlock("blessed_earth", BlessedEarthBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK));
    public static final DeferredBlock<Block> blessed_flower = BLOCKS.registerBlock("blessed_flower", BlessedFlowerBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION).noCollission().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ));


    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<BlockItem> cursed_earth_item = ITEMS.registerSimpleBlockItem("cursed_earth", cursed_earth);
    public static final DeferredItem<BlockItem> blessed_earth_item = ITEMS.registerSimpleBlockItem("blessed_earth", blessed_earth);
    public static final DeferredItem<BlockItem> blessed_flower_item = ITEMS.registerSimpleBlockItem("blessed_flower", blessed_flower);

    public static final TagKey<EntityType<?>> blacklisted_entities = create(ResourceLocation.fromNamespaceAndPath(MODID, "blacklisted"));
    public static final TagKey<Block> spreadable = BlockTags.create(ResourceLocation.fromNamespaceAndPath(MODID, "spreadable"));

    private static TagKey<EntityType<?>> create(ResourceLocation p_203849_) {
        return TagKey.create(Registries.ENTITY_TYPE, p_203849_);
    }

    public CursedEarth(IEventBus modEventBus, ModContainer modContainer) {

        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, CursedEarthConfig.GENERAL_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CursedEarthConfig.CLIENT_SPEC);

        if (Environment.get().getDist().isClient()) { modEventBus.addListener(this::onClientSetup); }
        NeoForge.EVENT_BUS.addListener(this::rose);
        NeoForge.EVENT_BUS.register(new MessageSpawns());
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(CursedEarth.cursed_earth.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(CursedEarth.blessed_earth.get(), RenderType.cutout());
    }

    private void rose(PlayerInteractEvent.RightClickBlock e) {

        // This disables using items to create either earth types if the doItemsMakeEarth config setting is false.
        if (!CursedEarthConfig.GENERAL.doItemsMakeEarth.get()) return;

        Player p = e.getEntity();
        Level w = p.level();
        BlockPos pos = e.getPos();
        boolean isBlockSpreadable = w.getBlockState(pos).is(spreadable);

        // Sets some holders to be used for comparison
        Optional<Holder.Reference<Item>> theCursedItem = BuiltInRegistries.ITEM.get(Objects.requireNonNull(ResourceLocation.tryParse(CursedEarthConfig.GENERAL.cursedItem.get())));
        Optional<Holder.Reference<Item>> theBlessedItem = BuiltInRegistries.ITEM.get(Objects.requireNonNull(ResourceLocation.tryParse(CursedEarthConfig.GENERAL.blessedItem.get())));
        Optional<Holder.Reference<Item>> theHandItem = Optional.of(e.getItemStack().getItem().builtInRegistryHolder());

        // Tests to see if the item in the hand is the same as the cursed item
        if (p.isShiftKeyDown() && !w.isClientSide() && theHandItem.equals(theCursedItem) && isBlockSpreadable ) {
            w.setBlockAndUpdate(pos, CursedEarth.cursed_earth.get().defaultBlockState());
            p.getItemInHand(p.getUsedItemHand()).shrink(1);
            e.setCanceled(true);
        }

        // Tests to see if the item in the hand is the same as the blessed item
        if (p.isShiftKeyDown() && !w.isClientSide() && theHandItem.equals(theBlessedItem) && isBlockSpreadable ) {
            w.setBlockAndUpdate(pos, CursedEarth.blessed_earth.get().defaultBlockState());
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

            blockColors.register(iBlockColor, CursedEarth.cursed_earth.get());
            blockColors.register(jBlockColor, CursedEarth.blessed_earth.get());


         // The following was changed by Mojang in 1.21.4 to now be set by the new client_item data driven system - and set using tint.
         //   ItemColors cursed_itemColors = Minecraft.getInstance().getItemColors();
         //   ItemColors blessed_itemColors = Minecraft.getInstance().getItemColors();

         //   final ItemColor cursed_itemBlockColor = (stack, tintIndex) -> {
         //       final BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
         //       return blockColors.getColor(state, null, null, tintIndex);
         //   };
          //  final ItemColor blessed_itemBlockColor = (stack, tintIndex) -> {
          //      final BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
          //      return blockColors.getColor(state, null, null, tintIndex);
         //   };

         //   cursed_itemColors.register(cursed_itemBlockColor, CursedEarth.cursed_earth);
         //   blessed_itemColors.register(blessed_itemBlockColor, CursedEarth.blessed_earth);
        }
    }

}


