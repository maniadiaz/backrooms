package io.github.maniadiaz.bedroom;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Bedroom.MOD_ID)
public final class Bedroom {
    public static final String MOD_ID = "bedroom";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Block> WALL = BLOCKS.register("wallpaper", () -> new Block(
            BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD)));
    public static final RegistryObject<Item> WALL_ITEM = ITEMS.register("wallpaper", () -> new BlockItem(WALL.get(), new Item.Properties()));

    public Bedroom(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        context.registerConfig(ModConfig.Type.COMMON, BedroomConfig.SPEC);
        MinecraftForge.EVENT_BUS.addListener(DevelopmentCommands::register);
    }
}
