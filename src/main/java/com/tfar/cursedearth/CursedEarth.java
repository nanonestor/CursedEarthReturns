package com.tfar.cursedearth;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
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

import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CursedEarth.MODID)
public class CursedEarth {

  public static final String MODID = "cursedearth";

  // Directly reference a log4j logger.
  @ObjectHolder(MODID+":cursed_earth")
  public static final Block cursed_earth = null;

  public static final Config SERVER;
  public static final ForgeConfigSpec SERVER_SPEC;

  static {
    final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
    SERVER_SPEC = specPair.getRight();
    SERVER = specPair.getLeft();
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
    public static ForgeConfigSpec.ConfigValue<List<String>> blacklistedEntities;

    Config(ForgeConfigSpec.Builder builder) {
      builder.push("general");

      minTickTime = builder
              .comment("minimum time between spawns in ticks")
              .defineInRange("min tick time", 50,1,Integer.MAX_VALUE);
      maxTickTime = builder
              .comment("maximum time between spawns in ticks")
              .defineInRange("max tick time", 250,1,Integer.MAX_VALUE);
      mobCap = builder
              .comment("max number of mobs before cursed earth stops spawning")
              .defineInRange("mob cap", 250,1,Integer.MAX_VALUE);
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
              .defineInRange("spawn radius", 1,1,Integer.MAX_VALUE);
      blacklistedEntities = builder
              .comment("What entities aren't allowed to spawn from cursed earth ex: [\"minecraft:creeper\"]")
              .define("entity blacklist", Lists.newArrayList(),String.class::isInstance);
      builder.pop();
    }
  }

  private static final Logger LOGGER = LogManager.getLogger();

  public CursedEarth() {
    ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
  }

  // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
  // Event bus for receiving Registry Events)
  @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
  public static class RegistryEvents {
    @SubscribeEvent
    public static void onBlocksRegistry(final RegistryEvent.Register<Block> event) {
      // register a new block here
      event.getRegistry().register(new CursedEarthBlock(Block.Properties.create(Material.ORGANIC)
              .hardnessAndResistance(.6f)).setRegistryName("cursed_earth"));
    }
    @SubscribeEvent
    public static void Block(final RegistryEvent.Register<Item> event) {
      event.getRegistry().register(new BlockItem(cursed_earth, new Item.Properties().group(ItemGroup.DECORATIONS))
              .setRegistryName("cursed_earth"));
    }
  }

  @Mod.EventBusSubscriber
  public static class Rose {
    @SubscribeEvent
    public static void applyRose(PlayerInteractEvent.RightClickBlock e){
      if (!Config.witherRose.get())return;
      PlayerEntity p = e.getPlayer();
      World w = p.world;
      BlockPos pos = e.getPos();
      if (p.isSneaking() && !w.isRemote && e.getItemStack().getItem() == Items.WITHER_ROSE && w.getBlockState(pos).getBlock() == Blocks.DIRT){
        w.setBlockState(pos,cursed_earth.getDefaultState());
      }
    }
  }
}
