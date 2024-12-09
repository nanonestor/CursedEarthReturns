package nanonestor.cursedearth;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;


import java.util.List;

public class CursedEarthBlock extends GrassBlock {
    public static final Block cursed_earth = new CursedEarthBlock(Properties.ofFullCopy(Blocks.GRASS_BLOCK));
    public static final Item cursed_earth_item = new BlockItem(cursed_earth,new Item.Properties());

    public CursedEarthBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        schedule(pos, world);
    }

    public void schedule(BlockPos pos,Level level) {
        int maxTime = CursedEarthConfig.GENERAL.maxTickTime.get();
        int minTime = CursedEarthConfig.GENERAL.minTickTime.get();
        level.scheduleTick(pos, this, level.random.nextInt(maxTime - minTime));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide) {
            schedule(pos,level);

            // Prevent loading unloaded chunks when checking neighbor's light and spreading
            if (!level.isAreaLoaded(pos, 3)) { return; }

            boolean dark = level.getMaxLocalRawBrightness(pos.above()) < CursedEarthConfig.GENERAL.burnLightLevel.get();
            if (!dark && CursedEarthConfig.GENERAL.diesFromLightLevel.get()) {
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                BlockPos up = pos.above();
                if (level.getBlockState(up).isAir()) {
                    level.setBlockAndUpdate(up,Blocks.FIRE.defaultBlockState());
                }
            } else {
                if (dark && CursedEarthConfig.GENERAL.naturallySpreads.get() && level.getBlockState(pos.above()).isAir()) {
                    BlockState blockstate = this.defaultBlockState();
                    for (int i = 0; i < 4; ++i) {
                        BlockPos pos1 = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                        if (level.getBlockState(pos1).is(CursedEarth.spreadable) && level.getBlockState(pos1.above()).isAir()) {
                            level.setBlockAndUpdate(pos1, blockstate.setValue(SNOWY, level.getBlockState(pos1.above()).getBlock() == Blocks.SNOW));
                        }
                    }
                }
            }

            //dont spawn in water
            if (!level.getFluidState(pos.above()).isEmpty()) return;
            //don't spawn in peaceful
            if (level.getLevelData().getDifficulty() == Difficulty.PEACEFUL) return;
            int r = CursedEarthConfig.GENERAL.spawnRadius.get();
            if (level.getEntitiesOfClass(Player.class, new AABB(-r, -r, -r, r, r, r)).size() > 0)
                return;
            Entity en = findMonsterToSpawn(level, pos.above(), random);
            if (en != null) {
                en.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
                if (!level.noCollision(en) || !level.isUnobstructed(en)) return;
                level.addFreshEntity(en);
            }
        }
    }


    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, boolean p_176473_4_) {
        return false;//no
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        //no
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return false;//no
    }

    private Entity findMonsterToSpawn(ServerLevel level, BlockPos pos, RandomSource rand) {
        //required to account for structure based mobs such as wither skeletons, remove blacklisted entities here, so they don't slow down spawning
        ServerChunkCache s = level.getChunkSource();
        List<MobSpawnSettings.SpawnerData> spawnOptions = s.getGenerator().getMobsAt(level.getBiome(pos), level.structureManager(), MobCategory.MONSTER, pos)
                .unwrap().stream().filter(spawners -> !spawners.type.is(CursedEarth.blacklisted_entities)).toList();
        //there is nothing to spawn
        if (spawnOptions.size() == 0) {
            return null;
        }

        int found = rand.nextInt(spawnOptions.size());
        MobSpawnSettings.SpawnerData entry = spawnOptions.get(found);

        // Checks if the mob can spawn here in chunk generation naturally to filter out some mobs which don't.
        if (!SpawnPlacements.checkSpawnRules(entry.type, level, MobSpawnType.CHUNK_GENERATION, pos, level.random)
                && !CursedEarthConfig.GENERAL.forceSpawn.get()) { return null; }

        EntityType<?> type = entry.type;
        Entity ent = type.create(level);
        if (ent instanceof Mob)
            ((Mob) ent).finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        return ent;
    }
}
