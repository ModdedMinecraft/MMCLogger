package net.moddedminecraft.mmclogger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class ViewLogCommand implements CommandExecutor {

    private final Main plugin;

    public ViewLogCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        Parameter.Value<UUID> userParameter = Parameter.user().key("player").build();

        Audience audience = context.cause().audience();

        final Optional<UUID> uuid = context.one(userParameter);
        User user = null;
        if (uuid.isPresent()) {
            try {
                if (Sponge.server().userManager().load(uuid.get()).get().isPresent()) {
                    user = Sponge.server().userManager().load(uuid.get()).get().get();
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        PaginationService paginationService = Sponge.serviceProvider().provide(PaginationService.class).get();
        List<Component> contents = new ArrayList<>();
        String folderDir = plugin.rootFolder.getName();
        File[] files = plugin.rootFolder.listFiles();

        if (user != null) {
            getHaste(new File(plugin.playersFolder + "/" + user.name() + ".log"), context.cause());
            return CommandResult.success();
        } else {

            for (File file : files) {
                TextComponent.Builder send = Component.text();
                if (file.isDirectory()) {
                    send.append(plugin.fromLegacy("&8[&e" + file.getName() + "&8]"));
                    send.clickEvent(SpongeComponents.executeCallback(viewDir(file)));//TextActions.executeCallback(viewDir(file)));
                    send.hoverEvent(HoverEvent.showText(plugin.fromLegacy("&eClick to view folder")));
                } else {
                    send.append(plugin.fromLegacy("&e" + file.getName()));
                    send.clickEvent(SpongeComponents.executeCallback(viewFile(file)));
                    send.hoverEvent(HoverEvent.showText(plugin.fromLegacy("&eClick to view this log in hastebin")));
                }
                contents.add(send.build());
            }

            paginationService.builder()
                    .title(plugin.fromLegacy("&8[&7" + folderDir + "&8]"))
                    .contents(contents)
                    .padding(plugin.fromLegacy("&8="))
                    .sendTo(audience);
            return CommandResult.success();
        }
    }


    private Consumer<CommandCause> viewDir(File folder) {
        return consumer -> {
            PaginationService paginationService = Sponge.serviceProvider().provide(PaginationService.class).get();
            List<Component> contents = new ArrayList<>();
            Sponge.asyncScheduler().executor(plugin.container).submit(() -> {
                for (File file : folder.listFiles()) {
                    TextComponent.Builder send = Component.text();
                    if (file.isDirectory()) {
                        send.append(plugin.fromLegacy("&8[&e" + file.getName() + "&8]"));
                        send.clickEvent(SpongeComponents.executeCallback(viewDir(file)));
                        send.hoverEvent(HoverEvent.showText(plugin.fromLegacy("&eClick to view folder")));
                    } else {
                        send.append(plugin.fromLegacy("&e" + file.getName()));
                        send.clickEvent(SpongeComponents.executeCallback(viewFile(file)));
                        send.hoverEvent(HoverEvent.showText(plugin.fromLegacy("&eClick to get a hastebin link for this file")));
                    }
                    contents.add(send.build());
                }

                paginationService.builder()
                        .title(plugin.fromLegacy("&8[&7" + folder.getName() + "&8]"))
                        .contents(contents)
                        .padding(plugin.fromLegacy("&8="))
                        .sendTo(consumer.audience());
            });
        };
    }

    private Consumer<CommandCause> viewFile(File file) {
        return consumer -> {
            getHaste(file, consumer);
        };
    }

    private void getHaste(File file, CommandCause cause) {
        Sponge.asyncScheduler().executor(plugin.container).submit(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL("https://hastebin.com/documents").openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer 9ef20a73735144f9017859d9354af73a7d97c3ebbbfb1d72b9b4bda1b2a21d9278842404f7e1a16908b116a7fa2503777e91c5d3a317a7af45cd03133e77850d");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                try(DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    List<String> fileList = Files.readAllLines(file.toPath(), getCharSet());
                    StringBuilder sb = new StringBuilder();
                    int numLines = fileList.size();
                    if (numLines > 6000) {
                        numLines = 6000;
                    }
                    List<String> lastLines = fileList.subList(fileList.size() - numLines, fileList.size());
                    for (String line : lastLines) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    wr.write(sb.toString().getBytes(getCharSet()));
                    wr.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                StringBuilder response = new StringBuilder();
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String inputLine;
                    while ((inputLine = rd.readLine()) != null) response.append(inputLine);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                JsonElement json = new JsonParser().parse(response.toString());
                if (!json.isJsonObject()) {
                    throw new IOException("Can't parse JSON");
                }

                TextComponent.Builder send = Component.text();
                String url = "http://hastebin.com/share/" + json.getAsJsonObject().get("key").getAsString();
                send.append(plugin.fromLegacy("&3" + url));
                send.clickEvent(ClickEvent.openUrl(new URL(url)));
                send.hoverEvent(HoverEvent.showText(plugin.fromLegacy("&eClick here to go to: &6" + url)));
                cause.audience().sendMessage(send.build());
            } catch (IOException e) {
                e.printStackTrace();
                cause.audience().sendMessage(plugin.fromLegacy("&cThere was an error getting your hastebin"));
                throw new RuntimeException(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private Charset getCharSet() {
        switch(plugin.config().vclCharset) {
            case "ISO-8859-1":
                return StandardCharsets.ISO_8859_1;
            case "UTF-8":
                return StandardCharsets.UTF_8;
            default:
                return StandardCharsets.UTF_8;
        }
    }
}
