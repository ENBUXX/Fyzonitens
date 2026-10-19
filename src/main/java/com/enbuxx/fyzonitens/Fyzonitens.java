package com.enbuxx.fyzonitens;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Fyzonitens extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("fyzon").setExecutor(this);
        getLogger().info("Fyzonitens ativado com sucesso para o ORIGEM MC!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Fyzonitens desativado.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fyzon")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("fyzonitens.admin")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSem permissão."));
                    return true;
                }
                reloadConfig();
                String msg = getConfig().getString("mensagens.config-recarregada", "&aConfiguração recarregada!");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return true;
            }
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[Fyzonitens] Plugin ativo e operacional!"));
                if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }
        }
        return true;
    }
}
