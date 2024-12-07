package nanonestor.cursedearth;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlessedFlowerBlock extends GrassBlock {

    public BlessedFlowerBlock(Properties properties) {
        super(properties.offsetType(OffsetType.XZ));
    }

    protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 10.0D, 13.0D, 10.0D);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
        //return SHAPE;
    }
}