package services.plasma.minecraftRats.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import services.plasma.minecraftRats.MinecraftRats;
import services.plasma.minecraftRats.config.ConfigManager;
import services.plasma.minecraftRats.managers.RatManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RatsCommand implements CommandExecutor, TabCompleter {

    private final MinecraftRats plugin;
    private final ConfigManager configManager;
    private final RatManager ratManager;

    public RatsCommand(MinecraftRats plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.ratManager = plugin.getRatManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "spawn":
                return handleSpawnCommand(sender, args);
            case "kill":
                return handleKillCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    /**
     * Handle the spawn subcommand
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled, false otherwise
     */
    private boolean handleSpawnCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minecraftrats.spawn")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getPrefix() + configManager.getNoPermissionMessage()));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount > 50) {
                    amount = 50;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number format. Usage: /rats spawn [amount]");
                return true;
            }
        }

        int spawnedAmount = ratManager.spawnRats(player.getLocation(), amount);

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                configManager.getPrefix() + configManager.getSpawnMessage().replace("%amount%", String.valueOf(spawnedAmount))));

        return true;
    }

    /**
     * Handle the kill subcommand
     *
     * @param sender The command sender
     * @return True if the command was handled, false otherwise
     */
    private boolean handleKillCommand(CommandSender sender) {
        if (!sender.hasPermission("minecraftrats.kill")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getPrefix() + configManager.getNoPermissionMessage()));
            return true;
        }

        int killedAmount = ratManager.removeAllRats();

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                configManager.getPrefix() + configManager.getKillMessage().replace("%amount%", String.valueOf(killedAmount))));

        return true;
    }

    /**
     * Handle the reload subcommand
     *
     * @param sender The command sender
     * @return True if the command was handled, false otherwise
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("minecraftrats.reload")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getPrefix() + configManager.getNoPermissionMessage()));
            return true;
        }

        plugin.reload();

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getPrefix() + configManager.getReloadMessage()));

        return true;
    }

    /**
     * Send the help message to the sender
     *
     * @param sender The command sender
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== MinecraftRats Commands ===");
        sender.sendMessage(ChatColor.GOLD + "/rats spawn [amount]" + ChatColor.WHITE + " - Spawn rats at your location");
        sender.sendMessage(ChatColor.GOLD + "/rats kill" + ChatColor.WHITE + " - Kill all rats");
        sender.sendMessage(ChatColor.GOLD + "/rats reload" + ChatColor.WHITE + " - Reload the plugin configuration");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("spawn", "kill", "reload");
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return Arrays.asList("1", "5", "10", "25", "50");
        }

        return completions;
    }

    /**
     * Filter completions by prefix
     *
     * @param options The available options
     * @param prefix The prefix to filter by
     * @return The filtered completions
     */
    private List<String> filterCompletions(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}