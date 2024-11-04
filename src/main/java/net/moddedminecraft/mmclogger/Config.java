package net.moddedminecraft.mmclogger;

import com.google.common.reflect.TypeToken;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

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
    public boolean consoleCommands;
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

    public Config(Main main) throws IOException {
        plugin = main;
        loader = HoconConfigurationLoader.builder().path(plugin.defaultConf).build();
        config = loader.load();
        checkFolders();
        configCheck();
    }

    public void configCheck() throws IOException {

        if (!plugin.configDir.toFile().exists()) {
            plugin.configDir.toFile().createNewFile();
        }

        prefix = check(config.node("chat-prefix"), prefix, "Prefix for chat messages sent by this plugin.").getString();

        commandToChatLog =  checkList(config.node("log", "command-log", "filter-to-chatlog"), commandToChatLogString, "Some commands you might want as chat log messages instead, EG: /reply, This will filter these commands into the chatlog file instead").getList(String.class);

        BlackList =  checkList(config.node("log", "command-log", "blacklist"), BlackListString, "what commands do you not want to be logged in any file?").getList(String.class);
        commandNotifyList =  checkList(config.node("log", "notifications", "commands"), commandNotifyListString, "what commands do you want to be notified of when they are sent?").getList(String.class);
        chatNotifyList =  checkList(config.node("log", "notifications", "chat"), chatNotifyListString, "What words do you want to be notified of when they are said?").getList(String.class);
        playerBlacklist =  check(config.node("log", "player", "blacklist"), Collections.EMPTY_LIST, "What players do you not want to be logged?").getList(String.class);

        globalCommands =  check(config.node("log", "toggle", "global-commands"), true, "Log all player command interactions to the main command files").getBoolean();
        globalChat =  check(config.node("log", "toggle", "global-chat"), true, "Log all chat interactions to the main chat files").getBoolean();
        playerCommands =  check(config.node("log", "toggle", "player-commands"), true, "Log players commands to their own files").getBoolean();
        playerChat =  check(config.node("log","toggle","player-chat"), true, "Log player's chat to their own files").getBoolean();
        consoleCommands =  check(config.node("log", "toggle", "console-commands"), true, "Log all console commands").getBoolean();
        logNotifyChat =  check(config.node("log", "toggle", "log-notify-chat"), true, "Log words specified in the notifications-chat section.").getBoolean();
        inGameNotifications =  check(config.node("log", "toggle", "in-game-notifications"), true, "Notify players in-game of specified words / commands").getBoolean();
        logNotifyCommands =  check(config.node("log", "toggle", "log-notify-commands"), true, "Log commands specified in the notifications-commands section.").getBoolean();
        playerLogin =  check(config.node("log", "toggle", "player-login"), true, "Log all player logins to the players own file.").getBoolean();
        globalLogin =  check(config.node("log", "toggle", "global-login"), true, "Log all player logins to the main log file.").getBoolean();
        logFormat =  check(config.node("log", "log-format"), "[%date] %name: %content", "The format of which the logs should be written. Formatting: %date, %world, %x, %y, %z, %name, %content").getString();

        disableFalsePositives =  check(config.node("log", "command-log", "disable-false-positives"), true, "If true, Any command will be checked to see if it is a registered command. (EG: '/help' is valid, '/heelp' might not be), If false, everything will be logged unless it is blacklisted").getBoolean();
        isWhitelist =  check(config.node("log", "command-log", "use-as-whitelist"), false, "If true, The blacklist will be treated as a whitelist and will only log the commands requested").getBoolean();
        checkForAliases =  check(config.node("log", "command-log", "check-for-aliases"), false, "If true, Commands in the blacklist will be checked to see if they have any aliases and blacklist those aswell.").getBoolean();

        vclCharset =  check(config.node("commands", "viewchatlog", "charset"), "UTF-8", "Default: UTF-8, Available: ISO-8859-1, UTF-8 (Only change if UTF-8 does not work for you)").getString();

        loader.save(config);

    }

    private CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue, String comment) throws SerializationException {
        if (node.virtual()) {
            node.set(defaultValue).comment(comment);
        }
        return node;
    }

    private CommentedConfigurationNode checkList(CommentedConfigurationNode node, String[] defaultValue, String comment) throws SerializationException {
        if (node.virtual()) {
            node.set(Arrays.asList(defaultValue)).comment(comment);
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
