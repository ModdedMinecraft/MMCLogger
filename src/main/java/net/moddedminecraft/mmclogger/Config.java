package net.moddedminecraft.mmclogger;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Config {

    private static Main plugin;

    static String[] BlackListString = {
            "help",
            "who",
            "home"
    };
    static String[] commandNotifyListString = {
            "item",
            "give",
            "sponge",
            "op"
    };
    static String[] chatNotifyListString = {
            "ddos",
            "hack",
            "flymod",
            "dupe",
            "duplicate",
            "duplication"};

    public static List<String> BlackList;
    public static List<String> commandNotifyList;
    public static List<String> chatNotifyList;
    public static boolean globalCommands;
    public static boolean globalChat;
    public static boolean playerCommands;
    public static boolean playerChat;
    public static boolean logNotifyChat;
    public static boolean inGameNotifications;
    public static boolean logNotifyCommands;
    public static boolean playerLogin;
    public static boolean GlobalLogin;
    public static String logFormat;

    private static ConfigurationLoader<CommentedConfigurationNode> loader;
    private static CommentedConfigurationNode config;

    public Config(Main main) throws IOException, ObjectMappingException {
        plugin = main;
        loader = HoconConfigurationLoader.builder().setPath(plugin.defaultConf).build();
        config = loader.load();
        checkFolders();
        configCheck();
    }

    public void configCheck() throws IOException, ObjectMappingException {

        if (!plugin.defaultConfFile.exists()) {
            plugin.defaultConfFile.createNewFile();
        }

        BlackList =  checkList(config.getNode("log", "command-log", "blacklist"), BlackListString, "what commands do you not want to be logged in any file?").getList(TypeToken.of(String.class));
        commandNotifyList =  checkList(config.getNode("log", "notifications", "commands"), commandNotifyListString, "what commands do you want to be notified of when they are sent?").getList(TypeToken.of(String.class));
        chatNotifyList =  checkList(config.getNode("log", "notifications", "chat"), chatNotifyListString, "What words do you want to be notified of when they are said?").getList(TypeToken.of(String.class));

        globalCommands =  check(config.getNode("log", "toggle", "global-commands"), true, "Log all command interactions to the main command files").getBoolean();
        globalChat =  check(config.getNode("log", "toggle", "global-chat"), true, "Log all chat interactions to the main chat files").getBoolean();
        playerCommands =  check(config.getNode("log", "toggle", "player-commands"), true, "Log players commands to their own files").getBoolean();
        playerChat =  check(config.getNode("log","toggle","player-chat"), true, "Log player's chat to their own files").getBoolean();
        logNotifyChat =  check(config.getNode("log", "toggle", "log-notify-chat"), true, "Log words specified in the notifications-chat section.").getBoolean();
        inGameNotifications =  check(config.getNode("log", "toggle", "in-game-notifications"), true, "Notify players in-game of specified words / commands").getBoolean();
        logNotifyCommands =  check(config.getNode("log", "toggle", "log-notify-commands"), true, "Log commands specified in the notifications-commands section.").getBoolean();
        playerLogin =  check(config.getNode("log", "toggle", "player-login"), true, "Log all player logins to the players own file.").getBoolean();
        GlobalLogin =  check(config.getNode("log", "toggle", "global-login"), true, "Log all player logins to the main log file.").getBoolean();
        logFormat =  check(config.getNode("log", "log-format"), "[%date] %name: %content", "The format of which the logs should be writen.").getString();

        loader.save(config);

    }

    private static CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue, String comment) {
        if (node.isVirtual()) {
            node.setValue(defaultValue).setComment(comment);
        }
        return node;
    }

    private static CommentedConfigurationNode checkList(CommentedConfigurationNode node, String[] defaultValue, String comment) {
        if (node.isVirtual()) {
            node.setValue(Arrays.asList(defaultValue)).setComment(comment);
        }
        return node;
    }

    private void checkFolders() {
        if (!plugin.chatlogFolder.isDirectory()) {
            plugin.chatlogFolder.mkdirs();
        }
        if (!plugin.commandlogFolder.isDirectory()) {
            plugin.commandlogFolder.mkdirs();
        }
        if (!plugin.playersFolder.isDirectory()) {
            plugin.playersFolder.mkdirs();
        }
        if (!plugin.clogFolder.isDirectory()) {
            plugin.clogFolder.mkdirs();
        }
        if (!plugin.logFolder.isDirectory()) {
            plugin.logFolder.mkdirs();
        }
    }

    static void checkPlayer(String name) throws IOException {
        File file = new File(plugin.playersFolder, name + ".log");
        if (!file.exists()) {
            file.createNewFile();
        }
    }


}
