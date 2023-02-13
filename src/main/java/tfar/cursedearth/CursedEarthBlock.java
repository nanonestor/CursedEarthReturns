package tfar.cursedearth;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static tfar.cursedearth.CursedEarth.ServerConfig.*;

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
             level.scheduleTick(pos, this, level.random.nextInt(maxTickTime.get() - minTickTime.get()));
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).isEmpty() && player.isShiftKeyDown()) {
            if (!world.isClientSide) {

                ServerChunkCache s = (ServerChunkCache) world.getChunkSource();
                List<MobSpawnSettings.SpawnerData> entries = s.getGenerator()
                        .getMobsAt(world.getBiome(pos), ((ServerLevel) world).structureFeatureManager(), MobCategory.MONSTER, pos.above())
                        .unwrap().stream().filter(spawners -> !spawners.type.is(CursedEarth.blacklisted_entities)).toList();
                // nothing can spawn, occurs in places such as mushroom biomes
                if (entries.size() == 0) {
                    player.displayClientMessage(new TranslatableComponent("text.cursedearth.nospawns"), true);
                } else {
                    TranslatableComponent names1 = new TranslatableComponent("Mobs: ");
                    for (int i = 0; i < entries.size(); i++) {
                        MobSpawnSettings.SpawnerData spawners = entries.get(i);
                        names1.append(spawners.type.getDescription());
                        if (i < entries.size() - 1) {
                            names1.append(new TextComponent(", "));
                        }
                    }
                     player.sendMessage(names1, Util.NIL_UUID);
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (!world.isClientSide) {
            schedule(pos,world);
            if (!world.isAreaLoaded(pos, 3))
                return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading

            boolean dark = world.getMaxLocalRawBrightness(pos.above()) <= 7;
            if (!dark && diesInSunlight.get()) {
                world.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                BlockPos up = pos.above();
                if (world.getBlockState(up).isAir()) {
                    world.setBlockAndUpdate(up,Blocks.FIRE.defaultBlockState());
                }
            } else {
                if (dark && naturallySpreads.get() && world.getBlockState(pos.above()).isAir()) {
                    BlockState blockstate = this.defaultBlockState();
                    for (int i = 0; i < 4; ++i) {
                        BlockPos pos1 = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                        if (world.getBlockState(pos1).is(CursedEarth.spreadable) && world.getBlockState(pos1.above()).isAir()) {
                            world.setBlockAndUpdate(pos1, blockstate.setValue(SNOWY, world.getBlockState(pos1.above()).getBlock() == Blocks.SNOW));
                        }
                    }
                }
            }

            //dont spawn in water
            if (!world.getFluidState(pos.above()).isEmpty()) return;
            //don't spawn in peaceful
            if (world.getLevelData().getDifficulty() == Difficulty.PEACEFUL) return;
            int r = spawnRadius.get();
            if (world.getEntitiesOfClass(Player.class, new AABB(-r, -r, -r, r, r, r)).size() > 0)
                return;
            Entity en = findMonsterToSpawn(world, pos.above(), random);
            if (en != null) {
                en.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
                if (!world.noCollision(en) || !world.isUnobstructed(en)) return;
                world.addFreshEntity(en);
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter world, BlockPos pos, BlockState state, boolean p_176473_4_) {
        return false;//no
    }

    @Override
    public void performBonemeal(ServerLevel world, Random random, BlockPos pos, BlockState state) {
        //no
    }

    @Override
    public boolean isBonemealSuccess(Level world, Random random, BlockPos pos, BlockState state) {
        return false;//no
    }

    private Entity findMonsterToSpawn(ServerLevel world, BlockPos pos, Random rand) {
        //required to account for structure based mobs such as wither skeletons, remove blacklisted entities here, so they don't slow down spawning
        ServerChunkCache s = world.getChunkSource();
        List<MobSpawnSettings.SpawnerData> spawnOptions = s.getGenerator().getMobsAt(world.getBiome(pos), world.structureFeatureManager(), MobCategory.MONSTER, pos)
                .unwrap().stream().filter(spawners -> !spawners.type.is(CursedEarth.blacklisted_entities)).toList();
        //there is nothing to spawn
        if (spawnOptions.size() == 0) {
            return null;
        }



        int found = rand.nextInt(spawnOptions.size());
        MobSpawnSettings.SpawnerData entry = spawnOptions.get(found);
        //can the mob actually spawn here naturally, filters out mobs such as slimes which have more specific spawn requirements but
        // still show up in spawnlist; ignore them when force spawning
        if (!SpawnPlacements.checkSpawnRules(entry.type, world, MobSpawnType.NATURAL, pos, world.random)
                && !forceSpawn.get())
            return null;
        EntityType<?> type = entry.type;
        Entity ent = type.create(world);
        if (ent instanceof Mob)
            ((Mob) ent).finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        return ent;
    }
}