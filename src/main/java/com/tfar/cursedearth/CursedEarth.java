package com.tfar.cursedearth;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CursedEarth.MODID)
public class CursedEarth {

  public static final String MODID = "cursedearth";

  // Directly reference a log4j logger.
  @ObjectHolder(MODID + ":cursed_earth")
  public static final Block cursed_earth = null;

  public static final Config SERVER;
  public static final ForgeConfigSpec SERVER_SPEC;
  public static final ClientConfig CLIENT;
  public static final ForgeConfigSpec CLIENT_SPEC;

  static {
    final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
    SERVER_SPEC = specPair.getRight();
    SERVER = specPair.getLeft();
    final Pair<ClientConfig, ForgeConfigSpec> specPair2 = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
    CLIENT_SPEC = specPair2.getRight();
    CLIENT = specPair2.getLeft();
  }

  public static class ClientConfig {
    public static ForgeConfigSpec.ConfigValue<String> color;

    ClientConfig(ForgeConfigSpec.Builder builder) {
      builder.push("client");
      color = builder
              .comment("Color of cursed earth, pick #CC00FF for the 1.6-1.7 style, pick #222222 for newer versions")
              .define("color", "#00FFFF", String.class::isInstance);
      builder.pop();
    }
  }

  public static class Config {

    public static ForgeConfigSpec.IntValue minTickTime;
    public static ForgeConfigSpec.IntValue maxTickTime;
    public static ForgeConfigSpec.IntValue mobCap;
    public static ForgeConfigSpec.BooleanValue forceSpawn;
    public static ForgeConfigSpec.BooleanValue diesInSunlight;
    public static ForgeConfigSpec.BooleanValue naturallySpreads;
    public static ForgeConfigSpec.IntValue spawnRadius;
    public static ForgeConfigSpec.BooleanValue witherRose;

    Config(ForgeConfigSpec.Builder builder) {
      builder.push("general");

      minTickTime = builder
              .comment("minimum time between spawns in ticks")
              .defineInRange("min tick time", 50, 1, Integer.MAX_VALUE);
      maxTickTime = builder
              .comment("maximum time between spawns in ticks")
              .defineInRange("max tick time", 250, 1, Integer.MAX_VALUE);
      mobCap = builder
              .comment("max number of mobs before cursed earth stops spawning")
              .defineInRange("mob cap", 250, 1, Integer.MAX_VALUE);
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

      builder.pop();
    }
  }

  private static final Logger LOGGER = LogManager.getLogger();

  public CursedEarth() {
    ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
  }

  // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
  // Event bus for receiving Registry Events)
  @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
  public static class RegistryEvents {
    @SubscribeEvent
    public static void blocks(final RegistryEvent.Register<Block> event) {
      // register a new block here
      event.getRegistry().register(new CursedEarthBlock(Block.Properties.create(Material.ORGANIC)
              .hardnessAndResistance(.6f).sound(SoundType.PLANT)).setRegistryName("cursed_earth"));
    }

    @SubscribeEvent
    public static void items(final RegistryEvent.Register<Item> event) {
      event.getRegistry().register(new BlockItem(cursed_earth, new Item.Properties().group(ItemGroup.DECORATIONS))
              .setRegistryName("cursed_earth"));
    }
  }

  @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
  public static class Colors {
    @SubscribeEvent
    public static void color(ModelRegistryEvent e) {
      BlockColors blockColors = Minecraft.getInstance().getBlockColors();
      IBlockColor iBlockColor = (blockState, iEnviromentBlockReader, blockPos, i) -> Integer.decode(ClientConfig.color.get());

      blockColors.register(iBlockColor, cursed_earth);
      ItemColors itemColors = Minecraft.getInstance().getItemColors();
      final IItemColor itemBlockColor = (stack, tintIndex) -> {
        final BlockState state = ((BlockItem) stack.getItem()).getBlock().getDefaultState();
        return blockColors.getColor(state, null, null, tintIndex);
      };
      itemColors.register(itemBlockColor, cursed_earth);
    }
  }

  @Mod.EventBusSubscriber
  public static class Rose {
    @SubscribeEvent
    public static void applyRose(PlayerInteractEvent.RightClickBlock e) {
      if (!Config.witherRose.get()) return;
      PlayerEntity p = e.getPlayer();
      World w = p.world;
      BlockPos pos = e.getPos();
      if (p.isSneaking() && !w.isRemote && e.getItemStack().getItem() == Items.WITHER_ROSE && w.getBlockState(pos).getBlock() == Blocks.DIRT) {
        w.setBlockState(pos, cursed_earth.getDefaultState());
      }
    }
  }
}
