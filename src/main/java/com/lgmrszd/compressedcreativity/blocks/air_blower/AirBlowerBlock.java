package com.lgmrszd.compressedcreativity.blocks.air_blower;


import com.lgmrszd.compressedcreativity.index.CCTileEntities;
import com.lgmrszd.compressedcreativity.index.CCShapes;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ITE;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.block.IPneumaticWrenchable;
import me.desht.pneumaticcraft.api.misc.IMiscHelpers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;


public class AirBlowerBlock extends Block implements IPneumaticWrenchable, IWrenchable, ITE<AirBlowerTileEntity> {

    public static final Property<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public static final BooleanProperty[] CONNECTION_PROPERTIES = new BooleanProperty[]{DOWN, UP, NORTH, SOUTH, WEST, EAST};

//    public static final VoxelShape shape = Block.box(2, 2, 2, 12, 12, 12);

    public AirBlowerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.defaultBlockState()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(UP, false)
                        .setValue(DOWN, false)
                        .setValue(NORTH, false)
                        .setValue(SOUTH, false)
                        .setValue(EAST, false)
                        .setValue(WEST, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder
                .add(FACING)
                .add(UP)
                .add(DOWN)
                .add(NORTH)
                .add(SOUTH)
                .add(EAST)
                .add(WEST);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection());
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, world, pos, neighbor);
        BlockEntity te = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
        if (te instanceof AirBlowerTileEntity abte) {
            abte.updateAirHandler();
        }
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
//        BlockEntity te = worldIn.getBlockEntity(currentPos);
//        if (te != null && te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, facing).isPresent()) {
        if (facing != stateIn.getValue(FACING)) {
            BlockEntity other_te = worldIn.getBlockEntity(currentPos.relative(facing));
            boolean has_connection = other_te != null && other_te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, facing.getOpposite()).isPresent();
            stateIn = stateIn.setValue(CONNECTION_PROPERTIES[facing.get3DDataValue()], has_connection);
        } else {
            stateIn = stateIn.setValue(CONNECTION_PROPERTIES[facing.get3DDataValue()], false);
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        InteractionResult result = IWrenchable.super.onWrenched(state, context);
        if (result == InteractionResult.SUCCESS) {
            IMiscHelpers miscHelpers = PneumaticRegistry.getInstance().getMiscHelpers();
            miscHelpers.forceClientShapeRecalculation(context.getLevel(), context.getClickedPos());
            if(!context.getLevel().isClientSide()){
                if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof AirBlowerTileEntity abte) {
                    abte.updateAirHandler();
                }
            }
        }
        return result;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof AirBlowerTileEntity abte) {
                IMiscHelpers miscHelpers = PneumaticRegistry.getInstance().getMiscHelpers();
                miscHelpers.playMachineBreakEffect(abte);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return CCShapes.AIR_BLOWER.get(state.getValue(FACING));
    }

    @Override
    public Class<AirBlowerTileEntity> getTileEntityClass() {
        return AirBlowerTileEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirBlowerTileEntity> getTileEntityType() {
        return CCTileEntities.AIR_BLOWER.get();
    }

    @Override
    public boolean onWrenched(Level world, Player player, BlockPos pos, Direction side, InteractionHand hand) {
        UseOnContext ctx = new UseOnContext(player, hand, new BlockHitResult(new Vec3(pos.getX(), pos.getY(), pos.getX()), side, pos, false));
        return ctx.getPlayer() != null && (
                ctx.getPlayer().isCrouching() ?
                        onSneakWrenched(world.getBlockState(pos), ctx) :
                        onWrenched(world.getBlockState(pos), ctx)
        )  == InteractionResult.SUCCESS;
    }
}
