package com.enbuxx.fyzonitens;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class Fyzonitens extends JavaPlugin implements Listener, CommandExecutor {

    private NamespacedKey keyMobType;
    private NamespacedKey keyTier;
    private NamespacedKey keyIsFull;
    private NamespacedKey keyOwnerUUID;
    private NamespacedKey keyMobHealth;
    private NamespacedKey keyMobMaxHealth;
    private NamespacedKey keyIsSpawnerPickaxe;
    private NamespacedKey keySpawnerType;
    private NamespacedKey keyIsDrusaPickaxe;
    private NamespacedKey keyIsChunkHopper;
    private NamespacedKey keyIsChunkTNT;
    
    private NamespacedKey keyChestWorld;
    private NamespacedKey keyChestX;
    private NamespacedKey keyChestY;
    private NamespacedKey keyChestZ;
    private NamespacedKey keyHopperOwner;
    private NamespacedKey keyItemOwner;

    private final Map<UUID, Location> selectedChests = new HashMap<>();
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 15000;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        keyMobType = new NamespacedKey(this, "mob_type");
        keyTier = new NamespacedKey(this, "pokebola_tier");
        keyIsFull = new NamespacedKey(this, "is_full");
        keyOwnerUUID = new NamespacedKey(this, "owner_uuid");
        keyMobHealth = new NamespacedKey(this, "mob_health");
        keyMobMaxHealth = new NamespacedKey(this, "mob_max_health");
        keyIsSpawnerPickaxe = new NamespacedKey(this, "is_spawner_pickaxe");
        keySpawnerType = new NamespacedKey(this, "spawner_type");
        keyIsDrusaPickaxe = new NamespacedKey(this, "is_drusa_pickaxe");
        keyIsChunkHopper = new NamespacedKey(this, "is_chunk_hopper");
        keyIsChunkTNT = new NamespacedKey(this, "is_chunk_tnt");

        keyChestWorld = new NamespacedKey(this, "chest_world");
        keyChestX = new NamespacedKey(this, "chest_x");
        keyChestY = new NamespacedKey(this, "chest_y");
        keyChestZ = new NamespacedKey(this, "chest_z");
        keyHopperOwner = new NamespacedKey(this, "hopper_owner_uuid");
        keyItemOwner = new NamespacedKey(this, "item_owner_uuid");

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("fyzon").setExecutor(this);

        iniciarTarefaFunilChunk();

        getLogger().info("Plugin Fyzonitens ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin Fyzonitens desativado.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("fyzonitens.admin")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cVocê não tem permissão."));
                return true;
            }
            reloadConfig();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.config-recarregada", "&aConfiguração recarregada!")));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("fyzonitens.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cVocê não tem permissão."));
            return true;
        }

        abrirGuiFyzon(player);
        return true;
    }

    private void abrirGuiFyzon(Player player) {
        String titulo = ChatColor.translateAlternateColorCodes('&', getConfig().getString("gui.titulo", "&8Fyzonitens"));
        int tamanho = getConfig().getInt("gui.tamanho", 27);
        Inventory gui = Bukkit.createInventory(null, tamanho, titulo);

        if (getConfig().getBoolean("gui.preencher-vazios", true)) {
            Material glassMat = Material.matchMaterial(getConfig().getString("gui.material-vazio", "LIGHT_GRAY_STAINED_GLASS_PANE"));
            if (glassMat == null) glassMat = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            
            ItemStack glass = new ItemStack(glassMat);
            ItemMeta glassMeta = glass.getItemMeta();
            if (glassMeta != null) {
                glassMeta.setDisplayName(" ");
                glass.setItemMeta(glassMeta);
            }
            for (int i = 0; i < tamanho; i++) {
                gui.setItem(i, glass);
            }
        }

        gui.setItem(getConfig().getInt("itens-gui.comum.slot", 10), criarItemGui("comum", player));
        gui.setItem(getConfig().getInt("itens-gui.intermediaria.slot", 12), criarItemGui("intermediaria", player));
        gui.setItem(getConfig().getInt("itens-gui.master.slot", 14), criarItemGui("master", player));
        gui.setItem(getConfig().getInt("itens-gui.picareta-spawner.slot", 16), criarPicaretaSpawnerParaGui(player));
        gui.setItem(getConfig().getInt("itens-gui.picareta-drusa.slot", 18), criarPicaretaDrusaParaGui(player));
        gui.setItem(getConfig().getInt("itens-gui.funil-chunk.slot", 20), criarFunilChunkParaGui());
        gui.setItem(getConfig().getInt("itens-gui.tnt-chunk.slot", 22), criarChunkTNTParaGui());

        player.openInventory(gui);
    }

    private ItemStack criarItemGui(String tier, Player player) {
        return criarPokebolaVazia(tier, player);
    }

    private ItemStack criarPicaretaSpawnerParaGui(Player player) {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("picareta-spawner.nome", "&b&lPicareta de Spawner")));
            List<String> rawLore = getConfig().getStringList("picareta-spawner.lore");
            List<String> lore = new ArrayList<>();
            for (String linha : rawLore) lore.add(ChatColor.translateAlternateColorCodes('&', linha));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyIsSpawnerPickaxe, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(keyItemOwner, PersistentDataType.STRING, player.getUniqueId().toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack criarPicaretaDrusaParaGui(Player player) {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("picareta-drusa.nome", "&d&lPicareta de Drusa")));
            List<String> rawLore = getConfig().getStringList("picareta-drusa.lore");
            List<String> lore = new ArrayList<>();
            for (String linha : rawLore) lore.add(ChatColor.translateAlternateColorCodes('&', linha));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyIsDrusaPickaxe, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(keyItemOwner, PersistentDataType.STRING, player.getUniqueId().toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack criarFunilChunkParaGui() {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("funil-chunk.nome", "&6&lFunil de Chunk")));
            List<String> rawLore = getConfig().getStringList("funil-chunk.lore");
            List<String> lore = new ArrayList<>();
            for (String linha : rawLore) lore.add(ChatColor.translateAlternateColorCodes('&', linha));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyIsChunkHopper, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack criarChunkTNTParaGui() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("tnt-chunk.nome", "&c&lTNT de Chunk")));
            List<String> rawLore = getConfig().getStringList("tnt-chunk.lore");
            List<String> lore = new ArrayList<>();
            for (String linha : rawLore) lore.add(ChatColor.translateAlternateColorCodes('&', linha));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyIsChunkTNT, PersistentDataType.BYTE, (byte) 1);
            
            if (getConfig().getBoolean("tnt-chunk.adicionar-brilho", true)) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String tituloGui = ChatColor.translateAlternateColorCodes('&', getConfig().getString("gui.titulo", "&8Fyzonitens"));
        if (event.getView().getTitle().equals(tituloGui)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            if (meta.getPersistentDataContainer().has(keyTier, PersistentDataType.STRING)) {
                String tier = meta.getPersistentDataContainer().get(keyTier, PersistentDataType.STRING);
                player.getInventory().addItem(criarPokebolaVazia(tier, player));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.ganhou", "&aItem recebido!")));
            } 
            else if (meta.getPersistentDataContainer().has(keyIsSpawnerPickaxe, PersistentDataType.BYTE)) {
                player.getInventory().addItem(criarPicaretaSpawnerParaGui(player));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.ganhou", "&aItem recebido!")));
            }
            else if (meta.getPersistentDataContainer().has(keyIsDrusaPickaxe, PersistentDataType.BYTE)) {
                player.getInventory().addItem(criarPicaretaDrusaParaGui(player));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.ganhou", "&aItem recebido!")));
            }
            else if (meta.getPersistentDataContainer().has(keyIsChunkHopper, PersistentDataType.BYTE)) {
                player.getInventory().addItem(criarFunilChunkParaGui());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.ganhou", "&aItem recebido!")));
            }
            else if (meta.getPersistentDataContainer().has(keyIsChunkTNT, PersistentDataType.BYTE)) {
                player.getInventory().addItem(criarChunkTNTParaGui());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.ganhou", "&aItem recebido!")));
            }
        }
    }

    private ItemStack criarPokebolaVazia(String tier, Player player) {
        String path = "pokebolas." + tier;
        Material mat = Material.matchMaterial(getConfig().getString(path + ".material", "EGG"));
        if (mat == null) mat = Material.EGG;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString(path + ".nome", "&fPokebola")));
            List<String> rawLore = getConfig().getStringList(path + ".lore");
            List<String> lore = new ArrayList<>();
            for (String linha : rawLore) lore.add(ChatColor.translateAlternateColorCodes('&', linha));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyIsFull, PersistentDataType.BYTE, (byte) 0);
            meta.getPersistentDataContainer().set(keyTier, PersistentDataType.STRING, tier);
            meta.getPersistentDataContainer().set(keyItemOwner, PersistentDataType.STRING, player.getUniqueId().toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack criarPokebolaCheia(String mobType, double health, double maxHealth, Player owner) {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String nomeTemplate = getConfig().getString("item-cheia.nome", "&b&lPokebola &e({mob})");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', nomeTemplate.replace("{mob}", mobType)));

            List<String> rawLore = getConfig().getStringList("item-cheia.lore");
            List<String> lore = new ArrayList<>();
            for (String linha : rawLore) {
                lore.add(ChatColor.translateAlternateColorCodes('&', linha
                        .replace("{mob}", mobType)
                        .replace("{vida}", String.format("%.1f", health))
                        .replace("{vida_max}", String.format("%.1f", maxHealth))
                        .replace("{dono}", owner.getName())));
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyIsFull, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(keyMobType, PersistentDataType.STRING, mobType);
            meta.getPersistentDataContainer().set(keyOwnerUUID, PersistentDataType.STRING, owner.getUniqueId().toString());
            meta.getPersistentDataContainer().set(keyMobHealth, PersistentDataType.DOUBLE, health);
            meta.getPersistentDataContainer().set(keyMobMaxHealth, PersistentDataType.DOUBLE, maxHealth);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(keyIsChunkTNT, PersistentDataType.BYTE)) {
            if (player.isSneaking()) {
                event.setCancelled(true);
                Block clickedBlock = event.getClickedBlock();
                if (clickedBlock == null) return;

                String worldName = player.getWorld().getName();
                List<String> mundosProibidosTNT = getConfig().getStringList("tnt-chunk.mundos-proibidos");
                if (mundosProibidosTNT.contains(worldName) && !player.hasPermission("fyzonitens.admin")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.tnt-mundo-proibido", "&cProibido neste mundo!")));
                    return;
                }

                Location tntLoc = clickedBlock.getLocation().add(0, 1, 0);

                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    playerHandClear(player, item.getType());
                }

                int delaySegundos = getConfig().getInt("tnt-chunk.tempo-atraso-segundos", 5);
                String msgPlantada = getConfig().getString("mensagens.tnt-plantada", "&cTNT plantada por {player}! Detonação em {tempo}s")
                        .replace("{player}", player.getName())
                        .replace("{tempo}", String.valueOf(delaySegundos));

                if (getConfig().getBoolean("tnt-chunk.anunciar-no-chat", true)) {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msgPlantada));
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msgPlantada));
                }

                tntLoc.getBlock().setType(Material.TNT);

                new BukkitRunnable() {
                    int tempoRestante = delaySegundos;

                    @Override
                    public void run() {
                        if (tempoRestante > 0) {
                            if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                                tntLoc.getWorld().playSound(tntLoc, Sound.valueOf(getConfig().getString("sons.plantar-tnt", "BLOCK_NOTE_BLOCK_PLING")), 1.0f, 0.5f);
                            }
                            tempoRestante--;
                        } else {
                            tntLoc.getBlock().setType(Material.AIR);
                            iniciarDestruicaoChunk(tntLoc);
                            cancel();
                        }
                    }
                }.runTaskTimer(this, 0L, 20L);
                return;
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(keyIsChunkHopper, PersistentDataType.BYTE)) {
                    event.setCancelled(true);
                    Location loc = block.getLocation();
                    selectedChests.put(player.getUniqueId(), loc);

                    String msg = getConfig().getString("mensagens.bau-selecionado", "&aBaú selecionado!")
                            .replace("{x}", String.valueOf(loc.getBlockX()))
                            .replace("{y}", String.valueOf(loc.getBlockY()))
                            .replace("{z}", String.valueOf(loc.getBlockZ()));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                        player.playSound(loc, Sound.valueOf(getConfig().getString("sons.selecionar-bau", "BLOCK_NOTE_BLOCK_PLING")), 1.0f, 2.0f);
                    }
                    return;
                }
            }
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        Byte isFull = meta.getPersistentDataContainer().get(keyIsFull, PersistentDataType.BYTE);
        if (isFull == null) return;

        String worldName = player.getWorld().getName();
        List<String> mundosProibidos = getConfig().getStringList("opcoes-gerais.mundos-proibidos-pokebola");
        if (mundosProibidos.contains(worldName)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.mundo-proibido", "&cNão pode usar pokebolas neste mundo!")));
            return;
        }

        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + COOLDOWN_TIME) - System.currentTimeMillis();
            if (timeLeft > 0) {
                event.setCancelled(true);
                long segundosRestantes = timeLeft / 1000 + 1;
                String msg = getConfig().getString("mensagens.cooldown", "&cEspere {tempo}s para lançar novamente!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg.replace("{tempo}", String.valueOf(segundosRestantes))));
                return;
            }
        }

        if (isFull == 1) {
            String ownerUUIDStr = meta.getPersistentDataContainer().get(keyOwnerUUID, PersistentDataType.STRING);
            if (ownerUUIDStr != null && !ownerUUIDStr.equals(player.getUniqueId().toString())) {
                if (!player.hasPermission("fyzonitens.admin")) {
                    event.setCancelled(true);
                    String msg = getConfig().getString("mensagens.nao-dono", "&cEsta pokebola não é sua!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    return;
                }
            }
        }

        event.setCancelled(true);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            playerHandClear(player, item.getType());
        }

        Projectile projectile;
        if (isFull == 1) {
            projectile = player.launchProjectile(Snowball.class);
            String mobType = meta.getPersistentDataContainer().get(keyMobType, PersistentDataType.STRING);
            Double hp = meta.getPersistentDataContainer().get(keyMobHealth, PersistentDataType.DOUBLE);
            Double maxHp = meta.getPersistentDataContainer().get(keyMobMaxHealth, PersistentDataType.DOUBLE);

            if (mobType != null) projectile.getPersistentDataContainer().set(keyMobType, PersistentDataType.STRING, mobType);
            if (hp != null) projectile.getPersistentDataContainer().set(keyMobHealth, PersistentDataType.DOUBLE, hp);
            if (maxHp != null) projectile.getPersistentDataContainer().set(keyMobMaxHealth, PersistentDataType.DOUBLE, maxHp);

            projectile.getPersistentDataContainer().set(keyIsFull, PersistentDataType.BYTE, (byte) 1);
        } else {
            String tier = meta.getPersistentDataContainer().get(keyTier, PersistentDataType.STRING);
            if (tier == null) tier = "comum";

            if (item.getType() == Material.EGG) {
                projectile = player.launchProjectile(Egg.class);
            } else {
                projectile = player.launchProjectile(Snowball.class);
            }

            projectile.getPersistentDataContainer().set(keyIsFull, PersistentDataType.BYTE, (byte) 0);
            projectile.getPersistentDataContainer().set(keyTier, PersistentDataType.STRING, tier);
        }

        if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
            player.getWorld().playSound(player.getLocation(), Sound.valueOf(getConfig().getString("sons.lançar-pokebola", "ENTITY_ENDER_PEARL_THROW")), 1.0f, 1.0f);
        }
    }

    private void iniciarDestruicaoChunk(Location loc) {
        Chunk chunk = loc.getChunk();
        org.bukkit.World world = chunk.getWorld();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        world.playSound(loc, Sound.ENTITY_TNT_PRIMED, 2.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLOSION, 2.0f, 0.5f);

        List<String> protegidos = getConfig().getStringList("tnt-chunk.blocos-protegidos");

        new BukkitRunnable() {
            int currentY = maxY;
            final boolean droparItens = getConfig().getBoolean("tnt-chunk.deve-dropar-itens", false);

            @Override
            public void run() {
                if (currentY < minY) {
                    world.playSound(loc, Sound.ENTITY_GENERIC_EXPLOSION, 3.0f, 0.8f);
                    cancel();
                    return;
                }

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        Block block = chunk.getBlock(x, currentY, z);
                        Material mat = block.getType();

                        if (mat == Material.AIR || protegidos.contains(mat.name())) {
                            continue;
                        }

                        if (droparItens) {
                            block.breakNaturally();
                        } else {
                            block.setType(Material.AIR, false);
                        }
                    }
                }

                if (getConfig().getBoolean("tnt-chunk.efeito-camada", true) && getConfig().getBoolean("efeitos.ativar-particulas", true)) {
                    world.spawnParticle(Particle.EXPLOSION, chunk.getBlock(8, currentY, 8).getLocation(), 1, 0.5, 0.5, 0.5, 0);
                }

                currentY--;
            }
        }.runTaskTimer(this, 0L, 2L);
    }

    private void playerHandClear(Player player, Material mat) {
        if (player.getInventory().getItemInMainHand().getType() == mat) {
            player.getInventory().setItemInMainHand(null);
        } else if (player.getInventory().getItemInOffHand().getType() == mat) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player)) return;
        Player player = (Player) projectile.getShooter();

        Byte isFull = projectile.getPersistentDataContainer().get(keyIsFull, PersistentDataType.BYTE);
        if (isFull == null) return;

        if (isFull == 0) {
            Entity hitEntity = event.getHitEntity();
            if (hitEntity instanceof LivingEntity && !(hitEntity instanceof Player)) {
                LivingEntity livingMob = (LivingEntity) hitEntity;
                String mobName = livingMob.getType().name();

                List<String> blacklist = getConfig().getStringList("blacklist");
                if (blacklist.contains(mobName)) {
                    String msg = getConfig().getString("mensagens.bloqueado", "&cVocê não pode capturar este mob!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    return;
                }

                String tier = projectile.getPersistentDataContainer().get(keyTier, PersistentDataType.STRING);
                if (tier == null) tier = "comum";

                double chance = getConfig().getDouble("pokebolas." + tier + ".chance", 25.0);
                double roll = random.nextDouble() * 100.0;

                if (roll <= chance) {
                    double currentHp = livingMob.getHealth();
                    double maxHp = livingMob.getMaxHealth();

                    if (getConfig().getBoolean("efeitos.ativar-particulas", true)) {
                        livingMob.getWorld().spawnParticle(Particle.HEART, livingMob.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                    }
                    if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                        livingMob.getWorld().playSound(livingMob.getLocation(), Sound.valueOf(getConfig().getString("sons.sucesso-captura", "ENTITY_PLAYER_LEVELUP")), 1.0f, 1.5f);
                    }

                    livingMob.remove();

                    ItemStack pokebolaCheia = criarPokebolaCheia(mobName, currentHp, maxHp, player);
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(pokebolaCheia);
                    } else {
                        player.getWorld().dropItemNaturally(player.getLocation(), pokebolaCheia);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.inventario-cheio", "&cInventário cheio! Item dropado.")));
                    }

                    String msg = getConfig().getString("mensagens.capturado", "&aMob capturado com sucesso!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg.replace("{mob}", mobName)));
                } else {
                    if (getConfig().getBoolean("efeitos.ativar-particulas", true)) {
                        projectile.getWorld().spawnParticle(Particle.SMOKE, projectile.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
                    }
                    if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                        projectile.getWorld().playSound(projectile.getLocation(), Sound.valueOf(getConfig().getString("sons.falha-captura", "ENTITY_ITEM_BREAK")), 1.0f, 1.0f);
                    }

                    String msg = getConfig().getString("mensagens.falhou", "&cO mob resistiu e escapou!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                }
            }
        } else {
            String mobType = projectile.getPersistentDataContainer().get(keyMobType, PersistentDataType.STRING);
            Double hp = projectile.getPersistentDataContainer().get(keyMobHealth, PersistentDataType.DOUBLE);

            if (mobType != null) {
                try {
                    EntityType entityType = EntityType.valueOf(mobType);
                    Entity spawned = projectile.getWorld().spawnEntity(projectile.getLocation(), entityType);

                    if (spawned instanceof LivingEntity && hp != null) {
                        LivingEntity living = (LivingEntity) spawned;
                        if (hp <= living.getMaxHealth()) {
                            living.setHealth(hp);
                        }
                    }

                    if (getConfig().getBoolean("efeitos.ativar-particulas", true)) {
                        projectile.getWorld().spawnParticle(Particle.PORTAL, projectile.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                    }
                    if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                        projectile.getWorld().playSound(projectile.getLocation(), Sound.valueOf(getConfig().getString("sons.soltar-mob", "ENTITY_CHICKEN_EGG")), 1.0f, 1.0f);
                    }

                    String msg = getConfig().getString("mensagens.solto", "&eMob libertado!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg.replace("{mob}", mobType)));
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Erro ao invocar o mob guardado.");
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();

        if (item.getType() == Material.SPAWNER) {
            String spawnerTypeStr = meta.getPersistentDataContainer().get(keySpawnerType, PersistentDataType.STRING);
            if (spawnerTypeStr == null) return;

            if (block.getState() instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) block.getState();
                try {
                    EntityType entityType = EntityType.valueOf(spawnerTypeStr);
                    spawner.setSpawnedType(entityType);
                    spawner.setMinSpawnDelay(getConfig().getInt("picareta-spawner.min-spawn-delay", 200));
                    spawner.setMaxSpawnDelay(getConfig().getInt("picareta-spawner.max-spawn-delay", 400));
                    spawner.update(true);
                } catch (Exception e) {
                    spawner.setSpawnedType(EntityType.PIG);
                    spawner.update(true);
                }
            }
        }
        else if (item.getType() == Material.HOPPER && meta.getPersistentDataContainer().has(keyIsChunkHopper, PersistentDataType.BYTE)) {
            Chunk chunk = block.getChunk();
            
            int limite = getConfig().getInt("funil-chunk.limite-por-chunk", 1);
            if (player.hasPermission("fyzonitens.vip")) {
                limite = getConfig().getInt("funil-chunk.limite-vip", 3);
            }

            int count = contarFunisNaChunk(chunk);
            if (count >= limite) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.limite-atingido", "&cLimite de funis atingido nesta chunk!")));
                if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                    player.playSound(block.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                return;
            }

            Location chestLoc = selectedChests.get(player.getUniqueId());
            if (chestLoc == null) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.funil-sem-bau", "&cSelecione um baú primeiro!")));
                return;
            }

            if (block.getState() instanceof org.bukkit.block.Hopper) {
                org.bukkit.block.Hopper hopperState = (org.bukkit.block.Hopper) block.getState();
                hopperState.getPersistentDataContainer().set(keyChestWorld, PersistentDataType.STRING, chestLoc.getWorld().getName());
                hopperState.getPersistentDataContainer().set(keyChestX, PersistentDataType.INTEGER, chestLoc.getBlockX());
                hopperState.getPersistentDataContainer().set(keyChestY, PersistentDataType.INTEGER, chestLoc.getBlockY());
                hopperState.getPersistentDataContainer().set(keyChestZ, PersistentDataType.INTEGER, chestLoc.getBlockZ());
                hopperState.getPersistentDataContainer().set(keyIsChunkHopper, PersistentDataType.BYTE, (byte) 1);
                hopperState.getPersistentDataContainer().set(keyHopperOwner, PersistentDataType.STRING, player.getUniqueId().toString());
                hopperState.update(true);

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.funil-linkado", "&aFunil linkado com sucesso!")));
                if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                    player.playSound(block.getLocation(), Sound.valueOf(getConfig().getString("sons.linkar-funil", "BLOCK_CONDUIT_ACTIVATE")), 0.8f, 1.5f);
                }
            }
        }
    }

    private int contarFunisNaChunk(Chunk chunk) {
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y += 4) {
                    Block b = chunk.getBlock(x, y, z);
                    if (b.getType() == Material.HOPPER) {
                        if (b.getState() instanceof org.bukkit.block.Hopper) {
                            org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) b.getState();
                            if (hopper.getPersistentDataContainer().has(keyIsChunkHopper, PersistentDataType.BYTE)) {
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;

        if (block.getType() == Material.SPAWNER && meta != null) {
            Byte isSpawnerPick = meta.getPersistentDataContainer().get(keyIsSpawnerPickaxe, PersistentDataType.BYTE);
            if (isSpawnerPick != null && isSpawnerPick == 1) {
                if (getConfig().getBoolean("picaretas-exclusivas.exigir-permissao", true)) {
                    String permSpawner = getConfig().getString("picaretas-exclusivas.permissoes.spawner", "fyzonitens.picareta.spawner");
                    if (!player.hasPermission(permSpawner) && !player.hasPermission("fyzonitens.admin")) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.sem-permissao-picareta", "&cVocê não tem permissão para usar esta picareta!")));
                        return;
                    }
                }

                if (getConfig().getBoolean("picaretas-exclusivas.apenas-dono-ou-vip", true)) {
                    String ownerUUID = meta.getPersistentDataContainer().get(keyItemOwner, PersistentDataType.STRING);
                    String permSpawner = getConfig().getString("picaretas-exclusivas.permissoes.spawner", "fyzonitens.picareta.spawner");
                    boolean temPermissaoVip = player.hasPermission(permSpawner) || player.hasPermission("fyzonitens.admin");
                    
                    if (ownerUUID != null && !ownerUUID.equals(player.getUniqueId().toString()) && !temPermissaoVip) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.picareta-protegida", "&cEsta picareta não é sua!")));
                        return;
                    }
                }

                event.setDropItems(false);
                CreatureSpawner spawner = (CreatureSpawner) block.getState();
                EntityType spawnerType = spawner.getSpawnedType();

                ItemStack spawnerItem = new ItemStack(Material.SPAWNER);
                ItemMeta spawnerMeta = spawnerItem.getItemMeta();
                if (spawnerMeta != null) {
                    String nomeMob = (spawnerType != null) ? spawnerType.name() : "PIG";
                    spawnerMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lSpawner de &e" + nomeMob));
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Mob: &f" + nomeMob));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Coloque no chão para usar."));
                    spawnerMeta.setLore(lore);
                    spawnerMeta.getPersistentDataContainer().set(keySpawnerType, PersistentDataType.STRING, nomeMob);
                    spawnerItem.setItemMeta(spawnerMeta);
                }

                darItemAoJogadorOuDropar(player, block, spawnerItem);
                if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                    block.getWorld().playSound(block.getLocation(), Sound.valueOf(getConfig().getString("sons.quebrar-spawner", "BLOCK_ANVIL_USE")), 1.0f, 1.0f);
                }
                if (getConfig().getBoolean("efeitos.ativar-particulas", true)) {
                    block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.1);
                }
                aplicarDesgasteSeNecessario(player, item, meta, "picareta-spawner.inquebravel");
            }
        }
        else if (block.getType() == Material.AMETHYST_CLUSTER && meta != null) {
            Byte isDrusaPick = meta.getPersistentDataContainer().get(keyIsDrusaPickaxe, PersistentDataType.BYTE);
            if (isDrusaPick != null && isDrusaPick == 1) {
                if (getConfig().getBoolean("picaretas-exclusivas.exigir-permissao", true)) {
                    String permDrusa = getConfig().getString("picaretas-exclusivas.permissoes.drusa", "fyzonitens.picareta.drusa");
                    if (!player.hasPermission(permDrusa) && !player.hasPermission("fyzonitens.admin")) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.sem-permissao-picareta", "&cVocê não tem permissão para usar esta picareta!")));
                        return;
                    }
                }

                if (getConfig().getBoolean("picaretas-exclusivas.apenas-dono-ou-vip", true)) {
                    String ownerUUID = meta.getPersistentDataContainer().get(keyItemOwner, PersistentDataType.STRING);
                    String permDrusa = getConfig().getString("picaretas-exclusivas.permissoes.drusa", "fyzonitens.picareta.drusa");
                    boolean temPermissaoVip = player.hasPermission(permDrusa) || player.hasPermission("fyzonitens.admin");

                    if (ownerUUID != null && !ownerUUID.equals(player.getUniqueId().toString()) && !temPermissaoVip) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.picareta-protegida", "&cEsta picareta não é sua!")));
                        return;
                    }
                }

                event.setDropItems(false);
                ItemStack drusaItem = new ItemStack(Material.AMETHYST_CLUSTER);
                darItemAoJogadorOuDropar(player, block, drusaItem);
                if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                    block.getWorld().playSound(block.getLocation(), Sound.valueOf(getConfig().getString("sons.quebrar-drusa", "BLOCK_AMETHYST_BLOCK_CHIME")), 1.0f, 1.2f);
                }
                if (getConfig().getBoolean("efeitos.ativar-particulas", true)) {
                    block.getWorld().spawnParticle(Particle.ENTITY_EFFECT, block.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 1);
                }
                aplicarDesgasteSeNecessario(player, item, meta, "picareta-drusa.inquebravel");
            }
        }
        else if (block.getType() == Material.HOPPER) {
            if (block.getState() instanceof org.bukkit.block.Hopper) {
                org.bukkit.block.Hopper hopperState = (org.bukkit.block.Hopper) block.getState();
                if (hopperState.getPersistentDataContainer().has(keyIsChunkHopper, PersistentDataType.BYTE)) {
                    String ownerUUID = hopperState.getPersistentDataContainer().get(keyHopperOwner, PersistentDataType.STRING);
                    if (ownerUUID != null && !ownerUUID.equals(player.getUniqueId().toString()) && !player.hasPermission("fyzonitens.admin")) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("mensagens.funil-protegido", "&cEste Funil de Chunk pertence a outro jogador!")));
                        return;
                    }

                    event.setDropItems(false);
                    ItemStack hopperItem = criarFunilChunkParaGui();
                    darItemAoJogadorOuDropar(player, block, hopperItem);
                    if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_HOPPER_DETACH, 1.0f, 1.0f);
                    }
                }
            }
        }
    }

    private void iniciarTarefaFunilChunk() {
        int segundos = getConfig().getInt("funil-chunk.intervalo-segundos", 2);
        long ticks = segundos * 20L;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    for (Chunk chunk : world.getLoadedChunks()) {
                        Location chestLoc = encontrarChestLinkadoNaChunk(chunk);
                        if (chestLoc == null) continue;

                        for (Entity entity : chunk.getEntities()) {
                            if (entity instanceof Item) {
                                Item droppedItem = (Item) entity;
                                if (droppedItem.isDead() || droppedItem.getItemStack().getType() == Material.AIR) continue;

                                Block chestBlock = chestLoc.getBlock();
                                if (chestBlock.getState() instanceof Chest) {
                                    Chest chest = (Chest) chestBlock.getState();
                                    HashMap<Integer, ItemStack> sobra = chest.getInventory().addItem(droppedItem.getItemStack());
                                    
                                    if (sobra.isEmpty()) {
                                        droppedItem.remove();
                                        if (getConfig().getBoolean("efeitos.ativar-particulas", true)) {
                                            world.spawnParticle(Particle.PORTAL, droppedItem.getLocation().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.05);
                                        }
                                    } else {
                                        droppedItem.setItemStack(sobra.values().iterator().next());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, ticks);
    }

    private Location encontrarChestLinkadoNaChunk(Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y += 4) {
                    Block b = chunk.getBlock(x, y, z);
                    if (b.getType() == Material.HOPPER) {
                        if (b.getState() instanceof org.bukkit.block.Hopper) {
                            org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) b.getState();
                            if (hopper.getPersistentDataContainer().has(keyIsChunkHopper, PersistentDataType.BYTE)) {
                                String wName = hopper.getPersistentDataContainer().get(keyChestWorld, PersistentDataType.STRING);
                                Integer cx = hopper.getPersistentDataContainer().get(keyChestX, PersistentDataType.INTEGER);
                                Integer cy = hopper.getPersistentDataContainer().get(keyChestY, PersistentDataType.INTEGER);
                                Integer cz = hopper.getPersistentDataContainer().get(keyChestZ, PersistentDataType.INTEGER);
                                
                                if (cx != null && cy != null && cz != null) {
                                    org.bukkit.World cw = Bukkit.getWorld(wName != null ? wName : chunk.getWorld().getName());
                                    if (cw != null) {
                                        return new Location(cw, cx, cy, cz);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void darItemAoJogadorOuDropar(Player player, Block block, ItemStack item) {
        Map<Integer, ItemStack> excesso = player.getInventory().addItem(item);
        if (!excesso.isEmpty()) {
            for (ItemStack drop : excesso.values()) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }
    }

    private void aplicarDesgasteSeNecessario(Player player, ItemStack item, ItemMeta meta, String configPath) {
        boolean inquebravel = getConfig().getBoolean(configPath, true);
        if (!inquebravel) {
            org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
            damageable.setDamage(damageable.getDamage() + 1);
            if (damageable.getDamage() >= item.getType().getMaxDurability()) {
                player.getInventory().setItemInMainHand(null);
                if (getConfig().getBoolean("efeitos.ativar-sons", true)) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                }
            } else {
                item.setItemMeta(meta);
            }
        }
    }
}
