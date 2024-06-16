package nanonestor.cursedearth;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;


public class BlessedFlowerBlock extends FlowerBlock {


    public static final Block blessed_flower = new BlessedFlowerBlock((Holder<MobEffect>) MobEffects.REGENERATION, 7, Properties.ofFullCopy(Blocks.DANDELION), BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION).noCollission().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ));
    public static final Item blessed_flower_item = new BlockItem(blessed_flower,new Item.Properties());

    public BlessedFlowerBlock(Holder<MobEffect> stewEffect, int stewDuration, Properties properties, Properties offsetType) {
        super(stewEffect, stewDuration, properties);
    }

}