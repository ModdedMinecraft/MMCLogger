package net.moddedminecraft.mmclogger;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.serializer.TextSerializers;

public class Util {

    public static void broadcastMessage(String message) {
        Sponge.getServer().getBroadcastChannel().send(fromLegacy(message), ChatTypes.SYSTEM);
    }

    static void sendMessage(CommandSource sender, String message) {
        sender.sendMessage(fromLegacy(message));
    }

    private static Text fromLegacy(String legacy) {
        return TextSerializers.FORMATTING_CODE.deserializeUnchecked(legacy);
    }

}
