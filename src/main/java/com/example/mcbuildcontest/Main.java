package com.example.mcbuildcontest;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private Location buildLobby;
    private Map<String, Location> regions = new HashMap<>();
    private List<String> themes;
    private Map<String, Integer> themeVotes = new HashMap<>();
    private String selectedTheme;
    private List<Player> participants = new ArrayList<>();
    private boolean votingActive = false;
    private boolean gameActive = false;
    private Set<Location> placedBlocks = new HashSet<>();
    private Map<Player, Integer> assignedRegions = new HashMap<>();
    private int votingDurationTicks;
    private int buildDurationTicks;
    private int minPlayers;
    private int maxPlayers;
    private BossBar bossBar;
    private int regionSize;
    private List<BlockChange> blockChanges = new ArrayList<>();
    private boolean replayActive = false;
    private int replayIndex = 0;
    private BukkitRunnable replayTask;

    private static class BlockChange {
        Location loc;
        Material oldType;
        Material newType;
        long time;

        BlockChange(Location loc, Material oldType, Material newType, long time) {
            this.loc = loc;
            this.oldType = oldType;
            this.newType = newType;
            this.time = time;
        }
    }

    private void loadConfig() {
        // BuildLobby
        String lobbyWorld = getConfig().getString("build-lobby.world");
        double lobbyX = getConfig().getDouble("build-lobby.x");
        double lobbyY = getConfig().getDouble("build-lobby.y");
        double lobbyZ = getConfig().getDouble("build-lobby.z");
        World world = Bukkit.getWorld(lobbyWorld);
        if (world != null) {
            buildLobby = new Location(world, lobbyX, lobbyY, lobbyZ);
        }

        // Regions
        List<?> regionList = getConfig().getList("regions");
        if (regionList != null) {
            for (Object obj : regionList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    String name = (String) map.get("name");
                    String regWorld = (String) map.get("world");
                    double x = ((Number) map.get("x")).doubleValue();
                    double y = ((Number) map.get("y")).doubleValue();
                    double z = ((Number) map.get("z")).doubleValue();
                    World regW = Bukkit.getWorld(regWorld);
                    if (regW != null) {
                        regions.put(name, new Location(regW, x, y, z));
                    }
                }
            }
        }

        // Themes
        themes = getConfig().getStringList("themes");

        // Timers
        votingDurationTicks = getConfig().getInt("timers.voting-duration") * 20; // saniye to ticks
        buildDurationTicks = getConfig().getInt("timers.build-duration") * 20;

        // Min players
        minPlayers = getConfig().getInt("min-players");

        // Max players
        maxPlayers = getConfig().getInt("max-players");

        // Region size
        regionSize = getConfig().getInt("region-size");
    }

    @Override
    public void onEnable() {
        getLogger().info("MC Build Contest enabled!");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        // Config'den yükle
        loadConfig();
        // BossBar oluştur
        bossBar = Bukkit.createBossBar("Build Contest", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setVisible(false);
    }
    }

    @Override
    public void onDisable() {
        getLogger().info("MC Build Contest disabled!");
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("buildermap")) {
            if (participants.size() >= maxPlayers) {
                player.sendMessage(ChatColor.RED + "Lobby dolu! Maksimum " + maxPlayers + " oyuncu.");
                return true;
            }
            player.teleport(buildLobby);
            giveCompass(player);
            if (!participants.contains(player)) {
                participants.add(player);
            }
            player.sendTitle(ChatColor.GREEN + "BuildLobby'ye Hoş Geldiniz!", ChatColor.YELLOW + "Pusulaya tıklayın.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.sendMessage("BuildLobby'ye hoş geldiniz! Pusulaya tıklayın.");
            // Eğer min oyuncu varsa oylama başlat
            if (participants.size() >= minPlayers && !votingActive && !gameActive) {
                startVoting();
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("buildreplay")) {
            if (args.length > 0) {
                String subCmd = args[0];
                if (subCmd.equalsIgnoreCase("start")) {
                    if (!replayActive && !blockChanges.isEmpty()) {
                        startReplay(player);
                    } else {
                        player.sendMessage("Replay aktif veya kayıt yok.");
                    }
                } else if (subCmd.equalsIgnoreCase("stop")) {
                    if (replayActive) {
                        stopReplay();
                    }
                } else if (subCmd.equalsIgnoreCase("forward")) {
                    if (replayActive) {
                        replayIndex = Math.min(replayIndex + 10, blockChanges.size() - 1);
                        applyReplayStep();
                    }
                } else if (subCmd.equalsIgnoreCase("backward")) {
                    if (replayActive) {
                        replayIndex = Math.max(replayIndex - 10, 0);
                        applyReplayStep();
                    }
                }
            } else {
                player.sendMessage("Kullanım: /buildreplay start|stop|forward|backward");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("buildreload")) {
            reloadConfig();
            loadConfig();
            player.sendMessage(ChatColor.GREEN + "Config reloaded!");
            return true;
        }

        return false;

    private void giveCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Tema Seç");
        compass.setItemMeta(meta);
        player.getInventory().addItem(compass);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInMainHand();
        if (item != null && item.getType() == Material.COMPASS && item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Tema Seç")) {
            openThemeGUI(player);
        }
    }

    private void openThemeGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 18, "Tema Seç");
        for (int i = 0; i < themes.size(); i++) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(themes.get(i));
            item.setItemMeta(meta);
            gui.setItem(i, item);
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Tema Seç")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.PAPER) {
                String theme = item.getItemMeta().getDisplayName();
                themeVotes.put(theme, themeVotes.getOrDefault(theme, 0) + 1);
                player.sendMessage("Oy verdiniz: " + theme);
                player.closeInventory();
            }
        }
    }

    // Oylama başlat (config'den süre)
    private void startVoting() {
        votingActive = true;
        themeVotes.clear();
        for (Player p : participants) {
            giveCompass(p);
        }
        sendTitleToAll(ChatColor.GOLD + "Tema Oylaması Başladı!", ChatColor.YELLOW + "Pusulaya tıklayın ve tema seçin.");
        playSoundToAll(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        updateBossBar("Tema Oylaması: " + (votingDurationTicks / 20) + " saniye", 1.0);
        updateTabList(ChatColor.GREEN + "Build Contest", ChatColor.AQUA + "Tema: Seçiliyor\nSüre: " + (votingDurationTicks / 20) + "s");
        Bukkit.broadcastMessage("Tema oylaması başladı! " + (votingDurationTicks / 20) + " saniye var.");
        new BukkitRunnable() {
            int timeLeft = votingDurationTicks / 20;
            @Override
            public void run() {
                timeLeft--;
                updateBossBar("Tema Oylaması: " + timeLeft + " saniye", (double) timeLeft / (votingDurationTicks / 20));
                updateTabList(ChatColor.GREEN + "Build Contest", ChatColor.AQUA + "Tema: Seçiliyor\nSüre: " + timeLeft + "s");
                if (timeLeft <= 0) {
                    endVoting();
                    cancel();
                }
            }
        }.runTaskTimer(this, 20, 20); // Her saniye
    }

    private void endVoting() {
        votingActive = false;
        hideBossBar();
        if (participants.size() >= minPlayers) {
            selectedTheme = themeVotes.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
            sendTitleToAll(ChatColor.GREEN + "Tema Seçildi!", ChatColor.YELLOW + selectedTheme);
            playSoundToAll(Sound.ENTITY_PLAYER_LEVELUP);
            updateTabList(ChatColor.GREEN + "Build Contest", ChatColor.AQUA + "Tema: " + selectedTheme + "\nDurum: Başlıyor");
            Bukkit.broadcastMessage("Seçilen tema: " + selectedTheme);
            new BukkitRunnable() {
                @Override
                public void run() {
                    startGame();
                }
            }.runTaskLater(this, 100); // 5 saniye bekle
        } else {
            sendTitleToAll(ChatColor.RED + "Yeterli Oyuncu Yok!", "");
            playSoundToAll(Sound.ENTITY_VILLAGER_NO);
            updateTabList(ChatColor.GREEN + "Build Contest", ChatColor.RED + "Yeterli oyuncu yok");
            Bukkit.broadcastMessage("Yeterli oyuncu yok (" + minPlayers + " gerekli).");
        }
    }

    private void startGame() {
        gameActive = true;
        blockChanges.clear();
        // Oyuncuları bölgelere dağıt ve duvar koy
        int regionIndex = 0;
        for (Player p : participants) {
            if (regionIndex < regions.size()) {
                String regionName = "Region" + (regionIndex + 1);
                Location regionLoc = regions.get(regionName);
                p.teleport(regionLoc);
                assignedRegions.put(p, regionIndex);
                p.setGameMode(GameMode.CREATIVE);
                // Duvar koy (örnek: etraflarına glass)
                buildWalls(regionLoc, regionIndex);
                regionIndex++;
            }
        }
        sendTitleToAll(ChatColor.BLUE + "Oyun Başladı!", ChatColor.GREEN + "Tema: " + selectedTheme);
        playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL);
        updateBossBar("Build Süresi: " + (buildDurationTicks / 20) + " saniye - Tema: " + selectedTheme, 1.0);
        updateTabList(ChatColor.GREEN + "Build Contest", ChatColor.AQUA + "Tema: " + selectedTheme + "\nSüre: " + (buildDurationTicks / 20) + "s");
        Bukkit.broadcastMessage("Oyun başladı! Tema: " + selectedTheme + " - " + (buildDurationTicks / 20) + " saniye build süresi.");
        new BukkitRunnable() {
            int timeLeft = buildDurationTicks / 20;
            @Override
            public void run() {
                timeLeft--;
                updateBossBar("Build Süresi: " + timeLeft + " saniye - Tema: " + selectedTheme, (double) timeLeft / (buildDurationTicks / 20));
                updateTabList(ChatColor.GREEN + "Build Contest", ChatColor.AQUA + "Tema: " + selectedTheme + "\nSüre: " + timeLeft + "s");
                if (timeLeft <= 0) {
                    endGame();
                    cancel();
                }
            }
        }.runTaskTimer(this, 20, 20);
    }

    private void buildWalls(Location center, int regionIndex) {
        World world = center.getWorld();
        int half = regionSize / 2;
        // Basit duvar: etrafına barrier koy (görünmez)
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                if (x == -half || x == half || z == -half || z == half) {
                    Location wallLoc = center.clone().add(x, 0, z);
                    wallLoc.getBlock().setType(Material.BARRIER);
                }
            }
        }
    }

    private void endGame() {
        gameActive = false;
        hideBossBar();
        sendTitleToAll(ChatColor.RED + "Oyun Bitti!", ChatColor.YELLOW + "Puanlama aşaması.");
        playSoundToAll(Sound.ENTITY_FIREWORK_ROCKET_BLAST);
        updateTabList(ChatColor.GREEN + "Build Contest", ChatColor.RED + "Oyun Bitti - Puanlama");
        // Oyuncuları lobby'ye teleport
        for (Player p : participants) {
            p.teleport(buildLobby);
            p.setGameMode(GameMode.SURVIVAL); // Normal mod
        }
        // Basit puanlama (örnek)
        Bukkit.broadcastMessage("Oyun bitti! Puanlama aşaması.");
        // ...
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitleToAll(ChatColor.GOLD + "Kazanan Belirlendi!", "");
                playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE);
                updateTabList(ChatColor.GREEN + "Build Contest", ChatColor.GOLD + "Kazanan: ...");
                Bukkit.broadcastMessage("Kazanan belirlendi!");
                // Reset
                participants.clear();
                themeVotes.clear();
                placedBlocks.clear();
                assignedRegions.clear();
            }
        }.runTaskLater(this, 100); // 5 saniye sonra
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (gameActive) {
            Player player = event.getPlayer();
            Location loc = event.getBlock().getLocation();
            if (!isInPlayerRegion(player, loc)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Sadece kendi alanında inşa edebilirsiniz!");
                return;
            }
            // Kayıt
            blockChanges.add(new BlockChange(loc, Material.AIR, event.getBlock().getType(), System.currentTimeMillis()));
            placedBlocks.add(loc);
        }
    }

    private boolean isInPlayerRegion(Player player, Location loc) {
        if (!assignedRegions.containsKey(player)) return false;
        int regionIndex = assignedRegions.get(player);
        Location regionCenter = regions.get("Region" + (regionIndex + 1));
        int half = regionSize / 2;
        return loc.getX() >= regionCenter.getX() - half && loc.getX() <= regionCenter.getX() + half &&
               loc.getZ() >= regionCenter.getZ() - half && loc.getZ() <= regionCenter.getZ() + half;
    }

    private void sendTitleToAll(String title, String subtitle) {
        for (Player p : participants) {
            p.sendTitle(title, subtitle, 10, 70, 20);
        }
    }

    private void playSoundToAll(Sound sound) {
        for (Player p : participants) {
            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    private void updateBossBar(String title, double progress) {
        bossBar.setTitle(title);
        bossBar.setProgress(progress);
        bossBar.removeAll();
        for (Player p : participants) {
            bossBar.addPlayer(p);
        }
        bossBar.setVisible(true);
    }

    private void hideBossBar() {
        bossBar.setVisible(false);
    }

    private void updateTabList(String header, String footer) {
        for (Player p : participants) {
            p.setPlayerListHeaderFooter(header, footer);
        }
    }

    private void startReplay(Player player) {
        replayActive = true;
        replayIndex = 0;
        player.setGameMode(GameMode.CREATIVE);
        player.teleport(regions.values().iterator().next()); // İlk bölgeye
        player.sendMessage("Replay başladı.");
        replayTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (replayIndex < blockChanges.size()) {
                    applyReplayStep();
                    replayIndex++;
                } else {
                    stopReplay();
                }
            }
        };
        replayTask.runTaskTimer(this, 0, 20); // Her saniye
    }

    private void stopReplay() {
        replayActive = false;
        if (replayTask != null) {
            replayTask.cancel();
        }
        // Blokları sıfırla (isteğe bağlı)
        for (BlockChange change : blockChanges) {
            change.loc.getBlock().setType(change.oldType);
        }
        Bukkit.broadcastMessage("Replay durduruldu.");
    }

    private void applyReplayStep() {
        if (replayIndex < blockChanges.size()) {
            BlockChange change = blockChanges.get(replayIndex);
            change.loc.getBlock().setType(change.newType);
        }
    }