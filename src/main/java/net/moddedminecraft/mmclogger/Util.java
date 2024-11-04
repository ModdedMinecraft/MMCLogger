package net.moddedminecraft.mmclogger;

import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import sawfowl.localeapi.api.TextUtils;

public class Util {

    public void broadcastMessage(String message) {
        Sponge.server().broadcastAudience().sendMessage(fromLegacy(message));
    }

    public static void sendMessage(CommandContext sender, String message) {
        sender.cause().audience().sendMessage(fromLegacy(message));
    }

    public static void sendMessage(ServerPlayer sender, String message) {
        sender.sendMessage(fromLegacy(message));
    }

    public static Component fromLegacy(String legacy) {
        return TextUtils.deserializeLegacy(legacy);
    }

}
