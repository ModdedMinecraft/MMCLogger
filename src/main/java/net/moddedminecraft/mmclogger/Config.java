package net.moddedminecraft.mmclogger;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Config {

    private static Main plugin;

    public String prefix = "&9[&6MMCLogger&9] ";

    private String[] commandToChatLogString = {
            "cmd1",
            "cmd2",
    };
    private String[] BlackListString = {
            "help",
            "who",
            "home"
    };
    private String[] commandNotifyListString = {
            "item",
            "give",
            "sponge",
            "op"
    };
    private String[] chatNotifyListString = {
            "ddos",
            "hack",
            "flymod",
            "dupe",
            "duplicate",
            "duplication"};

    public List<String> commandToChatLog;
    public List<String> BlackList;
    public List<String> commandNotifyList;
    public List<String> chatNotifyList;
    public boolean globalCommands;
    public boolean globalChat;
    public boolean playerCommands;
    public boolean playerChat;
    public boolean logNotifyChat;
    public boolean inGameNotifications;
    public boolean logNotifyCommands;
    public boolean playerLogin;
    public boolean globalLogin;
    public boolean disableFalsePositives;
    public boolean isWhitelist;
    public boolean checkForAliases;
    public String logFormat;
    public List<String> playerBlacklist;

    public String vclCharset;

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

        prefix = check(config.getNode("chat-prefix"), prefix, "Prefix for chat messages sent by this plugin.").getString();

        commandToChatLog =  checkList(config.getNode("log", "command-log", "filter-to-chatlog"), commandToChatLogString, "Some commands you might want as chat log messages instead, EG: /reply, This will filter these commands into the chatlog file instead").getList(TypeToken.of(String.class));

        BlackList =  checkList(config.getNode("log", "command-log", "blacklist"), BlackListString, "what commands do you not want to be logged in any file?").getList(TypeToken.of(String.class));
        commandNotifyList =  checkList(config.getNode("log", "notifications", "commands"), commandNotifyListString, "what commands do you want to be notified of when they are sent?").getList(TypeToken.of(String.class));
        chatNotifyList =  checkList(config.getNode("log", "notifications", "chat"), chatNotifyListString, "What words do you want to be notified of when they are said?").getList(TypeToken.of(String.class));
        playerBlacklist =  check(config.getNode("log", "player", "blacklist"), Collections.EMPTY_LIST, "What players do you not want to be logged?").getList(TypeToken.of(String.class));

        globalCommands =  check(config.getNode("log", "toggle", "global-commands"), true, "Log all command interactions to the main command files").getBoolean();
        globalChat =  check(config.getNode("log", "toggle", "global-chat"), true, "Log all chat interactions to the main chat files").getBoolean();
        playerCommands =  check(config.getNode("log", "toggle", "player-commands"), true, "Log players commands to their own files").getBoolean();
        playerChat =  check(config.getNode("log","toggle","player-chat"), true, "Log player's chat to their own files").getBoolean();
        logNotifyChat =  check(config.getNode("log", "toggle", "log-notify-chat"), true, "Log words specified in the notifications-chat section.").getBoolean();
        inGameNotifications =  check(config.getNode("log", "toggle", "in-game-notifications"), true, "Notify players in-game of specified words / commands").getBoolean();
        logNotifyCommands =  check(config.getNode("log", "toggle", "log-notify-commands"), true, "Log commands specified in the notifications-commands section.").getBoolean();
        playerLogin =  check(config.getNode("log", "toggle", "player-login"), true, "Log all player logins to the players own file.").getBoolean();
        globalLogin =  check(config.getNode("log", "toggle", "global-login"), true, "Log all player logins to the main log file.").getBoolean();
        logFormat =  check(config.getNode("log", "log-format"), "[%date] %name: %content", "The format of which the logs should be written. Formatting: %date, %world, %x, %y, %z, %name, %content").getString();

        disableFalsePositives =  check(config.getNode("log", "command-log", "disable-false-positives"), true, "If true, Any command will be checked to see if it is a registered command. (EG: '/help' is valid, '/heelp' might not be), If false, everything will be logged unless it is blacklisted").getBoolean();
        isWhitelist =  check(config.getNode("log", "command-log", "use-as-whitelist"), false, "If true, The blacklist will be treated as a whitelist and will only log the commands requested").getBoolean();
        checkForAliases =  check(config.getNode("log", "command-log", "check-for-aliases"), false, "If true, Commands in the blacklist will be checked to see if they have any aliases and blacklist those aswell.").getBoolean();

        vclCharset =  check(config.getNode("commands", "viewchatlog", "charset"), "UTF-8", "Default: UTF-8, Available: ISO-8859-1, UTF-8 (Only change if UTF-8 does not work for you)").getString();

        loader.save(config);

    }

    private CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue, String comment) {
        if (node.isVirtual()) {
            node.setValue(defaultValue).setComment(comment);
        }
        return node;
    }

    private CommentedConfigurationNode checkList(CommentedConfigurationNode node, String[] defaultValue, String comment) {
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

    public static void checkPlayer(String name) throws IOException {
        File file = new File(plugin.playersFolder, name + ".log");
        if (!file.exists()) {
            file.createNewFile();
        }
    }


}
