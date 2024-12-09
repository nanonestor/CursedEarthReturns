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
    public static final Block cursed_earth = new CursedEarthBlock(Properties.copy(Blocks.GRASS_BLOCK));
    public static final Item cursed_earth_item = new BlockItem(cursed_earth,new Item.Properties());

    public CursedEarthBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        schedule(pos, level);
    }

    public void schedule(BlockPos pos,Level level) {
        int maxTime = CursedEarth.ServerConfig.maxTickTime.get();
        int minTime = CursedEarth.ServerConfig.minTickTime.get();
        level.scheduleTick(pos, this, level.random.nextInt(maxTime - minTime));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).isEmpty() && player.isShiftKeyDown()) {
            if (!level.isClientSide) {

                MutableComponent names = Component.literal("");

                ServerChunkCache s = (ServerChunkCache) level.getChunkSource();
                List<MobSpawnSettings.SpawnerData> entries = s.getGenerator()
                        .getMobsAt(level.getBiome(pos), ((ServerLevel) level).structureManager(), MobCategory.MONSTER, pos.above())
                        .unwrap().stream().filter(spawners -> !spawners.type.is(CursedEarth.blacklisted_entities)).toList();
                // If nothing can spawn at the block location, message that no spawns will happen.
                if (entries.isEmpty()) {
                    player.displayClientMessage(Component.translatable("text.cursedearth.nospawns").withStyle(ChatFormatting.GOLD), true);
                } else {
                    // Creates the string to use as a message, running through each entry in the list
                    names.append(Component.literal("Cursed Earth Spawning Mobs: ").withStyle(ChatFormatting.DARK_PURPLE));
                    for (int i = 0; i < entries.size(); i++) {
                        MobSpawnSettings.SpawnerData spawners = entries.get(i);
                        MutableComponent this_name = (MutableComponent) spawners.type.getDescription();
                        names.append(this_name.withStyle(ChatFormatting.GOLD));
                        if (i < entries.size() - 1) { names.append(Component.literal(", ").withStyle(ChatFormatting.AQUA)); }
                       }
                    // Displays the message for which spawns can happen
                    player.displayClientMessage(names, false);
                    // Makes a particle effect over the block right-clicked
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ASH,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 60, 0.5, 1.2, 0.5, 0.05);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide) {
            schedule(pos,level);
            if (!level.isAreaLoaded(pos, 3))
                return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading

            boolean dark = level.getMaxLocalRawBrightness(pos.above()) < CursedEarth.ServerConfig.burnLightLevel.get();
            if (!dark && CursedEarth.ServerConfig.diesFromLightLevel.get()) {
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                BlockPos up = pos.above();
                if (level.getBlockState(up).isAir()) {
                    level.setBlockAndUpdate(up,Blocks.FIRE.defaultBlockState());
                }
            } else {
                if (dark && CursedEarth.ServerConfig.naturallySpreads.get() && level.getBlockState(pos.above()).isAir()) {
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
            int r = CursedEarth.ServerConfig.spawnRadius.get();
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

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean p_176473_4_) {
        return false;//no
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        //no
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return false;//no
    }

    private Entity findMonsterToSpawn(ServerLevel level, BlockPos pos, RandomSource rand) {
        //required to account for structure based mobs such as wither skeletons, remove blacklisted entities here, so they don't slow down spawning
        ServerChunkCache s = level.getChunkSource();
        List<MobSpawnSettings.SpawnerData> spawnOptions = s.getGenerator().getMobsAt(level.getBiome(pos), level.structureManager(), MobCategory.MONSTER, pos)
                .unwrap().stream().filter(spawners -> !spawners.type.is(CursedEarth.blacklisted_entities)).toList();
        //there is nothing to spawn
        if (spawnOptions.isEmpty()) {
            return null;
        }

        int found = rand.nextInt(spawnOptions.size());
        MobSpawnSettings.SpawnerData entry = spawnOptions.get(found);
        //can the mob actually spawn here naturally, filters out mobs such as slimes which have more specific spawn requirements but
        // still show up in spawnlist; ignore them when force spawning
        if (!SpawnPlacements.checkSpawnRules(entry.type, level, MobSpawnType.NATURAL, pos, level.random)
                && !CursedEarth.ServerConfig.forceSpawn.get())
            return null;
        EntityType<?> type = entry.type;
        Entity ent = type.create(level);
        if (ent instanceof Mob)
            ((Mob) ent).finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        return ent;
    }
}
