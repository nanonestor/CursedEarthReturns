package com.tfar.examplemod;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SpreadableSnowyDirtBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.Random;

import static com.tfar.examplemod.CursedEarth.Config.*;

public class CursedEarthBlock extends SpreadableSnowyDirtBlock {
  public CursedEarthBlock(Properties properties) {
    super(properties);
  }

  @Override
  public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
    world.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), 50);
  }

  @Override
  public void tick(BlockState state, World world, BlockPos pos, Random random) {
    if (!world.isRemote) {
      if (!world.isAreaLoaded(pos, 3))
        return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading
      if (isInDaylight(world, pos) && diesInSunlight.get()) {
        world.setBlockState(pos, Blocks.DIRT.getDefaultState());
      } else {
        if (world.getLight(pos.up()) <= 7 && naturallySpreads.get()) {
          BlockState blockstate = this.getDefaultState();

          for (int i = 0; i < 4; ++i) {
            BlockPos blockpos = pos.add(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (world.getBlockState(blockpos).getBlock() == Blocks.DIRT) {
              world.setBlockState(blockpos, blockstate.with(SNOWY, world.getBlockState(blockpos.up()).getBlock() == Blocks.SNOW));
            }
          }
        }
      }
      //dont spawn in water
      if (!world.getFluidState(pos.up()).isEmpty()) return;
      world.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), random.nextInt(maxTickTime.get() - minTickTime.get() + 1));
      //don't spawn in peaceful
      if (world.getWorldInfo().getDifficulty() == Difficulty.PEACEFUL) return;
      int light = world.getLight(pos.up());
      boolean canSpawn = light <= 7;
      //failure chance
      if ((!canSpawn || random.nextDouble() < light / 10d) && !ignoreLightLevels.get()) return;
      //mobcap
      long mobcount = ((ServerWorld) world).getEntities().filter(entity -> entity instanceof IMob).count();
      if (mobcount > mobCap.get()) return;
      int r = spawnRadius.get();
      if (world.getEntitiesWithinAABB(PlayerEntity.class, new AxisAlignedBB(-r, -r, -r, r, r, r)).size() > 0) return;
      MobEntity mob = findMonsterToSpawn(world, pos, random);
      if (mob != null) {
        mob.setPosition(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        if (!world.areCollisionShapesEmpty(mob) || !world.checkNoEntityCollision(mob)) return;
        world.addEntity(mob);
      }
    }
  }

  public boolean isInDaylight(World world, BlockPos pos) {
    return world.isDaytime() && world.getBrightness(pos.up()) > 0.5F && world.isSkyLightMax(pos.up());
  }

  private MobEntity findMonsterToSpawn(World world, BlockPos pos, Random rand) {
    List<Biome.SpawnListEntry> spawnOptions = world.getBiome(pos).getSpawns(EntityClassification.MONSTER);
    if (spawnOptions.size() == 0) {
      System.out.println("no spawns found");
      return null;
    }
    int found = rand.nextInt(spawnOptions.size());
    Biome.SpawnListEntry entry = spawnOptions.get(found);
    if (entry == null || entry.entityType == null) {
      return null;
    }
    EntityType entityEntry = entry.entityType;
    MobEntity monster = null;
    Entity ent = entityEntry.create(world);
    if (ent instanceof MobEntity)
      monster = (MobEntity) ent;
    return monster;
  }
}
