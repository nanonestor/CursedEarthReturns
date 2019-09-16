package com.tfar.cursedearth;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SpreadableSnowyDirtBlock;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.tfar.cursedearth.CursedEarth.Config.*;

public class CursedEarthBlock extends SpreadableSnowyDirtBlock {
  public CursedEarthBlock(Properties properties) {
    super(properties);
  }

  public static final Tag<Block> spreadable = new BlockTags.Wrapper(new ResourceLocation(CursedEarth.MODID, "spreadable"));
  public static final Tag<EntityType<?>> blacklisted_entities = new EntityTypeTags.Wrapper(new ResourceLocation(CursedEarth.MODID, "blacklisted"));

  @Override
  public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
    world.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), world.rand.nextInt(maxTickTime.get() - minTickTime.get() + 1));
  }

  @Override
  public boolean onBlockActivated(BlockState p_220051_1_, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult p_220051_6_) {
    if (player.getHeldItemMainhand().isEmpty() && player.isSneaking() && !world.isRemote && hand == Hand.MAIN_HAND) {

      ServerChunkProvider s = (ServerChunkProvider) world.getChunkProvider();

      List<SpawnDetail> spawnInfo = new ArrayList<>();

      BlockPos up = pos.up();

      List<Biome.SpawnListEntry> entries = s.getChunkGenerator().getPossibleCreatures(EntityClassification.MONSTER, up);
      // nothing can spawn, occurs in places such as mushroom biomes
      if (entries.size() == 0) {
        player.sendMessage(new TranslationTextComponent("text.cursedearth.nospawns"));
        return true;
      } else {
        for (Biome.SpawnListEntry entry : entries) {
          spawnInfo.add(new SpawnDetail(entry, EntityClassification.MONSTER));
        }
        ITextComponent names1 = new TranslationTextComponent("Names: ");
        for (SpawnDetail detail : spawnInfo) {
          names1.appendSibling(new TranslationTextComponent(detail.displayName)).appendSibling(new StringTextComponent(", "));
        }
        player.sendMessage(names1);
      }
      return true;
    }
    return false;
  }

  public static class SpawnDetail {

    private int itemWeight;
    private String displayName;
    private String creatureTypeName;

    //    private boolean lightEnabled = true;
    public SpawnDetail(Biome.SpawnListEntry entry, EntityClassification creatureType) {
      itemWeight = entry.itemWeight;
      creatureTypeName = creatureType.name();
      displayName = entry.entityType.getTranslationKey().replace("Entity", "");
    }
  }

  @Override
  public void tick(BlockState state, World world, BlockPos pos, Random random) {
    if (!world.isRemote) {
      if (!world.isAreaLoaded(pos, 3))
        return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading
      if (isInDaylight(world, pos) && diesInSunlight.get()) {
        world.setBlockState(pos, Blocks.DIRT.getDefaultState());
      } else {
        if (world.getLight(pos.up()) <= 7 && naturallySpreads.get() && world.getBlockState(pos.up()).isAir(null,null)) {
          BlockState blockstate = this.getDefaultState();
          for (int i = 0; i < 4; ++i) {
            BlockPos pos1 = pos.add(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (world.getBlockState(pos1).getBlock().isIn(spreadable) && world.getBlockState(pos1.up()).isAir(world,pos1.up())) {
              world.setBlockState(pos1, blockstate.with(SNOWY, world.getBlockState(pos1.up()).getBlock() == Blocks.SNOW));
            }
          }
        }
      }
      world.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), random.nextInt(maxTickTime.get() - minTickTime.get() + 1));
      //dont spawn in water
      if (!world.getFluidState(pos.up()).isEmpty()) return;
      //don't spawn in peaceful
      if (world.getWorldInfo().getDifficulty() == Difficulty.PEACEFUL) return;
      //mobcap used because mobs are laggy in large numbers todo: how well does this work on servers
      long mobcount = ((ServerWorld) world).getEntities().filter(IMob.class::isInstance).count();
      if (mobcount > mobCap.get()) return;
      int r = spawnRadius.get();
      if (world.getEntitiesWithinAABB(PlayerEntity.class, new AxisAlignedBB(-r, -r, -r, r, r, r)).size() > 0) return;
      MobEntity mob = findMonsterToSpawn(world, pos.up(), random);
      if (mob != null) {
        mob.setPosition(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        if (!world.areCollisionShapesEmpty(mob) || !world.checkNoEntityCollision(mob)) return;
        world.addEntity(mob);
      }
    }
  }

  @Override
  public boolean addDestroyEffects(BlockState state, World world, BlockPos pos, ParticleManager manager) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public boolean isSolid(BlockState p_200124_1_) {
    return true;
  }

  public boolean isInDaylight(World world, BlockPos pos) {
    return world.isDaytime() && world.getBrightness(pos.up()) > 0.5F && world.isSkyLightMax(pos.up());
  }

  private MobEntity findMonsterToSpawn(World world, BlockPos pos, Random rand) {
    //required to account for structure based mobs such as wither skeletons
    ServerChunkProvider s = (ServerChunkProvider) world.getChunkProvider();
    List<Biome.SpawnListEntry> spawnOptions = s.getChunkGenerator().getPossibleCreatures(EntityClassification.MONSTER, pos);
    //there is nothing to spawn
    if (spawnOptions.size() == 0) {
      return null;
    }
    int found = rand.nextInt(spawnOptions.size());
    Biome.SpawnListEntry entry = spawnOptions.get(found);
    //can the mob actually spawn here naturally, filters out mobs such as slimes; ignore them when force spawning
    if (!EntitySpawnPlacementRegistry.func_223515_a(entry.entityType, world, SpawnReason.NATURAL, pos, world.rand)
            && !forceSpawn.get() || blacklisted_entities.contains(entry.entityType))
      return null;
    EntityType type = entry.entityType;
    Entity ent = type.create(world);
    //cursed earth only works with hostiles
    if (!(ent instanceof MobEntity))return null;
    ((MobEntity)ent).onInitialSpawn(world, world.getDifficultyForLocation(pos), SpawnReason.NATURAL, null, null);
    return (MobEntity) ent;
  }
}