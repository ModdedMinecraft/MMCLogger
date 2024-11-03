package net.moddedminecraft.mmclogger;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Plugin(id = "mmclogger", name = "MMCLogger", description = "Chat and command logger", version = "1.7.4", authors = {"Leelawd93"})
public class Main {

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    public Path defaultConf;

    @Inject
    @DefaultConfig(sharedRoot = false)
    public File defaultConfFile;

    private Config config;

    File rootFolder= new File("chatlogs/");;
    File chatlogFolder = new File("chatlogs/logs");
    File commandlogFolder = new File(configDir, "chatlogs/commandlogs");
    File playersFolder = new File(configDir, "chatlogs/players");

    File logFolder = new File(chatlogFolder, getFolderDate());
    File clogFolder = new File(commandlogFolder, getFolderDate());

    private File notifyChatFile = new File(configDir, "chatlogs/notifyChat.log");
    private File notifyCommandFile = new File(configDir, "chatlogs/notifyCommands.log");

    private Scheduler scheduler = Sponge.getScheduler();

    private CommandManager cmdManager = Sponge.getCommandManager();

    @Listener
    public void onInitialization(GameInitializationEvent e) throws IOException, ObjectMappingException {
        Sponge.getEventManager().registerListeners(this, new EventListener(this));
        this.config = new Config(this);

        CommandSpec viewLog = CommandSpec.builder()
                .description(Text.of("View chat logs"))
                .executor(new ViewLogCommand(this))
                .arguments(GenericArguments.optional(GenericArguments.user(Text.of("player"))))
                .permission("mmclogger.viewlogs")
                .build();
        cmdManager.register(this, viewLog, "viewchatlogs", "vcl");
        //scheduler.createTaskBuilder().execute(this::checkDate).interval(1, TimeUnit.SECONDS).name("mmclogger-S-DateChecker").submit(this);
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) throws IOException {
        logger.info("MMCLogger Loaded");
    }

    @Listener
    public void onPluginReload(GameReloadEvent event) throws IOException, ObjectMappingException {
        this.config = new Config(this);
        logger.info("MMCLogger Config Reloaded");
    }

    void processInformation(String playerName, String chat, int x, int y, int z, String worldName, String date) {
        boolean globalChat = config().globalChat;
        boolean playerChat = config().playerChat;
        boolean logNotifyChat = config().logNotifyChat;
        boolean inGameNotifications = config().inGameNotifications;

        File playerFile = new File(playersFolder, playerName + ".log");
        String message = chat.replaceAll("(&([a-f0-9]))", "");

        try {
            if (globalChat && (!playerCheck(playerName))) {
                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, message, x, y, z, worldName, date), getChatFile())).async().name("mmclogger-A-GlobalChatLog").submit(this);
            }
            if (playerChat && (!playerCheck(playerName))) {
                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, message, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerChatLog").submit(this);
            }
            if ((checkNotifyListPlayer(message)) && (logNotifyChat) && (!playerCheck(playerName))) {
                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, message, x, y, z, worldName, date), notifyChatFile)).async().name("mmclogger-A-NotifyChatLog").submit(this);
            }
            if ((checkNotifyListPlayer(message)) && (inGameNotifications) && (!playerCheck(playerName))) {
                notifyPlayer(config.prefix + playerName + "&f: " + message);
            }
        } catch (ObjectMappingException | IOException e) {
            e.printStackTrace();
        }
    }

    void processCMDInformation(Player player, String playerName, String command, String args, int x, int y, int z, String worldName, String date) {

        boolean playerCommand = config().playerCommands;
        boolean globalCommand = config().globalCommands;
        boolean logNotifyCommands = config().logNotifyCommands;
        boolean inGameNotifications = config().inGameNotifications;

        File playerFile = new File(playersFolder, playerName + ".log");
        String commandLine = "/" + command + " " + args;

        Optional<? extends CommandMapping> optionalCommandMapping = Sponge.getCommandManager().get(command, player);
        Set<String> commands = optionalCommandMapping.map(commandMapping -> commandMapping.getAllAliases().stream().map(String::toLowerCase).collect(Collectors.toSet())).orElseGet(() -> Sets.newHashSet("....."));

        if (command.equalsIgnoreCase("sponge:callback")) {
            return;
        }

        if (config().isWhitelist) {
            if (config().disableFalsePositives) {
                if (commands.stream().anyMatch(command.toLowerCase()::equals)) {
                    try {
                        if (config().checkForAliases) {
                            if (globalCommand && commandCheck(commands) && !playerCheck(playerName)) {
                                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile())).async().name("mmclogger-A-GlobalCommandLog").submit(this);
                            }
                            if (playerCommand && commandCheck(commands) && !playerCheck(playerName)) {
                                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerCommandLog").submit(this);
                            }
                        } else {
                            if (globalCommand && commandCheck(command) && !playerCheck(playerName)) {
                                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile())).async().name("mmclogger-A-GlobalCommandLog").submit(this);
                            }
                            if (playerCommand && commandCheck(command) && !playerCheck(playerName)) {
                                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerCommandLog").submit(this);
                            }
                        }
                        if (checkNotifyListCMD(command) && logNotifyCommands && !playerCheck(playerName)) {
                            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), notifyCommandFile)).async().name("mmclogger-A-NotifyCommandLog").submit(this);
                        }
                        if (checkNotifyListCMD(command) && inGameNotifications && !playerCheck(playerName)) {
                            notifyPlayer(config.prefix + playerName + "&f: " + commandLine);
                        }
                    } catch (ObjectMappingException | IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    if (config().checkForAliases) {
                        if (globalCommand && commandCheck(commands) && !playerCheck(playerName)) {
                            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile())).async().name("mmclogger-A-GlobalCommandLog").submit(this);
                        }
                        if (playerCommand && commandCheck(commands) && !playerCheck(playerName)) {
                            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerCommandLog").submit(this);
                        }
                    } else {
                        if (globalCommand && commandCheck(command) && !playerCheck(playerName)) {
                            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile())).async().name("mmclogger-A-GlobalCommandLog").submit(this);
                        }
                        if (playerCommand && commandCheck(command) && !playerCheck(playerName)) {
                            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerCommandLog").submit(this);
                        }
                    }
                    if (checkNotifyListCMD(command) && logNotifyCommands && !playerCheck(playerName)) {
                        scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), notifyCommandFile)).async().name("mmclogger-A-NotifyCommandLog").submit(this);
                    }
                    if (checkNotifyListCMD(command) && inGameNotifications && !playerCheck(playerName)) {
                        notifyPlayer(config.prefix + playerName + "&f: " + commandLine);
                    }
                } catch (ObjectMappingException | IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (config().disableFalsePositives) {
                if (commands.stream().anyMatch(command.toLowerCase()::equals)) {
                    try {
                        if (config().checkForAliases) {
                            if (globalCommand && !commandCheck(commands) && !playerCheck(playerName)) {
                                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile())).async().name("mmclogger-A-GlobalCommandLog").submit(this);
                            }
                            if (playerCommand && !commandCheck(commands) && !playerCheck(playerName)) {
                                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerCommandLog").submit(this);
                            }
                        } else {
                            if (globalCommand && !commandCheck(command) && !playerCheck(playerName)) {
                                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile())).async().name("mmclogger-A-GlobalCommandLog").submit(this);
                            }
                            if (playerCommand && !commandCheck(command) && !playerCheck(playerName)) {
                                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerCommandLog").submit(this);
                            }
                        }
                        if (checkNotifyListCMD(command) && logNotifyCommands && !playerCheck(playerName)) {
                            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), notifyCommandFile)).async().name("mmclogger-A-NotifyCommandLog").submit(this);
                        }
                        if (checkNotifyListCMD(command) && inGameNotifications && !playerCheck(playerName)) {
                            notifyPlayer(config.prefix + playerName + "&f: " + commandLine);
                        }
                    } catch (ObjectMappingException | IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    if (config().checkForAliases) {
                        if (globalCommand && !commandCheck(commands) && !playerCheck(playerName)) {
                            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile())).async().name("mmclogger-A-GlobalCommandLog").submit(this);
                        }
                        if (playerCommand && !commandCheck(commands) && !playerCheck(playerName)) {
                            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerCommandLog").submit(this);
                        }
                    } else {
                        if (globalCommand && !commandCheck(command) && !playerCheck(playerName)) {
                            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile())).async().name("mmclogger-A-GlobalCommandLog").submit(this);
                        }
                        if (playerCommand && !commandCheck(command) && !playerCheck(playerName)) {
                            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerCommandLog").submit(this);
                        }
                    }
                    if (checkNotifyListCMD(command) && logNotifyCommands && !playerCheck(playerName)) {
                        scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), notifyCommandFile)).async().name("mmclogger-A-NotifyCommandLog").submit(this);
                    }
                    if (checkNotifyListCMD(command) && inGameNotifications && !playerCheck(playerName)) {
                        notifyPlayer(config.prefix + playerName + "&f: " + commandLine);
                    }
                } catch (ObjectMappingException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void processCMDInformationConsole(String playerName, String command, String args, int x, int y, int z, String worldName, String date) {

        if (!config().consoleCommands) {
            return;
        }

        File playerFile = new File(playersFolder, playerName + ".log");
        String commandLine = "/" + command + " " + args;

        if (command.equalsIgnoreCase("sponge:callback")) {
            return;
        }

        try {
            scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-ConsoleCmdLog").submit(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void processInformationJoin(String playerName, int x, int y, int z, String worldName, String date) throws IOException, ObjectMappingException {
        boolean globalLogin = config().globalLogin;
        boolean playerLogin = config().playerLogin;

        File playerFile = new File(playersFolder, playerName + ".log");
        String log = "logged in.";
        String[] content = formatLog(playerName, log, x, y, z, worldName, date);

        if (globalLogin && (!playerCheck(playerName))) {
            scheduler.createTaskBuilder().execute(new WriteFile(content, getChatFile())).async().name("mmclogger-A-globalLogin").submit(this);
        }
        if (playerLogin && (!playerCheck(playerName))) {
            scheduler.createTaskBuilder().execute(new WriteFile(content, playerFile)).async().name("mmclogger-A-playerLogin").submit(this);
        }
    }

    public void processInformationQuit(String playerName, int x, int y, int z, String worldName, String date) throws IOException, ObjectMappingException {
        boolean globalLogin = config().globalLogin;
        boolean playerLogin = config().playerLogin;

        File playerFile = new File(playersFolder, playerName + ".log");
        String log = "logged out.";
        String[] content = formatLog(playerName, log, x, y, z, worldName, date);

        if (globalLogin && (!playerCheck(playerName))) {
            scheduler.createTaskBuilder().execute(new WriteFile(content, getChatFile())).async().name("mmclogger-A-globalLogin").submit(this);
        }
        if (playerLogin && (!playerCheck(playerName))) {
            scheduler.createTaskBuilder().execute(new WriteFile(content, playerFile)).async().name("mmclogger-A-playerLogin").submit(this);
        }
    }

    private boolean commandCheck(String command) throws ObjectMappingException {
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

    private boolean commandCheck(Set<String> commands) throws ObjectMappingException {
        List<String> blacklists = config().BlackList;
        return !Collections.disjoint(commands, blacklists);
    }

    private boolean playerCheck(String blacklist) throws ObjectMappingException {
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

    private String[] formatLog(String playerName, String command, int x, int y, int z, String worldName, String date) throws IOException {
        String log = config().logFormat;
        if (log.contains("%date")) {
            log = log.replaceAll("%date", date);
        }
        if (log.contains("%world")) {
            log = log.replaceAll("%world", worldName);
        }
        if (log.contains("%x")) {
            log = log.replaceAll("%x", Integer.toString(x));
        }
        if (log.contains("%y")) {
            log = log.replaceAll("%y", Integer.toString(y));
        }
        if (log.contains("%z")) {
            log = log.replaceAll("%z", Integer.toString(z));
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

    private boolean checkNotifyListPlayer(String message) throws ObjectMappingException  {
        List<String> messageList = config().chatNotifyList;
        for (String aMessageList : messageList) {
            if (message.toLowerCase().contains(aMessageList)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkNotifyListCMD(String command) throws ObjectMappingException {
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
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            if (player.hasPermission("mmclogger.notify")) {
                Util.sendMessage(player, string);
            }
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public Text fromLegacy(String legacy) {
        return TextSerializers.FORMATTING_CODE.deserializeUnchecked(legacy);
    }

}
