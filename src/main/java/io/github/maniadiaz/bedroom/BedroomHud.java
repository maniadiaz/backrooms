package io.github.maniadiaz.bedroom;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only drawing. Common/server code never accesses Minecraft's GUI classes. */
@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BedroomHud {
    private static BedroomNetwork.Hud current;
    public static void update(BedroomNetwork.Hud message) { current = message; }
    static boolean canCycleCamera() { return current != null && (current.state().equals("SPECTATING") || current.state().equals("OBSERVING")); }
    static boolean activeLevel() { return current != null && current.visible() && current.state().equals("ACTIVE"); }
    static float sanity() { return current == null ? 100 : current.sanity(); }

    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("match_status", (gui, graphics, partialTick, width, height) -> {
            var mc = Minecraft.getInstance();
            var hud = current;
            if (hud == null || !hud.visible() || mc.player == null || mc.options.hideGui) return;
            if (hud.state().equals("ACTIVE") && hud.sanity() < 50) for (int i = 0; i < 32; i++) {
                int color = ((int) ((1 - i / 32F) * (1 - hud.sanity() / 50F) * 180)) << 24;
                graphics.fill(i, i, width - i, i + 1, color); graphics.fill(i, height - i - 1, width - i, height - i, color);
                graphics.fill(i, i, i + 1, height - i, color); graphics.fill(width - i - 1, i, width - i, height - i, color);
            }
            int x = 12, y = Math.max(12, height / 2 - 62), w = 190, h = hud.voteSeconds() > 0 ? 190 : 160;
            graphics.fill(x, y, x + w, y + h, 0xBD11110E);
            graphics.fill(x, y, x + 2, y + h, 0xFFE0C46E);
            graphics.fill(x + 8, y + 23, x + w - 8, y + 24, 0x665B5138);
            graphics.drawString(mc.font, "BACKROOMS: " + hud.state(), x + 10, y + 9, 0xFFE0C46E, false);
            graphics.drawString(mc.font, "MUERTES  " + hud.deaths() + " / 3", x + 10, y + 32, 0xFFF3EDE0, false);
            for (int i = 0; i < 3; i++) graphics.fill(x + 120 + i * 10, y + 33, x + 126 + i * 10, y + 39,
                    i < hud.deaths() ? 0xFFE46565 : 0xFF514A3D);
            String detail = switch (hud.state()) {
                case "LIMBO" -> "SALIDA: " + timer(hud.seconds());
                case "REST" -> "DESCANSO: " + timer(hud.seconds());
                case "VICTORY" -> "¡VICTORIA!  "+timer(hud.seconds());
                case "WAITING" -> "ESPERANDO UN PUESTO";
                case "SPECTATING", "OBSERVING" -> "CAMARAS: [ / ]";
                case "LOBBY" -> "GRUPO: " + hud.players() + " / 4";
                default -> "NIVEL " + hud.level() + " | GRUPO " + hud.players() + "/4";
            };
            graphics.drawString(mc.font, detail, x + 10, y + 56, 0xFFB9B3A5, false);
            graphics.drawString(mc.font, "CORDURA  " + Math.round(hud.sanity()) + "%", x + 10, y + 71, hud.sanity() < 25 ? 0xFFFF7777 : 0xFFE0C46E, false);
            graphics.drawString(mc.font, hud.battery() < 0 ? "LINTERNA APAGADA" : "BATERIA  " + hud.battery() + "%", x + 10, y + 85, 0xFFB9B3A5, false);
            if(hud.state().equals("ACTIVE")||hud.state().equals("SPECTATING")||hud.state().equals("OBSERVING")) {
                graphics.drawString(mc.font,"PANELES  "+hud.panels()+" / 3",x+10,y+100,0xFFE0C46E,false);
                graphics.drawString(mc.font,hud.panels()==3?(hud.charge()>0?"NO-CLIP  "+hud.charge()*100/40+"%":"SALIDA HABILITADA"):(hud.level()==0?"BUSCA LOS TRES FUSIBLES":"BUSCA LOS TRES RELÉS"),x+10,y+115,0xFF80DBCE,false);
            }
            graphics.drawString(mc.font,hud.difficulty(),x+10,y+132,0xFFB9B3A5,false);
            if(hud.state().equals("ACTIVE"))graphics.drawString(mc.font,"CONTROL: "+hud.objective(),x+10,y+146,0xFF80DBCE,false);
            if (hud.voteSeconds() > 0) {
                graphics.drawString(mc.font, "REINICIO " + hud.voteYes() + "/" + hud.voteRequired() + " | " + hud.voteSeconds() + "s", x + 10, y + 163, 0xFF80DBCE, false);
                graphics.drawString(mc.font, "/backrooms vote ...", x + 10, y + 177, 0xFFB9B3A5, false);
            }
        });
        event.registerAboveAll("noclip_blackout",(gui,graphics,partial,width,height)->{if(NoClipClient.black())graphics.fill(0,0,width,height,0xFF000000);});
    }

    private static String timer(int seconds) { return String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60); }

    @Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, value = Dist.CLIENT)
    public static final class Lifecycle {
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { current = null;NoClipClient.reset(); }
    }

    private BedroomHud() {}
}
