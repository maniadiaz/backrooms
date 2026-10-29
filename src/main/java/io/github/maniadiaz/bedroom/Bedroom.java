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
    public static final RegistryObject<Item> NUTRITION_BAR = ITEMS.register("nutrition_bar", () -> new Item(new Item.Properties().stacksTo(16)
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(8).saturationMod(.3F)
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100), 1).alwaysEat().build())));
    public static final RegistryObject<Block> PANEL = block("wall_panel", () -> new PanelBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> CARPET = block("damp_carpet", () -> new Block(BlockBehaviour.Properties.of().strength(1).sound(SoundType.WOOL)));
    public static final RegistryObject<Block> WET_CARPET = block("wet_carpet", () -> new Block(BlockBehaviour.Properties.of().strength(1).sound(SoundType.WOOL)));
    public static final RegistryObject<Block> CEILING = block("ceiling_tile", () -> new Block(BlockBehaviour.Properties.of().strength(1).sound(SoundType.STONE)));
    public static final RegistryObject<Block> LIGHT = block("fluorescent", () -> new Block(BlockBehaviour.Properties.of().strength(1).lightLevel(state -> 13).sound(SoundType.GLASS)));
    public static final RegistryObject<Block> DEAD_LIGHT = block("broken_fluorescent", () -> new Block(BlockBehaviour.Properties.of().strength(1).sound(SoundType.GLASS)));
    public static final RegistryObject<Block> EXIT = block("transition_wall", () -> new Block(BlockBehaviour.Properties.of().strength(-1, 3600000).sound(SoundType.WOOD)));
    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> GENERATORS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CHUNK_GENERATOR, MOD_ID);
    public static final RegistryObject<com.mojang.serialization.Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> LEVEL_0_GENERATOR =
            GENERATORS.register("level_0", () -> Level0Generator.CODEC);

    private static RegistryObject<Block> block(String name, java.util.function.Supplier<Block> factory) {
        RegistryObject<Block> block = BLOCKS.register(name, factory);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public Bedroom(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        GENERATORS.register(bus);
        context.registerConfig(ModConfig.Type.COMMON, BedroomConfig.SPEC);
        BedroomNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(DevelopmentCommands::register);
    }
}
