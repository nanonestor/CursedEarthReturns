package nanonestor.cursedearth;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

import static net.minecraft.world.InteractionHand.MAIN_HAND;

public class MessageSpawns {
    @SubscribeEvent
    public void whatthat(final PlayerInteractEvent.RightClickBlock e) {

        Player player = e.getEntity();
        BlockPos pos = e.getPos();
        Level level = e.getLevel();
        Block block = level.getBlockState(pos).getBlock();

        // If block is neither a cursed or blessed earth block, return
        if (!(block instanceof CursedEarthBlock || block instanceof BlessedEarthBlock)) { return; }

        // Make a blank names variable
        MutableComponent names = Component.literal("");

        // Checks to see if the level is clientside, also because RightClickBlock fires twice, once for each hand,
        //  only go ahead if it's the MAIN_HAND firing so that the offhand second firing is skipped, avoiding duplicate messages.
        // Also checks that the actual player hand is empty and shift is held.
        if (!level.isClientSide && e.getHand() == MAIN_HAND && player.getMainHandItem().isEmpty() && player.isShiftKeyDown() ) {

            if (block instanceof CursedEarthBlock) {
                // Makes a list of mob entries based on the spawn data at that location, including structure spawning mobs, and not adding entries in the blacklisted data tag.
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

            if (block instanceof BlessedEarthBlock) {
                ServerChunkCache s = (ServerChunkCache) level.getChunkSource();
                List<MobSpawnSettings.SpawnerData> entries = s.getGenerator()
                        .getMobsAt(level.getBiome(pos), ((ServerLevel) level).structureManager(), MobCategory.CREATURE, pos.above())
                        .unwrap().stream().filter(spawners -> !spawners.type.is(CursedEarth.blacklisted_entities)).toList();
                // if nothing can spawn at the block location
                if (entries.isEmpty()) {
                    player.displayClientMessage(Component.translatable("text.cursedearth.nospawns").withStyle(ChatFormatting.GOLD), true);
                } else {
                    names.append(Component.literal("Blessed Earth Spawning Mobs: ").withStyle(ChatFormatting.DARK_AQUA));
                    for (int i = 0; i < entries.size(); i++) {
                        MobSpawnSettings.SpawnerData spawners = entries.get(i);
                        MutableComponent this_name = (MutableComponent) spawners.type.getDescription();
                        names.append(this_name.withStyle(ChatFormatting.GOLD));
                        if (i < entries.size() - 1) {
                            names.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
                        }
                    }
                    player.displayClientMessage(names, false);

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.OMINOUS_SPAWNING,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 60, 0.5, 1.2, 0.5, 0.05);
                    }
                }
            }

        }
    }

}