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
import org.spongepowered.api.scheduler.Task;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Plugin(id = "mmclogger", name = "MMCLogger", version = "1.0")
public class Main {

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private File defaultConf;

    private HoconConfigurationLoader loader;
    public ConfigurationNode rootNode;

    private File chatlogFolder = new File(configDir, "chatlogs/logs");
    private File commandlogFolder = new File(configDir, "chatlogs/commandlogs");
    public File playersFolder = new File(configDir, "chatlogs/players");

    private File logFolder = new File(chatlogFolder, getFolderDate());
    private File clogFolder = new File(commandlogFolder, getFolderDate());

    public File notifyChatFile = new File(configDir, "chatlogs/notifyChat.log");
    public File notifyCommandFile = new File(configDir, "chatlogs/notifyCommands.log");

    private Scheduler scheduler = Sponge.getScheduler();
    public Task.Builder taskBuilder = scheduler.createTaskBuilder();

    @Listener
    public void onInitialization(GameInitializationEvent e) throws ObjectMappingException, IOException {
        loader = HoconConfigurationLoader.builder().setFile(defaultConf).build();

        Sponge.getEventManager().registerListeners(this, new EventListener(this));

        rootNode = loader.load();

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

        configCheck();


        taskBuilder.execute(new Runnable() {
            public void run() {
                checkDate();
            }
        }).interval(1, TimeUnit.SECONDS).name("check date").submit(this);
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) throws IOException {
        logger.info("MMCLogger Loaded");
    }

    void processInformation(Player player, String playerName, String chat, int x, int y, int z, String worldName, String date) {
        boolean globalChat = rootNode.getNode("Log", "Toggle", "GlobalChat").getBoolean();
        boolean playerChat = rootNode.getNode("Log", "Toggle", "PlayerChat").getBoolean();
        boolean logNotifyChat = rootNode.getNode("Log", "Toggle", "LogNotifyChat").getBoolean();
        boolean inGameNotifications = rootNode.getNode("Log", "Toggle", "InGameNotifications").getBoolean();

        File playerFile = new File(playersFolder, playerName + ".log");
        String message = chat.replaceAll("(&([a-f0-9]))", "");

        try {
            if (globalChat) {
                Task task = taskBuilder.execute(new WriteFile(formatLog(playerName, message, x, y, z, worldName, date), getChatFile())).async().name("Global Chat Log").submit(this);
            }
            if (playerChat) {
                Task task = taskBuilder.execute(new WriteFile(formatLog(playerName, message, x, y, z, worldName, date), playerFile)).async().name("Player Chat Log").submit(this);
            }
            if ((checkNotifyListPlayer(message)) && (logNotifyChat)) {
                Task task = taskBuilder.execute(new WriteFile(formatLog(playerName, message, x, y, z, worldName, date), notifyChatFile)).async().name("Notify Chat Log").submit(this);
            }
            if ((checkNotifyListPlayer(message)) && (inGameNotifications)) {
                notifyPlayer("&9[&6MMCLogger&9] &6" + playerName + "&f: " + message);
            }
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void processCMDInformation(Player player, String playerName, String command, int x, int y, int z, String worldName, String date) {

        boolean playerCommand = rootNode.getNode("Log", "Toggle", "PlayerCommands").getBoolean();
        boolean globalCommand = rootNode.getNode("Log", "Toggle", "GlobalCommands").getBoolean();
        boolean logNotifyCommands = rootNode.getNode("Log", "Toggle", "LogNotifyCommands").getBoolean();
        boolean inGameNotifications = rootNode.getNode("Log", "Toggle", "InGameNotifications").getBoolean();

        File playerFile = new File(playersFolder, playerName + ".log");

        try {
            if ((globalCommand) && (!commandCheck(command))) {
                Task task = taskBuilder.execute(new WriteFile(formatLog(playerName, command, x, y, z, worldName, date), getCmdFile())).async().name("Global Command Log").submit(this);
            }
            if ((playerCommand) && (!commandCheck(command))) {
                Task task = taskBuilder.execute(new WriteFile(formatLog(playerName, command, x, y, z, worldName, date), playerFile)).async().name("Player Command Log").submit(this);
            }
            if ((checkNotifyListCMD(command)) && (logNotifyCommands)) {
                Task task = taskBuilder.execute(new WriteFile(formatLog(playerName, command, x, y, z, worldName, date), notifyCommandFile)).async().name("Notify Command Log").submit(this);
            }
            if ((checkNotifyListCMD(command)) && (inGameNotifications)) {
                notifyPlayer("&9[&6MMCLogger&9] &6" + playerName + "&f: " + command);
            }
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean commandCheck(String command) throws ObjectMappingException {
        List commands = rootNode.getNode("Log", "CommandLog", "Blacklist").getList(TypeToken.of(String.class));
        String[] commandsplit = command.split(" ");
        String commandconvert = commandsplit[0];
        for (int i = 0; i < commands.size(); i++) {
            if (commandconvert.matches((String)commands.get(i))) {
                return true;
            }
        }
        return false;
    }

    private String[] formatLog(String playerName, String command, int x, int y, int z, String worldName, String date) throws IOException {
        String log = rootNode.getNode("Log", "LogFormat").getString();
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

    public void checkPlayer(String name) throws IOException
    {
        File file = new File(playersFolder, name + ".log");
        if (!file.exists()) {
            file.createNewFile();
        }
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
        List messageList = rootNode.getNode("Log", "Notifications", "Chat").getList(TypeToken.of(String.class));
        for (int i = 0; i < messageList.size(); i++) {
            if (message.toLowerCase().contains((CharSequence)messageList.get(i))) {
                return true;
            }
        }
        return false;
    }

    public boolean checkNotifyListCMD(String command) throws ObjectMappingException {
        List commands = rootNode.getNode("Log", "Notifications", "Commands").getList(TypeToken.of(String.class));
        String[] commandsplit = command.split(" ");
        String commandconvert = commandsplit[0];
        for (int i = 0; i < commands.size(); i++) {
            if (commandconvert.matches((String)commands.get(i))) {
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

    private void configCheck()
    {
        String[] blacklist = {
                "help",
                "who",
                "home" };
        String[] commandNotifyList = {
                "item",
                "give",
                "sponge",
                "op" };
        String[] chatNotifyList = {
                "ddos",
                "hack",
                "flymod",
                "dupe",
                "duplicate",
                "duplication" };

        try {
            if (!defaultConf.exists()) {
                defaultConf.createNewFile();
                rootNode.getNode("Log", "Toggle", "GlobalCommands").setValue(true);
                rootNode.getNode("Log", "Toggle", "GlobalChat").setValue(true);
                rootNode.getNode("Log", "Toggle", "PlayerCommands").setValue(true);
                rootNode.getNode("Log", "Toggle", "PlayerChat").setValue(true);
                rootNode.getNode("Log", "Toggle", "LogNotifyChat").setValue(true);
                rootNode.getNode("Log", "Toggle", "InGameNotifications").setValue(true);
                rootNode.getNode("Log", "Toggle", "LogNotifyCommands").setValue(true);
                rootNode.getNode("Log", "Toggle", "PlayerLogin").setValue(true);
                rootNode.getNode("Log", "Toggle", "GlobalLogin").setValue(true);
                rootNode.getNode("Log", "CommandLog", "Blacklist").setValue(Arrays.asList(blacklist));
                rootNode.getNode("Log", "LogFormat").setValue("[%date] %name: %content");
                rootNode.getNode("Log", "Notifications", "Chat").setValue(Arrays.asList(chatNotifyList));
                rootNode.getNode("Log", "Notifications", "Commands").setValue(Arrays.asList(commandNotifyList));
            }
            loader.save(rootNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
