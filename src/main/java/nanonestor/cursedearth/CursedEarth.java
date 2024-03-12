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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.commons.lang3.tuple.Pair;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CursedEarth.MODID)
public class CursedEarth {

    public static final String MODID = "cursedearth";
    public static final TagKey<EntityType<?>> blacklisted_entities = create(new ResourceLocation(MODID, "blacklisted"));
    public static final TagKey<Block> spreadable = BlockTags.create(new ResourceLocation(MODID, "spreadable"));

    private static TagKey<EntityType<?>> create(ResourceLocation p_203849_) {
        return TagKey.create(Registries.ENTITY_TYPE, p_203849_);
    }

    public CursedEarth() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        if (FMLEnvironment.dist.isClient()) {
            bus.addListener(this::onClientSetup);
        }
        bus.addListener(this::blocks);
        NeoForge.EVENT_BUS.addListener(this::rose);
    }

    public static final ServerConfig SERVER;
    public static final ModConfigSpec SERVER_SPEC;
    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        final Pair<ServerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
        final Pair<ClientConfig, ModConfigSpec> specPair2 = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair2.getRight();
        CLIENT = specPair2.getLeft();
    }

    public static class ClientConfig {
        public static ModConfigSpec.ConfigValue<String> color;

        ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("client");
            color = builder
                    .comment("Color of cursed earth, pick #CC00FF classic style color, pick #222222 for brighter newage color, or any hex code color you would like.")
                    .define("color", "#CC00FF", String.class::isInstance);
            builder.pop();
        }
    }

    public static class ServerConfig {

        public static ModConfigSpec.IntValue minTickTime;
        public static ModConfigSpec.IntValue maxTickTime;

        public static ModConfigSpec.BooleanValue forceSpawn;
        public static ModConfigSpec.BooleanValue diesInSunlight;
        public static ModConfigSpec.BooleanValue naturallySpreads;
        public static ModConfigSpec.IntValue spawnRadius;
        public static ModConfigSpec.BooleanValue witherRose;
        public static ModConfigSpec.ConfigValue<String> item;

        ServerConfig(ModConfigSpec.Builder builder) {
            builder.push("general");

            minTickTime = builder
                    .comment("minimum time between spawns in ticks")
                    .defineInRange("min tick time", 50, 1, Integer.MAX_VALUE);
            maxTickTime = builder
                    .comment("maximum time between spawns in ticks")
                    .defineInRange("max tick time", 250, 1, Integer.MAX_VALUE);
            forceSpawn = builder
                    .comment("Force spawns to occur regardless of conditions such as light level and elevation")
                    .define("force spawns", false);
            diesInSunlight = builder
                    .comment("does cursed earth die in sunlight")
                    .define("die in sunlight", true);
            naturallySpreads = builder
                    .comment("does cursed earth naturally spread")
                    .define("naturally spread", true);
            witherRose = builder
                    .comment("does the wither rose make cursed earth")
                    .define("wither rose", true);
            spawnRadius = builder
                    .comment("minimum distance cursed earth has to be away from players before it spawns mobs")
                    .defineInRange("spawn radius", 1, 1, Integer.MAX_VALUE);

            item = builder
                    .comment("item used to create cursed earth")
                    .define("item", BuiltInRegistries.ITEM.getKey(Items.WITHER_ROSE).toString(), o -> o instanceof String s&&
                            BuiltInRegistries.ITEM.getOptional(new ResourceLocation(s)).isPresent());

            builder.pop();
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(CursedEarthBlock.cursed_earth, RenderType.cutout());
    }

    public void blocks(final RegisterEvent event) {

        event.register(Registries.BLOCK,new ResourceLocation(MODID,"cursed_earth"),() -> CursedEarthBlock.cursed_earth);

        event.register(Registries.ITEM,new ResourceLocation(MODID,"cursed_earth"),() -> CursedEarthBlock.cursed_earth_item);

    }

    private void rose(PlayerInteractEvent.RightClickBlock e) {
        if (!ServerConfig.witherRose.get()) return;
        Player p = e.getEntity();
        Level w = p.level();
        BlockPos pos = e.getPos();
        if (p.isShiftKeyDown() && !w.isClientSide() && e.getItemStack().getItem() ==
                BuiltInRegistries.ITEM.get(new ResourceLocation(ServerConfig.item.get())) && w.getBlockState(pos).getBlock() == Blocks.DIRT) {
            w.setBlockAndUpdate(pos, CursedEarthBlock.cursed_earth.defaultBlockState());
            e.setCanceled(true);
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Colors {
        @SubscribeEvent
        public static void color(FMLClientSetupEvent e) {
            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            BlockColor iBlockColor = (blockState, iEnviromentBlockReader, blockPos, i) -> Integer.decode(ClientConfig.color.get());

            blockColors.register(iBlockColor, CursedEarthBlock.cursed_earth);
            ItemColors itemColors = Minecraft.getInstance().getItemColors();
            final ItemColor itemBlockColor = (stack, tintIndex) -> {
                final BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
                return blockColors.getColor(state, null, null, tintIndex);
            };
            itemColors.register(itemBlockColor, CursedEarthBlock.cursed_earth);
        }
    }
}

