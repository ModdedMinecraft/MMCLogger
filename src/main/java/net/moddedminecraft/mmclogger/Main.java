package net.moddedminecraft.mmclogger;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.TaskFuture;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import sawfowl.localeapi.api.TextUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Plugin("mmclogger")
public class Main {

    public static final Logger logger = LogManager.getLogger("MMCLogger");

    @Inject
    @DefaultConfig(sharedRoot = false)
    public Path defaultConf;

    @Inject
    @ConfigDir(sharedRoot = false)
    public Path configDir;

    private Config config;

    public File rootFolder;
    public File chatlogFolder;
    public File commandlogFolder;
    public File playersFolder;

    public File logFolder;
    public File clogFolder;

    private File notifyChatFile;
    private File notifyCommandFile;

    private Scheduler scheduler = Sponge.asyncScheduler();

    public final PluginContainer container;
    
    @Inject
    public Main(final PluginContainer container) {
        this.container = container;
    }

    @Listener
    public void onServerAboutStart(ConstructPluginEvent event) throws IOException {
        Sponge.eventManager().registerListeners(container, new EventListener(this));
        rootFolder= new File("chatlogs/");;
        chatlogFolder = new File("chatlogs/logs");
        commandlogFolder = new File(configDir.toFile(), "chatlogs/commandlogs");
        playersFolder = new File(configDir.toFile(), "chatlogs/players");

        logFolder = new File(chatlogFolder, getFolderDate());
        clogFolder = new File(commandlogFolder, getFolderDate());

        notifyChatFile = new File(configDir.toFile(), "chatlogs/notifyChat.log");
        notifyCommandFile = new File(configDir.toFile(), "chatlogs/notifyCommands.log");


        this.config = new Config(this);
        //scheduler.executor(container).submit(() -> this::checkDate).interval(1, TimeUnit.SECONDS).name("mmclogger-S-DateChecker").submit(this);
    }

    @Listener
    public void onServerStart(StartedEngineEvent<Server> event) {
        logger.info("MMCLogger Loaded");
    }

    @Listener
    public void onPluginReload(RefreshGameEvent event) throws IOException {
        this.config = new Config(this);
        logger.info("MMCLogger Config Reloaded");
    }
    
    @Listener
    public void onRegisterSpongeCommand(final RegisterCommandEvent<Command.Parameterized> event) {
        event.register(this.container,Command.builder()
                .shortDescription(Component.text("View chat logs"))
                .permission("mmclogger.viewlogs")
                .addParameters(Parameter.user().optional().key("player").build())
                        .executor(new ViewLogCommand(this))
                .build(), "viewchatlogs", "vcl");
    }

    public void processInformation(String playerName, Component chat, double x, double y, double z, String worldName, String date) {
        boolean globalChat = config().globalChat;
        boolean playerChat = config().playerChat;
        boolean logNotifyChat = config().logNotifyChat;
        boolean inGameNotifications = config().inGameNotifications;

        File playerFile = new File(playersFolder, playerName + ".log");
        String message = LegacyComponentSerializer.legacyAmpersand().serialize(chat);//"(&([a-f0-9]))", "");

        try {
            if (globalChat && (!playerCheck(playerName))) {
                scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, message, x, y, z, worldName, date), getChatFile()));
            }
            if (playerChat && (!playerCheck(playerName))) {
                scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, message, x, y, z, worldName, date), playerFile));
            }
            if ((checkNotifyListPlayer(message)) && (logNotifyChat) && (!playerCheck(playerName))) {
                scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, message, x, y, z, worldName, date), notifyChatFile));
            }
            if ((checkNotifyListPlayer(message)) && (inGameNotifications) && (!playerCheck(playerName))) {
                notifyPlayer(config.prefix + playerName + "&f: " + message);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void processCMDInformation(ServerPlayer player, String playerName, String command, String args, double x, double y, double z, String worldName, String date) {

        boolean playerCommand = config().playerCommands;
        boolean globalCommand = config().globalCommands;
        boolean logNotifyCommands = config().logNotifyCommands;
        boolean inGameNotifications = config().inGameNotifications;

        File playerFile = new File(playersFolder, playerName + ".log");
        String commandLine = "/" + command + " " + args;

        Optional<? extends CommandMapping> optionalCommandMapping = Sponge.server().commandManager().commandMapping(command);
        Set<String> commands = optionalCommandMapping.map(commandMapping -> commandMapping.allAliases().stream().map(String::toLowerCase).collect(Collectors.toSet())).orElseGet(() -> Sets.newHashSet("....."));

        if (command.equalsIgnoreCase("sponge:callback")) {
            return;
        }

        if (config().isWhitelist) {
            if (config().disableFalsePositives) {
                if (commands.stream().anyMatch(command.toLowerCase()::equals)) {
                    if (config().checkForAliases) {
                        if (globalCommand && commandCheck(commands) && !playerCheck(playerName)) {
                            scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile()));
                        }
                        if (playerCommand && commandCheck(commands) && !playerCheck(playerName)) {
                            scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile));
                        }
                    } else {
                        if (globalCommand && commandCheck(command) && !playerCheck(playerName)) {
                            scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile()));
                        }
                        if (playerCommand && commandCheck(command) && !playerCheck(playerName)) {
                            scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile));
                        }
                    }
                    if (checkNotifyListCMD(command) && logNotifyCommands && !playerCheck(playerName)) {
                        scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), notifyCommandFile));
                    }
                    if (checkNotifyListCMD(command) && inGameNotifications && !playerCheck(playerName)) {
                        notifyPlayer(config.prefix + playerName + "&f: " + commandLine);
                    }
                }
            } else {
                try {
                    if (config().checkForAliases) {
                        if (globalCommand && commandCheck(commands) && !playerCheck(playerName)) {
                            scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile()));
                        }
                        if (playerCommand && commandCheck(commands) && !playerCheck(playerName)) {
                            scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile));
                        }
                    } else {
                        if (globalCommand && commandCheck(command) && !playerCheck(playerName)) {
                            scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile()));
                        }
                        if (playerCommand && commandCheck(command) && !playerCheck(playerName)) {
                            scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile));
                        }
                    }
                    if (checkNotifyListCMD(command) && logNotifyCommands && !playerCheck(playerName)) {
                        scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), notifyCommandFile));
                    }
                    if (checkNotifyListCMD(command) && inGameNotifications && !playerCheck(playerName)) {
                        notifyPlayer(config.prefix + playerName + "&f: " + commandLine);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            if (config().disableFalsePositives) {
                if (commands.stream().anyMatch(command.toLowerCase()::equals)) {
                    try {
                        if (config().checkForAliases) {
                            if (globalCommand && !commandCheck(commands) && !playerCheck(playerName)) {
                                scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile()));
                            }
                            if (playerCommand && !commandCheck(commands) && !playerCheck(playerName)) {
                                scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile));
                            }
                        } else {
                            if (globalCommand && !commandCheck(command) && !playerCheck(playerName)) {
                                scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile()));
                            }
                            if (playerCommand && !commandCheck(command) && !playerCheck(playerName)) {
                                scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile));
                            }
                        }
                        if (checkNotifyListCMD(command) && logNotifyCommands && !playerCheck(playerName)) {
                            scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), notifyCommandFile));
                        }
                        if (checkNotifyListCMD(command) && inGameNotifications && !playerCheck(playerName)) {
                            notifyPlayer(config.prefix + playerName + "&f: " + commandLine);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                if (config().checkForAliases) {
                    if (globalCommand && !commandCheck(commands) && !playerCheck(playerName)) {
                        scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile()));
                    }
                    if (playerCommand && !commandCheck(commands) && !playerCheck(playerName)) {
                        scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile));
                    }
                } else {
                    if (globalCommand && !commandCheck(command) && !playerCheck(playerName)) {
                        scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile()));
                    }
                    if (playerCommand && !commandCheck(command) && !playerCheck(playerName)) {
                        scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile));
                    }
                }
                if (checkNotifyListCMD(command) && logNotifyCommands && !playerCheck(playerName)) {
                    scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), notifyCommandFile));
                }
                if (checkNotifyListCMD(command) && inGameNotifications && !playerCheck(playerName)) {
                    notifyPlayer(config.prefix + playerName + "&f: " + commandLine);
                }
            }
        }
    }

    public void processCMDInformationConsole(String playerName, String command, String args, double x, double y, double z, String worldName, String date) {

        if (!config().consoleCommands) {
            return;
        }

        File playerFile = new File(playersFolder, playerName + ".log");
        String commandLine = "/" + command + " " + args;

        if (command.equalsIgnoreCase("sponge:callback")) {
            return;
        }

        scheduler.executor(container).submit(() -> writeFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile));

    }

    public void processInformationJoin(String playerName, double x, double y, double z, String worldName, String date) throws IOException {
        boolean globalLogin = config().globalLogin;
        boolean playerLogin = config().playerLogin;

        File playerFile = new File(playersFolder, playerName + ".log");
        String log = "logged in.";
        String[] content = formatLog(playerName, log, x, y, z, worldName, date);

        if (globalLogin && (!playerCheck(playerName))) {
            scheduler.executor(container).submit(() -> writeFile(content, getChatFile()));
        }
        if (playerLogin && (!playerCheck(playerName))) {
            scheduler.executor(container).submit(() -> writeFile(content, playerFile));
        }
    }

    public void processInformationQuit(String playerName, double x, double y, double z, String worldName, String date) throws IOException {
        boolean globalLogin = config().globalLogin;
        boolean playerLogin = config().playerLogin;

        File playerFile = new File(playersFolder, playerName + ".log");
        String log = "logged out.";
        String[] content = formatLog(playerName, log, x, y, z, worldName, date);

        if (globalLogin && (!playerCheck(playerName))) {
            scheduler.executor(container).submit(() -> writeFile(content, getChatFile()));
        }
        if (playerLogin && (!playerCheck(playerName))) {
            scheduler.executor(container).submit(() -> writeFile(content, playerFile));
        }
    }

    private boolean commandCheck(String command) {
        List<String> blacklists = config().BlackList;
        String[] commandsplit = command.split(" ");
        String commandconvert = commandsplit[0];
        for (String blacklist1 : blacklists) {
            if (commandconvert.matches(blacklist1)) {
                return true;
            }
        }
        return false;
    }

    private boolean commandCheck(Set<String> commands) {
        List<String> blacklists = config().BlackList;
        return !Collections.disjoint(commands, blacklists);
    }

    private boolean playerCheck(String blacklist) {
        List<String> blacklists = config().playerBlacklist;
        String[] blacklistsplit = blacklist.split(" ");
        String blacklistconvert = blacklistsplit[0];
        for (String blacklist1 : blacklists) {
            if (blacklistconvert.matches(blacklist1)) {
                return true;
            }
        }
        return false;
    }

    private String[] formatLog(String playerName, String command, double x, double y, double z, String worldName, String date) {
        String log = config().logFormat;
        if (log.contains("%date")) {
            log = log.replaceAll("%date", date);
        }
        if (log.contains("%world")) {
            log = log.replaceAll("%world", worldName);
        }
        if (log.contains("%x")) {
            log = log.replaceAll("%x", Double.toString(x));
        }
        if (log.contains("%y")) {
            log = log.replaceAll("%y", Double.toString(y));
        }
        if (log.contains("%z")) {
            log = log.replaceAll("%z", Double.toString(z));
        }
        if (log.contains("%name")) {
            log = log.replaceAll("%name", playerName);
        }
        if (log.contains("%content")) {
            log = log.replaceAll("%content", java.util.regex.Matcher.quoteReplacement(command));
        }

        return new String[]{log};
    }

    public Config config() {
        return config;
    }


    String getDate() {
        DateFormat dateFormat = new SimpleDateFormat("MMM/dd/yyyy hh:mm:ss a");
        Date date = new Date();
        return dateFormat.format(date);
    }

    private static String getFolderDate() {
        DateFormat dateFormat = new SimpleDateFormat("MMMMMMMMM");
        Date date = new Date();
        return dateFormat.format(date);
    }

    private static String getFileDate()
    {
        DateFormat dateFormat = new SimpleDateFormat("MMM-dd-yyyy");
        Date date = new Date();
        return dateFormat.format(date);
    }

    /*private void checkDate() {
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    }*/


    private File getChatFile() {
        return new File(logFolder, getFileDate() + "-chat.log");
    }

    private File getCmdFile() {
        return new File(clogFolder, getFileDate() + "-cmd.log");
    }

    private boolean checkNotifyListPlayer(String message) {
        List<String> messageList = config().chatNotifyList;
        for (String aMessageList : messageList) {
            if (message.toLowerCase().contains(aMessageList)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkNotifyListCMD(String command) {
        List<String> commands = config().commandNotifyList;
        String[] commandsplit = command.split(" ");
        String commandconvert = commandsplit[0];
        for (String command1 : commands) {
            if (commandconvert.matches(command1)) {
                return true;
            }
        }
        return false;
    }

    private void notifyPlayer(String string) {
        for (ServerPlayer player : Sponge.server().onlinePlayers()) {
            if (player.hasPermission("mmclogger.notify")) {
                Util.sendMessage(player, string);
            }
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public Component fromLegacy(String legacy) {
        return TextUtils.deserializeLegacy(legacy);
    }
    
    public void writeFile(String[] index, File t) {
        BufferedWriter buffwriter;
        FileWriter filewriter;
        try {
            filewriter = new FileWriter(t, true);
            buffwriter = new BufferedWriter(filewriter);

            for (String s : index) {
                buffwriter.write(s);
                buffwriter.newLine();
            }

            buffwriter.flush();
        }
        catch (IOException i) {
            throw new RuntimeException(i);
        }
    }

}
