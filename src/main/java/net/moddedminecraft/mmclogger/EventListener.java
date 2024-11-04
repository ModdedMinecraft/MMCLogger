package net.moddedminecraft.mmclogger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.world.server.ServerLocation;

import java.io.IOException;
import java.util.List;

public class EventListener {
    private Main plugin;

    EventListener(Main main) {
        plugin = main;
    }

    @Listener
    public void onPlayerChat(MessageEvent event, @Root ServerPlayer player) {
        String name = player.name();
        Component message = event.message();
        ServerLocation location = player.serverLocation();
        double xLocation = location.x();
        double yLocation = location.y();
        double zLocation = location.z();
        String world = location.world().key().value();
        String date = plugin.getDate();
        try {
            Config.checkPlayer(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        plugin.processInformation(name, message, xLocation, yLocation, zLocation, world, date);
    }

    @Listener
    public void onPlayerCommand(ExecuteCommandEvent.Pre event, @Root ServerPlayer player) throws IOException {
        String command = event.command().toLowerCase();
        String arguments = event.arguments();
        String name = player.name();
        ServerLocation location = player.serverLocation();
        double xLocation = location.x();
        double yLocation = location.y();
        double zLocation = location.z();
        String world = location.world().key().value();
        String date = plugin.getDate();
        Config.checkPlayer(name);
        List<String> commandToChatLog = plugin.config().commandToChatLog;
        for (String cmd : commandToChatLog) {
            if (command.equalsIgnoreCase(cmd)) {
                Component commandLine = Component.text("/" + command + " " + arguments);
                plugin.processInformation(name, commandLine, xLocation, yLocation, zLocation, world, date);
                return;
            }
        }
        plugin.processCMDInformation(player, name, command, arguments, xLocation, yLocation, zLocation, world, date);
    }

    @Listener
    public void onConsoleCommand(ExecuteCommandEvent.Pre context) throws IOException {
        if (context.cause().root() instanceof ServerPlayer) {
            return;
        }
        String command = context.command().toLowerCase();
        String arguments = context.arguments();
        String date = plugin.getDate();
        Config.checkPlayer("Console");
        List<String> commandToChatLog = plugin.config().commandToChatLog;
        for (String cmd : commandToChatLog) {
            if (command.equalsIgnoreCase(cmd)) {
                Component commandLine = Component.text("/" + command + " " + arguments);
                plugin.processInformation("Console", commandLine, 0, 0, 0, "Console", date);
                return;
            }
        }
        plugin.processCMDInformationConsole("Console", command, arguments,  0, 0, 0, "Console", date);
    }

    @Listener
    public void onPlayerLogin(ServerSideConnectionEvent.Join event) throws IOException {
        ServerPlayer player = event.player();
        String name = player.name();
        ServerLocation location = player.serverLocation();
        double xLocation = location.x();
        double yLocation = location.y();
        double zLocation = location.z();
        String world = location.world().key().value();
        String date = plugin.getDate();
        Config.checkPlayer(name);

        plugin.processInformationJoin(name, xLocation, yLocation, zLocation, world, date);
    }

    @Listener
    public void onPlayerDisconnect(ServerSideConnectionEvent.Disconnect event) throws IOException {
        ServerPlayer player = event.player();
        String name = player.name();
        ServerLocation location = player.serverLocation();
        double xLocation = location.x();
        double yLocation = location.y();
        double zLocation = location.z();
        String world = location.world().key().value();
        String date = plugin.getDate();
        Config.checkPlayer(name);

        plugin.processInformationQuit(name, xLocation, yLocation, zLocation, world, date);
    }
}
