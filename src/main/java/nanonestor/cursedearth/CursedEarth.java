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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

@Mod(CursedEarth.MODID)
public class CursedEarth {


    public static final String MODID = "cursedearth";
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("cursed_earth", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.nullToEmpty("Cursed Earth"))
            .icon(() -> CursedEarthBlock.cursed_earth_item.getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CursedEarthBlock.cursed_earth_item.getDefaultInstance()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(BlessedEarthBlock.blessed_earth_item.getDefaultInstance());
                output.accept(BlessedFlowerBlock.blessed_flower_item.getDefaultInstance());
            }).build());

    public static final TagKey<EntityType<?>> blacklisted_entities = create(new ResourceLocation(MODID, "blacklisted"));
    public static final TagKey<Block> spreadable = BlockTags.create(new ResourceLocation(MODID, "spreadable"));

    private static TagKey<EntityType<?>> create(ResourceLocation p_203849_) {
        return TagKey.create(Registries.ENTITY_TYPE, p_203849_);
    }

    public CursedEarth() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(this::onClientSetup);
        }
        modEventBus.addListener(this::blocks);
        EVENT_BUS.addListener(this::rose);

        CREATIVE_MODE_TABS.register(modEventBus);
        //modEventBus.addListener(this::addCreative)
    }

    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
        final Pair<ClientConfig, ForgeConfigSpec> specPair2 = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair2.getRight();
        CLIENT = specPair2.getLeft();
    }

    public static class ClientConfig {
        public static ForgeConfigSpec.ConfigValue<String> color_cursed_earth;
        public static ForgeConfigSpec.ConfigValue<String> color_blessed_earth;

        ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("client");
            color_cursed_earth = builder
                    .comment("Color of cursed earth, pick #CC00FF classic style color, pick #222222 for brighter newage color, or any hex code color you would like.")
                    .define("color_cursed_earth", "#CC00FF", String.class::isInstance);
            color_blessed_earth = builder
                    .comment("Color of blessed earth, default value is #00BCD4")
                    .define("color_blessed_earth", "#00BCD4", String.class::isInstance);
            builder.pop();
        }
    }

    public static class ServerConfig {

        public static ForgeConfigSpec.IntValue minTickTime;
        public static ForgeConfigSpec.IntValue maxTickTime;
        public static ForgeConfigSpec.IntValue burnLightLevel;
        public static ForgeConfigSpec.BooleanValue forceSpawn;
        public static ForgeConfigSpec.BooleanValue diesFromLightLevel;
        public static ForgeConfigSpec.BooleanValue naturallySpreads;
        public static ForgeConfigSpec.IntValue spawnRadius;
        public static ForgeConfigSpec.BooleanValue doItemsMakeEarth;
        public static ForgeConfigSpec.ConfigValue<String> cursed_item;
        public static ForgeConfigSpec.ConfigValue<String> blessed_item;

        ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.push("general");

            minTickTime = builder
                    .comment("minimum time between spawns in ticks")
                    .defineInRange("min tick time", 100, 1, Integer.MAX_VALUE);
            maxTickTime = builder
                    .comment("maximum time between spawns in ticks")
                    .defineInRange("max tick time", 600, 1, Integer.MAX_VALUE);
            burnLightLevel = builder
                    .comment("the light level above which cursed earth blocks burn - default 7 - allowed values 1 to 15")
                    .defineInRange("burn light level", 7, 1, 15);
            forceSpawn = builder
                    .comment("Force spawns to occur regardless of conditions such as light level and elevation")
                    .define("force spawns", false);
            diesFromLightLevel = builder
                    .comment("does cursed earth die from light levels")
                    .define("dies from light level", true);
            naturallySpreads = builder
                    .comment("does cursed earth naturally spread")
                    .define("naturally spreads", true);
            doItemsMakeEarth = builder
                    .comment("do the items set as 'cursed item' and 'blessed item' make earths - set false to disable")
                    .define("do items make earth", true);
            spawnRadius = builder
                    .comment("minimum distance cursed earth has to be away from players before it spawns mobs")
                    .defineInRange("spawn radius", 1, 1, Integer.MAX_VALUE);
            cursed_item = builder
                    .comment("item used to create cursed earth")
                    .define("cursed_item", BuiltInRegistries.ITEM.getKey(Items.WITHER_ROSE).toString(), o -> o instanceof String s&&
                            BuiltInRegistries.ITEM.getOptional(new ResourceLocation(s)).isPresent());
            blessed_item = builder
                    .comment("item used to create blessed earth")
                    .define("blessed_item", ("cursedearth:blessed_flower").toString(), o -> o instanceof String s&&
                            BuiltInRegistries.ITEM.getOptional(new ResourceLocation(s)).isPresent());

            builder.pop();
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(CursedEarthBlock.cursed_earth, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlessedEarthBlock.blessed_earth, RenderType.cutout());
    }

    public void blocks(final RegisterEvent event) {

        event.register(Registries.BLOCK,new ResourceLocation(MODID,"cursed_earth"),() -> CursedEarthBlock.cursed_earth);

        event.register(Registries.ITEM,new ResourceLocation(MODID,"cursed_earth"),() -> CursedEarthBlock.cursed_earth_item);

        event.register(Registries.BLOCK,new ResourceLocation(MODID,"blessed_earth"),() -> BlessedEarthBlock.blessed_earth);

        event.register(Registries.ITEM,new ResourceLocation(MODID,"blessed_earth"),() -> BlessedEarthBlock.blessed_earth_item);

        event.register(Registries.BLOCK,new ResourceLocation(MODID,"blessed_flower"),() -> BlessedFlowerBlock.blessed_flower);

        event.register(Registries.ITEM,new ResourceLocation(MODID,"blessed_flower"),() -> BlessedFlowerBlock.blessed_flower_item);
    }

    private void rose(PlayerInteractEvent.RightClickBlock e) {

        if (!ServerConfig.doItemsMakeEarth.get()) return;
        Player p = e.getEntity();
        Level w = p.level();
        BlockPos pos = e.getPos();
        boolean isBlockSpreadable = w.getBlockState(pos).is(spreadable);

        if (p.isShiftKeyDown() && !w.isClientSide() && e.getItemStack().getItem() ==
                BuiltInRegistries.ITEM.get(new ResourceLocation(ServerConfig.cursed_item.get())) && isBlockSpreadable ) {
                // Below if wanting to allow only vanilla WITHER_ROSE item, instead of above
                // Items.WITHER_ROSE && w.getBlockState(pos).getBlock() == Blocks.DIRT) {
            w.setBlockAndUpdate(pos, CursedEarthBlock.cursed_earth.defaultBlockState());
            p.getItemInHand(p.getUsedItemHand()).shrink(1);
            e.setCanceled(true);
        }

        if (p.isShiftKeyDown() && !w.isClientSide() && e.getItemStack().getItem() ==
                BuiltInRegistries.ITEM.get(new ResourceLocation(ServerConfig.blessed_item.get())) && isBlockSpreadable ) {
                // Below if wanting to only allow mod's blessed_flower_item, instead of above
                // BlessedFlowerBlock.blessed_flower_item && w.getBlockState(pos).getBlock() == Blocks.DIRT) {
            w.setBlockAndUpdate(pos, BlessedEarthBlock.blessed_earth.defaultBlockState());
            p.getItemInHand(p.getUsedItemHand()).shrink(1);
            e.setCanceled(true);
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Colors {
        @SubscribeEvent
        public static void color(FMLClientSetupEvent e) {
            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            BlockColor iBlockColor = (blockState, iEnviromentBlockReader, blockPos, i) -> Integer.decode(ClientConfig.color_cursed_earth.get());
            BlockColor jBlockColor = (blockState, iEnviromentBlockReader, blockPos, i) -> Integer.decode(ClientConfig.color_blessed_earth.get());

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

