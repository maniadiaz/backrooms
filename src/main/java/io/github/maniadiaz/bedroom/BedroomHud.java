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

    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("match_status", (gui, graphics, partialTick, width, height) -> {
            var mc = Minecraft.getInstance();
            var hud = current;
            if (hud == null || !hud.visible() || mc.player == null || mc.options.hideGui) return;
            int x = 12, y = Math.max(12, height / 2 - 38), w = 164;
            graphics.fill(x, y, x + w, y + 76, 0xBD11110E);
            graphics.fill(x, y, x + 2, y + 76, 0xFFE0C46E);
            graphics.fill(x + 8, y + 23, x + w - 8, y + 24, 0x665B5138);
            graphics.drawString(mc.font, "BEDROOM: " + hud.state(), x + 10, y + 9, 0xFFE0C46E, false);
            graphics.drawString(mc.font, "MUERTES  " + hud.deaths() + " / 3", x + 10, y + 32, 0xFFF3EDE0, false);
            for (int i = 0; i < 3; i++) graphics.fill(x + 120 + i * 10, y + 33, x + 126 + i * 10, y + 39,
                    i < hud.deaths() ? 0xFFE46565 : 0xFF514A3D);
            String detail = switch (hud.state()) {
                case "LIMBO" -> "SALIDA: " + timer(hud.seconds());
                case "REST" -> "DESCANSO: " + timer(hud.seconds());
                case "WAITING" -> "ESPERANDO UN PUESTO";
                case "SPECTATING" -> "OBSERVANDO AL EQUIPO";
                case "LOBBY" -> "GRUPO: " + hud.players() + " / 4";
                default -> "NIVEL " + hud.level() + " | GRUPO " + hud.players() + "/4";
            };
            graphics.drawString(mc.font, detail, x + 10, y + 56, 0xFFB9B3A5, false);
        });
    }

    private static String timer(int seconds) { return String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60); }

    @Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, value = Dist.CLIENT)
    public static final class Lifecycle {
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { current = null; }
    }

    private BedroomHud() {}
}
