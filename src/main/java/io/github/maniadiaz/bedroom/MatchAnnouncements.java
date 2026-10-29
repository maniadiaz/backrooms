package io.github.maniadiaz.bedroom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

final class MatchAnnouncements {
    static void show(ServerPlayer p, String title, String subtitle, ChatFormatting color) {
        p.connection.send(new ClientboundClearTitlesPacket(true));
        p.connection.send(new ClientboundSetTitlesAnimationPacket(12, 50, 22));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle).withStyle(ChatFormatting.GRAY)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(color, ChatFormatting.BOLD)));
    }

    static void state(ServerPlayer p, MatchData.Member member, int level) {
        switch (member.state) {
            case ACTIVE -> show(p, "NIVEL " + level, "Busca tres fusibles. Repara los tres paneles.", ChatFormatting.GOLD);
            case LOBBY -> show(p, "THE BEDROOM", "Reúne al equipo. Pulsa para empezar.", ChatFormatting.GOLD);
            case LIMBO -> show(p, "NO ES EL FINAL", "30 segundos. Encuentra la salida.", ChatFormatting.WHITE);
            case SPECTATING -> show(p, "SIN RETORNO", "Tu equipo aún puede salvarte.", ChatFormatting.RED);
            case REST -> show(p, "UN RESPIRO", "90 segundos o pulsa para continuar.", ChatFormatting.AQUA);
        }
    }
    private MatchAnnouncements() {}
}
