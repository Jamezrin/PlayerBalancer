package me.jaimemartz.lobbybalancer.commands;

import me.jaimemartz.faucet.Messager;
import me.jaimemartz.faucet.Replacement;
import me.jaimemartz.faucet.StringCombiner;
import me.jaimemartz.lobbybalancer.LobbyBalancer;
import me.jaimemartz.lobbybalancer.configuration.ConfigEntries;
import me.jaimemartz.lobbybalancer.connection.ConnectionIntent;
import me.jaimemartz.lobbybalancer.ping.ServerStatus;
import me.jaimemartz.lobbybalancer.section.ServerSection;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

public class ManageCommand extends Command {
    private final LobbyBalancer plugin;

    public ManageCommand(LobbyBalancer plugin) {
        super("section");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Messager msgr = new Messager(sender);
        if (sender.hasPermission("lobbybalancer.admin")) {
            if (args.length != 0) {
                switch (args[0].toLowerCase()) {
                    case "connect": {
                        if (args.length >= 2) {
                            String input = args[1];
                            ServerSection section = plugin.getSectionManager().getByName(input);
                            if (section != null) {
                                if (args.length == 3) {
                                    ProxiedPlayer player = plugin.getProxy().getPlayer(args[2]);
                                    if (player != null) {
                                        ConnectionIntent.connect(plugin, player, section);
                                    } else {
                                        msgr.send("&cThere is no player with that name connected to this proxy");
                                    }
                                } else {
                                    if (sender instanceof ProxiedPlayer) {
                                        ConnectionIntent.connect(plugin, (ProxiedPlayer) sender, section);
                                    } else {
                                        msgr.send("&cThis command can only be executed by a player");
                                    }
                                }
                            } else {
                                msgr.send(ConfigEntries.UNKNOWN_SECTION_MESSAGE.get());
                            }
                        } else {
                            help.accept(msgr);
                        }
                        break;
                    }

                    case "info": {
                        if (args.length == 2) {
                            String input = args[1];
                            ServerSection section = plugin.getSectionManager().getByName(input);
                            if (section != null) {
                                msgr.send("&7&m-----------------------------------------------------");

                                msgr.send("&7Information of section &b{name}",
                                        new Replacement("{name}", ChatColor.RED + section.getName()));

                                msgr.send("&7Principal: &b{status}",
                                        new Replacement("{status}", section.isPrincipal() ? ChatColor.GREEN + "yes" : ChatColor.RED + "no"));

                                if (section.hasParent()) {
                                    TextComponent message = new TextComponent("Parent: ");
                                    message.setColor(ChatColor.GRAY);

                                    TextComponent extra = new TextComponent(section.getParent().getName());
                                    extra.setColor(ChatColor.AQUA);
                                    extra.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/section info %s", section.getParent().getName())));
                                    extra.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click me for info").color(ChatColor.RED).create()));

                                    message.addExtra(extra);
                                    sender.sendMessage(message);
                                } else {
                                    msgr.send("&7Parent: &bNone");
                                }

                                msgr.send("&7Position: &b{position}",
                                        new Replacement("{position}", String.valueOf(section.getPosition())));

                                msgr.send("&7Provider: &b{name} &7({relation}&7)",
                                        new Replacement("{name}", section.getProvider().name()),
                                        new Replacement("{relation}", section.hasInheritedProvider() ? "Inherited" : "Specified"));

                                msgr.send("&7Dummy: &b{status}", new Replacement("{status}", section.isDummy() ? ChatColor.GREEN + "yes" : ChatColor.RED + "no"));

                                msgr.send("&7Section Server: &b{name}", new Replacement("{name}", section.hasServer() ? section.getServer().getName() : "None"));

                                if (section.hasCommand()) {
                                    msgr.send("&7Section Command: &b{name}&7, Permission: &b{permission}&7, Aliases: &b{aliases}",
                                            new Replacement("{name}", section.getCommand().getName()),
                                            new Replacement("{permission}", section.getCommand().getPermission().equals("") ? "None" : section.getCommand().getPermission()),
                                            new Replacement("{aliases}", section.getCommand().getAliases().length == 0 ? "None" : StringCombiner.combine(section.getCommand().getAliases(), ", ")));
                                } else {
                                    msgr.send("&7Section Command: &bNone");
                                }

                                msgr.send("&7Valid: &b{status}",
                                        new Replacement("{status}", section.isValid() ? ChatColor.GREEN + "yes" : ChatColor.RED + "no"));

                                if (!section.getServers().isEmpty()) {
                                    msgr.send("&7Section Servers: ");
                                    section.getServers().forEach(server -> {
                                        ServerStatus status = plugin.getPingManager().getStatus(server);
                                        msgr.send("&7> Server &b{name} &c({connected}/{maximum}) &7({status}&7)",
                                                new Replacement("{name}", server.getName()),
                                                new Replacement("{connected}", String.valueOf(status.getOnlinePlayers())),
                                                new Replacement("{maximum}", String.valueOf(status.getMaximumPlayers())),
                                                new Replacement("{status}", status.isAccessible() ? ChatColor.GREEN + "Accessible" : ChatColor.RED + "Inaccessible")
                                        );
                                    });
                                } else {
                                    msgr.send("&7Section Servers: &bNone");
                                }

                                msgr.send("&7&m-----------------------------------------------------");
                            } else {
                                msgr.send(ConfigEntries.UNKNOWN_SECTION_MESSAGE.get());
                            }
                        } else {
                            help.accept(msgr);
                        }
                        break;
                    }

                    case "list": {
                        Set<String> keys = plugin.getSectionManager().getSections().keySet();
                        Iterator<String> iterator = keys.iterator();
                        TextComponent message = new TextComponent("There are ");
                        message.addExtra(new TextComponent(new ComponentBuilder(String.valueOf(keys.size())).color(ChatColor.AQUA).create()));
                        message.addExtra(" configured sections:\n");
                        message.setColor(ChatColor.GRAY);

                        if (iterator.hasNext()) {
                            while (iterator.hasNext()) {
                                String name = iterator.next();
                                TextComponent extra = new TextComponent(name);
                                extra.setColor(ChatColor.GREEN);
                                extra.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/section info %s", name)));
                                extra.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click me for info").color(ChatColor.RED).create()));

                                if (iterator.hasNext()) {
                                    TextComponent sep = new TextComponent(", ");
                                    sep.setColor(ChatColor.GRAY);
                                    extra.addExtra(sep);
                                }

                                message.addExtra(extra);
                            }
                        } else {
                            TextComponent extra = new TextComponent("There are no sections to list");
                            extra.setColor(ChatColor.RED);
                            message.addExtra(extra);
                        }

                        sender.sendMessage(message);
                        break;
                    }

                    default: {
                        msgr.send("&cThis is not a valid argument for this command!");
                        help.accept(msgr);
                    }
                }
            } else {
                help.accept(msgr);
            }
        } else {
            msgr.send(ChatColor.RED + "You do not have permission to execute this command!");
        }
    }

    private static final Consumer<Messager> help = (msgr) -> msgr.send(
            "&7&m-----------------------------------------------------",
            "&7Available commands:",
            "&3/section list &7- &cTells you which sections are configured in the plugin",
            "&3/section info <section> &7- &cTells you info about the section",
            "&3/section connect <section> [player] &7- &cConnects you or the specified player to that section",
            "&7&m-----------------------------------------------------"
    );
}
