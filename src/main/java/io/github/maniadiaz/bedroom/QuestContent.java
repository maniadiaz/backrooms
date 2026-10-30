package io.github.maniadiaz.bedroom;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

final class QuestContent {
    static RegistryObject<Block> node(String name){return Bedroom.BLOCKS.register(name,()->new Block(BlockBehaviour.Properties.of().strength(-1,3600000).sound(SoundType.METAL)));}
    static final RegistryObject<Block> NOTE=Bedroom.BLOCKS.register("lore_note",()->new Block(BlockBehaviour.Properties.of().strength(-1,3600000).noCollission().noOcclusion().sound(SoundType.GRASS)){
        @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return Block.box(2,0,2,14,1,14);}
    });
    static final RegistryObject<Block> LOCKER=Bedroom.BLOCKS.register("hiding_locker",()->new Block(BlockBehaviour.Properties.of().strength(-1,3600000).noCollission().noOcclusion().sound(SoundType.METAL))),TOOLBOX=node("quest_toolbox"),SECURITY=node("hazmat_locker"),CONSOLE=node("master_console"),DOOR=node("armored_exit"),GATE=node("maintenance_gate");
    static final RegistryObject<Block> BLUE=node("puzzle_blue"),YELLOW=node("puzzle_yellow"),RED=node("puzzle_red"),GREEN=node("puzzle_green");
    static final RegistryObject<Item> WRENCH=Bedroom.ITEMS.register("wrench",()->new Item(new Item.Properties().stacksTo(1)));
    static final RegistryObject<Item> KEY=Bedroom.ITEMS.register("armored_key",()->new Item(new Item.Properties().stacksTo(1)));
    static RegistryObject<Item> armor(String name,ArmorItem.Type type){return Bedroom.ITEMS.register(name,()->new ArmorItem(ArmorMaterials.GOLD,type,new Item.Properties().stacksTo(1)){
        @Override public String getArmorTexture(ItemStack stack,net.minecraft.world.entity.Entity entity,net.minecraft.world.entity.EquipmentSlot slot,String layer){return "bedroom:textures/models/armor/hazmat_layer_1.png";}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
                @Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity entity,ItemStack stack,net.minecraft.world.entity.EquipmentSlot slot,net.minecraft.client.model.HumanoidModel<?> original){
                    if(slot==net.minecraft.world.entity.EquipmentSlot.CHEST){original.leftLeg.visible=true;original.rightLeg.visible=true;}return original;
                }
            });
        }
    });}
    static final RegistryObject<Item> HELMET=armor("hazmat_helmet",ArmorItem.Type.HELMET),SUIT=armor("hazmat_suit",ArmorItem.Type.CHESTPLATE),BOOTS=armor("hazmat_boots",ArmorItem.Type.BOOTS);
    static void init(){}
    private QuestContent(){}
}
