package io.github.maniadiaz.bedroom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID,value=Dist.CLIENT)
public final class CheckpointMenu {
    private static void command(String command){var mc=Minecraft.getInstance();if(mc.player!=null){mc.player.connection.sendCommand(command);mc.setScreen(null);}}
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event){
        if(!(event.getScreen() instanceof PauseScreen screen)||!BedroomHud.inMatch())return;
        event.addListener(Button.builder(Component.literal("Checkpoint del mundo"),b->Minecraft.getInstance().setScreen(new Menu(screen))).bounds(8,screen.height-28,180,20).build());
    }
    private static final class Menu extends net.minecraft.client.gui.screens.Screen {
        private final PauseScreen parent;
        Menu(PauseScreen parent){super(Component.literal("Checkpoint del mundo"));this.parent=parent;}
        @Override protected void init(){
            int x=width/2-100,y=height/2-36;
            if(BedroomHud.inLobby()){
                addRenderableWidget(Button.builder(Component.literal("Empezar desde cero"),b->command("backrooms start")).bounds(x,y,200,20).build());
                addRenderableWidget(Button.builder(Component.literal("Último checkpoint"),b->command("backrooms continue")).bounds(x,y+24,200,20).build());
            }else addRenderableWidget(Button.builder(Component.literal("Guardar checkpoint"),b->command("backrooms save")).bounds(x,y,200,20).build());
            addRenderableWidget(Button.builder(Component.literal("Volver"),b->onClose()).bounds(x,height/2+40,200,20).build());
        }
        @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
        @Override public void render(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,float partial){
            renderBackground(graphics);super.render(graphics,mouseX,mouseY,partial);
            graphics.drawCenteredString(font,title,width/2,height/2-76,0xFFE0C46E);
            graphics.drawCenteredString(font,"Solo el anfitrión guarda o inicia.",width/2,height/2-58,0xFFB9B3A5);
        }
    }
    private CheckpointMenu(){}
}
