package tfar.cursedearth;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GrassBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static tfar.cursedearth.CursedEarth.ServerConfig.*;

import net.minecraft.block.AbstractBlock.Properties;

public class CursedEarthBlock extends GrassBlock {
    public CursedEarthBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        int i = minTickTime.get();
        if (i == 0) {
            i = 1;
        }
        world.getBlockTicks().scheduleTick(pos, this, world.random.nextInt(maxTickTime.get() - minTickTime.get()) + i);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (player.getItemInHand(hand).isEmpty() && player.isShiftKeyDown()) {
            if (!world.isClientSide) {

                ServerChunkProvider s = (ServerChunkProvider) world.getChunkSource();
                List<MobSpawnInfo.Spawners> entries = s.getGenerator().getMobsAt(world.getBiome(pos), ((ServerWorld) world).structureFeatureManager(), EntityClassification.MONSTER, pos.above())
                        .stream().filter(spawners -> !spawners.type.is(CursedEarth.blacklisted_entities)).collect(Collectors.toList());
                // nothing can spawn, occurs in places such as mushroom biomes
                if (entries.size() == 0) {
                    player.displayClientMessage(new TranslationTextComponent("text.cursedearth.nospawns"), true);
                } else {
                    TranslationTextComponent names1 = new TranslationTextComponent("Names: ");
                    for (int i = 0; i < entries.size(); i++) {
                        MobSpawnInfo.Spawners spawners = entries.get(i);
                        names1.append(spawners.type.getDescription());
                        if (i < entries.size() - 1) {
                            names1.append(new StringTextComponent(", "));
                        }
                    }
                     player.sendMessage(names1, Util.NIL_UUID);
                }
            }
            return ActionResultType.sidedSuccess(world.isClientSide);
        }
        return ActionResultType.PASS;
    }

    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!world.isClientSide) {
            int j = minTickTime.get();
            if (j == 0) {
                j = 1;
            }
            world.getBlockTicks().scheduleTick(pos, this, world.random.nextInt(maxTickTime.get() - minTickTime.get()) + j);
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
                        if (world.getBlockState(pos1).getBlock().is(CursedEarth.spreadable) && world.getBlockState(pos1.above()).isAir()) {
                            world.setBlockAndUpdate(pos1, blockstate.setValue(SNOWY, world.getBlockState(pos1.above()).getBlock() == Blocks.SNOW));
                        }
                    }
                }
            }

            world.getBlockTicks().scheduleTick(pos, state.getBlock(), random.nextInt(maxTickTime.get() - minTickTime.get() + 1));
            //dont spawn in water
            if (!world.getFluidState(pos.above()).isEmpty()) return;
            //don't spawn in peaceful
            if (world.getLevelData().getDifficulty() == Difficulty.PEACEFUL) return;
            //mobcap used because mobs are laggy in large numbers todo: how well does this work on servers
            long mobcount = world.getEntities().filter(IMob.class::isInstance).count();
            if (mobcount > mobCap.get()) return;
            int r = spawnRadius.get();
            if (world.getEntitiesOfClass(PlayerEntity.class, new AxisAlignedBB(-r, -r, -r, r, r, r)).size() > 0)
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
    public boolean isValidBonemealTarget(IBlockReader world, BlockPos pos, BlockState state, boolean p_176473_4_) {
        return false;//no
    }

    @Override
    public void performBonemeal(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        //no
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPos pos, BlockState state) {
        return false;//no
    }

    private Entity findMonsterToSpawn(ServerWorld world, BlockPos pos, Random rand) {
        //required to account for structure based mobs such as wither skeletons, remove blacklisted entities here, so they don't slow down spawning
        ServerChunkProvider s = world.getChunkSource();
        List<MobSpawnInfo.Spawners> spawnOptions = s.getGenerator().getMobsAt(world.getBiome(pos), world.structureFeatureManager(), EntityClassification.MONSTER, pos)
                .stream().filter(spawners -> !spawners.type.is(CursedEarth.blacklisted_entities)).collect(Collectors.toList());
        //there is nothing to spawn
        if (spawnOptions.size() == 0) {
            return null;
        }



        int found = rand.nextInt(spawnOptions.size());
        MobSpawnInfo.Spawners entry = spawnOptions.get(found);
        //can the mob actually spawn here naturally, filters out mobs such as slimes which have more specific spawn requirements but
        // still show up in spawnlist; ignore them when force spawning
        if (!EntitySpawnPlacementRegistry.checkSpawnRules(entry.type, world, SpawnReason.NATURAL, pos, world.random)
                && !forceSpawn.get())
            return null;
        EntityType<?> type = entry.type;
        Entity ent = type.create(world);
        if (ent instanceof MobEntity)
            ((MobEntity) ent).finalizeSpawn(world, world.getCurrentDifficultyAt(pos), SpawnReason.NATURAL, null, null);
        return ent;
    }
}