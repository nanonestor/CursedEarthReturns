package nanonestor.cursedearth;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BlessedEarthBlock extends GrassBlock {

    public BlessedEarthBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        schedule(pos, world);
    }

    // Schedules the tick for the block to be a random number between the max and min entered in configs,
    //  but with a multiplier.
    public void schedule(BlockPos pos,Level level) {
        int maxTime = (5 * CursedEarthConfig.GENERAL.maxTickTime.get());
        int minTime = (3 * CursedEarthConfig.GENERAL.minTickTime.get());
        level.scheduleTick(pos, this, level.random.nextInt(maxTime - minTime));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide) {
            schedule(pos,level);

            // Prevent loading unloaded chunks when checking neighbor's light and spreading
            if (!level.isAreaLoaded(pos, 3)) { return; }

            // If the light level is dark (less than or equal to 7), turn the block into a cursed earth block.
            boolean dark = level.getMaxLocalRawBrightness(pos.above()) <= 7;
            if (dark && level.getBlockState(pos.above()).isAir()) {
                level.setBlockAndUpdate(pos, CursedEarth.cursed_earth.get().defaultBlockState());

            // If it's not dark, is allowed to spread from config, and is air above, randomly checks the surrounding blocks.
            //  If a block data tagged as spreadable is found set it to blessed earth.
            } else if (!dark && CursedEarthConfig.GENERAL.naturallySpreads.get() && level.getBlockState(pos.above()).isAir()) {
                    BlockState blockstate = this.defaultBlockState();
                    for (int i = 0; i < 4; ++i) {
                        BlockPos pos1 = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                        if (level.getBlockState(pos1).is(CursedEarth.spreadable) && level.getBlockState(pos1.above()).isAir()) {
                            level.setBlockAndUpdate(pos1, blockstate.setValue(SNOWY, level.getBlockState(pos1.above()).getBlock() == Blocks.SNOW));
                        }
                    }
            }

            // If the fluid state above the block is not empty return and don't spawn.
            if (!level.getFluidState(pos.above()).isEmpty()) return;
            // If the game difficulty is set to peaceful don't spawn.
            if (level.getLevelData().getDifficulty() == Difficulty.PEACEFUL) return;

            if (!dark) {
                // Checks the distance to any players vs the config setting allowance.
                int r = CursedEarthConfig.GENERAL.spawnRadius.get();
                if (level.getEntitiesOfClass(Player.class, new AABB(-r, -r, -r, r, r, r)).size() > 0)
                    return;

                // Runs findMonsterToSpawn to determine which mob to spawn.  If not null returned then spawn it!
                Entity entity = findMobToSpawn(level, pos.above(), random);
                if (entity != null) {
                    entity.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
                    if (!level.noCollision(entity) || !level.isUnobstructed(entity)) return;
                    level.addFreshEntity(entity);
                }
            }
        }
    }

    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, boolean p_176473_4_) {
        return false;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return false;
    }

    private Entity findMobToSpawn(ServerLevel level, BlockPos pos, RandomSource rand) {
        // Makes a list of mob entries based on the spawn data at that location, including structure spawning mobs, and not adding entries in the blacklisted data tag.
        ServerChunkCache s = level.getChunkSource();
        List<MobSpawnSettings.SpawnerData> spawnOptions = s.getGenerator().getMobsAt(level.getBiome(pos), level.structureManager(), MobCategory.CREATURE, pos)
                .unwrap().stream().filter(spawners -> !spawners.type.is(CursedEarth.blacklisted_entities)).toList();
        // If there is nothing to spawn just return with null
        if (spawnOptions.isEmpty()) { return null; }

        // Picks a random entry in the spawn options list.
        int found = rand.nextInt(spawnOptions.size());
        MobSpawnSettings.SpawnerData entry = spawnOptions.get(found);

       //  // Checks if the mob can spawn here in chunk generation naturally to filter out some mobs which don't.
       // if (!SpawnPlacements.checkSpawnRules(entry.type, level, EntitySpawnReason.CHUNK_GENERATION, pos, level.random)
       //         && !CursedEarthConfig.GENERAL.forceSpawn.get()) { return null; }

        EntityType<?> type = entry.type;
        Entity ent = type.create(level,EntitySpawnReason.NATURAL);
        if (ent instanceof Mob)
            ((Mob) ent).finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        return ent;
    }
}
