package com.tfar.cursedearth;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GrassBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.tfar.cursedearth.CursedEarth.ServerConfig.*;

public class CursedEarthBlock extends GrassBlock {
  public CursedEarthBlock(Properties properties) {
    super(properties);
  }

  @Override
  public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
    super.onBlockAdded(state, world, pos, oldState, isMoving);
    int i = minTickTime.get();
    if (i == 0) {
      i = 1;
    }
    world.getPendingBlockTicks().scheduleTick(pos, this, world.rand.nextInt(maxTickTime.get() - minTickTime.get()) + i);
  }

  @Override
  public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
    if (player.getHeldItemMainhand().isEmpty() && player.isSneaking() && !world.isRemote && hand == Hand.MAIN_HAND) {

      ServerChunkProvider s = (ServerChunkProvider) world.getChunkProvider();

      List<SpawnDetail> spawnInfo = new ArrayList<>();

      List<Biome.SpawnListEntry> entries = s.getChunkGenerator().func_230353_a_(world.getBiome(pos), ((ServerWorld) world).func_241112_a_(), EntityClassification.MONSTER, pos.up());
      // nothing can spawn, occurs in places such as mushroom biomes
      if (entries.size() == 0) {
        player.sendStatusMessage(new TranslationTextComponent("text.cursedearth.nospawns"), true);
        return ActionResultType.SUCCESS;
      } else {
        for (Biome.SpawnListEntry entry : entries) {
          spawnInfo.add(new SpawnDetail(entry));
        }
        TranslationTextComponent names1 = new TranslationTextComponent("Names: ");
        for (int i = 0; i < spawnInfo.size(); i++) {
          SpawnDetail detail = spawnInfo.get(i);
          names1.append(new TranslationTextComponent(detail.displayName));
            if (i < spawnInfo.size() - 1) {
              names1.append(new StringTextComponent(", "));
            }
        }
        player.sendStatusMessage(names1, true);
      }
      return ActionResultType.SUCCESS;
    }
    return ActionResultType.PASS;
  }

  public static class SpawnDetail {

    private final String displayName;

    //    private boolean lightEnabled = true;
    public SpawnDetail(Biome.SpawnListEntry entry) {
      displayName = entry.entityType.getTranslationKey().replace("Entity", "");
    }
  }

  @Override
  public void tick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    if (!world.isRemote) {
      int j = minTickTime.get();
      if (j == 0) {
        j = 1;
      }
      world.getPendingBlockTicks().scheduleTick(pos, this, world.rand.nextInt(maxTickTime.get() - minTickTime.get()) + j);
      if (!world.isAreaLoaded(pos, 3))
        return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading
      if (isInDaylight(world, pos) && diesInSunlight.get()) {
        world.setBlockState(pos, Blocks.DIRT.getDefaultState());
      } else {
        if (world.getLight(pos.up()) <= 7 && naturallySpreads.get() && world.getBlockState(pos.up()).isAir(null,null)) {
          BlockState blockstate = this.getDefaultState();
          for (int i = 0; i < 4; ++i) {
            BlockPos pos1 = pos.add(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (world.getBlockState(pos1).getBlock().isIn(CursedEarth.spreadable) && world.getBlockState(pos1.up()).isAir(world,pos1.up())) {
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
      long mobcount = world.getEntities().filter(IMob.class::isInstance).count();
      if (mobcount > mobCap.get()) return;
      int r = spawnRadius.get();
      if (world.getEntitiesWithinAABB(PlayerEntity.class, new AxisAlignedBB(-r, -r, -r, r, r, r)).size() > 0) return;
      MobEntity mob = findMonsterToSpawn(world, pos.up(), random);
      if (mob != null) {
        mob.setPosition(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        if (!world.hasNoCollisions(mob) || !world.checkNoEntityCollision(mob)) return;
        world.addEntity(mob);
      }
    }
  }

  @Override
  public boolean canGrow(IBlockReader world, BlockPos pos, BlockState state, boolean p_176473_4_) {
    return false;//no
  }

  @Override
  public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
    //no
  }

  @Override
  public boolean canUseBonemeal(World world, Random random, BlockPos pos, BlockState state) {
    return false;//no
  }

  public static boolean isInDaylight(World world, BlockPos pos) {
    return world.isDaytime() && world.getBrightness(pos.up()) > 0.5F;
  }

  private MobEntity findMonsterToSpawn(ServerWorld world, BlockPos pos, Random rand) {
    //required to account for structure based mobs such as wither skeletons
    ServerChunkProvider s = world.getChunkProvider();
    List<Biome.SpawnListEntry> spawnOptions = s.getChunkGenerator().func_230353_a_(world.getBiome(pos), world.func_241112_a_(), EntityClassification.MONSTER, pos);
    //there is nothing to spawn
    if (spawnOptions.size() == 0) {
      return null;
    }
    int found = rand.nextInt(spawnOptions.size());
    Biome.SpawnListEntry entry = spawnOptions.get(found);
    //can the mob actually spawn here naturally, filters out mobs such as slimes which have more specific spawn requirements but
    // still show up in spawnlist; ignore them when force spawning
    if (!EntitySpawnPlacementRegistry.canSpawnEntity(entry.entityType, world, SpawnReason.NATURAL, pos, world.rand)
            && !forceSpawn.get() || CursedEarth.blacklisted_entities.contains(entry.entityType))
      return null;
    //noinspection rawtypes
    EntityType type = entry.entityType;
    Entity ent = type.create(world);
    //cursed earth only works with hostiles
    if (!(ent instanceof MobEntity))return null;
    ((MobEntity)ent).onInitialSpawn(world, world.getDifficultyForLocation(pos), SpawnReason.NATURAL, null, null);
    return (MobEntity) ent;
  }
}