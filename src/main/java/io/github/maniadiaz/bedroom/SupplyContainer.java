package io.github.maniadiaz.bedroom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class SupplyContainer extends Block implements EntityBlock {
    static final BooleanProperty OPENED = BooleanProperty.create("opened");
    private final boolean filing;
    SupplyContainer(boolean filing) {
        super(Properties.of().strength(2).sound(filing ? net.minecraft.world.level.block.SoundType.METAL : net.minecraft.world.level.block.SoundType.WOOD).noOcclusion());
        this.filing = filing; registerDefaultState(stateDefinition.any().setValue(OPENED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(OPENED); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(2, 0, 2, 14, filing ? 16 : 12, 14); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Contents(pos, state); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && p instanceof net.minecraft.server.level.ServerPlayer player && SurvivalSystem.active(player)
                && level.getBlockEntity(pos) instanceof Contents contents) {
            contents.unpackLootTable(player);
            level.setBlock(pos, state.setValue(OPENED, true), 3);
            player.openMenu(contents);
            MatchData.get(player.server).members.get(player.getUUID()).survival.quietTicks = 0;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    static final class Contents extends RandomizableContainerBlockEntity {
        private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        Contents(BlockPos pos, BlockState state) { super(Bedroom.SUPPLY_ENTITY.get(), pos, state); }
        @Override public int getContainerSize() { return 27; }
        @Override protected NonNullList<ItemStack> getItems() { return items; }
        @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
        @Override protected Component getDefaultName() { return Component.translatable(getBlockState().is(Bedroom.FILING_CABINET.get()) ? "block.bedroom.filing_cabinet" : "block.bedroom.cardboard_box"); }
        @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return ChestMenu.threeRows(id, inventory, this); }
        @Override protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag); if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items);
        }
        @Override public void load(CompoundTag tag) {
            super.load(tag); items = NonNullList.withSize(27, ItemStack.EMPTY);
            if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items);
        }
    }
}
