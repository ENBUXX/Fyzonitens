package com.enbuxx.fyzonitens;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class PokebolaListener implements Listener {

    private final Fyzonitens plugin;
    private final NamespacedKey mobKey;

    public PokebolaListener(Fyzonitens plugin) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, "captured_mob");
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isPokebola(item)) return;
        if (!(event.getRightClicked() instanceof LivingEntity)) return;
        
        LivingEntity target = (LivingEntity) event.getRightClicked();
        if (target instanceof Player) return;

        event.setCancelled(true);

        String mobType = target.getType().name();
        target.remove();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, mobType);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Contém: " + ChatColor.YELLOW + mobType);
            lore.add(ChatColor.GREEN + "Clica no chão para soltar!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[Fyzonitens] Mob capturado com sucesso!"));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().name().contains("RIGHT")) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isPokebola(item)) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(mobKey, PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);
        String mobType = meta.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);

        try {
            org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(mobType);
            player.getWorld().spawnEntity(player.getLocation().add(player.getLocation().getDirection().multiply(2)), type);
            
            meta.getPersistentDataContainer().remove(mobKey);
            meta.setLore(new ArrayList<>());
            item.setItemMeta(meta);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[Fyzonitens] Mob libertado!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Erro ao soltar o mob.");
        }
    }

    private boolean isPokebola(ItemStack item) {
        if (item == null || item.getType() != Material.MAGMA_CREAM) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return item.getItemMeta().getDisplayName().contains("Pokebola");
    }
}
