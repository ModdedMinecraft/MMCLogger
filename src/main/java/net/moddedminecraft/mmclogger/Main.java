package net.moddedminecraft.mmclogger;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

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
    public Logger logger;
    private CommandManager cmdManager = Sponge.getCommandManager();

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private File defaultConf;

    private HoconConfigurationLoader loader;
    private ConfigurationNode rootNode;

    public File chatlogFolder = new File(configDir, "chatlogs/logs");
    public File commandlogFolder = new File(configDir, "chatlogs/commandlogs");
    public File playersFolder = new File(configDir, "chatlogs/players");

    public File logFolder = new File(chatlogFolder, getFolderDate());
    public File clogFolder = new File(commandlogFolder, getFolderDate());

    public File notifyChatFile = new File(configDir, "chatlogs/notifyChat.log");
    public File notifyCommandFile = new File(configDir, "chatlogs/notifyCommands.log");

    Scheduler scheduler = Sponge.getScheduler();
    Task.Builder taskBuilder = scheduler.createTaskBuilder();

    @Listener
    public void onInitialization(GameInitializationEvent e) throws ObjectMappingException, IOException {
        loader = HoconConfigurationLoader.builder().setFile(defaultConf).build();

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

    @Listener
    public void onPlayerChat(MessageChannelEvent.Chat event, @Root Player p) throws IOException
    {
        Player player = p;
        String name = player.getName();
        String message = TextSerializers.FORMATTING_CODE.serialize(event.getFormatter().getBody().toText());
        Location<World> location = player.getLocation();
        int xLocation = (int) location.getX();
        int yLocation = (int) location.getY();
        int zLocation = (int) location.getZ();
        String world = location.getExtent().getName();
        String date = getDate();
        checkPlayer(name);

        processInformation(player, name, message, xLocation, yLocation, zLocation, world, date);
    }

    public void processInformation(Player player, String playerName, String chat, int x, int y, int z, String worldName, String date) throws IOException {
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
            if ((checkNotifyList(message)) && (logNotifyChat)) {
                Task task = taskBuilder.execute(new WriteFile(formatLog(playerName, message, x, y, z, worldName, date), notifyChatFile)).async().name("Notify Chat Log").submit(this);
            }
            if ((checkNotifyList(message)) && (inGameNotifications)) {
                notifyPlayer(TextColors.BLUE + "[" + TextColors.GOLD + "MMCLogger" + TextColors.BLUE + "] " + TextColors.GOLD + playerName + ": " + TextColors.WHITE + message);
            }
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }
    }

    public void checkPlayer(String name) throws IOException
    {
        File file = new File(playersFolder, name + ".log");
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    public String[] formatLog(String playerName, String command, int x, int y, int z, String worldName, String date) throws IOException {
        String format = rootNode.getNode("Log", "LogFormat").getString();
        String log = format;
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

        return new String[]{ log };
    }

    public String getDate() {
        DateFormat dateFormat = new SimpleDateFormat("MMM/dd/yyyy hh:mm:ss a");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getFolderDate() {
        DateFormat dateFormat = new SimpleDateFormat("MMMMMMMMM");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getFileDate()
    {
        DateFormat dateFormat = new SimpleDateFormat("MMM-dd-yyyy");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public File getChatFile() {
        File chatFile = new File(logFolder, getFileDate() + "-chat.log");
        return chatFile;
    }

    public File getCmdFile() {
        File commandFile = new File(clogFolder, getFileDate() + "-cmd.log");
        return commandFile;
    }

    public boolean checkNotifyList(String message) throws IOException, ObjectMappingException {
        List messageList = rootNode.getNode("Log", "Notifications", "Chat").getList(TypeToken.of(String.class));
        for (int i = 0; i < messageList.size(); i++) {
            if (message.toLowerCase().contains((CharSequence)messageList.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void checkDate() {
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

    public void configCheck()
    {
        String[] blacklist = {
                "/help",
                "/who",
                "/home" };
        String[] commandNotifyList = {
                "/pl",
                "/item",
                "/give",
                "/plugins",
                "/version",
                "/ver",
                "/op" };
        String[] chatNotifyList = {
                "ddos",
                "hack",
                "flymod",
                "dupe",
                "duplicate",
                "duplication",
                "homo",
                "faggot" };

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
                rootNode.getNode("Log", "Commands", "Blacklist").setValue(Arrays.asList(blacklist));
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
