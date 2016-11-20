package net.moddedminecraft.mmclogger;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Scheduler;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Plugin(id = "mmclogger", name = "MMCLogger", version = "1.0", authors = {"Leelawd93"})
public class Main {

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    public File defaultConf;

    public HoconConfigurationLoader loader;
    public ConfigurationNode rootNode;
    private Config config;

    public String prefix = "&9[&6MMCLogger&9] &6";

    private File chatlogFolder = new File(configDir, "chatlogs/logs");
    private File commandlogFolder = new File(configDir, "chatlogs/commandlogs");
    public File playersFolder = new File(configDir, "chatlogs/players");

    private File logFolder = new File(chatlogFolder, getFolderDate());
    private File clogFolder = new File(commandlogFolder, getFolderDate());

    public File notifyChatFile = new File(configDir, "chatlogs/notifyChat.log");
    public File notifyCommandFile = new File(configDir, "chatlogs/notifyCommands.log");

    private Scheduler scheduler = Sponge.getScheduler();

    @Listener
    public void onInitialization(GameInitializationEvent e){
        loader = HoconConfigurationLoader.builder().setFile(defaultConf).build();

        Sponge.getEventManager().registerListeners(this, new EventListener(this));

        try {
            rootNode = loader.load();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        if (!chatlogFolder.isDirectory()) {
            chatlogFolder.mkdirs();
        }
        if (!commandlogFolder.isDirectory()) {
            commandlogFolder.mkdirs();
        }
        if (!playersFolder.isDirectory()) {
            playersFolder.mkdirs();
        }
        if (!clogFolder.isDirectory()) {
            clogFolder.mkdirs();
        }
        if (!logFolder.isDirectory()) {
            logFolder.mkdirs();
        }

        this.config = new Config(defaultConf, this);


        scheduler.createTaskBuilder().execute(this::checkDate).interval(1, TimeUnit.SECONDS).name("mmclogger-S-DateChecker").submit(this);
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) throws IOException {
        logger.info("MMCLogger Loaded");
    }

    void processInformation(Player player, String playerName, String chat, int x, int y, int z, String worldName, String date) {
        boolean globalChat = rootNode.getNode("log", "toggle", "global-chat").getBoolean();
        boolean playerChat = rootNode.getNode("log", "toggle", "player-chat").getBoolean();
        boolean logNotifyChat = rootNode.getNode("log", "toggle", "log-notify-chat").getBoolean();
        boolean inGameNotifications = rootNode.getNode("log", "toggle", "in-game-notifications").getBoolean();

        File playerFile = new File(playersFolder, playerName + ".log");
        String message = chat.replaceAll("(&([a-f0-9]))", "");

        try {
            if (globalChat) {
                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, message, x, y, z, worldName, date), getChatFile())).async().name("mmclogger-A-GlobalChatLog").submit(this);
            }
            if (playerChat) {
                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, message, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerChatLog").submit(this);
            }
            if ((checkNotifyListPlayer(message)) && (logNotifyChat)) {
                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, message, x, y, z, worldName, date), notifyChatFile)).async().name("mmclogger-A-NotifyChatLog").submit(this);
            }
            if ((checkNotifyListPlayer(message)) && (inGameNotifications)) {
                notifyPlayer(prefix + playerName + "&f: " + message);
            }
        } catch (ObjectMappingException | IOException e) {
            e.printStackTrace();
        }
    }

    void processCMDInformation(Player player, String playerName, String command, String args, int x, int y, int z, String worldName, String date) {

        boolean playerCommand = rootNode.getNode("log", "toggle", "player-commands").getBoolean();
        boolean globalCommand = rootNode.getNode("log", "toggle", "global-commands").getBoolean();
        boolean logNotifyCommands = rootNode.getNode("log", "toggle", "log-notify-commands").getBoolean();
        boolean inGameNotifications = rootNode.getNode("log", "toggle", "in-game-notifications").getBoolean();

        File playerFile = new File(playersFolder, playerName + ".log");
        String commandLine = "/" +command + " " + args;

        try {
            if ((globalCommand) && (!commandCheck(command))) {
                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), getCmdFile())).async().name("mmclogger-A-GlobalCommandLog").submit(this);
            }
            if ((playerCommand) && (!commandCheck(command))) {
               scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), playerFile)).async().name("mmclogger-A-PlayerCommandLog").submit(this);
            }
            if ((checkNotifyListCMD(command)) && (logNotifyCommands)) {
                scheduler.createTaskBuilder().execute(new WriteFile(formatLog(playerName, commandLine, x, y, z, worldName, date), notifyCommandFile)).async().name("mmclogger-A-NotifyCommandLog").submit(this);
            }
            if ((checkNotifyListCMD(command)) && (inGameNotifications)) {
                notifyPlayer(prefix + playerName + "&f: " + commandLine);
            }
        } catch (ObjectMappingException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean commandCheck(String blacklist) throws ObjectMappingException {
        List<String> blacklists = rootNode.getNode("log", "command-log", "blacklist").getList(TypeToken.of(String.class));
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
        String log = rootNode.getNode("log", "log-format").getString();
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


    public String getDate() {
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

    public File getChatFile() {
        return new File(logFolder, getFileDate() + "-chat.log");
    }

    public File getCmdFile() {
        return new File(clogFolder, getFileDate() + "-cmd.log");
    }

    public boolean checkNotifyListPlayer(String message) throws ObjectMappingException  {
        List<String> messageList = rootNode.getNode("log", "notifications", "chat").getList(TypeToken.of(String.class));
        for (String aMessageList : messageList) {
            if (message.toLowerCase().contains(aMessageList)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkNotifyListCMD(String command) throws ObjectMappingException {
        List<String> commands = rootNode.getNode("log", "notifications", "commands").getList(TypeToken.of(String.class));
        String[] commandsplit = command.split(" ");
        String commandconvert = commandsplit[0];
        for (String command1 : commands) {
            if (commandconvert.matches(command1)) {
                return true;
            }
        }
        return false;
    }

    private void checkDate() {
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    }

    public void notifyPlayer(String string) {
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            if (player.hasPermission("mmclogger.notify")) {
                Util.sendMessage(player, string);
            }
        }
    }
}
