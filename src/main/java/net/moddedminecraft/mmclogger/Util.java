package net.moddedminecraft.mmclogger;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.serializer.TextSerializers;

public class Util {
    public static void broadcastMessage(String message) {
        Sponge.getServer().getBroadcastChannel().send(processColours(message), ChatTypes.SYSTEM);
    }

    public static void sendMessage(CommandSource sender, String message) {
        sender.sendMessage(processColours(message));
    }

    public static Text processColours(String str) {
        return fromLegacy('&', str);
    }

    public static Text fromLegacy(char legacyChar, String legacy) {
        return TextSerializers.formattingCode(legacyChar).deserializeUnchecked(legacy);
    }
}
