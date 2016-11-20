package net.moddedminecraft.mmclogger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Config {

    private static Main plugin;
    private File confFile;

    public Config(File file, Main main) {
        plugin = main;
        confFile = file;
        configCheck();
    }


    public void configCheck() {
        String[] blacklist = {
                "help",
                "who",
                "home"};
        String[] commandNotifyList = {
                "item",
                "give",
                "sponge",
                "op"};
        String[] chatNotifyList = {
                "ddos",
                "hack",
                "flymod",
                "dupe",
                "duplicate",
                "duplication"};

        try {
            if (!confFile.exists()) {
                confFile.createNewFile();
            }
            if (plugin.rootNode.getNode("log", "toggle", "global-commands").isVirtual()) {
                plugin.rootNode.getNode("log", "toggle", "global-commands").setValue(true);
            }
            if (plugin.rootNode.getNode("log", "toggle", "global-chat").isVirtual()) {
                plugin.rootNode.getNode("log", "toggle", "global-chat").setValue(true);
            }
            if (plugin.rootNode.getNode("log", "toggle", "player-commands").isVirtual()) {
                plugin.rootNode.getNode("log", "toggle", "player-commands").setValue(true);
            }
            if (plugin.rootNode.getNode("log","toggle","player-chat").isVirtual()) {
                plugin.rootNode.getNode("log","toggle","player-chat"). setValue(true);
            }
            if (plugin.rootNode.getNode("log", "toggle", "log-notify-chat").isVirtual()) {
                plugin.rootNode.getNode("log", "toggle", "log-notify-chat").setValue(true);
            }
            if (plugin.rootNode.getNode("log", "toggle", "in-game-notifications").isVirtual()) {
                plugin.rootNode.getNode("log", "toggle", "in-game-notifications").setValue(true);
            }
            if (plugin.rootNode.getNode("log", "toggle", "log-notify-commands").isVirtual()) {
                plugin.rootNode.getNode("log", "toggle", "log-notify-commands").setValue(true);
            }
            if (plugin.rootNode.getNode("log", "toggle", "player-login").isVirtual()) {
                plugin.rootNode.getNode("log", "toggle", "player-login").setValue(true);
            }
            if (plugin.rootNode.getNode("log", "toggle", "global-login").isVirtual()) {
                plugin.rootNode.getNode("log", "toggle", "global-login").setValue(true);
            }
            if (plugin.rootNode.getNode("log", "command-log", "blacklist").isVirtual()) {
                plugin.rootNode.getNode("log", "command-log", "blacklist").setValue(Arrays.asList(blacklist));
            }
            if (plugin.rootNode.getNode("log", "log-format").isVirtual()) {
                plugin.rootNode.getNode("log", "log-format").setValue("[%date] %name: %content");
            }
            if (plugin.rootNode.getNode("log", "notifications", "chat").isVirtual()) {
                plugin.rootNode.getNode("log", "notifications", "chat").setValue(Arrays.asList(chatNotifyList));
            }
            if (plugin.rootNode.getNode("log", "notifications", "commands").isVirtual()) {
                plugin.rootNode.getNode("log", "notifications", "commands").setValue(Arrays.asList(commandNotifyList));
            }
            plugin.loader.save(plugin.rootNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkPlayer(String name) throws IOException
    {
        File file = new File(plugin.playersFolder, name + ".log");
        if (!file.exists()) {
            file.createNewFile();
        }
    }

}
