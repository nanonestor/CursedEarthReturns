package nanonestor.cursedearth;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

import java.util.List;

public class CursedEarthBlock extends GrassBlock {

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
                schedule(pos, level);

                // Prevent loading unloaded chunks when checking neighbor's light and spreading
                if (!level.isAreaLoaded(pos, 3)) { return; }

                // If the light level at the block is more than the config setting for burn level, set the block to dirt and make some fire!
                boolean dark = level.getMaxLocalRawBrightness(pos.above()) < CursedEarthConfig.GENERAL.burnLightLevel.get();
                if (!dark && CursedEarthConfig.GENERAL.diesFromLightLevel.get()) {
                    level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                    BlockPos up = pos.above();
                    if (level.getBlockState(up).isAir()) {
                        level.setBlockAndUpdate(up, Blocks.FIRE.defaultBlockState());
                    }

                // If it's dark, is allowed to spread from config, and is air above, randomly checks the surrounding blocks.
                //  If a block data tagged as spreadable is found set it to cursed earth.
                } else if (dark && CursedEarthConfig.GENERAL.naturallySpreads.get() && level.getBlockState(pos.above()).isAir())
                     {
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
                // Checks the distance to any players vs the config setting allowance.
                int r = CursedEarthConfig.GENERAL.spawnRadius.get();
                if (level.getEntitiesOfClass(Player.class, new AABB(-r, -r, -r, r, r, r)).size() > 0)
                    return;

                // Runs findMonsterToSpawn to determine which mob to spawn.  If not null returned then spawn it!
                Entity entity = findMonsterToSpawn(level, pos.above(), random);
                if (entity != null) {
                    entity.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
                    if (!level.noCollision(entity) || !level.isUnobstructed(entity)) return;
                    level.addFreshEntity(entity);
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

    private Entity findMonsterToSpawn(ServerLevel level, BlockPos pos, RandomSource rand){
        // Makes a list of mob entries based on the spawn data at that location, including structure spawning mobs, and not adding entries in the blacklisted data tag.
        ServerChunkCache s = level.getChunkSource();
        List<MobSpawnSettings.SpawnerData> spawnOptions = s.getGenerator().getMobsAt(level.getBiome(pos), level.structureManager(), MobCategory.MONSTER, pos)
                .unwrap().stream().filter(spawners -> !spawners.type.is(CursedEarth.blacklisted_entities)).toList();
        // If there is nothing to spawn just return with null
        if (spawnOptions.isEmpty()) { return null; }

        // Picks a random entry in the spawn options list.
        int found = rand.nextInt(spawnOptions.size());
        MobSpawnSettings.SpawnerData entry = spawnOptions.get(found);

        // Checks if the mob can spawn here in chunk generation naturally to filter out some mobs which don't.
        if (!SpawnPlacements.checkSpawnRules(entry.type, level, EntitySpawnReason.CHUNK_GENERATION, pos, level.random)
                && !CursedEarthConfig.GENERAL.forceSpawn.get()) { return null; }

        EntityType<?> type = entry.type;
        Entity ent = type.create(level,EntitySpawnReason.NATURAL);
        if (ent instanceof Mob)
            ((Mob) ent).finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        return ent;
    }

}
