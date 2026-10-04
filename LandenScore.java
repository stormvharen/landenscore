package nl.landen;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/*
 * LandenScore: de hele plugin in één Java-bestand.
 * Naast dit bestand heb je alleen plugin.yml nodig (en pom.xml om te bouwen).
 */

public class LandenScore extends JavaPlugin implements Listener {
    private CountryManager countries;
    private StatsManager stats;
    private MoneyService money;
    private BankMenu bank;
    private TopMenu topMenu;
    private Runnable economyShutdown = () -> { };
    private boolean ownEconomy;
    private StoreMenu store;
    private ShopManager shop;
    private ShopMenu shopMenu;
    private AfkRewards afkRewards;
    private RtpManager rtp;
    private KeyallManager keyall;
    private RewardManager rewards;
    private RewardMenu rewardMenu;
    private ScoreboardManager scoreboards;
    private LobbyManager lobby;
    private GuiManager gui;
    private MenuManager menu;
    private AdminMenu admin;
    private NpcManager npcs;
    private DuelManager duels;
    private Actions actions;

    @Override
    public void onEnable() {
        writeDefaultConfig();
        countries = new CountryManager(this);
        if (getServer().getPluginManager().isPluginEnabled("Vault")) economyShutdown = EconomySetup.register(this);
        money = new MoneyService(this);
        bank = new BankMenu(this);
        topMenu = new TopMenu(this);
        stats = new StatsManager(this);
        store = new StoreMenu(this);
        shop = new ShopManager(this);
        shopMenu = new ShopMenu(this);
        afkRewards = new AfkRewards(this);
        rtp = new RtpManager(this);
        keyall = new KeyallManager(this);
        rewards = new RewardManager(this);
        rewardMenu = new RewardMenu(this);
        scoreboards = new ScoreboardManager(this);
        lobby = new LobbyManager(this);
        gui = new GuiManager(this);
        menu = new MenuManager(this);
        admin = new AdminMenu(this);
        npcs = new NpcManager(this);
        duels = new DuelManager(this);
        actions = new Actions(this);
        scoreboards.start();
        gui.start();
        stats.start();
        keyall.start();
        afkRewards.start();

        LandCommand land = new LandCommand(this);
        for (String n : List.of("land", "sb")) {
            PluginCommand pc = getCommand(n);
            if (pc != null) { pc.setExecutor(land); pc.setTabCompleter(land); }
        }
        GameCommands game = new GameCommands(this);
        for (String n : List.of("lobby", "setlobby", "afk", "setafk", "menu", "queue", "arena", "npc", "beheer",
                "beloningen", "setbeloningen", "keyall", "shards", "store", "buyalert", "toplijst", "rtp", "winkel", "setwinkel", "geld", "betaal", "geldtop", "geldbeheer", "bank", "dagelijks")) {
            PluginCommand pc = getCommand(n);
            if (pc != null) { pc.setExecutor(game); pc.setTabCompleter(game); }
        }

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(lobby, this);
        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(stats, this);
        getServer().getPluginManager().registerEvents(npcs, this);
        getServer().getPluginManager().registerEvents(duels, this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new LandenExpansion(this).register();
            getLogger().info("PlaceholderAPI-placeholders geregistreerd (%landen_land% e.d.).");
        }
    }

    /** Schrijft de ingebouwde config.yml bij de eerste start, zodat alles in dit ene bestand zit. */
    private void writeDefaultConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "config.yml");
        if (!f.exists()) {
            try {
                getDataFolder().mkdirs();
                java.nio.file.Files.writeString(f.toPath(), DefaultConfig.TEXT, java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                getLogger().severe("Kon config.yml niet maken: " + e.getMessage());
            }
        }
        reloadConfig();
    }

    @Override
    public void onDisable() {
        if (gui != null) gui.stop();
        if (keyall != null) keyall.stop();
        economyShutdown.run();
        if (afkRewards != null) afkRewards.stop();
        if (stats != null) stats.stop();
        if (duels != null) duels.shutdown();
        if (scoreboards != null) scoreboards.stop();
        if (countries != null) countries.save();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { scoreboards.remove(e.getPlayer()); }

    /** /afk altijd door deze plugin laten afhandelen (voor EssentialsX), zodra de AFK-zone is ingesteld. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAfkCommand(PlayerCommandPreprocessEvent e) {
        if (!getConfig().getBoolean("afk.override-essentials", true)) return;
        String m = e.getMessage().toLowerCase();
        if (!(m.equals("/afk") || m.startsWith("/afk "))) return;
        if (lobby.get("afk") == null) return; // geen zone: laat Essentials zijn gang gaan
        e.setCancelled(true);
        e.getPlayer().performCommand("landenscore:afk");
    }

    /** Met de eigen economie: /balance, /pay en /baltop gaan naar /geld, /betaal en /geldtop. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEconomyCommand(PlayerCommandPreprocessEvent e) {
        if (!ownEconomy || !getConfig().getBoolean("economy.override-commands", true)) return;
        String m = e.getMessage();
        int sp = m.indexOf(' ');
        String label = (sp < 0 ? m.substring(1) : m.substring(1, sp)).toLowerCase();
        String rest = sp < 0 ? "" : m.substring(sp);
        String to = switch (label) {
            case "balance", "bal", "money" -> "geld";
            case "pay" -> "betaal";
            case "baltop" -> "geldtop";
            default -> null;
        };
        if (to != null) e.setMessage("/" + to + rest);
    }

    public void msg(CommandSender s, String text) {
        s.sendMessage(Text.c(getConfig().getString("messages.prefix", "") + text));
    }

    public CountryManager countries() { return countries; }
    public ScoreboardManager scoreboards() { return scoreboards; }
    public LobbyManager lobby() { return lobby; }
    public StatsManager stats() { return stats; }
    public MoneyService money() { return money; }
    public BankMenu bank() { return bank; }
    public TopMenu topMenu() { return topMenu; }
    public void setOwnEconomyActive(boolean v) { ownEconomy = v; }
    public StoreMenu store() { return store; }
    public ShopManager shop() { return shop; }
    public ShopMenu shopMenu() { return shopMenu; }
    public RtpManager rtp() { return rtp; }
    public KeyallManager keyall() { return keyall; }
    public RewardManager rewards() { return rewards; }
    public RewardMenu rewardMenu() { return rewardMenu; }
    public GuiManager gui() { return gui; }
    public MenuManager menu() { return menu; }
    public AdminMenu admin() { return admin; }
    public NpcManager npcs() { return npcs; }
    public DuelManager duels() { return duels; }
    public Actions actions() { return actions; }
}

/**
 * Acties voor menu-items en NPC's:
 * world:<wereld> (Multiverse) | queue:1v1|2v2|tower | lobby[:naam] | cmd:<commando>
 * menu | rewards | daily | shop | store | bank | top[:money|shards|kills|deaths] | rtp[:wereld] | afk
 */
class Actions {
    private static final Set<String> TYPES = Set.of("world", "queue", "lobby", "cmd", "menu", "rewards", "daily",
            "shop", "store", "bank", "top", "rtp", "afk");

    private final LandenScore plugin;

    public Actions(LandenScore plugin) { this.plugin = plugin; }

    public static boolean valid(String action) {
        if (action == null) return false;
        return TYPES.contains(action.split(":", 2)[0].trim().toLowerCase());
    }

    private boolean busy(Player p) {
        if (plugin.duels().inMatch(p)) { plugin.msg(p, "&cJe zit in een match."); return true; }
        return false;
    }

    public void run(Player p, String action) {
        if (action == null || action.isBlank()) return;
        String[] parts = action.split(":", 2);
        String type = parts[0].trim().toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";
        switch (type) {
            case "world" -> {
                if (busy(p)) return;
                World w = Bukkit.getWorld(arg);
                if (w == null) { plugin.msg(p, "&cDe wereld &f" + arg + " &cbestaat niet of is niet geladen."); return; }
                plugin.duels().leaveQueue(p);
                p.teleport(w.getSpawnLocation());
            }
            case "queue" -> plugin.duels().queue(p, arg);
            case "lobby" -> {
                if (busy(p)) return;
                plugin.duels().leaveQueue(p);
                boolean ok = arg.isBlank() ? plugin.lobby().teleport(p) : plugin.lobby().teleport(p, arg);
                if (!ok) plugin.msg(p, "&cDie lobby is niet ingesteld.");
            }
            case "afk" -> {
                if (busy(p)) return;
                plugin.duels().leaveQueue(p);
                if (!plugin.lobby().teleport(p, "afk")) plugin.msg(p, "&cDe AFK-zone is nog niet ingesteld.");
            }
            case "menu" -> plugin.menu().open(p);
            case "rewards" -> plugin.rewardMenu().open(p);
            case "daily" -> plugin.rewardMenu().claimDaily(p);
            case "shop" -> plugin.shopMenu().open(p, 0);
            case "store" -> plugin.store().open(p);
            case "bank" -> plugin.bank().open(p);
            case "top" -> plugin.topMenu().open(p, arg.isBlank() ? "money" : arg);
            case "rtp" -> plugin.rtp().teleport(p, arg.isBlank() ? null : arg);
            case "cmd" -> p.performCommand(arg);
            default -> plugin.msg(p, "&cOnbekende actie: " + action);
        }
    }
}

/** Beheermenu voor de owner: arena's, lobby's, NPC's, landen, werelden, beloningen en keyall. */
class AdminMenu {
    private record Entry(ItemStack item, Consumer<InventoryClickEvent> click) {}

    private static final int PER_PAGE = 28;
    private final LandenScore plugin;

    public AdminMenu(LandenScore plugin) { this.plugin = plugin; }

    // ---------- hulpjes ----------
    private GuiManager.Page page(String title, int rows) {
        GuiManager.Page pg = new GuiManager.Page(title, rows, true);
        pg.border();
        return pg;
    }

    private void back(GuiManager.Page pg, int col, Runnable r) {
        pg.set((pg.rows - 1) * 9 + col, GuiManager.item(Material.ARROW, "&c« Terug", "&7Ga een stap terug."), e -> r.run());
    }

    private String nameOf(UUID id) {
        return Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName()).orElse("?");
    }

    private OfflinePlayer find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        return off.hasPlayedBefore() ? off : null;
    }

    private boolean validName(String n) { return n.matches("[A-Za-z0-9_]{1,24}"); }

    private void list(Player p, String title, List<Entry> entries, int page, Consumer<Integer> self,
                      Runnable back, ItemStack addItem, Consumer<InventoryClickEvent> addClick) {
        int pages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
        final int cur = Math.max(0, Math.min(page, pages - 1));
        GuiManager.Page pg = page(title, 6);
        int i = cur * PER_PAGE;
        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 7; c++) {
                if (i >= entries.size()) break;
                Entry en = entries.get(i++);
                pg.set(r * 9 + c, en.item(), en.click());
            }
        }
        if (entries.isEmpty()) pg.set(22, GuiManager.item(Material.BARRIER, "&7Nog niets hier"), null);
        if (pages > 1) pg.set(4, GuiManager.item(Material.PAPER, "&f&lPagina " + (cur + 1) + "/" + pages), null);
        if (cur > 0) pg.set(45 + 1, GuiManager.item(Material.SPECTRAL_ARROW, "&e« Vorige pagina"), e -> self.accept(cur - 1));
        if (cur < pages - 1) pg.set(45 + 7, GuiManager.item(Material.SPECTRAL_ARROW, "&eVolgende pagina »"), e -> self.accept(cur + 1));
        if (addItem != null) {
            back(pg, 3, back);
            pg.set(5 * 9 + 5, addItem, addClick);
        } else {
            back(pg, 4, back);
        }
        pg.open(p);
    }

    private void confirm(Player p, String what, Runnable yes, Runnable no) {
        GuiManager.Page pg = page("&8» &c&lWeet je het zeker? &8«", 3);
        pg.set(11, GuiManager.glow(Material.LIME_CONCRETE, "&a&lJa", "&7" + what), e -> yes.run());
        pg.set(13, GuiManager.item(Material.PAPER, "&f" + what), null);
        pg.set(15, GuiManager.item(Material.RED_CONCRETE, "&c&lNee"), e -> no.run());
        pg.open(p);
    }

    // ---------- hoofdmenu ----------
    public void main(Player p) {
        GuiManager.Page pg = page("&8» <gb:#ff9966:#ff5e62>BEHEER</g> &8«", 6);
        pg.set(13, GuiManager.glow(Material.NETHER_STAR, "&6&lServer beheer",
                "&7Alles op één plek.", "",
                "&7Arena's: &f" + plugin.duels().arenas().size(),
                "&7Lobby's: &f" + plugin.lobby().names().size(),
                "&7Landen: &f" + plugin.countries().all().size(),
                "&7Beloningen: &f" + plugin.rewards().all().size(),
                "&7In een match: &f" + plugin.duels().playing(),
                "&7Volgende keyall: &f" + Fmt.time(plugin.keyall().remainingSeconds())), null);
        pg.set(11, GuiManager.glow(Material.EMERALD, "&a&lWinkel", "&7Items verkopen voor geld, diamanten", "&7of shards.", "", "&eKlik om te beheren"), e -> shop(p, 0));
        pg.set(20, GuiManager.item(Material.IRON_SWORD, "&c&lArena's", "&7PvP en Tower Drop beheren.", "", "&eKlik om te openen"), e -> arenas(p, 0));
        pg.set(22, GuiManager.item(Material.RED_BED, "&a&lLobby's", "&7Lobby's en de AFK-zone.", "", "&eKlik om te openen"), e -> lobbies(p, 0));
        pg.set(24, GuiManager.item(Material.VILLAGER_SPAWN_EGG, "&b&lNPC's", "&7NPC's plaatsen en verwijderen.", "", "&eKlik om te openen"), e -> npcs(p));
        pg.set(29, GuiManager.item(Material.WHITE_BANNER, "&e&lLanden", "&7Landen aanmaken en verwijderen.", "", "&eKlik om te openen"), e -> lands(p, 0));
        pg.set(31, GuiManager.item(Material.GRASS_BLOCK, "&2&lWerelden", "&7Teleporteer naar een (Multiverse-)wereld.", "", "&eKlik om te openen"), e -> worlds(p, 0));
        pg.set(33, GuiManager.glow(Material.ENDER_CHEST, "&6&lBeloningen", "&7Maak beloningen met crate-sleutels,", "&7LuckPerms-ranks, diamanten en geld.", "", "&eKlik om te openen"), e -> rewards(p, 0));
        pg.set(38, GuiManager.glow(Material.TRIPWIRE_HOOK, "&b&lKeyall", "&7Timer en commando's voor de keyall.", "", "&eKlik om te openen"), e -> keyall(p));
        pg.set(40, GuiManager.item(Material.REPEATING_COMMAND_BLOCK, "&d&lConfig herladen", "&7Laadt config.yml opnieuw.", "", "&eKlik om te herladen"), e -> {
            plugin.reloadConfig();
            plugin.scoreboards().start();
            plugin.msg(p, "Config herladen.");
            main(p);
        });
        pg.set(42, GuiManager.item(Material.COMPASS, "&a&lSpelersmenu", "&7Bekijk het menu zoals spelers het zien."), e -> plugin.menu().open(p));
        pg.open(p);
    }

    // ---------- arena's ----------
    private Material icon(String mode) {
        return switch (mode) {
            case "2v2" -> Material.DIAMOND_SWORD;
            case "tower" -> Material.BRICKS;
            default -> Material.IRON_SWORD;
        };
    }

    public void arenas(Player p, int page) {
        List<Entry> en = new ArrayList<>();
        for (DuelManager.Arena a : plugin.duels().arenas()) {
            en.add(new Entry(GuiManager.item(icon(a.mode), "&f&l" + a.name,
                    "&7Modus: &f" + a.mode,
                    "&7Spawns: &f" + a.sideA.size() + (a.tower() ? "" : " / " + a.sideB.size()),
                    "&7Status: " + (a.ready() ? "&aklaar" : "&cniet klaar") + (a.active != null ? " &e(bezet)" : ""),
                    "", "&eKlik om te beheren"), e -> arena(p, a)));
        }
        list(p, "&8» &c&lArena's &8«", en, page, pg -> arenas(p, pg), () -> main(p),
                GuiManager.glow(Material.LIME_DYE, "&a&l+ Nieuwe arena", "&7Kies een modus en een naam."), e -> newArena(p));
    }

    private void newArena(Player p) {
        GuiManager.Page pg = page("&8» &a&lNieuwe arena &8«", 3);
        modeButton(pg, p, 11, Material.IRON_SWORD, "&c&lPvP 1v1", "1v1");
        modeButton(pg, p, 13, Material.DIAMOND_SWORD, "&6&lPvP 2v2", "2v2");
        modeButton(pg, p, 15, Material.BRICKS, "&e&lTower Drop", "tower");
        back(pg, 4, () -> arenas(p, 0));
        pg.open(p);
    }

    private void modeButton(GuiManager.Page pg, Player p, int slot, Material m, String label, String mode) {
        pg.set(slot, GuiManager.glow(m, label, "&7Modus: &f" + mode, "", "&eKlik om te kiezen"), e ->
                plugin.gui().prompt(p, "&eTyp de naam van de arena (letters/cijfers):", name -> {
                    if (!validName(name) || plugin.duels().arena(name) != null) {
                        plugin.msg(p, "&cOngeldige of bestaande naam.");
                        arenas(p, 0);
                        return;
                    }
                    DuelManager.Arena a = plugin.duels().createArena(name, mode);
                    plugin.msg(p, "Arena &f" + name + " &7aangemaakt. Zet gouden blokken neer en klik op &6Scan&7.");
                    arena(p, a);
                }));
    }

    public void arena(Player p, DuelManager.Arena a) {
        GuiManager.Page pg = page("&8» &f&l" + a.name + " &8«", 4);
        int radius = plugin.getConfig().getInt("arena.scan-radius", 60);
        pg.set(11, GuiManager.glow(Material.GOLD_BLOCK, "&6&lScan gouden blokken",
                "&7Zet gouden blokken neer: één per spawn.",
                a.tower() ? "&7Tower: elk blok is een toren." : "&7De ene helft wordt kant A, de andere kant B.",
                "&7Sta in het midden van de arena.", "&7Radius: &f" + radius, "", "&eKlik om te scannen"), e -> {
            plugin.msg(p, plugin.duels().scan(a, p.getLocation(), radius));
            arena(p, a);
        });
        pg.set(13, GuiManager.item(Material.ENDER_PEARL, "&b&lTeleporteer", "&7Naar de eerste spawn.", "", "&eKlik"), e -> {
            Location l = a.sideA.isEmpty() ? null : a.sideA.get(0).toLocation();
            if (l == null) plugin.msg(p, "&cDeze arena heeft nog geen spawn."); else p.teleport(l);
        });
        pg.set(15, GuiManager.item(Material.PAPER, "&f&lInfo",
                "&7Modus: &f" + a.mode,
                "&7Spawns A: &f" + a.sideA.size(),
                a.tower() ? "&7(Tower: alle spawns staan bij A)" : "&7Spawns B: &f" + a.sideB.size(),
                "&7Status: " + (a.ready() ? "&aklaar" : "&cniet klaar")), null);
        pg.set(20, GuiManager.item(Material.LIGHT_BLUE_WOOL, "&b&lSpawn hier toevoegen" + (a.tower() ? "" : " (kant A)"),
                "&7Handmatig, op jouw plek.", "", "&eKlik"), e -> {
            plugin.duels().addSpawn(a, 1, p.getLocation());
            plugin.msg(p, "Spawn toegevoegd.");
            arena(p, a);
        });
        pg.set(22, GuiManager.item(Material.BARRIER, "&c&lVerwijder arena", "&7Kan niet tijdens een match.", "", "&eKlik"), e ->
                confirm(p, "Arena " + a.name + " verwijderen", () -> {
                    plugin.msg(p, plugin.duels().deleteArena(a.name) ? "Arena verwijderd." : "&cNu in gebruik.");
                    arenas(p, 0);
                }, () -> arena(p, a)));
        if (!a.tower()) {
            pg.set(24, GuiManager.item(Material.ORANGE_WOOL, "&6&lSpawn hier toevoegen (kant B)",
                    "&7Handmatig, op jouw plek.", "", "&eKlik"), e -> {
                plugin.duels().addSpawn(a, 2, p.getLocation());
                plugin.msg(p, "Spawn toegevoegd.");
                arena(p, a);
            });
        }
        back(pg, 4, () -> arenas(p, 0));
        pg.open(p);
    }

    // ---------- lobby's ----------
    public void lobbies(Player p, int page) {
        List<Entry> en = new ArrayList<>();
        for (String name : new ArrayList<>(plugin.lobby().names())) {
            Location l = plugin.lobby().get(name);
            String where = l == null ? "&cwereld niet geladen" : "&f" + l.getWorld().getName() + " &7(" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ")";
            Material icon = name.equals("main") ? Material.NETHER_STAR : name.equals("afk") ? Material.CLOCK : Material.RED_BED;
            en.add(new Entry(GuiManager.item(icon, "&a&l" + name,
                    "&7Plek: " + where, "", "&eLinksklik: teleporteer", "&cShift+rechtsklik: verwijderen"), e -> {
                if (e.isShiftClick() && e.isRightClick()) {
                    confirm(p, "Lobby " + name + " verwijderen", () -> { plugin.lobby().delete(name); lobbies(p, 0); }, () -> lobbies(p, 0));
                } else {
                    p.closeInventory();
                    plugin.lobby().teleport(p, name);
                }
            }));
        }
        list(p, "&8» &a&lLobby's &8«", en, page, pg -> lobbies(p, pg), () -> main(p),
                GuiManager.glow(Material.LIME_DYE, "&a&l+ Nieuwe lobby hier", "&7Op jouw huidige plek.", "&7'main' = hoofdlobby, 'afk' = AFK-zone."),
                e -> plugin.gui().prompt(p, "&eTyp de naam van de lobby (bv. main, pvp, minigames, afk):", name -> {
                    if (!validName(name)) { plugin.msg(p, "&cOngeldige naam."); lobbies(p, 0); return; }
                    plugin.lobby().set(name, p.getLocation());
                    plugin.msg(p, "Lobby &f" + name.toLowerCase() + " &7ingesteld.");
                    lobbies(p, 0);
                }));
    }

    // ---------- NPC's ----------
    public void npcs(Player p) {
        GuiManager.Page pg = page("&8» &b&lNPC's &8«", 3);
        pg.set(11, GuiManager.glow(Material.LIME_DYE, "&a&l+ Nieuwe NPC hier", "&7Kies een actie en een naam.", "", "&eKlik"), e -> npcAction(p));
        pg.set(15, GuiManager.item(Material.BARRIER, "&c&lDichtstbijzijnde NPC verwijderen", "&7Binnen 5 blokken van jou.", "", "&eKlik"), e -> {
            Villager v = plugin.npcs().nearest(p);
            if (v == null) plugin.msg(p, "&cGeen NPC binnen 5 blokken."); else { v.remove(); plugin.msg(p, "NPC verwijderd."); }
            npcs(p);
        });
        back(pg, 4, () -> main(p));
        pg.open(p);
    }

    private void npcAction(Player p) {
        GuiManager.Page pg = page("&8» &b&lWat doet de NPC? &8«", 6);
        String[][] acts = {
                {"IRON_SWORD", "&c&lPvP 1v1", "queue:1v1", "Zet spelers in de 1v1-wachtrij."},
                {"DIAMOND_SWORD", "&6&lPvP 2v2", "queue:2v2", "Zet spelers in de 2v2-wachtrij."},
                {"BRICKS", "&e&lTower Drop", "queue:tower", "Zet spelers in de Tower Drop-wachtrij."},
                {"COMPASS", "&a&lSpelmenu", "menu", "Opent het grote spelmenu."},
                {"ENDER_CHEST", "&6&lBeloningen", "rewards", "Opent het beloningenmenu."},
                {"GOLD_INGOT", "&e&lDagelijkse beloning", "daily", "Claimt meteen de dagelijkse beloning."},
                {"EMERALD", "&a&lWinkel", "shop", "Opent de winkel."},
                {"NETHER_STAR", "&6&lWebshop", "store", "Opent de webshop met links."},
                {"GOLD_BLOCK", "&a&lBank", "bank", "Opent het economie-menu."},
                {"GOLDEN_HELMET", "&e&lToplijst", "top", "Opent de toplijsten."},
                {"ENDER_PEARL", "&5&lRTP", "rtp", "Willekeurig teleporteren."},
                {"CLOCK", "&7&lAFK-zone", "afk", "Naar de AFK-zone."},
        };
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23};
        for (int i = 0; i < acts.length; i++) {
            String action = acts[i][2];
            pg.set(slots[i], GuiManager.item(Material.valueOf(acts[i][0]), acts[i][1], "&7" + acts[i][3], "", "&eKlik om te kiezen"), e -> chooseName(p, action));
        }
        pg.set(30, GuiManager.glow(Material.RED_BED, "&d&lNaar een lobby...", "&7Kies daarna de lobby."), e -> lobbyPicker(p, 0));
        pg.set(32, GuiManager.glow(Material.GRASS_BLOCK, "&2&lNaar een wereld (Multiverse)...", "&7Kies daarna de wereld."), e -> worldPicker(p, 0));
        back(pg, 4, () -> npcs(p));
        pg.open(p);
    }

    private void lobbyPicker(Player p, int page) {
        List<Entry> en = new ArrayList<>();
        for (String n : new ArrayList<>(plugin.lobby().names()))
            en.add(new Entry(GuiManager.item(Material.RED_BED, "&a&l" + n, "&eKlik om te kiezen"), ev -> chooseName(p, "lobby:" + n)));
        list(p, "&8» &d&lKies een lobby &8«", en, page, pg -> lobbyPicker(p, pg), () -> npcAction(p), null, null);
    }

    private void worldPicker(Player p, int page) {
        List<Entry> en = new ArrayList<>();
        for (World w : Bukkit.getWorlds())
            en.add(new Entry(GuiManager.item(worldIcon(w), "&2&l" + w.getName(), "&eKlik om te kiezen"), ev -> chooseName(p, "world:" + w.getName())));
        list(p, "&8» &2&lKies een wereld &8«", en, page, pg -> worldPicker(p, pg), () -> npcAction(p), null, null);
    }

    private void chooseName(Player p, String action) {
        plugin.gui().prompt(p, "&eTyp de naam van de NPC (kleurcodes met &, spaties met _):", name -> {
            plugin.npcs().create(p.getLocation(), name.replace('_', ' '), action);
            plugin.msg(p, "NPC aangemaakt (&f" + action + "&7).");
            npcs(p);
        });
    }

    // ---------- landen ----------
    public void lands(Player p, int page) {
        List<Entry> en = new ArrayList<>();
        for (Country c : new ArrayList<>(plugin.countries().all())) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Leider: &f" + nameOf(c.owner));
            lore.add("&7Leden: &f" + c.members.size());
            c.members.stream().limit(5).forEach(id -> lore.add("&8 - &7" + nameOf(id)));
            lore.add("");
            lore.add("&cShift+rechtsklik: verwijderen");
            en.add(new Entry(GuiManager.build(Material.WHITE_BANNER, "&f&l" + c.name, lore, false), e -> {
                if (e.isShiftClick() && e.isRightClick())
                    confirm(p, "Land " + c.name + " verwijderen", () -> { plugin.countries().delete(c); lands(p, 0); }, () -> lands(p, 0));
            }));
        }
        list(p, "&8» &e&lLanden &8«", en, page, pg -> lands(p, pg), () -> main(p),
                GuiManager.glow(Material.LIME_DYE, "&a&l+ Nieuw land", "&7Kies een naam en een leider."),
                e -> plugin.gui().prompt(p, "&eTyp de naam van het nieuwe land:", name -> {
                    if (!name.matches("[A-Za-z0-9_]{1,16}") || plugin.countries().get(name) != null) {
                        plugin.msg(p, "&cOngeldige of bestaande naam.");
                        lands(p, 0);
                        return;
                    }
                    plugin.gui().prompt(p, "&eTyp de naam van de leider (speler):", leader -> {
                        OfflinePlayer t = find(leader);
                        if (t == null || plugin.countries().ofPlayer(t.getUniqueId()) != null) {
                            plugin.msg(p, "&cSpeler niet gevonden of zit al in een land.");
                            lands(p, 0);
                            return;
                        }
                        plugin.countries().create(name, t.getUniqueId());
                        plugin.msg(p, "Land &f" + name + " &7aangemaakt.");
                        lands(p, 0);
                    });
                }));
    }

    // ---------- werelden ----------
    private Material worldIcon(World w) {
        return switch (w.getEnvironment()) {
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }

    public void worlds(Player p, int page) {
        List<Entry> en = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            en.add(new Entry(GuiManager.item(worldIcon(w), "&2&l" + w.getName(),
                    "&7Spelers: &f" + w.getPlayers().size(), "", "&eKlik om te teleporteren"), e -> {
                p.closeInventory();
                p.teleport(w.getSpawnLocation());
            }));
        }
        list(p, "&8» &2&lWerelden &8«", en, page, pg -> worlds(p, pg), () -> main(p), null, null);
    }

    // ---------- beloningen ----------
    private String cd(RewardManager.Reward r) { return r.cooldown < 0 ? "eenmalig" : Fmt.time(r.cooldown); }

    public void rewards(Player p, int page) {
        List<Entry> en = new ArrayList<>();
        for (RewardManager.Reward r : plugin.rewards().all()) {
            en.add(new Entry(GuiManager.item(r.icon, "&f" + r.name,
                    "&7Cooldown: &f" + cd(r), "&7Commando's: &f" + r.commands.size(),
                    "&7Rechten: &f" + (r.permission.isBlank() ? "geen" : r.permission),
                    "", "&eKlik om te bewerken"), e -> reward(p, r)));
        }
        list(p, "&8» <gb:#f6d365:#fda085>BELONINGEN BEHEREN</g> &8«", en, page, pg -> rewards(p, pg), () -> main(p),
                GuiManager.glow(Material.LIME_DYE, "&a&l+ Nieuwe beloning", "&7Kies een naam."),
                e -> plugin.gui().prompt(p, "&eTyp de naam van de beloning (kleurcodes met &):", name -> {
                    RewardManager.Reward r = plugin.rewards().create(name);
                    plugin.msg(p, "Beloning aangemaakt. Voeg nu commando's toe.");
                    reward(p, r);
                }));
    }

    private void edit(Player p, RewardManager.Reward r, String question, Consumer<String> apply) {
        plugin.gui().prompt(p, question, text -> {
            apply.accept(text);
            plugin.rewards().save();
            reward(p, r);
        });
    }

    public void reward(Player p, RewardManager.Reward r) {
        GuiManager.Page pg = page("&8» &6&lBeloning bewerken &8«", 5);
        pg.set(11, GuiManager.glow(r.icon, "&e&lIcoon", "&7Houd een item in je hand en klik", "&7om het icoon te veranderen.", "", "&eKlik"), e -> {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) plugin.msg(p, "&cHoud eerst een item in je hand.");
            else { r.icon = hand.getType(); plugin.rewards().save(); plugin.msg(p, "Icoon aangepast."); }
            reward(p, r);
        });
        pg.set(13, GuiManager.item(Material.NAME_TAG, "&e&lNaam", "&7Nu: &f" + r.name, "", "&eKlik om te wijzigen"), e ->
                edit(p, r, "&eTyp de nieuwe naam (kleurcodes met &):", t -> r.name = t));
        pg.set(15, GuiManager.item(Material.CLOCK, "&e&lCooldown", "&7Nu: &f" + cd(r), "", "&eKlik om te wijzigen"), e ->
                edit(p, r, "&eTyp de cooldown (bv. 24h, 7d, 1d12h of 'eenmalig'):", t -> {
                    long v = Fmt.parseDuration(t);
                    if (v == -2) plugin.msg(p, "&cOngeldige tijd."); else r.cooldown = v;
                }));
        pg.set(20, GuiManager.item(Material.BOOK, "&e&lBeschrijving",
                "&7Nu: &f" + (r.description.isBlank() ? "(leeg)" : r.description), "&7Gebruik | voor een nieuwe regel.", "", "&eKlik om te wijzigen"), e ->
                edit(p, r, "&eTyp de beschrijving:", t -> r.description = t));
        List<String> cmds = new ArrayList<>();
        cmds.add("&7Aantal: &f" + r.commands.size());
        for (String c : r.commands.stream().limit(8).toList()) cmds.add("&8 - &f" + c);
        cmds.add("");
        cmds.add("&cShift+rechtsklik: alles wissen");
        pg.set(22, GuiManager.build(Material.COMMAND_BLOCK, "&e&lCommando's", cmds, false), e -> {
            if (e.isShiftClick() && e.isRightClick()) {
                r.commands.clear();
                plugin.rewards().save();
                reward(p, r);
            }
        });
        pg.set(24, GuiManager.item(Material.SHIELD, "&e&lPermission", "&7Nu: &f" + (r.permission.isBlank() ? "geen" : r.permission),
                "&7Alleen spelers met deze permission", "&7kunnen de beloning claimen.", "", "&eKlik om te wijzigen"), e ->
                edit(p, r, "&eTyp de permission (of 'geen'):", t -> r.permission = t.equalsIgnoreCase("geen") ? "" : t));
        pg.set(29, GuiManager.glow(Material.LIME_DYE, "&a&l+ Commando toevoegen", "&7Crate-sleutel, LuckPerms-rank,", "&7diamanten, geld of eigen commando.", "", "&eKlik"), e ->
                templates(p, cmd -> {
                    if (cmd != null) { r.commands.add(cmd); plugin.rewards().save(); plugin.msg(p, "Commando toegevoegd: &f" + cmd); }
                    reward(p, r);
                }, () -> reward(p, r)));
        pg.set(31, GuiManager.item(Material.EMERALD, "&a&lTest op mezelf", "&7Voert de commando's nu uit voor jou", "&7(zonder cooldown).", "", "&eKlik"), e -> {
            plugin.rewards().run(p.getName(), r);
            plugin.msg(p, "Test uitgevoerd.");
        });
        pg.set(33, GuiManager.item(Material.BARRIER, "&c&lVerwijderen", "&7Verwijdert deze beloning.", "", "&eKlik"), e ->
                confirm(p, "Beloning verwijderen", () -> { plugin.rewards().delete(r.id); rewards(p, 0); }, () -> reward(p, r)));
        back(pg, 4, () -> rewards(p, 0));
        pg.open(p);
    }

    // ---------- commando-sjablonen (crate / LuckPerms / diamanten / geld / eigen) ----------
    private String tpl(String key, String def) { return plugin.getConfig().getString("rewards.templates." + key, def); }

    /** add krijgt het commando, of null bij annuleren/ongeldig. */
    private void templates(Player p, Consumer<String> add, Runnable back) {
        GuiManager.Page pg = page("&8» &d&lCommando toevoegen &8«", 5);
        pg.set(20, GuiManager.glow(Material.TRIPWIRE_HOOK, "&6&lCrate-sleutel", "&7Geeft sleutels (Phoenix Crates).", "&7Commando staat in config:", "&8rewards.templates.crate-key", "", "&eKlik"), e -> crateCmd(p, add));
        pg.set(22, GuiManager.glow(Material.NAME_TAG, "&d&lLuckPerms-rank", "&7Geeft een rank (blijvend of tijdelijk).", "", "&eKlik"), e -> rankCmd(p, add));
        pg.set(24, GuiManager.glow(Material.AMETHYST_SHARD, "&d&lShards", "&7Geeft shards (eigen valuta).", "", "&eKlik"), e ->
                amountCmd(p, add, tpl("diamonds", "shards give {player} {amount}"), "Hoeveel shards?"));
        pg.set(33, GuiManager.glow(Material.DIAMOND, "&b&lDiamanten (items)", "&7Geeft echte diamanten.", "", "&eKlik"), e ->
                amountCmd(p, add, tpl("diamond-item", "give {player} diamond {amount}"), "Hoeveel diamanten?"));
        pg.set(29, GuiManager.glow(Material.GOLD_INGOT, "&a&lGeld", "&7Geeft geld (EssentialsX /eco).", "", "&eKlik"), e ->
                amountCmd(p, add, tpl("money", "eco give {player} {amount}"), "Hoeveel geld?"));
        pg.set(31, GuiManager.item(Material.COMMAND_BLOCK, "&f&lEigen commando", "&7Typ zelf een console-commando.", "&7Gebruik {player} voor de speler.", "", "&eKlik"), e ->
                plugin.gui().prompt(p, "&eTyp het commando zonder / (gebruik {player}):", t -> {
                    String cmd = t.startsWith("/") ? t.substring(1) : t;
                    add.accept(cmd.isBlank() ? null : cmd);
                }));
        back(pg, 4, back);
        pg.open(p);
    }

    private void crateCmd(Player p, Consumer<String> add) {
        plugin.gui().prompt(p, "&eTyp de naam van de crate (zoals in Phoenix Crates):", crate -> {
            if (!crate.matches("[A-Za-z0-9_\\-]{1,32}")) { plugin.msg(p, "&cOngeldige crate-naam."); add.accept(null); return; }
            plugin.gui().prompt(p, "&eHoeveel sleutels? (getal):", amt -> {
                if (!amt.matches("[0-9]{1,6}") || Integer.parseInt(amt) <= 0) { plugin.msg(p, "&cOngeldig aantal."); add.accept(null); return; }
                add.accept(tpl("crate-key", "crate key give {player} {crate} {amount}").replace("{crate}", crate).replace("{amount}", amt));
            });
        });
    }

    private void rankCmd(Player p, Consumer<String> add) {
        plugin.gui().prompt(p, "&eTyp de LuckPerms-groep (bv. vip):", group -> {
            if (!group.matches("[A-Za-z0-9_\\-]{1,32}")) { plugin.msg(p, "&cOngeldige groepsnaam."); add.accept(null); return; }
            plugin.gui().prompt(p, "&eTyp de duur (bv. 7d, 1mo) of 'perm' voor blijvend:", dur -> {
                if (dur.equalsIgnoreCase("perm")) {
                    add.accept(tpl("lp-rank", "lp user {player} parent add {group}").replace("{group}", group));
                } else if (Fmt.parseDuration(dur) > 0) {
                    add.accept(tpl("lp-rank-temp", "lp user {player} parent addtemp {group} {duration}")
                            .replace("{group}", group).replace("{duration}", dur.toLowerCase()));
                } else {
                    plugin.msg(p, "&cOngeldige duur.");
                    add.accept(null);
                }
            });
        });
    }

    private void amountCmd(Player p, Consumer<String> add, String template, String question) {
        plugin.gui().prompt(p, "&e" + question + " (getal):", amt -> {
            if (!amt.matches("[0-9]{1,9}") || Long.parseLong(amt) <= 0) { plugin.msg(p, "&cOngeldig aantal."); add.accept(null); return; }
            add.accept(template.replace("{amount}", amt));
        });
    }

    // ---------- keyall ----------
    public void keyall(Player p) {
        KeyallManager k = plugin.keyall();
        GuiManager.Page pg = page("&8» <gb:#43e97b:#38f9d7>KEYALL</g> &8«", 5);
        List<String> info = new ArrayList<>();
        info.add("&7Elke &f" + k.interval() + " &7minuten.");
        info.add("&7Volgende over: &f" + Fmt.time(k.remainingSeconds()));
        info.add("&7Commando's: &f" + k.commands().size());
        for (String c : k.commands().stream().limit(8).toList()) info.add("&8 - &f" + c);
        pg.set(13, GuiManager.build(Material.CLOCK, "&b&lKeyall", info, true), null);
        pg.set(20, GuiManager.item(Material.REPEATER, "&e&lInterval", "&7Nu: &f" + k.interval() + " minuten", "", "&eKlik om te wijzigen"), e ->
                plugin.gui().prompt(p, "&eTyp het aantal minuten tussen keyalls:", t -> {
                    if (!t.matches("[0-9]{1,5}") || Integer.parseInt(t) < 1) plugin.msg(p, "&cOngeldig getal.");
                    else { k.setInterval(Integer.parseInt(t)); plugin.msg(p, "Interval aangepast."); }
                    keyall(p);
                }));
        pg.set(22, GuiManager.glow(Material.LIME_DYE, "&a&l+ Commando toevoegen", "&7Wordt uitgevoerd voor elke speler.", "", "&eKlik"), e ->
                templates(p, cmd -> {
                    if (cmd != null) { k.addCommand(cmd); plugin.msg(p, "Commando toegevoegd: &f" + cmd); }
                    keyall(p);
                }, () -> keyall(p)));
        pg.set(24, GuiManager.item(Material.BARRIER, "&c&lCommando's wissen", "&7Verwijdert alle keyall-commando's.", "", "&eKlik"), e ->
                confirm(p, "Alle keyall-commando's wissen", () -> { k.clearCommands(); keyall(p); }, () -> keyall(p)));
        pg.set(31, GuiManager.glow(Material.FIREWORK_ROCKET, "&6&lNu starten", "&7Start de keyall meteen", "&7en zet de timer opnieuw.", "", "&eKlik"), e ->
                confirm(p, "Keyall nu starten", () -> { k.fire(); keyall(p); }, () -> keyall(p)));
        back(pg, 4, () -> main(p));
        pg.open(p);
    }

    // ---------- winkel ----------
    public void shop(Player p, int page) {
        List<Entry> en = new ArrayList<>();
        for (ShopManager.Item it : plugin.shop().all()) {
            en.add(new Entry(GuiManager.item(it.icon, "&f" + it.name,
                    "&7Prijs: " + plugin.shop().priceLabel(it), "&7Commando's: &f" + it.commands.size(),
                    "", "&eKlik om te bewerken"), e -> shopItem(p, it)));
        }
        list(p, "&8» &a&lWinkel beheren &8«", en, page, pg -> shop(p, pg), () -> main(p),
                GuiManager.glow(Material.LIME_DYE, "&a&l+ Nieuw item", "&7Kies een naam."),
                e -> plugin.gui().prompt(p, "&eTyp de naam van het item (kleurcodes met &):", name -> {
                    ShopManager.Item it = plugin.shop().create(name);
                    plugin.msg(p, "Item aangemaakt. Stel prijs en commando's in.");
                    shopItem(p, it);
                }));
    }

    private void editShop(Player p, ShopManager.Item it, String question, Consumer<String> apply) {
        plugin.gui().prompt(p, question, text -> {
            apply.accept(text);
            plugin.shop().save();
            shopItem(p, it);
        });
    }

    public void shopItem(Player p, ShopManager.Item it) {
        GuiManager.Page pg = page("&8» &a&lItem bewerken &8«", 5);
        pg.set(11, GuiManager.glow(it.icon, "&e&lIcoon", "&7Houd een item in je hand en klik.", "", "&eKlik"), e -> {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) plugin.msg(p, "&cHoud eerst een item in je hand.");
            else { it.icon = hand.getType(); plugin.shop().save(); plugin.msg(p, "Icoon aangepast."); }
            shopItem(p, it);
        });
        pg.set(13, GuiManager.item(Material.NAME_TAG, "&e&lNaam", "&7Nu: &f" + it.name, "", "&eKlik om te wijzigen"), e ->
                editShop(p, it, "&eTyp de nieuwe naam (kleurcodes met &):", t -> it.name = t));
        pg.set(15, GuiManager.item(Material.GOLD_NUGGET, "&e&lPrijs", "&7Nu: " + plugin.shop().priceLabel(it), "", "&eKlik om te wijzigen"), e ->
                editShop(p, it, "&eTyp de prijs (getal):", t -> {
                    if (!t.matches("[0-9]{1,12}") || Long.parseLong(t) <= 0) plugin.msg(p, "&cOngeldig getal."); else it.price = Long.parseLong(t);
                }));
        pg.set(20, GuiManager.glow(Material.EMERALD, "&e&lBetaalmiddel", "&7Nu: &f" + plugin.shop().curName(it.currency),
                "&7Geld, echte diamanten of shards.", "", "&eKlik om te wisselen"), e -> {
            it.currency = it.currency.equals("money") ? "diamond" : it.currency.equals("diamond") ? "shards" : "money";
            plugin.shop().save();
            shopItem(p, it);
        });
        List<String> cmds = new ArrayList<>();
        cmds.add("&7Aantal: &f" + it.commands.size());
        for (String c : it.commands.stream().limit(8).toList()) cmds.add("&8 - &f" + c);
        cmds.add("");
        cmds.add("&cShift+rechtsklik: alles wissen");
        pg.set(22, GuiManager.build(Material.COMMAND_BLOCK, "&e&lCommando's", cmds, false), e -> {
            if (e.isShiftClick() && e.isRightClick()) {
                it.commands.clear();
                plugin.shop().save();
                shopItem(p, it);
            }
        });
        pg.set(24, GuiManager.item(Material.SHIELD, "&e&lPermission", "&7Nu: &f" + (it.permission.isBlank() ? "geen" : it.permission), "", "&eKlik om te wijzigen"), e ->
                editShop(p, it, "&eTyp de permission (of 'geen'):", t -> it.permission = t.equalsIgnoreCase("geen") ? "" : t));
        pg.set(29, GuiManager.glow(Material.LIME_DYE, "&a&l+ Commando toevoegen", "&7Crate-sleutel, LuckPerms-rank,", "&7shards, diamanten, geld of eigen.", "", "&eKlik"), e ->
                templates(p, cmd -> {
                    if (cmd != null) { it.commands.add(cmd); plugin.shop().save(); plugin.msg(p, "Commando toegevoegd: &f" + cmd); }
                    shopItem(p, it);
                }, () -> shopItem(p, it)));
        pg.set(31, GuiManager.item(Material.BOOK, "&e&lBeschrijving", "&7Nu: &f" + (it.description.isBlank() ? "(leeg)" : it.description),
                "&7Gebruik | voor een nieuwe regel.", "", "&eKlik om te wijzigen"), e ->
                editShop(p, it, "&eTyp de beschrijving:", t -> it.description = t));
        pg.set(33, GuiManager.item(Material.EMERALD_BLOCK, "&a&lTest op mezelf", "&7Voert de commando's uit, zonder te betalen.", "", "&eKlik"), e -> {
            plugin.shop().run(p.getName(), it);
            plugin.msg(p, "Test uitgevoerd.");
        });
        pg.set(41, GuiManager.item(Material.BARRIER, "&c&lVerwijderen", "&7Haalt dit item uit de winkel.", "", "&eKlik"), e ->
                confirm(p, "Item verwijderen", () -> { plugin.shop().delete(it.id); shop(p, 0); }, () -> shopItem(p, it)));
        back(pg, 3, () -> shop(p, 0));
        pg.open(p);
    }
}

/** Spelers in de AFK-zone krijgen elke minuut diamanten. */
class AfkRewards {
    private final LandenScore plugin;
    private BukkitTask task;

    public AfkRewards(LandenScore plugin) { this.plugin = plugin; }

    public void start() { task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1200L, 1200L); }

    public void stop() { if (task != null) task.cancel(); }

    private void tick() {
        int n = plugin.getConfig().getInt("afk.shards-per-minute", 1);
        if (n <= 0) return;
        double r = plugin.getConfig().getDouble("afk.radius", 20);
        Location zone = plugin.lobby().get("afk");
        if (zone == null || zone.getWorld() == null) return;
        String cur = plugin.getConfig().getString("currency.name", "Diamanten").toLowerCase();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(zone.getWorld())) continue;
            if (p.getLocation().distanceSquared(zone) > r * r) continue;
            plugin.stats().addShards(p.getUniqueId(), n);
            p.sendActionBar(Text.c("&b+" + n + " " + cur + " &7voor AFK zijn"));
        }
    }
}

/** Groot economie-menu: saldo, betalen, dagelijkse beloning, toplijst en snelkoppelingen. */
class BankMenu {
    private static final int PER_PAGE = 28;
    private final LandenScore plugin;

    public BankMenu(LandenScore plugin) { this.plugin = plugin; }

    public void open(Player p) {
        GuiManager.Page pg = new GuiManager.Page("&8» <gb:#f6d365:#fda085>BANK</g> &8«", 6, true);
        pg.border();
        Runnable paint = () -> pg.inv.setItem(13, GuiManager.build(Material.GOLD_BLOCK, "&6&lJouw bank", List.of(
                "&a$ &7Money: " + plugin.shop().balanceLabel(p, "money"),
                "&d" + plugin.getConfig().getString("currency.symbol", "✦") + " &7" + plugin.getConfig().getString("currency.name", "Shards") + ": " + plugin.shop().balanceLabel(p, "shards"),
                "&b◆ &7Diamanten (items): " + plugin.shop().balanceLabel(p, "diamond")), true));
        paint.run();
        pg.set(13, pg.inv.getItem(13), null);
        pg.refresh = paint;
        pg.set(20, GuiManager.item(Material.PAPER, "&a&lBetalen", "&7Betaal geld aan een andere speler.", "", "&eKlik om een speler te kiezen"), e -> pickPlayer(p, 0));
        pg.set(22, GuiManager.glow(Material.CLOCK, "&e&lDagelijkse beloning", "&7Claim je dagelijkse beloning", "&7(geld, shards en een sleutel).", "", "&eKlik om te claimen"), e -> {
            plugin.rewardMenu().claimDaily(p);
            open(p);
        });
        pg.set(24, GuiManager.item(Material.GOLDEN_HELMET, "&6&lToplijst", "&7De rijkste spelers.", "", "&eKlik"), e -> plugin.topMenu().open(p, "money"));
        pg.set(29, GuiManager.item(Material.EMERALD, "&a&lWinkel", "&7Koop items met geld, diamanten of shards.", "", "&eKlik"), e -> plugin.shopMenu().open(p, 0));
        pg.set(31, GuiManager.glow(Material.ENDER_CHEST, "&6&lBeloningen", "&7Al je beloningen op een rij.", "", "&eKlik"), e -> plugin.rewardMenu().open(p));
        pg.set(33, GuiManager.item(Material.NETHER_STAR, "&6&lWebshop", "&7Koop ranks en meer.", "", "&eKlik"), e -> plugin.store().open(p));
        pg.set(49, GuiManager.item(Material.ARROW, "&c« Terug", "&7Naar het spelmenu."), e -> plugin.menu().open(p));
        pg.open(p);
    }

    private void pickPlayer(Player p, int page) {
        List<Player> others = new ArrayList<>();
        for (Player o : Bukkit.getOnlinePlayers()) if (!o.equals(p)) others.add(o);
        int pages = Math.max(1, (others.size() + PER_PAGE - 1) / PER_PAGE);
        final int cur = Math.max(0, Math.min(page, pages - 1));
        GuiManager.Page pg = new GuiManager.Page("&8» <gb:#f6d365:#fda085>KIES EEN SPELER</g> &8«", 6, true);
        pg.border();
        for (int i = 0; i < PER_PAGE && cur * PER_PAGE + i < others.size(); i++) {
            Player t = others.get(cur * PER_PAGE + i);
            int slot = (1 + i / 7) * 9 + 1 + i % 7;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(t);
            sm.displayName(Text.c("&f" + t.getName()));
            sm.lore(List.of(Text.c("&eKlik om te betalen")));
            head.setItemMeta(sm);
            pg.set(slot, head, e -> plugin.gui().prompt(p, "&eHoeveel wil je aan &f" + t.getName() + " &ebetalen? (bv. 100 of 12.50)", txt -> {
                if (!txt.matches("[0-9]{1,12}(\\.[0-9]{1,2})?")) { plugin.msg(p, "&cOngeldig bedrag."); open(p); return; }
                double amt = Double.parseDouble(txt);
                OfflinePlayer target = Bukkit.getOfflinePlayer(t.getUniqueId());
                String err = plugin.money().pay(p, target, amt);
                plugin.msg(p, err != null ? err : "Je hebt &a" + plugin.money().format(amt) + " &7betaald aan &f" + t.getName() + "&7.");
                open(p);
            }));
        }
        if (others.isEmpty()) pg.set(22, GuiManager.item(Material.BARRIER, "&7Er zijn geen andere spelers online"), null);
        if (cur > 0) pg.set(46, GuiManager.item(Material.SPECTRAL_ARROW, "&e« Vorige pagina"), e -> pickPlayer(p, cur - 1));
        if (cur < pages - 1) pg.set(52, GuiManager.item(Material.SPECTRAL_ARROW, "&eVolgende pagina »"), e -> pickPlayer(p, cur + 1));
        pg.set(49, GuiManager.item(Material.ARROW, "&c« Terug", "&7Naar de bank."), e -> open(p));
        pg.open(p);
    }
}

class Country {
    public final String name;
    public UUID owner;
    public final Set<UUID> members = new HashSet<>();

    public Country(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members.add(owner);
    }
}

class CountryManager {
    private final LandenScore plugin;
    private final File file;
    private final Map<String, Country> countries = new HashMap<>();
    public final Map<UUID, String> invites = new HashMap<>();

    public CountryManager(LandenScore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "countries.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = y.getConfigurationSection("countries");
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            Country c = new Country(s.getString(key + ".name", key), UUID.fromString(s.getString(key + ".owner")));
            c.members.clear();
            for (String m : s.getStringList(key + ".members")) c.members.add(UUID.fromString(m));
            countries.put(key.toLowerCase(), c);
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Country c : countries.values()) {
            String k = c.name.toLowerCase();
            y.set("countries." + k + ".name", c.name);
            y.set("countries." + k + ".owner", c.owner.toString());
            y.set("countries." + k + ".members", c.members.stream().map(UUID::toString).toList());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Kon countries.yml niet opslaan: " + e.getMessage());
        }
    }

    public Country get(String name) { return countries.get(name.toLowerCase()); }
    public Collection<Country> all() { return countries.values(); }

    public Country ofPlayer(UUID id) {
        for (Country c : countries.values()) if (c.members.contains(id)) return c;
        return null;
    }

    public Country create(String name, UUID owner) {
        Country c = new Country(name, owner);
        countries.put(name.toLowerCase(), c);
        save();
        return c;
    }

    public Country rename(Country old, String newName) {
        countries.remove(old.name.toLowerCase());
        Country c = new Country(newName, old.owner);
        c.members.clear();
        c.members.addAll(old.members);
        countries.put(newName.toLowerCase(), c);
        invites.replaceAll((k, v) -> v.equalsIgnoreCase(old.name) ? newName : v);
        save();
        return c;
    }

    public void delete(Country c) {
        countries.remove(c.name.toLowerCase());
        save();
    }
}

/** PvP 1v1, 2v2 en Tower Drop: wachtrij, arenas (gouden blokken), afteltimer, kit en terug naar de lobby. */
class DuelManager implements Listener {
    enum State { COUNTDOWN, RUNNING, ENDED }

    static class Arena {
        final String name;
        final String mode; // 1v1, 2v2 of tower
        final List<LocData> sideA = new ArrayList<>();
        final List<LocData> sideB = new ArrayList<>();
        Match active;

        Arena(String name, String mode) { this.name = name; this.mode = mode; }

        boolean tower() { return mode.equals("tower"); }
        int teamSize() { return mode.equals("2v2") ? 2 : 1; }
        int capacity() { return tower() ? sideA.size() : teamSize() * 2; }

        boolean ready() {
            boolean okA = !sideA.isEmpty() && sideA.stream().allMatch(d -> d.toLocation() != null);
            if (tower()) return okA && sideA.size() >= 2;
            return okA && !sideB.isEmpty() && sideB.stream().allMatch(d -> d.toLocation() != null);
        }
    }

    static class Saved {
        ItemStack[] contents, armor;
        ItemStack offhand;
        GameMode mode;
        Location loc;
    }

    static class Match {
        final Arena arena;
        final List<Set<UUID>> teams = new ArrayList<>();
        final Set<UUID> alive = new HashSet<>();
        final Map<UUID, Saved> saved = new HashMap<>();
        final Set<Block> placed = new HashSet<>();
        State state = State.COUNTDOWN;
        BukkitTask dropTask;
        double voidY = -1000;

        Match(Arena arena, int teamCount) {
            this.arena = arena;
            for (int i = 0; i < teamCount; i++) teams.add(new HashSet<>());
        }

        int teamOf(UUID id) {
            for (int i = 0; i < teams.size(); i++) if (teams.get(i).contains(id)) return i;
            return -1;
        }
    }

    private static final List<String> MODES = List.of("1v1", "2v2", "tower");

    private final LandenScore plugin;
    private final File file;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<String, List<UUID>> queues = new HashMap<>();
    private final Map<UUID, Match> matches = new HashMap<>();
    private BukkitTask towerWait;
    private int towerLeft;
    private final Random random = new Random();

    public DuelManager(LandenScore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        for (String m : MODES) queues.put(m, new ArrayList<>());
        load();
    }

    public static String mode(String s) {
        return switch (s.toLowerCase()) {
            case "1v1", "1" -> "1v1";
            case "2v2", "2" -> "2v2";
            case "tower", "towerdrop", "tower_drop", "toren" -> "tower";
            default -> null;
        };
    }

    // ---------- opslag ----------
    private void readSide(ConfigurationSection sec, List<LocData> out) {
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            LocData d = LocData.read(sec.getConfigurationSection(k));
            if (d != null) out.add(d);
        }
    }

    private void load() {
        if (!file.exists()) return;
        ConfigurationSection s = YamlConfiguration.loadConfiguration(file).getConfigurationSection("arenas");
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            String mode = s.getString(key + ".mode");
            if (mode == null) mode = s.getInt(key + ".teamsize", 1) == 2 ? "2v2" : "1v1";
            Arena a = new Arena(s.getString(key + ".name", key), mode);
            readSide(s.getConfigurationSection(key + ".a"), a.sideA);
            readSide(s.getConfigurationSection(key + ".b"), a.sideB);
            LocData o1 = LocData.read(s.getConfigurationSection(key + ".s1"));
            LocData o2 = LocData.read(s.getConfigurationSection(key + ".s2"));
            if (o1 != null && a.sideA.isEmpty()) a.sideA.add(o1);
            if (o2 != null && a.sideB.isEmpty()) a.sideB.add(o2);
            arenas.put(key.toLowerCase(), a);
        }
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Arena a : arenas.values()) {
            String k = "arenas." + a.name.toLowerCase();
            y.set(k + ".name", a.name);
            y.set(k + ".mode", a.mode);
            for (int i = 0; i < a.sideA.size(); i++) a.sideA.get(i).write(y.createSection(k + ".a." + i));
            for (int i = 0; i < a.sideB.size(); i++) a.sideB.get(i).write(y.createSection(k + ".b." + i));
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Kon arenas.yml niet opslaan: " + e.getMessage());
        }
    }

    // ---------- arena-beheer ----------
    public Collection<Arena> arenas() { return arenas.values(); }

    public Arena arena(String name) { return arenas.get(name.toLowerCase()); }

    public Arena createArena(String name, String mode) {
        Arena a = new Arena(name, mode);
        arenas.put(name.toLowerCase(), a);
        save();
        return a;
    }

    public boolean deleteArena(String name) {
        Arena a = arena(name);
        if (a == null || a.active != null) return false;
        arenas.remove(name.toLowerCase());
        save();
        return true;
    }

    public void addSpawn(Arena a, int side, Location l) {
        if (side == 2 && !a.tower()) a.sideB.add(LocData.of(l)); else a.sideA.add(LocData.of(l));
        save();
    }

    public void setSpawn(Arena a, int side, Location l) {
        if (side == 2 && !a.tower()) a.sideB.clear(); else a.sideA.clear();
        addSpawn(a, side, l);
    }

    /** Zoekt gouden blokken rond een punt en maakt er spawns van. Geeft een melding terug. */
    public String scan(Arena a, Location center, int radius) {
        radius = Math.max(5, Math.min(100, radius));
        Material marker = Material.matchMaterial(plugin.getConfig().getString("arena.marker-block", "GOLD_BLOCK"));
        if (marker == null) marker = Material.GOLD_BLOCK;
        World w = center.getWorld();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        int minY = Math.max(w.getMinHeight(), cy - radius);
        int maxY = Math.min(w.getMaxHeight() - 1, cy + radius);
        List<Block> found = new ArrayList<>();
        for (int x = cx - radius; x <= cx + radius; x++)
            for (int z = cz - radius; z <= cz + radius; z++)
                for (int y = minY; y <= maxY; y++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() == marker) found.add(b);
                }
        if (found.size() < 2)
            return "&cJe hebt minstens 2 gouden blokken nodig (gevonden: " + found.size() + "). Sta in het midden van de arena.";

        boolean remove = plugin.getConfig().getBoolean("arena.remove-markers", false);
        double dy = remove ? 0 : 1;
        double sx = 0, sy = 0, sz = 0;
        for (Block b : found) { sx += b.getX() + 0.5; sy += b.getY(); sz += b.getZ() + 0.5; }
        Location centroid = new Location(w, sx / found.size(), sy / found.size(), sz / found.size());

        List<Location> locs = new ArrayList<>();
        for (Block b : found) {
            Location l = new Location(w, b.getX() + 0.5, b.getY() + dy, b.getZ() + 0.5);
            Vector dir = centroid.toVector().subtract(l.toVector());
            dir.setY(0);
            if (dir.lengthSquared() > 0.01) l.setDirection(dir);
            l.setPitch(0f);
            locs.add(l);
        }

        a.sideA.clear();
        a.sideB.clear();
        if (a.tower()) {
            for (Location l : locs) a.sideA.add(LocData.of(l));
        } else {
            Location p0 = locs.get(0), p1 = locs.get(1);
            double best = -1;
            for (int i = 0; i < locs.size(); i++)
                for (int j = i + 1; j < locs.size(); j++) {
                    double d = locs.get(i).distanceSquared(locs.get(j));
                    if (d > best) { best = d; p0 = locs.get(i); p1 = locs.get(j); }
                }
            for (Location l : locs) {
                if (l.distanceSquared(p0) <= l.distanceSquared(p1)) a.sideA.add(LocData.of(l));
                else a.sideB.add(LocData.of(l));
            }
        }
        if (remove) for (Block b : found) b.setType(Material.AIR);
        save();
        return a.tower()
                ? "&aGescand: &f" + a.sideA.size() + " &7torens gevonden."
                : "&aGescand: &f" + a.sideA.size() + " &7spawn(s) voor kant A en &f" + a.sideB.size() + " &7voor kant B.";
    }

    // ---------- wachtrij ----------
    public boolean inMatch(Player p) { return matches.containsKey(p.getUniqueId()); }

    public int queueSize(String mode) { return queues.getOrDefault(mode, List.of()).size(); }

    public int playing() { return matches.size(); }

    public boolean leaveQueue(Player p) {
        boolean removed = false;
        for (List<UUID> q : queues.values()) removed |= q.remove(p.getUniqueId());
        return removed;
    }

    public void queue(Player p, String modeInput) {
        String mode = mode(modeInput);
        if (mode == null) { plugin.msg(p, "&cOnbekende modus. Gebruik 1v1, 2v2 of tower."); return; }
        if (inMatch(p)) { plugin.msg(p, "&cJe zit al in een match."); return; }
        List<UUID> q = queues.get(mode);
        if (q.contains(p.getUniqueId())) {
            leaveQueue(p);
            plugin.msg(p, "Je hebt de wachtrij verlaten.");
            return;
        }
        if (arenas.values().stream().noneMatch(a -> a.mode.equals(mode) && a.ready())) {
            plugin.msg(p, "&cEr is nog geen arena voor deze modus.");
            return;
        }
        leaveQueue(p);
        q.add(p.getUniqueId());
        if (mode.equals("tower"))
            plugin.msg(p, "Je staat in de wachtrij voor &fTower Drop &7(" + q.size() + " speler(s)). Typ &e/queue leave &7om te stoppen.");
        else
            plugin.msg(p, "Je staat in de wachtrij voor &f" + mode + " &7(" + q.size() + "/" + (arenaNeed(mode)) + "). Typ &e/queue leave &7om te stoppen.");
        tryStart(mode);
    }

    private int arenaNeed(String mode) { return mode.equals("2v2") ? 4 : 2; }

    private Arena freeArena(String mode) {
        return arenas.values().stream().filter(a -> a.mode.equals(mode) && a.active == null && a.ready()).findFirst().orElse(null);
    }

    private void tryStart(String mode) {
        if (mode.equals("tower")) { checkTower(); return; }
        List<UUID> q = queues.get(mode);
        q.removeIf(id -> Bukkit.getPlayer(id) == null);
        int need = arenaNeed(mode);
        while (q.size() >= need) {
            Arena free = freeArena(mode);
            if (free == null) {
                for (UUID id : q) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) plugin.msg(p, "Er is nog geen vrije arena. Je blijft in de wachtrij.");
                }
                return;
            }
            startFromQueue(free, mode, need);
        }
    }

    private void startFromQueue(Arena arena, String mode, int n) {
        List<UUID> q = queues.get(mode);
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < n && !q.isEmpty(); i++) {
            Player p = Bukkit.getPlayer(q.remove(0));
            if (p != null) players.add(p);
        }
        startMatch(arena, players);
    }

    private int towerMin(Arena a) {
        return Math.max(2, Math.min(plugin.getConfig().getInt("tower.min-players", 2), a.capacity()));
    }

    private void stopTowerWait() {
        if (towerWait != null) towerWait.cancel();
        towerWait = null;
    }

    private void checkTower() {
        List<UUID> q = queues.get("tower");
        q.removeIf(id -> Bukkit.getPlayer(id) == null);
        Arena free = freeArena("tower");
        if (free == null) { stopTowerWait(); return; }
        if (q.size() >= free.capacity()) {
            stopTowerWait();
            startFromQueue(free, "tower", free.capacity());
            checkTower();
            return;
        }
        if (q.size() < towerMin(free)) { stopTowerWait(); return; }
        if (towerWait != null) return;
        towerLeft = Math.max(3, plugin.getConfig().getInt("tower.queue-wait", 20));
        towerWait = new BukkitRunnable() {
            @Override
            public void run() {
                List<UUID> qq = queues.get("tower");
                qq.removeIf(id -> Bukkit.getPlayer(id) == null);
                Arena fa = freeArena("tower");
                if (fa == null || qq.size() < towerMin(fa)) { stopTowerWait(); return; }
                if (towerLeft <= 0) {
                    stopTowerWait();
                    startFromQueue(fa, "tower", Math.min(qq.size(), fa.capacity()));
                    checkTower();
                    return;
                }
                for (UUID id : qq) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) p.sendActionBar(Text.c("&eTower Drop start over &f" + towerLeft + "s &7(" + qq.size() + "/" + fa.capacity() + ")"));
                }
                towerLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ---------- match ----------
    private void startMatch(Arena arena, List<Player> players) {
        boolean tower = arena.tower();
        List<Location> a = new ArrayList<>();
        for (LocData d : arena.sideA) a.add(d.toLocation());
        List<Location> b = new ArrayList<>();
        for (LocData d : arena.sideB) b.add(d.toLocation());
        Collections.shuffle(players);
        if (tower) Collections.shuffle(a);

        Match m = new Match(arena, tower ? players.size() : 2);
        arena.active = m;
        double minY = Double.MAX_VALUE;
        for (Location l : a) minY = Math.min(minY, l.getY());
        if (tower) m.voidY = minY - plugin.getConfig().getInt("tower.kill-below", 30);

        int[] idx = new int[2];
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int team = tower ? i : i % 2;
            Location spawn = tower ? a.get(i) : (team == 0 ? a.get(idx[0]++ % a.size()) : b.get(idx[1]++ % b.size()));
            UUID id = p.getUniqueId();
            m.teams.get(team).add(id);
            m.alive.add(id);
            matches.put(id, m);
            m.saved.put(id, save(p));
            prepare(p, tower ? "tower.kit" : "duels.kit");
            p.teleport(spawn);
        }
        countdown(m);
    }

    private Saved save(Player p) {
        Saved s = new Saved();
        s.contents = p.getInventory().getStorageContents().clone();
        s.armor = p.getInventory().getArmorContents().clone();
        s.offhand = p.getInventory().getItemInOffHand();
        s.mode = p.getGameMode();
        s.loc = p.getLocation();
        return s;
    }

    private void resetState(Player p) {
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        for (var ef : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(ef.getType());
    }

    private void prepare(Player p, String kitPath) {
        p.getInventory().clear();
        p.setGameMode(GameMode.SURVIVAL);
        resetState(p);
        ConfigurationSection k = plugin.getConfig().getConfigurationSection(kitPath);
        if (k == null) return;
        p.getInventory().setHelmet(item(k.getString("helmet")));
        p.getInventory().setChestplate(item(k.getString("chestplate")));
        p.getInventory().setLeggings(item(k.getString("leggings")));
        p.getInventory().setBoots(item(k.getString("boots")));
        for (String s : k.getStringList("items")) {
            ItemStack it = item(s);
            if (it != null) p.getInventory().addItem(it);
        }
    }

    private ItemStack item(String s) {
        if (s == null || s.isBlank()) return null;
        String[] parts = s.split(":");
        Material m = Material.matchMaterial(parts[0].trim());
        if (m == null) return null;
        int amt = 1;
        if (parts.length > 1) {
            try { amt = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException ignored) { }
        }
        return new ItemStack(m, amt);
    }

    private List<Player> online(Match m) {
        List<Player> l = new ArrayList<>();
        for (Set<UUID> t : m.teams)
            for (UUID id : t) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && matches.get(id) == m) l.add(p);
            }
        return l;
    }

    private void broadcast(Match m, String text) {
        for (Player p : online(m)) plugin.msg(p, text);
    }

    private void countdown(Match m) {
        String cfg = m.arena.tower() ? "tower" : "duels";
        int total = Math.max(1, plugin.getConfig().getInt(cfg + ".countdown", 5));
        new BukkitRunnable() {
            int left = total;

            @Override
            public void run() {
                if (m.state != State.COUNTDOWN) { cancel(); return; }
                if (left > 0) {
                    for (Player p : online(m)) {
                        p.showTitle(Title.title(Text.c("&e" + left), Text.c("&7De match begint zo!"),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }
                    left--;
                } else {
                    m.state = State.RUNNING;
                    for (Player p : online(m)) {
                        p.showTitle(Title.title(Text.c("&a&lSTART!"), Text.c(""),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(200))));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    }
                    if (m.arena.tower()) startDrops(m);
                    int max = plugin.getConfig().getInt(cfg + ".max-seconds", 600);
                    if (max > 0) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (m.state == State.RUNNING) end(m, -1);
                        }, max * 20L);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ---------- Tower Drop ----------
    private void startDrops(Match m) {
        int every = Math.max(2, plugin.getConfig().getInt("tower.drop-seconds", 10));
        m.dropTask = new BukkitRunnable() {
            int left = every;

            @Override
            public void run() {
                if (m.state != State.RUNNING) { cancel(); return; }
                left--;
                if (left <= 0) { dropRound(m); left = every; }
                for (Player p : online(m))
                    if (m.alive.contains(p.getUniqueId())) p.sendActionBar(Text.c("&6Volgend item over &f" + left + "s"));
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private ItemStack randomDrop() {
        List<String> pool = plugin.getConfig().getStringList("tower.drops");
        for (int tries = 0; tries < 10 && !pool.isEmpty(); tries++) {
            ItemStack it = item(pool.get(random.nextInt(pool.size())));
            if (it != null) return it;
        }
        return new ItemStack(Material.COBBLESTONE, 16);
    }

    private void dropRound(Match m) {
        for (Player p : online(m)) {
            if (!m.alive.contains(p.getUniqueId())) continue;
            ItemStack it = randomDrop();
            Map<Integer, ItemStack> left = p.getInventory().addItem(it);
            for (ItemStack rest : left.values()) p.getWorld().dropItem(p.getLocation(), rest);
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
            plugin.msg(p, "Nieuw item: &f" + it.getAmount() + "x " + it.getType().name().toLowerCase().replace('_', ' '));
        }
    }

    // ---------- einde ----------
    private void eliminate(Player p, Match m, Player killer) {
        m.alive.remove(p.getUniqueId());
        if (plugin.getConfig().getBoolean("stats.count-duels", true)) {
            plugin.stats().addDeath(p.getUniqueId());
            if (killer != null) plugin.stats().addKill(killer.getUniqueId());
        }
        p.setGameMode(GameMode.SPECTATOR);
        p.setHealth(20.0);
        Location s = m.arena.sideA.isEmpty() ? null : m.arena.sideA.get(0).toLocation();
        if (s != null) p.teleport(s.clone().add(0, 3, 0));
        broadcast(m, killer != null
                ? "&c" + p.getName() + " &7is verslagen door &f" + killer.getName() + "&7."
                : "&c" + p.getName() + " &7is uitgeschakeld.");
        checkWin(m);
    }

    private void checkWin(Match m) {
        if (m.state == State.ENDED) return;
        int aliveTeams = 0, last = -1;
        for (int i = 0; i < m.teams.size(); i++) {
            if (m.teams.get(i).stream().anyMatch(m.alive::contains)) { aliveTeams++; last = i; }
        }
        if (aliveTeams == 0) end(m, -1);
        else if (aliveTeams == 1) end(m, last);
    }

    private void end(Match m, int winner) {
        if (m.state == State.ENDED) return;
        m.state = State.ENDED;
        if (m.dropTask != null) m.dropTask.cancel();
        for (Player p : online(m)) {
            boolean won = winner >= 0 && m.teams.get(winner).contains(p.getUniqueId());
            String big = winner < 0 ? "&e&lGELIJKSPEL" : won ? "&a&lGEWONNEN!" : "&c&lVERLOREN";
            p.showTitle(Title.title(Text.c(big), Text.c("&7Je gaat zo terug naar de lobby."),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))));
            p.playSound(p.getLocation(), won ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        if (winner >= 0) {
            List<String> names = new ArrayList<>();
            for (UUID id : m.teams.get(winner)) names.add(Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName()).orElse("?"));
            broadcast(m, "&6Winnaar: &f" + String.join(", ", names));
        } else {
            broadcast(m, "&eHet is gelijkspel.");
        }
        String cfg = m.arena.tower() ? "tower" : "duels";
        long delay = Math.max(1, plugin.getConfig().getInt(cfg + ".end-delay", 5)) * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> finish(m), delay);
    }

    private void cleanup(Match m) {
        for (Block b : m.placed) b.setType(Material.AIR);
        m.placed.clear();
    }

    private void finish(Match m) {
        cleanup(m);
        for (Set<UUID> t : m.teams) {
            for (UUID id : t) {
                if (matches.get(id) != m) continue;
                matches.remove(id);
                Player p = Bukkit.getPlayer(id);
                if (p != null) restore(p, m, id);
            }
        }
        m.arena.active = null;
        tryStart(m.arena.mode);
    }

    private void restore(Player p, Match m, UUID id) {
        Saved s = m.saved.get(id);
        if (s == null) return;
        p.setGameMode(s.mode);
        p.getInventory().clear();
        p.getInventory().setStorageContents(s.contents);
        p.getInventory().setArmorContents(s.armor);
        p.getInventory().setItemInOffHand(s.offhand == null ? new ItemStack(Material.AIR) : s.offhand);
        resetState(p);
        String lobbyName = plugin.getConfig().getString(m.arena.tower() ? "tower.lobby" : "duels.lobby", "main");
        if (plugin.lobby().teleport(p, lobbyName)) return;
        if (plugin.lobby().teleport(p)) return;
        if (s.loc != null) p.teleport(s.loc);
    }

    public void shutdown() {
        stopTowerWait();
        for (Match m : new HashSet<>(matches.values())) {
            m.state = State.ENDED;
            if (m.dropTask != null) m.dropTask.cancel();
            cleanup(m);
            for (UUID id : new ArrayList<>(m.saved.keySet())) {
                if (matches.get(id) != m) continue;
                matches.remove(id);
                Player p = Bukkit.getPlayer(id);
                if (p != null) restore(p, m, id);
            }
            m.arena.active = null;
        }
    }

    // ---------- events ----------
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Player damager = null;
        if (e.getDamager() instanceof Player d1) damager = d1;
        else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player d2) damager = d2;
        if (damager == null) return;
        Match m = matches.get(victim.getUniqueId());
        if (m == null) return;
        if (matches.get(damager.getUniqueId()) != m || m.state != State.RUNNING
                || m.teamOf(victim.getUniqueId()) == m.teamOf(damager.getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Match m = matches.get(p.getUniqueId());
        if (m == null) return;
        if (m.state != State.RUNNING || !m.alive.contains(p.getUniqueId())) { e.setCancelled(true); return; }
        if (p.getHealth() - e.getFinalDamage() > 0) return;
        e.setCancelled(true);
        Player killer = null;
        if (e instanceof EntityDamageByEntityEvent ed) {
            if (ed.getDamager() instanceof Player k1) killer = k1;
            else if (ed.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player k2) killer = k2;
        }
        eliminate(p, m, killer);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Match m = matches.get(p.getUniqueId());
        if (m == null) return;
        Location f = e.getFrom();
        Location t = e.getTo();
        if (t == null) return;
        if (m.state == State.COUNTDOWN) {
            if (f.getX() == t.getX() && f.getZ() == t.getZ()) return;
            Location n = f.clone();
            n.setYaw(t.getYaw());
            n.setPitch(t.getPitch());
            e.setTo(n);
            return;
        }
        if (m.state == State.RUNNING && m.arena.tower() && m.alive.contains(p.getUniqueId()) && t.getY() < m.voidY) {
            UUID id = p.getUniqueId();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (m.state == State.RUNNING && m.alive.contains(id)) eliminate(p, m, null);
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        leaveQueue(p);
        Match m = matches.get(p.getUniqueId());
        if (m == null) return;
        m.alive.remove(p.getUniqueId());
        matches.remove(p.getUniqueId());
        restore(p, m, p.getUniqueId());
        broadcast(m, "&c" + p.getName() + " &7heeft het spel verlaten.");
        checkWin(m);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Match m = matches.get(e.getPlayer().getUniqueId());
        if (m == null) return;
        if (!m.arena.tower() || m.state != State.RUNNING) { e.setCancelled(true); return; }
        m.placed.add(e.getBlockPlaced());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Match m = matches.get(e.getPlayer().getUniqueId());
        if (m == null) return;
        if (m.arena.tower() && m.state == State.RUNNING && m.placed.remove(e.getBlock())) return; // eigen blokken mogen weg
        e.setCancelled(true);
    }
}

/** Meldt de eigen economie aan bij Vault (apart gehouden zodat Vault alleen hier nodig is). */
final class EconomySetup {
    private EconomySetup() {}

    static Runnable register(LandenScore plugin) {
        String mode = plugin.getConfig().getString("economy.mode", "auto").toLowerCase();
        boolean other = Bukkit.getServicesManager().getRegistration(Economy.class) != null;
        if (mode.equals("vault") || (mode.equals("auto") && other)) {
            plugin.getLogger().info("Eigen economie staat uit; de bestaande Vault-economie wordt gebruikt (economy.mode: " + mode + ").");
            return () -> { };
        }
        OwnEconomy own = new OwnEconomy(plugin);
        Bukkit.getServicesManager().register(Economy.class, own, plugin, ServicePriority.Highest);
        BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, own::save, 6000L, 6000L);
        plugin.setOwnEconomyActive(true);
        plugin.getLogger().info("Eigen economie actief (Vault-provider LandenScore).");
        return () -> { t.cancel(); own.save(); };
    }
}

final class Fmt {
    private static final String[] SUFFIX = {"", "k", "M", "B", "T"};
    private static final Pattern DUR = Pattern.compile("(\\d+)(mo|w|d|h|m|s)");

    private Fmt() {}

    /** 2456000000 -> 2.456B, 7800 -> 7.8k */
    public static String compact(double v) {
        double a = Math.abs(v);
        int i = 0;
        while (a >= 1000 && i < SUFFIX.length - 1) { a /= 1000; v /= 1000; i++; }
        String s = i == 0 ? String.format(Locale.US, "%.2f", v) : String.format(Locale.US, "%.3f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s + SUFFIX[i];
    }

    /** 47d 15h, 3h 12m, 14m 28s, 9s */
    public static String time(long sec) {
        if (sec < 0) sec = 0;
        long d = sec / 86400, h = sec % 86400 / 3600, m = sec % 3600 / 60, s = sec % 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    /** "1d12h" -> seconden; "eenmalig" -> -1; ongeldig -> -2 */
    public static long parseDuration(String in) {
        String s = in.trim().toLowerCase(Locale.ROOT);
        if (s.equals("eenmalig") || s.equals("once")) return -1;
        Matcher m = DUR.matcher(s);
        long total = 0;
        int end = 0;
        boolean any = false;
        while (m.find()) {
            if (m.start() != end) return -2;
            long n = Long.parseLong(m.group(1));
            total += n * switch (m.group(2)) {
                case "mo" -> 2592000L;
                case "w" -> 604800L;
                case "d" -> 86400L;
                case "h" -> 3600L;
                case "m" -> 60L;
                default -> 1L;
            };
            end = m.end();
            any = true;
        }
        return (!any || end != s.length()) ? -2 : total;
    }
}

class GameCommands implements CommandExecutor, TabCompleter {
    private final LandenScore plugin;

    public GameCommands(LandenScore plugin) { this.plugin = plugin; }

    private boolean admin(CommandSender s) { return s.hasPermission("landen.admin"); }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        Player p = s instanceof Player pl ? pl : null;
        switch (c.getName().toLowerCase()) {
            case "lobby" -> {
                if (p == null) return true;
                if (plugin.duels().inMatch(p)) { plugin.msg(p, "&cJe zit in een match."); return true; }
                plugin.duels().leaveQueue(p);
                boolean ok = a.length > 0 ? plugin.lobby().teleport(p, a[0]) : plugin.lobby().teleport(p);
                if (!ok) plugin.msg(p, a.length > 0 ? "&cDie lobby bestaat niet. Beschikbaar: " + String.join(", ", plugin.lobby().names())
                        : "&cDe lobby is nog niet ingesteld. Een admin gebruikt /setlobby.");
            }
            case "setlobby" -> {
                if (p == null || !admin(s)) { plugin.msg(s, "&cGeen rechten (of geen speler)."); return true; }
                String name = a.length > 0 ? a[0].toLowerCase() : "main";
                if (!name.matches("[a-z0-9_]{1,24}")) { plugin.msg(p, "&cOngeldige naam."); return true; }
                plugin.lobby().set(name, p.getLocation());
                plugin.msg(p, "Lobby &f" + name + " &7ingesteld op jouw plek.");
            }
            case "afk" -> {
                if (p == null) return true;
                if (plugin.duels().inMatch(p)) { plugin.msg(p, "&cJe zit in een match."); return true; }
                plugin.duels().leaveQueue(p);
                if (plugin.lobby().teleport(p, "afk")) plugin.msg(p, "Je bent naar de AFK-zone gegaan. Typ &e/lobby &7om terug te gaan.");
                else plugin.msg(p, "&cDe AFK-zone is nog niet ingesteld. Een admin gebruikt /setafk.");
            }
            case "setafk" -> {
                if (p == null || !admin(s)) { plugin.msg(s, "&cGeen rechten (of geen speler)."); return true; }
                plugin.lobby().set("afk", p.getLocation());
                plugin.msg(p, "AFK-zone ingesteld op jouw plek (wereld &f" + p.getWorld().getName() + "&7).");
            }
            case "menu" -> { if (p != null) plugin.menu().open(p); }
            case "geld" -> {
                OfflinePlayer t = null;
                if (a.length > 0) {
                    t = Bukkit.getPlayerExact(a[0]);
                    if (t == null) t = Bukkit.getOfflinePlayer(a[0]);
                    if (!t.isOnline() && !t.hasPlayedBefore()) { plugin.msg(s, "&cSpeler niet gevonden."); return true; }
                } else if (p != null) t = p;
                if (t == null) { plugin.msg(s, "Gebruik: /geld <speler>"); return true; }
                plugin.msg(s, "&7Saldo van &f" + t.getName() + "&7: &a" + plugin.money().format(plugin.money().balance(t)));
            }
            case "betaal" -> {
                if (p == null) return true;
                if (a.length < 2) { plugin.msg(p, "Gebruik: /betaal <speler> <bedrag>"); return true; }
                OfflinePlayer t = Bukkit.getPlayerExact(a[0]);
                if (t == null) t = Bukkit.getOfflinePlayer(a[0]);
                if (!t.isOnline() && !t.hasPlayedBefore()) { plugin.msg(p, "&cSpeler niet gevonden."); return true; }
                if (!a[1].matches("[0-9]{1,12}(\\.[0-9]{1,2})?")) { plugin.msg(p, "&cOngeldig bedrag."); return true; }
                double amt = Double.parseDouble(a[1]);
                String err = plugin.money().pay(p, t, amt);
                plugin.msg(p, err != null ? err : "Je hebt &a" + plugin.money().format(amt) + " &7betaald aan &f" + t.getName() + "&7.");
            }
            case "geldtop" -> {
                if (p != null) plugin.topMenu().open(p, "money");
                else {
                    plugin.msg(s, "&6&lTop 10 Money");
                    int pos = 1;
                    for (Map.Entry<UUID, Double> en : plugin.money().top(10))
                        plugin.msg(s, "&e" + (pos++) + ". &f" + Optional.ofNullable(Bukkit.getOfflinePlayer(en.getKey()).getName()).orElse("?") + " &7- &f" + plugin.money().format(en.getValue()));
                }
            }
            case "geldbeheer" -> {
                if (!admin(s)) { plugin.msg(s, "&cGeen rechten."); return true; }
                if (a.length < 3) { plugin.msg(s, "Gebruik: /geldbeheer <give|take|set> <speler> <bedrag>"); return true; }
                OfflinePlayer t = Bukkit.getPlayerExact(a[1]);
                if (t == null) t = Bukkit.getOfflinePlayer(a[1]);
                if (!t.isOnline() && !t.hasPlayedBefore()) { plugin.msg(s, "&cSpeler niet gevonden."); return true; }
                if (!a[2].matches("[0-9]{1,12}(\\.[0-9]{1,2})?")) { plugin.msg(s, "&cOngeldig bedrag."); return true; }
                double amt = Double.parseDouble(a[2]);
                boolean ok = switch (a[0].toLowerCase()) {
                    case "give" -> plugin.money().give(t, amt);
                    case "take" -> plugin.money().take(t, amt);
                    case "set" -> plugin.money().set(t, amt);
                    default -> false;
                };
                plugin.msg(s, ok ? "&f" + t.getName() + " &7heeft nu &a" + plugin.money().format(plugin.money().balance(t)) + "&7."
                        : "&cMislukt (onvoldoende saldo, geen economie of onbekend subcommando).");
            }
            case "bank" -> { if (p != null) plugin.bank().open(p); }
            case "dagelijks" -> { if (p != null) plugin.rewardMenu().claimDaily(p); }
            case "winkel" -> { if (p != null) plugin.shopMenu().open(p, 0); }
            case "setwinkel" -> {
                if (p == null || !admin(s)) { plugin.msg(s, "&cGeen rechten (of geen speler)."); return true; }
                plugin.admin().shop(p, 0);
            }
            case "store" -> { if (p != null) plugin.store().open(p); }
            case "buyalert" -> {
                if (!admin(s)) { plugin.msg(s, "&cGeen rechten."); return true; }
                if (a.length < 2) { plugin.msg(s, "Gebruik: /buyalert <speler> <pakket>"); return true; }
                plugin.store().announce(a[0], String.join(" ", Arrays.copyOfRange(a, 1, a.length)));
            }
            case "toplijst" -> {
                String type = a.length > 0 ? a[0].toLowerCase() : "money";
                if (!List.of("money", "kills", "deaths", "shards").contains(type)) { plugin.msg(s, "Gebruik: /toplijst <money|kills|deaths|shards>"); return true; }
                if (p != null) { plugin.topMenu().open(p, type); return true; }
                if (type.equals("money")) {
                    plugin.msg(s, "&6&lTop 10 Money");
                    int pos = 1;
                    for (Map.Entry<UUID, Double> en : plugin.money().top(10))
                        plugin.msg(s, "&e" + (pos++) + ". &f" + Optional.ofNullable(Bukkit.getOfflinePlayer(en.getKey()).getName()).orElse("?") + " &7- &f" + plugin.money().format(en.getValue()));
                    return true;
                }
                String label = type.equals("shards") ? plugin.getConfig().getString("currency.name", "Diamanten") : type.substring(0, 1).toUpperCase() + type.substring(1);
                plugin.msg(s, "&6&lTop 10 " + label);
                List<Map.Entry<UUID, Long>> top = plugin.stats().top(type, 10);
                if (top.isEmpty()) plugin.msg(s, "&7Nog niemand in de lijst.");
                int rank = 1;
                for (Map.Entry<UUID, Long> en : top)
                    plugin.msg(s, "&e" + (rank++) + ". &f" + Optional.ofNullable(Bukkit.getOfflinePlayer(en.getKey()).getName()).orElse("?") + " &7- &f" + en.getValue());
            }
            case "rtp" -> { if (p != null) plugin.rtp().teleport(p, a.length > 0 ? a[0] : null); }
            case "beloningen" -> { if (p != null) plugin.rewardMenu().open(p); }
            case "setbeloningen" -> {
                if (p == null || !admin(s)) { plugin.msg(s, "&cGeen rechten (of geen speler)."); return true; }
                plugin.admin().rewards(p, 0);
            }
            case "keyall" -> {
                if (a.length > 0 && a[0].equalsIgnoreCase("now")) {
                    if (!admin(s)) { plugin.msg(s, "&cGeen rechten."); return true; }
                    plugin.keyall().fire();
                } else {
                    plugin.msg(s, "Volgende keyall over &f" + Fmt.time(plugin.keyall().remainingSeconds()) + "&7.");
                }
            }
            case "shards" -> shards(s, p, a);
            case "beheer" -> {
                if (p == null || !admin(s)) { plugin.msg(s, "&cGeen rechten (of geen speler)."); return true; }
                plugin.admin().main(p);
            }
            case "queue" -> {
                if (p == null) return true;
                if (a.length == 0) { plugin.msg(p, "Gebruik: /queue <1v1|2v2|tower|leave>"); return true; }
                if (a[0].equalsIgnoreCase("leave")) {
                    plugin.msg(p, plugin.duels().leaveQueue(p) ? "Je hebt de wachtrij verlaten." : "Je stond niet in een wachtrij.");
                } else plugin.duels().queue(p, a[0]);
            }
            case "arena" -> {
                if (!admin(s)) { plugin.msg(s, "&cGeen rechten."); return true; }
                arena(s, p, a);
            }
            case "npc" -> {
                if (!admin(s)) { plugin.msg(s, "&cGeen rechten."); return true; }
                npc(s, p, a);
            }
            default -> { }
        }
        return true;
    }

    private void shards(CommandSender s, Player p, String[] a) {
        StatsManager st = plugin.stats();
        String cur = plugin.getConfig().getString("currency.name", "Diamanten").toLowerCase();
        if (a.length == 0) {
            if (p == null) { plugin.msg(s, "Gebruik: /shards <give|take|set> <speler> <aantal>"); return; }
            plugin.msg(p, "Je hebt &f" + st.shards(p.getUniqueId()) + " &7" + cur + ".");
            return;
        }
        if (!admin(s)) { plugin.msg(s, "&cGeen rechten."); return; }
        if (a.length < 3) { plugin.msg(s, "Gebruik: /shards <give|take|set> <speler> <aantal>"); return; }
        OfflinePlayer t = Bukkit.getPlayerExact(a[1]);
        if (t == null) t = Bukkit.getOfflinePlayer(a[1]);
        if (!t.isOnline() && !t.hasPlayedBefore()) { plugin.msg(s, "&cSpeler niet gevonden."); return; }
        long n;
        try { n = Long.parseLong(a[2]); } catch (NumberFormatException ex) { plugin.msg(s, "&cOngeldig aantal."); return; }
        if (n < 0) { plugin.msg(s, "&cGebruik een positief aantal."); return; }
        switch (a[0].toLowerCase()) {
            case "give" -> st.addShards(t.getUniqueId(), n);
            case "take" -> st.addShards(t.getUniqueId(), -n);
            case "set" -> st.setShards(t.getUniqueId(), n);
            default -> { plugin.msg(s, "Gebruik: /shards <give|take|set> <speler> <aantal>"); return; }
        }
        plugin.msg(s, "&f" + t.getName() + " &7heeft nu &f" + st.shards(t.getUniqueId()) + " &7" + cur + ".");
    }

    private void arena(CommandSender s, Player p, String[] a) {
        DuelManager d = plugin.duels();
        if (a.length == 0) {
            plugin.msg(s, "&e/arena create <naam> <1v1|2v2|tower> &8| &e/arena scan <naam> [radius] &8| &e/arena setspawn|addspawn <naam> <1|2> &8| &e/arena info <naam> &8| &e/arena delete <naam> &8| &e/arena list");
            return;
        }
        switch (a[0].toLowerCase()) {
            case "create" -> {
                String mode = a.length >= 3 ? DuelManager.mode(a[2]) : null;
                if (mode == null) { plugin.msg(s, "Gebruik: /arena create <naam> <1v1|2v2|tower>"); return; }
                if (!a[1].matches("[A-Za-z0-9_]{1,24}")) { plugin.msg(s, "&cOngeldige naam."); return; }
                if (d.arena(a[1]) != null) { plugin.msg(s, "&cDie arena bestaat al."); return; }
                d.createArena(a[1], mode);
                plugin.msg(s, "Arena &f" + a[1] + " &7(" + mode + ") aangemaakt. Zet gouden blokken neer en gebruik &e/arena scan " + a[1] + "&7.");
            }
            case "scan" -> {
                if (p == null) { plugin.msg(s, "Alleen voor spelers."); return; }
                if (a.length < 2) { plugin.msg(s, "Gebruik: /arena scan <naam> [radius]"); return; }
                DuelManager.Arena ar = d.arena(a[1]);
                if (ar == null) { plugin.msg(s, "&cArena niet gevonden."); return; }
                int radius = plugin.getConfig().getInt("arena.scan-radius", 60);
                if (a.length >= 3) {
                    try { radius = Integer.parseInt(a[2]); } catch (NumberFormatException ignored) { }
                }
                plugin.msg(s, d.scan(ar, p.getLocation(), radius));
            }
            case "setspawn", "addspawn" -> {
                if (p == null) { plugin.msg(s, "Alleen voor spelers."); return; }
                if (a.length < 3 || !(a[2].equals("1") || a[2].equals("2"))) { plugin.msg(s, "Gebruik: /arena " + a[0] + " <naam> <1|2>"); return; }
                DuelManager.Arena ar = d.arena(a[1]);
                if (ar == null) { plugin.msg(s, "&cArena niet gevonden."); return; }
                int side = Integer.parseInt(a[2]);
                if (a[0].equalsIgnoreCase("setspawn")) d.setSpawn(ar, side, p.getLocation()); else d.addSpawn(ar, side, p.getLocation());
                plugin.msg(s, "Spawn voor kant &f" + a[2] + " &7van &f" + ar.name + " &7ingesteld.");
            }
            case "info" -> {
                if (a.length < 2) { plugin.msg(s, "Gebruik: /arena info <naam>"); return; }
                DuelManager.Arena ar = d.arena(a[1]);
                if (ar == null) { plugin.msg(s, "&cArena niet gevonden."); return; }
                plugin.msg(s, "&f" + ar.name + " &7| " + ar.mode + " | kant A: &f" + ar.sideA.size() + " &7| kant B: &f" + ar.sideB.size()
                        + " &7| " + (ar.ready() ? "&aklaar" : "&cniet klaar"));
            }
            case "delete" -> {
                if (a.length < 2) { plugin.msg(s, "Gebruik: /arena delete <naam>"); return; }
                plugin.msg(s, d.deleteArena(a[1]) ? "Arena verwijderd." : "&cArena niet gevonden of nu in gebruik.");
            }
            case "list" -> {
                if (d.arenas().isEmpty()) { plugin.msg(s, "Nog geen arena's."); return; }
                for (DuelManager.Arena ar : d.arenas())
                    plugin.msg(s, "&f" + ar.name + " &7(" + ar.mode + ") " + (ar.ready() ? "&aklaar" : "&cniet klaar") + (ar.active != null ? " &e(bezet)" : ""));
            }
            default -> plugin.msg(s, "&cOnbekend subcommando.");
        }
    }

    private void npc(CommandSender s, Player p, String[] a) {
        if (p == null) { plugin.msg(s, "Alleen voor spelers."); return; }
        if (a.length == 0) {
            plugin.msg(p, "&e/npc create <naam_met_underscores> <actie> &8| &e/npc setaction <actie> &8| &e/npc remove &7(dichtstbijzijnde NPC)");
            plugin.msg(p, "Acties: &fworld:<wereld> &7| &fqueue:1v1 &7| &fqueue:2v2 &7| &fqueue:tower &7| &fmenu &7| &flobby[:naam] &7| &fcmd:<commando>");
            return;
        }
        switch (a[0].toLowerCase()) {
            case "create" -> {
                if (a.length < 3) { plugin.msg(p, "Gebruik: /npc create <naam> <actie>  (gebruik _ voor spaties)"); return; }
                String action = String.join(" ", Arrays.copyOfRange(a, 2, a.length));
                if (!Actions.valid(action)) { plugin.msg(p, "&cOngeldige actie. Typ /npc voor de lijst."); return; }
                plugin.npcs().create(p.getLocation(), a[1].replace('_', ' '), action);
                plugin.msg(p, "NPC aangemaakt met actie &f" + action + "&7.");
            }
            case "setaction" -> {
                if (a.length < 2) { plugin.msg(p, "Gebruik: /npc setaction <actie>"); return; }
                String action = String.join(" ", Arrays.copyOfRange(a, 1, a.length));
                Villager v = plugin.npcs().nearest(p);
                if (v == null) { plugin.msg(p, "&cGeen NPC binnen 5 blokken."); return; }
                if (!Actions.valid(action)) { plugin.msg(p, "&cOngeldige actie."); return; }
                plugin.npcs().setAction(v, action);
                plugin.msg(p, "Actie aangepast naar &f" + action + "&7.");
            }
            case "remove" -> {
                Villager v = plugin.npcs().nearest(p);
                if (v == null) { plugin.msg(p, "&cGeen NPC binnen 5 blokken."); return; }
                v.remove();
                plugin.msg(p, "NPC verwijderd.");
            }
            default -> plugin.msg(p, "&cOnbekend subcommando.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        String n = c.getName().toLowerCase();
        if (n.equals("queue") && a.length == 1) return List.of("1v1", "2v2", "tower", "leave");
        if (n.equals("toplijst") && a.length == 1) return List.of("money", "kills", "deaths", "shards");
        if ((n.equals("geld") || n.equals("betaal")) && a.length == 1) return null;
        if (n.equals("geldbeheer")) { if (!admin(s)) return List.of(); if (a.length == 1) return List.of("give", "take", "set"); return null; }
        if (n.equals("rtp") && a.length == 1) return plugin.getConfig().getStringList("rtp.worlds");
        if (n.equals("keyall") && a.length == 1) return admin(s) ? List.of("now") : List.of();
        if (n.equals("shards")) {
            if (!admin(s)) return List.of();
            if (a.length == 1) return List.of("give", "take", "set");
            return null;
        }
        if (n.equals("lobby") && a.length == 1) return new ArrayList<>(plugin.lobby().names());
        if (!admin(s)) return List.of();
        if (n.equals("setlobby") && a.length == 1) return new ArrayList<>(plugin.lobby().names());
        if (n.equals("arena")) {
            if (a.length == 1) return List.of("create", "scan", "setspawn", "addspawn", "info", "delete", "list");
            if (a.length == 2 && !a[0].equalsIgnoreCase("create") && !a[0].equalsIgnoreCase("list"))
                return plugin.duels().arenas().stream().map(x -> x.name).toList();
            if (a.length == 3 && a[0].equalsIgnoreCase("create")) return List.of("1v1", "2v2", "tower");
            if (a.length == 3 && (a[0].equalsIgnoreCase("setspawn") || a[0].equalsIgnoreCase("addspawn"))) return List.of("1", "2");
        }
        if (n.equals("npc")) {
            if (a.length == 1) return List.of("create", "setaction", "remove");
            if ((a[0].equalsIgnoreCase("create") && a.length == 3) || (a[0].equalsIgnoreCase("setaction") && a.length == 2))
                return List.of("world:", "queue:1v1", "queue:2v2", "queue:tower", "menu", "lobby", "cmd:");
        }
        return List.of();
    }
}

/** Mooie menu's: bewegende kleurenrand, klikgeluid, chat-invoer. */
class GuiManager implements Listener {
    private static final Material[] WAVE = {
            Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE, Material.PINK_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE,
            Material.CYAN_STAINED_GLASS_PANE
    };

    public static class Page implements InventoryHolder {
        final Inventory inv;
        final int rows;
        final boolean animated;
        final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();
        final List<Integer> ring = new ArrayList<>();
        public Runnable refresh;

        public Page(String title, int rows, boolean animated) {
            this.rows = rows;
            this.animated = animated;
            this.inv = Bukkit.createInventory(this, rows * 9, Text.c(title));
        }

        @Override
        public @NotNull Inventory getInventory() { return inv; }

        public Page set(int slot, ItemStack it, Consumer<InventoryClickEvent> handler) {
            inv.setItem(slot, it);
            if (handler != null) handlers.put(slot, handler); else handlers.remove(slot);
            return this;
        }

        public void border() {
            for (int c = 0; c < 9; c++) ring.add(c);
            for (int r = 1; r < rows; r++) ring.add(r * 9 + 8);
            for (int c = 7; c >= 0; c--) ring.add((rows - 1) * 9 + c);
            for (int r = rows - 2; r >= 1; r--) ring.add(r * 9);
            paint(0);
        }

        void paint(int phase) {
            for (int i = 0; i < ring.size(); i++) {
                int slot = ring.get(i);
                if (handlers.containsKey(slot)) continue;
                ItemStack cur = inv.getItem(slot);
                if (cur != null && cur.getType() != Material.AIR && !cur.getType().name().endsWith("_STAINED_GLASS_PANE")) continue;
                inv.setItem(slot, pane(WAVE[(i + phase) % WAVE.length]));
            }
        }

        public void open(Player p) { p.openInventory(inv); }
    }

    private final LandenScore plugin;
    private final Map<UUID, Consumer<String>> prompts = new HashMap<>();
    private BukkitTask task;
    private int phase = 0;

    public GuiManager(LandenScore plugin) { this.plugin = plugin; }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            phase++;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getOpenInventory().getTopInventory().getHolder() instanceof Page pg) {
                    if (pg.animated) pg.paint(phase);
                    if (pg.refresh != null && phase % 3 == 0) pg.refresh.run();
                }
            }
        }, 10L, 6L);
    }

    public void stop() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers())
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Page) p.closeInventory();
    }

    // ---------- items ----------
    static ItemStack pane(Material m) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(" "));
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack build(Material m, String name, List<String> lore, boolean glow) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Text.c(name));
        if (lore != null && !lore.isEmpty()) meta.lore(lore.stream().map(Text::c).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        if (glow) meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack item(Material m, String name, String... lore) {
        return build(m, name, Arrays.asList(lore), false);
    }

    public static ItemStack glow(Material m, String name, String... lore) {
        return build(m, name, Arrays.asList(lore), true);
    }

    // ---------- chat-invoer ----------
    public void prompt(Player p, String question, Consumer<String> callback) {
        p.closeInventory();
        prompts.put(p.getUniqueId(), callback);
        plugin.msg(p, question + " &8(typ &7annuleer &8om te stoppen)");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Consumer<String> cb = prompts.remove(e.getPlayer().getUniqueId());
        if (cb == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("annuleer")) { plugin.msg(p, "Geannuleerd."); return; }
            cb.accept(text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { prompts.remove(e.getPlayer().getUniqueId()); }

    // ---------- klikken ----------
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Page pg)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getInventory()) return;
        Consumer<InventoryClickEvent> h = pg.handlers.get(e.getSlot());
        if (h == null) return;
        if (e.getWhoClicked() instanceof Player p) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        Bukkit.getScheduler().runTask(plugin, () -> h.accept(e));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Page) e.setCancelled(true);
    }
}

/** Keyall-timer: elke X minuten voert de server commando's uit voor alle online spelers. */
class KeyallManager {
    private final LandenScore plugin;
    private final File file;
    private int intervalMin;
    private long nextAt;
    private final List<String> commands = new ArrayList<>();
    private BukkitTask task;

    public KeyallManager(LandenScore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "keyall.yml");
        YamlConfiguration y = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        intervalMin = Math.max(1, y.getInt("interval", plugin.getConfig().getInt("keyall.interval-minutes", 15)));
        commands.addAll(y.contains("commands") ? y.getStringList("commands") : plugin.getConfig().getStringList("keyall.commands"));
        nextAt = y.getLong("next", 0);
    }

    public void start() {
        if (nextAt <= System.currentTimeMillis()) nextAt = System.currentTimeMillis() + intervalMin * 60_000L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (System.currentTimeMillis() >= nextAt) fire();
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        save();
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("interval", intervalMin);
        y.set("commands", commands);
        y.set("next", nextAt);
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Kon keyall.yml niet opslaan: " + e.getMessage());
        }
    }

    public long remainingSeconds() { return Math.max(0, (nextAt - System.currentTimeMillis()) / 1000); }
    public int interval() { return intervalMin; }
    public List<String> commands() { return commands; }

    public void setInterval(int minutes) {
        intervalMin = Math.max(1, minutes);
        nextAt = System.currentTimeMillis() + intervalMin * 60_000L;
        save();
    }

    public void addCommand(String cmd) { commands.add(cmd); save(); }
    public void clearCommands() { commands.clear(); save(); }

    public void fire() {
        nextAt = System.currentTimeMillis() + intervalMin * 60_000L;
        save();
        Bukkit.broadcast(Text.c(plugin.getConfig().getString("keyall.message", "&6&lKEYALL! &7Iedereen krijgt een beloning!")));
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (String cmd : commands) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName()));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }
}

class LandCommand implements CommandExecutor, TabCompleter {
    private final LandenScore plugin;

    public LandCommand(LandenScore plugin) { this.plugin = plugin; }

    private void msg(CommandSender s, String text) {
        s.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getConfig().getString("messages.prefix", "") + text));
    }

    private boolean admin(CommandSender s) { return s.hasPermission("landen.admin"); }

    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        return off.hasPlayedBefore() ? off : null;
    }

    private String nameOf(UUID id) {
        return Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName()).orElse("?");
    }

    private boolean validName(String n) {
        return n.matches("[A-Za-z0-9_]+") && n.length() <= plugin.getConfig().getInt("land.max-name-length", 16);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        CountryManager cm = plugin.countries();

        if (cmd.getName().equalsIgnoreCase("sb")) {
            if (!(sender instanceof Player p)) return true;
            msg(p, plugin.scoreboards().toggle(p) ? "Scoreboard &aaan&7." : "Scoreboard &cuit&7.");
            return true;
        }

        if (a.length == 0) {
            msg(sender, "&e/land info [land] &8| &e/land list &8| &e/land invite <speler> &8| &e/land join <land> &8| &e/land leave &8| &e/land kick <speler>");
            if (admin(sender))
                msg(sender, "&cAdmin: &e/land create <naam> [leider] &8| &e/land delete <land> &8| &e/land rename <land> <nieuw> &8| &e/land setleader <land> <speler> &8| &e/land add <land> <speler> &8| &e/land remove <land> <speler> &8| &e/land reload");
            return true;
        }

        String sub = a[0].toLowerCase();

        // ---- Commando's voor iedereen / console ----
        if (sub.equals("list")) {
            msg(sender, "Landen: &f" + (cm.all().isEmpty() ? "-" : String.join(", ", cm.all().stream().map(c -> c.name).toList())));
            return true;
        }
        if (sub.equals("reload")) {
            if (!admin(sender)) { msg(sender, "&cGeen rechten."); return true; }
            plugin.reloadConfig();
            plugin.scoreboards().start();
            msg(sender, "Config herladen.");
            return true;
        }

        // ---- Admin-commando's (ook vanuit console) ----
        switch (sub) {
            case "create" -> {
                boolean allowed = admin(sender) || plugin.getConfig().getBoolean("land.players-can-create", false);
                if (!allowed) { msg(sender, "&cAlleen admins kunnen landen aanmaken."); return true; }
                if (a.length < 2) { msg(sender, "Gebruik: /land create <naam> [leider]"); return true; }
                if (!validName(a[1])) { msg(sender, "&cOngeldige naam (letters/cijfers/_, max lengte uit config)."); return true; }
                if (cm.get(a[1]) != null) { msg(sender, "&cDie naam bestaat al."); return true; }
                OfflinePlayer leader;
                if (a.length >= 3) {
                    leader = findPlayer(a[2]);
                    if (leader == null) { msg(sender, "&cSpeler niet gevonden."); return true; }
                } else if (sender instanceof Player p) {
                    leader = p;
                } else { msg(sender, "Geef een leider op: /land create <naam> <leider>"); return true; }
                if (cm.ofPlayer(leader.getUniqueId()) != null) { msg(sender, "&cDie speler zit al in een land."); return true; }
                cm.create(a[1], leader.getUniqueId());
                msg(sender, "Land &f" + a[1] + " &7aangemaakt met leider &f" + leader.getName() + "&7.");
                return true;
            }
            case "rename" -> {
                if (!admin(sender)) { msg(sender, "&cGeen rechten."); return true; }
                if (a.length < 3) { msg(sender, "Gebruik: /land rename <land> <nieuwe naam>"); return true; }
                Country c = cm.get(a[1]);
                if (c == null) { msg(sender, "&cLand niet gevonden."); return true; }
                if (!validName(a[2]) || (cm.get(a[2]) != null && !a[1].equalsIgnoreCase(a[2]))) { msg(sender, "&cOngeldige of bestaande naam."); return true; }
                cm.rename(c, a[2]);
                msg(sender, "Land hernoemd naar &f" + a[2] + "&7.");
                return true;
            }
            case "setleader" -> {
                if (!admin(sender)) { msg(sender, "&cGeen rechten."); return true; }
                if (a.length < 3) { msg(sender, "Gebruik: /land setleader <land> <speler>"); return true; }
                Country c = cm.get(a[1]);
                OfflinePlayer t = findPlayer(a[2]);
                if (c == null || t == null) { msg(sender, "&cLand of speler niet gevonden."); return true; }
                Country other = cm.ofPlayer(t.getUniqueId());
                if (other != null && other != c) { msg(sender, "&cDie speler zit in een ander land."); return true; }
                c.members.add(t.getUniqueId());
                c.owner = t.getUniqueId();
                cm.save();
                msg(sender, "&f" + t.getName() + " &7is nu leider van &f" + c.name + "&7.");
                return true;
            }
            case "add" -> {
                if (!admin(sender)) { msg(sender, "&cGeen rechten."); return true; }
                if (a.length < 3) { msg(sender, "Gebruik: /land add <land> <speler>"); return true; }
                Country c = cm.get(a[1]);
                OfflinePlayer t = findPlayer(a[2]);
                if (c == null || t == null) { msg(sender, "&cLand of speler niet gevonden."); return true; }
                if (cm.ofPlayer(t.getUniqueId()) != null) { msg(sender, "&cDie speler zit al in een land."); return true; }
                c.members.add(t.getUniqueId());
                cm.save();
                msg(sender, "&f" + t.getName() + " &7toegevoegd aan &f" + c.name + "&7.");
                return true;
            }
            case "remove" -> {
                if (!admin(sender)) { msg(sender, "&cGeen rechten."); return true; }
                if (a.length < 3) { msg(sender, "Gebruik: /land remove <land> <speler>"); return true; }
                Country c = cm.get(a[1]);
                OfflinePlayer t = findPlayer(a[2]);
                if (c == null || t == null || !c.members.contains(t.getUniqueId())) { msg(sender, "&cLand of lid niet gevonden."); return true; }
                if (c.owner.equals(t.getUniqueId())) { msg(sender, "&cDat is de leider. Gebruik eerst /land setleader of /land delete."); return true; }
                c.members.remove(t.getUniqueId());
                cm.save();
                msg(sender, "&f" + t.getName() + " &7verwijderd uit &f" + c.name + "&7.");
                return true;
            }
            case "delete" -> {
                if (a.length >= 2 && admin(sender)) {
                    Country c = cm.get(a[1]);
                    if (c == null) { msg(sender, "&cLand niet gevonden."); return true; }
                    cm.delete(c);
                    msg(sender, "Land &f" + c.name + " &7verwijderd.");
                    return true;
                }
            }
            default -> { }
        }

        // ---- Speler-commando's ----
        if (!(sender instanceof Player p)) { msg(sender, "Alleen voor spelers."); return true; }
        Country mine = cm.ofPlayer(p.getUniqueId());
        boolean leader = mine != null && mine.owner.equals(p.getUniqueId());

        switch (sub) {
            case "invite" -> {
                if (a.length < 2) { msg(p, "Gebruik: /land invite <speler>"); return true; }
                Country target = mine;
                if (a.length >= 3 && admin(p)) target = cm.get(a[2]);
                else if (!leader) { msg(p, "&cAlleen de leider kan uitnodigen."); return true; }
                if (target == null) { msg(p, "&cLand niet gevonden."); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { msg(p, "&cSpeler niet online."); return true; }
                if (cm.ofPlayer(t.getUniqueId()) != null) { msg(p, "&cDie speler zit al in een land."); return true; }
                cm.invites.put(t.getUniqueId(), target.name);
                msg(p, "Uitnodiging gestuurd naar &f" + t.getName() + "&7.");
                msg(t, "Je bent uitgenodigd voor &f" + target.name + "&7. Typ &e/land join " + target.name);
            }
            case "kick" -> {
                if (!leader) { msg(p, "&cAlleen de leider kan leden verwijderen."); return true; }
                if (a.length < 2) { msg(p, "Gebruik: /land kick <speler>"); return true; }
                OfflinePlayer t = findPlayer(a[1]);
                if (t == null || !mine.members.contains(t.getUniqueId())) { msg(p, "&cDie speler zit niet in jouw land."); return true; }
                if (t.getUniqueId().equals(p.getUniqueId())) { msg(p, "&cJe kunt jezelf niet kicken."); return true; }
                mine.members.remove(t.getUniqueId());
                cm.save();
                msg(p, "&f" + t.getName() + " &7is uit het land gezet.");
            }
            case "join" -> {
                if (a.length < 2) { msg(p, "Gebruik: /land join <land>"); return true; }
                if (mine != null) { msg(p, "&cJe zit al in een land."); return true; }
                Country c = cm.get(a[1]);
                if (c == null || !a[1].equalsIgnoreCase(cm.invites.get(p.getUniqueId()))) {
                    msg(p, "&cGeen uitnodiging voor dat land."); return true;
                }
                c.members.add(p.getUniqueId());
                cm.invites.remove(p.getUniqueId());
                cm.save();
                msg(p, "Je bent nu lid van &f" + c.name + "&7.");
            }
            case "leave" -> {
                if (mine == null) { msg(p, "&cJe zit in geen land."); return true; }
                if (leader) { msg(p, "&cJe bent de leider. Vraag een admin om het land over te dragen of te verwijderen."); return true; }
                mine.members.remove(p.getUniqueId());
                cm.save();
                msg(p, "Je hebt het land verlaten.");
            }
            case "delete" -> {
                if (!admin(p)) { msg(p, "&cAlleen admins kunnen landen verwijderen."); return true; }
                msg(p, "Gebruik: /land delete <land>");
            }
            case "info" -> {
                Country c = a.length > 1 ? cm.get(a[1]) : mine;
                if (c == null) { msg(p, "&cLand niet gevonden."); return true; }
                msg(p, "&f" + c.name + " &7| Leider: &f" + nameOf(c.owner) + " &7| Leden: &f" + c.members.size());
            }
            default -> msg(p, "&cOnbekend subcommando.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (!c.getName().equalsIgnoreCase("land")) return List.of();
        List<String> base = new ArrayList<>(List.of("info", "list", "invite", "join", "leave", "kick"));
        if (admin(s)) base.addAll(List.of("create", "delete", "rename", "setleader", "add", "remove", "reload"));
        if (a.length == 1) return base.stream().filter(x -> x.startsWith(a[0].toLowerCase())).toList();
        if (a.length == 2 && List.of("info", "join", "delete", "rename", "setleader", "add", "remove").contains(a[0].toLowerCase()))
            return plugin.countries().all().stream().map(x -> x.name).filter(x -> x.toLowerCase().startsWith(a[1].toLowerCase())).toList();
        return null; // spelersnamen
    }
}

/** Placeholders voor TAB e.d.: %landen_land% %landen_leden% %landen_rol% %landen_kills% %landen_deaths% %landen_kd% %landen_shards% %landen_playtime% %landen_keyall% */
class LandenExpansion extends PlaceholderExpansion {
    private final LandenScore plugin;

    public LandenExpansion(LandenScore plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "landen"; }
    @Override public @NotNull String getAuthor() { return "LandenScore"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        UUID id = player.getUniqueId();
        Country c = plugin.countries().ofPlayer(id);
        String none = plugin.getConfig().getString("messages.no-land", "Geen land");
        StatsManager st = plugin.stats();
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "land", "team" -> c != null ? c.name : none;
            case "leden" -> c != null ? String.valueOf(c.members.size()) : "0";
            case "rol" -> c == null ? "-" : c.owner.equals(id) ? "Leider" : "Lid";
            case "kills" -> String.valueOf(st.kills(id));
            case "deaths" -> String.valueOf(st.deaths(id));
            case "kd" -> st.deaths(id) == 0 ? String.valueOf(st.kills(id))
                    : String.format(Locale.US, "%.2f", (double) st.kills(id) / st.deaths(id));
            case "shards" -> Fmt.compact(st.shards(id));
            case "shards_raw" -> String.valueOf(st.shards(id));
            case "playtime" -> Fmt.time(st.playtimeSeconds(player));
            case "keyall" -> Fmt.time(plugin.keyall().remainingSeconds());
            default -> null;
        };
    }
}

/** Meerdere lobby's (bv. main, pvp, minigames). "main" is de standaard. */
class LobbyManager implements Listener {
    private final LandenScore plugin;
    private final File file;
    private final NamespacedKey selectorKey;
    private final Map<String, LocData> lobbies = new LinkedHashMap<>();

    public LobbyManager(LandenScore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lobby.yml");
        this.selectorKey = new NamespacedKey(plugin, "selector");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        LocData old = LocData.read(y.getConfigurationSection("lobby")); // oude versie
        if (old != null) lobbies.put("main", old);
        ConfigurationSection s = y.getConfigurationSection("lobbies");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            LocData d = LocData.read(s.getConfigurationSection(k));
            if (d != null) lobbies.put(k.toLowerCase(), d);
        }
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, LocData> e : lobbies.entrySet()) e.getValue().write(y.createSection("lobbies." + e.getKey()));
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Kon lobby.yml niet opslaan: " + ex.getMessage());
        }
    }

    public Set<String> names() { return lobbies.keySet(); }

    public Location get(String name) {
        LocData d = lobbies.get(name.toLowerCase());
        return d == null ? null : d.toLocation();
    }

    /** "main", of anders de eerste lobby die bestaat. */
    public Location get() {
        Location l = get("main");
        if (l != null) return l;
        for (Map.Entry<String, LocData> e : lobbies.entrySet()) {
            if (e.getKey().equals("afk")) continue;
            Location x = e.getValue().toLocation();
            if (x != null) return x;
        }
        return null;
    }

    public void set(String name, Location l) {
        lobbies.put(name.toLowerCase(), LocData.of(l));
        save();
    }

    public boolean delete(String name) {
        boolean r = lobbies.remove(name.toLowerCase()) != null;
        if (r) save();
        return r;
    }

    public boolean teleport(Player p) {
        Location l = get();
        if (l == null) return false;
        p.teleport(l);
        return true;
    }

    public boolean teleport(Player p, String name) {
        Location l = get(name);
        if (l == null) return false;
        p.teleport(l);
        return true;
    }

    private boolean isLobbyWorld(World w) {
        for (LocData d : lobbies.values()) if (d.world().equals(w.getName())) return true;
        return false;
    }

    private ItemStack selector() {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("lobby.selector-material", "COMPASS"));
        ItemStack it = new ItemStack(mat == null ? Material.COMPASS : mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(Text.c(plugin.getConfig().getString("lobby.selector-name", "&aKies een spel")));
        m.getPersistentDataContainer().set(selectorKey, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    public boolean isSelector(ItemStack it) {
        return it != null && it.hasItemMeta()
                && it.getItemMeta().getPersistentDataContainer().has(selectorKey, PersistentDataType.BYTE);
    }

    public void giveSelector(Player p) {
        for (ItemStack it : p.getInventory().getContents()) if (isSelector(it)) return;
        p.getInventory().setItem(plugin.getConfig().getInt("lobby.selector-slot", 4), selector());
    }

    private boolean protectedFor(Player p) {
        return plugin.getConfig().getBoolean("lobby.protect", true)
                && isLobbyWorld(p.getWorld()) && !plugin.duels().inMatch(p);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (plugin.getConfig().getBoolean("lobby.tp-on-join", true) && get() != null)
            Bukkit.getScheduler().runTask(plugin, () -> teleport(p));
        if (plugin.getConfig().getBoolean("lobby.give-selector-on-join", true)) giveSelector(p);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isSelector(e.getItem())) return;
        e.setCancelled(true);
        plugin.menu().open(e.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && protectedFor(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && protectedFor(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (protectedFor(e.getPlayer()) && !e.getPlayer().hasPermission("landen.build")) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (protectedFor(e.getPlayer()) && !e.getPlayer().hasPermission("landen.build")) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (isSelector(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if ((isSelector(e.getCurrentItem()) || isSelector(e.getCursor()))
                && e.getWhoClicked().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
    }
}

/** Locatie met wereldnaam (handig als Multiverse-werelden pas later laden). */
record LocData(String world, double x, double y, double z, float yaw, float pitch) {
    public static LocData of(Location l) {
        return new LocData(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }

    public void write(ConfigurationSection s) {
        s.set("world", world);
        s.set("x", x);
        s.set("y", y);
        s.set("z", z);
        s.set("yaw", (double) yaw);
        s.set("pitch", (double) pitch);
    }

    public static LocData read(ConfigurationSection s) {
        if (s == null || s.getString("world") == null) return null;
        return new LocData(s.getString("world"), s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }
}

class MenuManager {
    private final LandenScore plugin;

    public MenuManager(LandenScore plugin) { this.plugin = plugin; }

    public String ph(Player p, String s) {
        s = plugin.scoreboards().fill(p, s);
        return s.replace("{q_1v1}", String.valueOf(plugin.duels().queueSize("1v1")))
                .replace("{q_2v2}", String.valueOf(plugin.duels().queueSize("2v2")))
                .replace("{q_tower}", String.valueOf(plugin.duels().queueSize("tower")))
                .replace("{playing}", String.valueOf(plugin.duels().playing()));
    }

    public void open(Player p) {
        FileConfiguration cfg = plugin.getConfig();
        int rows = Math.max(3, Math.min(6, cfg.getInt("menu.rows", 6)));
        GuiManager.Page pg = new GuiManager.Page(cfg.getString("menu.title", "&8Menu"), rows, cfg.getBoolean("menu.animated", true));
        pg.border();
        List<Runnable> painters = new ArrayList<>();

        ConfigurationSection items = cfg.getConfigurationSection("menu.items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection s = items.getConfigurationSection(key);
                if (s == null) continue;
                int slot = s.getInt("slot", -1);
                if (slot < 0 || slot >= rows * 9) continue;
                Material parsed = Material.matchMaterial(s.getString("material", "STONE"));
                Material material = parsed == null ? Material.STONE : parsed;
                String rawName = s.getString("name", key);
                List<String> rawLore = s.getStringList("lore");
                boolean glow = s.getBoolean("glow", false);
                String action = s.getString("action", "");
                Supplier<ItemStack> make = () -> GuiManager.build(material, ph(p, rawName),
                        rawLore.stream().map(x -> ph(p, x)).toList(), glow);
                pg.set(slot, make.get(), action.isBlank() ? null : e -> {
                    if (!action.equals("menu")) p.closeInventory();
                    plugin.actions().run(p, action);
                });
                painters.add(() -> pg.inv.setItem(slot, make.get()));
            }
        }

        int ps = cfg.getInt("menu.profile-slot", -1);
        if (ps >= 0 && ps < rows * 9) {
            Supplier<ItemStack> head = () -> {
                ItemStack h = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) h.getItemMeta();
                sm.setOwningPlayer(p);
                sm.displayName(Text.c("&6&l" + p.getName()));
                sm.lore(cfg.getStringList("menu.profile-lines").stream().map(x -> Text.c(ph(p, x))).toList());
                h.setItemMeta(sm);
                return h;
            };
            pg.set(ps, head.get(), null);
            painters.add(() -> pg.inv.setItem(ps, head.get()));
        }

        int as = cfg.getInt("menu.admin-slot", -1);
        if (as >= 0 && as < rows * 9 && p.hasPermission("landen.admin")) {
            pg.set(as, GuiManager.glow(Material.COMMAND_BLOCK, "&c&lBeheer", "&7Alleen zichtbaar voor admins.", "", "&eKlik om te openen"),
                    e -> plugin.admin().main(p));
        }
        pg.refresh = () -> painters.forEach(Runnable::run);
        pg.open(p);
    }
}

/** Geld via Vault: werkt met de eigen economie en met EssentialsX. */
class MoneyService {
    private final LandenScore plugin;
    private List<Map.Entry<UUID, Double>> topCache = new ArrayList<>();
    private long topAt = 0;

    public MoneyService(LandenScore plugin) { this.plugin = plugin; }

    public Economy eco() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    public double balance(OfflinePlayer p) {
        Economy e = eco();
        return e == null ? 0 : e.getBalance(p);
    }

    public String format(double v) { return "$" + Fmt.compact(v); }

    public boolean give(OfflinePlayer p, double amount) {
        Economy e = eco();
        return e != null && amount >= 0 && e.depositPlayer(p, amount).transactionSuccess();
    }

    public boolean take(OfflinePlayer p, double amount) {
        Economy e = eco();
        return e != null && amount >= 0 && e.withdrawPlayer(p, amount).transactionSuccess();
    }

    public boolean set(OfflinePlayer p, double amount) {
        Economy e = eco();
        if (e == null || amount < 0) return false;
        double cur = e.getBalance(p);
        if (amount >= cur) return e.depositPlayer(p, amount - cur).transactionSuccess();
        return e.withdrawPlayer(p, cur - amount).transactionSuccess();
    }

    /** null = gelukt, anders een foutmelding. */
    public String pay(Player from, OfflinePlayer to, double amount) {
        Economy e = eco();
        if (e == null) return "&cGeen economie gevonden.";
        double min = plugin.getConfig().getDouble("economy.min-pay", 1);
        if (amount < min) return "&cHet minimale bedrag is &f" + format(min) + "&c.";
        if (from.getUniqueId().equals(to.getUniqueId())) return "&cJe kunt jezelf niet betalen.";
        if (!e.has(from, amount)) return "&cJe hebt niet genoeg geld.";
        EconomyResponse w = e.withdrawPlayer(from, amount);
        if (!w.transactionSuccess()) return "&cBetaling mislukt.";
        EconomyResponse d = e.depositPlayer(to, amount);
        if (!d.transactionSuccess()) {
            e.depositPlayer(from, amount);
            return "&cBetaling mislukt.";
        }
        Player tp = to.getPlayer();
        if (tp != null) plugin.msg(tp, "&f" + from.getName() + " &7heeft je &a" + format(amount) + " &7betaald.");
        return null;
    }

    /** Top-saldo's (60 seconden gecached). */
    public List<Map.Entry<UUID, Double>> top(int n) {
        long now = System.currentTimeMillis();
        if (now - topAt > 60_000) {
            List<Map.Entry<UUID, Double>> l = new ArrayList<>();
            Economy e = eco();
            if (e != null) {
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    if (!e.hasAccount(op)) continue;
                    l.add(Map.entry(op.getUniqueId(), e.getBalance(op)));
                }
            }
            l.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            topCache = l;
            topAt = now;
        }
        return topCache.size() > n ? new ArrayList<>(topCache.subList(0, n)) : topCache;
    }
}

/** Eenvoudige NPC's (stilstaande dorpelingen). De actie staat op de entity zelf opgeslagen. */
class NpcManager implements Listener {
    private final LandenScore plugin;
    private final NamespacedKey key;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public NpcManager(LandenScore plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "npc_action");
    }

    public Villager create(Location loc, String name, String action) {
        return loc.getWorld().spawn(loc, Villager.class, v -> {
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setPersistent(true);
            v.setRemoveWhenFarAway(false);
            v.setCollidable(false);
            v.customName(Text.c(name));
            v.setCustomNameVisible(true);
            v.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
        });
    }

    public boolean isNpc(Entity e) {
        return e.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public void setAction(Entity npc, String action) {
        npc.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
    }

    public Villager nearest(Player p) {
        Villager best = null;
        double bd = 25;
        for (Entity e : p.getNearbyEntities(5, 5, 5)) {
            if (e instanceof Villager v && isNpc(v)) {
                double d = v.getLocation().distanceSquared(p.getLocation());
                if (d < bd) { bd = d; best = v; }
            }
        }
        return best;
    }

    private void click(Player p, Entity npc) {
        long now = System.currentTimeMillis();
        if (now - cooldown.getOrDefault(p.getUniqueId(), 0L) < 600) return;
        cooldown.put(p.getUniqueId(), now);
        plugin.actions().run(p, npc.getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!isNpc(e.getRightClicked())) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        click(e.getPlayer(), e.getRightClicked());
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!isNpc(e.getEntity())) return;
        e.setCancelled(true);
        if (e.getDamager() instanceof Player p) click(p, e.getEntity());
    }
}

/** Eigen economie die zich als Vault-economie aanmeldt (saldo's in balances.yml). */
class OwnEconomy implements Economy {
    private final LandenScore plugin;
    private final File file;
    private final Map<UUID, Double> bal = new HashMap<>();
    private boolean dirty;

    public OwnEconomy(LandenScore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "balances.yml");
        if (file.exists()) {
            ConfigurationSection s = YamlConfiguration.loadConfiguration(file).getConfigurationSection("balances");
            if (s != null) for (String k : s.getKeys(false)) {
                try { bal.put(UUID.fromString(k), s.getDouble(k)); } catch (IllegalArgumentException ignored) { }
            }
        }
    }

    public synchronized void save() {
        if (!dirty && file.exists()) return;
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Double> e : bal.entrySet()) y.set("balances." + e.getKey(), e.getValue());
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Kon balances.yml niet opslaan: " + e.getMessage());
        }
    }

    private double start() { return plugin.getConfig().getDouble("economy.start-balance", 100); }

    private synchronized double balanceOf(UUID id) {
        Double b = bal.get(id);
        if (b == null) { b = start(); bal.put(id, b); dirty = true; }
        return b;
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private synchronized EconomyResponse withdraw(UUID id, double amount) {
        double cur = balanceOf(id);
        if (Double.isNaN(amount) || amount < 0) return new EconomyResponse(0, cur, EconomyResponse.ResponseType.FAILURE, "Ongeldig bedrag");
        if (cur < amount) return new EconomyResponse(0, cur, EconomyResponse.ResponseType.FAILURE, "Onvoldoende saldo");
        double n = round(cur - amount);
        bal.put(id, n);
        dirty = true;
        return new EconomyResponse(amount, n, EconomyResponse.ResponseType.SUCCESS, null);
    }

    private synchronized EconomyResponse deposit(UUID id, double amount) {
        double cur = balanceOf(id);
        if (Double.isNaN(amount) || amount < 0) return new EconomyResponse(0, cur, EconomyResponse.ResponseType.FAILURE, "Ongeldig bedrag");
        double n = round(cur + amount);
        bal.put(id, n);
        dirty = true;
        return new EconomyResponse(amount, n, EconomyResponse.ResponseType.SUCCESS, null);
    }

    private UUID id(String name) { return Bukkit.getOfflinePlayer(name).getUniqueId(); }

    private EconomyResponse noBank() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banken worden niet ondersteund");
    }

    // ---------- Economy ----------
    @Override public boolean isEnabled() { return plugin.isEnabled(); }
    @Override public String getName() { return "LandenScore"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return 2; }
    @Override public String format(double amount) { return String.format(Locale.US, "$%,.2f", amount); }
    @Override public String currencyNamePlural() { return "dollars"; }
    @Override public String currencyNameSingular() { return "dollar"; }

    @Override public synchronized boolean hasAccount(String playerName) { return bal.containsKey(id(playerName)); }
    @Override public synchronized boolean hasAccount(OfflinePlayer player) { return bal.containsKey(player.getUniqueId()); }
    @Override public boolean hasAccount(String playerName, String worldName) { return hasAccount(playerName); }
    @Override public boolean hasAccount(OfflinePlayer player, String worldName) { return hasAccount(player); }

    @Override public double getBalance(String playerName) { return balanceOf(id(playerName)); }
    @Override public double getBalance(OfflinePlayer player) { return balanceOf(player.getUniqueId()); }
    @Override public double getBalance(String playerName, String world) { return getBalance(playerName); }
    @Override public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }

    @Override public boolean has(String playerName, double amount) { return getBalance(playerName) >= amount; }
    @Override public boolean has(OfflinePlayer player, double amount) { return getBalance(player) >= amount; }
    @Override public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }
    @Override public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }

    @Override public EconomyResponse withdrawPlayer(String playerName, double amount) { return withdraw(id(playerName), amount); }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) { return withdraw(player.getUniqueId(), amount); }
    @Override public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return withdrawPlayer(playerName, amount); }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player, amount); }

    @Override public EconomyResponse depositPlayer(String playerName, double amount) { return deposit(id(playerName), amount); }
    @Override public EconomyResponse depositPlayer(OfflinePlayer player, double amount) { return deposit(player.getUniqueId(), amount); }
    @Override public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return depositPlayer(playerName, amount); }
    @Override public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player, amount); }

    @Override public EconomyResponse createBank(String name, String player) { return noBank(); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return noBank(); }
    @Override public EconomyResponse deleteBank(String name) { return noBank(); }
    @Override public EconomyResponse bankBalance(String name) { return noBank(); }
    @Override public EconomyResponse bankHas(String name, double amount) { return noBank(); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return noBank(); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return noBank(); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return noBank(); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return noBank(); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return noBank(); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return noBank(); }
    @Override public List<String> getBanks() { return List.of(); }

    @Override public synchronized boolean createPlayerAccount(String playerName) { return create(id(playerName)); }
    @Override public synchronized boolean createPlayerAccount(OfflinePlayer player) { return create(player.getUniqueId()); }
    @Override public boolean createPlayerAccount(String playerName, String worldName) { return createPlayerAccount(playerName); }
    @Override public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return createPlayerAccount(player); }

    private synchronized boolean create(UUID id) {
        if (bal.containsKey(id)) return false;
        bal.put(id, start());
        dirty = true;
        return true;
    }
}

/** Beloningen met cooldown. Commando's worden als console uitgevoerd ({player} = speler). */
class RewardManager {
    static class Reward {
        final String id;
        String name;
        Material icon = Material.CHEST;
        String description = "";
        long cooldown = 86400;      // seconden; -1 = eenmalig
        String permission = "";
        final List<String> commands = new ArrayList<>();

        Reward(String id, String name) { this.id = id; this.name = name; }
    }

    private final LandenScore plugin;
    private final File rewardFile;
    private final File claimFile;
    private final Map<String, Reward> rewards = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> claims = new HashMap<>();

    public RewardManager(LandenScore plugin) {
        this.plugin = plugin;
        this.rewardFile = new File(plugin.getDataFolder(), "rewards.yml");
        this.claimFile = new File(plugin.getDataFolder(), "claims.yml");
        loadRewards();
        loadClaims();
    }

    private void loadRewards() {
        if (!rewardFile.exists()) { defaults(); return; }
        ConfigurationSection s = YamlConfiguration.loadConfiguration(rewardFile).getConfigurationSection("rewards");
        if (s == null) return;
        for (String id : s.getKeys(false)) {
            Reward r = new Reward(id, s.getString(id + ".name", id));
            Material m = Material.matchMaterial(s.getString(id + ".icon", "CHEST"));
            r.icon = m == null ? Material.CHEST : m;
            r.description = s.getString(id + ".description", "");
            r.cooldown = s.getLong(id + ".cooldown", 86400);
            r.permission = s.getString(id + ".permission", "");
            r.commands.addAll(s.getStringList(id + ".commands"));
            rewards.put(id, r);
        }
    }

    private String tpl(String key, String def) { return plugin.getConfig().getString("rewards.templates." + key, def); }

    private void defaults() {
        Reward d = new Reward("dagelijks", "&e&lDagelijkse beloning");
        d.icon = Material.GOLD_INGOT;
        d.description = "Elke dag gratis geld, shards en een sleutel.";
        d.cooldown = 86400;
        d.commands.add(tpl("money", "geldbeheer give {player} {amount}").replace("{amount}", "500"));
        d.commands.add(tpl("diamonds", "shards give {player} {amount}").replace("{amount}", "5"));
        d.commands.add(tpl("crate-key", "crate key give {player} {crate} {amount}").replace("{crate}", "daily").replace("{amount}", "1"));
        rewards.put(d.id, d);

        Reward w = new Reward("wekelijks", "&b&lWekelijkse beloning");
        w.icon = Material.DIAMOND;
        w.description = "Elke week een grote beloning.";
        w.cooldown = 604800;
        w.commands.add(tpl("money", "geldbeheer give {player} {amount}").replace("{amount}", "5000"));
        w.commands.add(tpl("diamonds", "shards give {player} {amount}").replace("{amount}", "50"));
        rewards.put(w.id, w);

        Reward v = new Reward("premium", "&d&lPremium Crate &7(rank)");
        v.icon = Material.ENDER_CHEST;
        v.description = "Alleen voor spelers met een rank.|Elke week een premium sleutel.";
        v.cooldown = 604800;
        v.permission = "landen.reward.vip";
        v.commands.add(tpl("crate-key", "crate key give {player} {crate} {amount}").replace("{crate}", "premium").replace("{amount}", "1"));
        rewards.put(v.id, v);
        save();
    }

    private void loadClaims() {
        if (!claimFile.exists()) return;
        ConfigurationSection s = YamlConfiguration.loadConfiguration(claimFile).getConfigurationSection("claims");
        if (s == null) return;
        for (String u : s.getKeys(false)) {
            try {
                Map<String, Long> m = new HashMap<>();
                ConfigurationSection c = s.getConfigurationSection(u);
                if (c != null) for (String id : c.getKeys(false)) m.put(id, c.getLong(id));
                claims.put(UUID.fromString(u), m);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Reward r : rewards.values()) {
            String k = "rewards." + r.id;
            y.set(k + ".name", r.name);
            y.set(k + ".icon", r.icon.name());
            y.set(k + ".description", r.description);
            y.set(k + ".cooldown", r.cooldown);
            y.set(k + ".permission", r.permission);
            y.set(k + ".commands", r.commands);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(rewardFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Kon rewards.yml niet opslaan: " + e.getMessage());
        }
    }

    private void saveClaims() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Long>> e : claims.entrySet())
            for (Map.Entry<String, Long> c : e.getValue().entrySet()) y.set("claims." + e.getKey() + "." + c.getKey(), c.getValue());
        try {
            plugin.getDataFolder().mkdirs();
            y.save(claimFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Kon claims.yml niet opslaan: " + e.getMessage());
        }
    }

    public Collection<Reward> all() { return rewards.values(); }

    public Reward get(String id) { return rewards.get(id); }

    public Reward create(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("&[0-9a-fk-or]", "").replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) base = "beloning";
        String id = base;
        int n = 2;
        while (rewards.containsKey(id)) id = base + "_" + n++;
        Reward r = new Reward(id, name);
        rewards.put(id, r);
        save();
        return r;
    }

    public void delete(String id) {
        rewards.remove(id);
        save();
    }

    /** -2 = geen toegang, -1 = al geclaimd (eenmalig), 0 = beschikbaar, >0 = seconden wachten */
    public long wait(Player p, Reward r) {
        if (!r.permission.isBlank() && !p.hasPermission(r.permission)) return -2;
        Map<String, Long> mine = claims.getOrDefault(p.getUniqueId(), Map.of());
        Long last = mine.get(r.id);
        if (last == null) return 0;
        if (r.cooldown < 0) return -1;
        long left = (last + r.cooldown * 1000 - System.currentTimeMillis() + 999) / 1000;
        return Math.max(0, left);
    }

    public boolean claim(Player p, Reward r) {
        if (wait(p, r) != 0) return false;
        claims.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(r.id, System.currentTimeMillis());
        saveClaims();
        run(p.getName(), r);
        return true;
    }

    public void run(String playerName, Reward r) {
        for (String cmd : r.commands) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
    }
}

class RewardMenu {
    private final LandenScore plugin;

    public RewardMenu(LandenScore plugin) { this.plugin = plugin; }

    public void open(Player p) {
        GuiManager.Page pg = new GuiManager.Page("&8» <gb:#f6d365:#fda085>BELONINGEN</g> &8«", 6, true);
        pg.border();
        int i = 0, available = 0;
        for (RewardManager.Reward r : plugin.rewards().all()) {
            long w = plugin.rewards().wait(p, r);
            if (w == 0) available++;
            if (i >= 28) continue;
            int slot = (1 + i / 7) * 9 + 1 + i % 7;
            i++;
            List<String> lore = new ArrayList<>();
            if (!r.description.isBlank()) {
                for (String line : r.description.split("\\|")) lore.add("&7" + line.trim());
                lore.add("");
            }
            lore.add("&7Cooldown: &f" + (r.cooldown < 0 ? "eenmalig" : Fmt.time(r.cooldown)));
            lore.add("");
            if (w == 0) lore.add("&a✔ Beschikbaar! &eKlik om te claimen");
            else if (w > 0) lore.add("&c⌛ Nog &f" + Fmt.time(w));
            else if (w == -1) lore.add("&8Al geclaimd (eenmalig)");
            else { lore.add("&c✖ Vereist een rank"); lore.add("&eKlik voor de webshop"); }
            pg.set(slot, GuiManager.build(r.icon, "&f" + r.name, lore, w == 0), e -> {
                if (w == -2) { p.closeInventory(); plugin.store().open(p); return; }
                if (plugin.rewards().claim(p, r)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    plugin.msg(p, "Je hebt &f" + r.name + " &7geclaimd!");
                } else {
                    plugin.msg(p, "&cJe kunt deze beloning nu niet claimen.");
                }
                open(p);
            });
        }
        if (plugin.rewards().all().isEmpty()) pg.set(22, GuiManager.item(Material.BARRIER, "&7Er zijn nog geen beloningen"), null);
        pg.set(4, GuiManager.glow(Material.NETHER_STAR, "&6&lJouw beloningen",
                "&7Claim hier je gratis beloningen.", "", "&7Beschikbaar: &a" + available), null);
        pg.set(49, GuiManager.item(Material.ARROW, "&c« Terug", "&7Naar het spelmenu."), e -> plugin.menu().open(p));
        if (p.hasPermission("landen.admin"))
            pg.set(53, GuiManager.item(Material.COMMAND_BLOCK, "&c&lBeloningen beheren", "&7Alleen voor admins.", "", "&eKlik"),
                    e -> plugin.admin().rewards(p, 0));
        pg.open(p);
    }

    /** Claimt de dagelijkse beloning (rewards.daily-id in config.yml). */
    public void claimDaily(Player p) {
        String id = plugin.getConfig().getString("rewards.daily-id", "dagelijks");
        RewardManager.Reward r = plugin.rewards().get(id);
        if (r == null) { plugin.msg(p, "&cEr is geen dagelijkse beloning ingesteld."); return; }
        long w = plugin.rewards().wait(p, r);
        if (w == 0 && plugin.rewards().claim(p, r)) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            p.showTitle(Title.title(Text.c("&6&lDAGELIJKSE BELONING"), Text.c("&7Tot morgen!"),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400))));
            plugin.msg(p, "Je hebt je dagelijkse beloning geclaimd!");
        } else if (w > 0) {
            plugin.msg(p, "&cJe kunt over &f" + Fmt.time(w) + " &copnieuw claimen.");
        } else if (w == -2) {
            plugin.msg(p, "&cDeze beloning is alleen voor spelers met een rank (/store).");
        } else {
            plugin.msg(p, "&cJe hebt deze beloning al geclaimd.");
        }
    }
}

/** /rtp: willekeurig teleporteren in een gewone (Multiverse-)wereld. */
class RtpManager {
    private static final Set<Material> UNSAFE = EnumSet.of(Material.CACTUS, Material.MAGMA_BLOCK, Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE, Material.POWDER_SNOW, Material.SWEET_BERRY_BUSH, Material.LAVA, Material.WATER,
            Material.FIRE, Material.WITHER_ROSE);

    private final LandenScore plugin;
    private final Map<UUID, Long> last = new HashMap<>();
    private final Random random = new Random();

    public RtpManager(LandenScore plugin) { this.plugin = plugin; }

    public void teleport(Player p, String worldArg) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("rtp.enabled", true)) { plugin.msg(p, "&cRTP staat uit."); return; }
        if (plugin.duels().inMatch(p)) { plugin.msg(p, "&cJe zit in een match."); return; }
        String name = worldArg != null ? worldArg : cfg.getString("rtp.default-world", "survival");
        List<String> allowed = cfg.getStringList("rtp.worlds");
        if (!allowed.contains(name)) {
            plugin.msg(p, "&cRTP kan niet in die wereld. Beschikbaar: &f" + String.join(", ", allowed));
            return;
        }
        World w = Bukkit.getWorld(name);
        if (w == null || w.getEnvironment() != World.Environment.NORMAL) {
            plugin.msg(p, "&cDie wereld bestaat niet, is niet geladen of ondersteunt geen RTP.");
            return;
        }
        long cd = cfg.getLong("rtp.cooldown-seconds", 30) * 1000;
        long now = System.currentTimeMillis();
        Long l = last.get(p.getUniqueId());
        if (l != null && now - l < cd && !p.hasPermission("landen.admin")) {
            plugin.msg(p, "&cWacht nog &f" + Fmt.time((cd - (now - l) + 999) / 1000) + " &cvoor je opnieuw RTP't.");
            return;
        }
        Location target = find(w);
        if (target == null) { plugin.msg(p, "&cGeen veilige plek gevonden, probeer het opnieuw."); return; }
        last.put(p.getUniqueId(), now);
        plugin.duels().leaveQueue(p);
        p.teleportAsync(target).thenAccept(ok -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (ok) plugin.msg(p, "Je bent willekeurig geteleporteerd naar &f" + target.getBlockX() + ", " + target.getBlockZ() + "&7.");
        }));
    }

    private Location find(World w) {
        FileConfiguration cfg = plugin.getConfig();
        Location center = w.getSpawnLocation();
        int min = Math.max(0, cfg.getInt("rtp.min-radius", 200));
        int max = Math.max(min + 1, cfg.getInt("rtp.radius", 5000));
        for (int i = 0; i < 15; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = min + random.nextDouble() * (max - min);
            int x = (int) Math.round(center.getX() + Math.cos(angle) * dist);
            int z = (int) Math.round(center.getZ() + Math.sin(angle) * dist);
            if (!w.getWorldBorder().isInside(new Location(w, x, 64, z))) continue;
            int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block ground = w.getBlockAt(x, y, z);
            if (!ground.getType().isSolid() || UNSAFE.contains(ground.getType())) continue;
            if (!w.getBlockAt(x, y + 1, z).isPassable() || !w.getBlockAt(x, y + 2, z).isPassable()) continue;
            return new Location(w, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }
}

class ScoreboardManager {
    private final LandenScore plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Set<UUID> disabled = new HashSet<>();
    private Economy economy;
    private boolean papi;
    private int taskId = -1;

    public ScoreboardManager(LandenScore plugin) { this.plugin = plugin; }

    public void start() {
        stop();
        papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = rsp != null ? rsp.getProvider() : null;
        if (economy == null) plugin.getLogger().warning("Geen Vault-economie gevonden (installeer Vault + EssentialsX). Geld toont 0.");
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;
        long ticks = Math.max(10, plugin.getConfig().getLong("scoreboard.update-ticks", 20));
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) update(p);
        }, 20L, ticks).getTaskId();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        for (Player p : Bukkit.getOnlinePlayers()) remove(p);
        boards.clear();
    }

    public boolean toggle(Player p) {
        if (disabled.remove(p.getUniqueId())) return true;
        disabled.add(p.getUniqueId());
        remove(p);
        return false;
    }

    public void remove(Player p) {
        boards.remove(p.getUniqueId());
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private String entry(int i) { return "\u00A7" + Integer.toHexString(i) + "\u00A7r"; }

    /** Verbergt de rode cijfers rechts (Paper 1.20.4+). Faalt stil als de API ontbreekt. */
    private void hideNumbers(Objective obj) {
        try {
            Class<?> nf = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
            Object blank = nf.getMethod("blank").invoke(null);
            Objective.class.getMethod("numberFormat", nf).invoke(obj, blank);
        } catch (Throwable ignored) { }
    }

    private void update(Player p) {
        if (disabled.contains(p.getUniqueId())) return;
        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        if (lines.size() > 15) lines = lines.subList(0, 15);

        Scoreboard board = boards.get(p.getUniqueId());
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("landen", Criteria.DUMMY, Component.empty());
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            hideNumbers(obj);
            for (int i = 0; i < lines.size(); i++) {
                Team t = board.registerNewTeam("l" + i);
                t.addEntry(entry(i));
                obj.getScore(entry(i)).setScore(lines.size() - i);
            }
            boards.put(p.getUniqueId(), board);
            p.setScoreboard(board);
        }
        Objective obj = board.getObjective("landen");
        if (obj == null) return;
        obj.displayName(Text.c(fill(p, plugin.getConfig().getString("scoreboard.title", ""))));
        for (int i = 0; i < lines.size(); i++) {
            Team t = board.getTeam("l" + i);
            if (t != null) t.prefix(Text.c(fill(p, lines.get(i))));
        }
    }

    public String fill(Player p, String s) {
        UUID id = p.getUniqueId();
        Country c = plugin.countries().ofPlayer(id);
        double money = economy != null ? economy.getBalance(p) : 0;
        StatsManager st = plugin.stats();
        int k = st.kills(id), d = st.deaths(id);
        String land = c != null ? c.name : plugin.getConfig().getString("messages.no-land", "Geen land");
        s = s.replace("{player}", p.getName())
                .replace("{money}", Fmt.compact(money))
                .replace("{money_full}", String.format(Locale.US, "%,.2f", money))
                .replace("{shards}", Fmt.compact(st.shards(id)))
                .replace("{currency}", plugin.getConfig().getString("currency.name", "Diamanten"))
                .replace("{symbol}", plugin.getConfig().getString("currency.symbol", "\u25C6"))
                .replace("{kills}", String.valueOf(k))
                .replace("{deaths}", String.valueOf(d))
                .replace("{kd}", d == 0 ? String.valueOf(k) : String.format(Locale.US, "%.2f", (double) k / d))
                .replace("{playtime}", Fmt.time(st.playtimeSeconds(p)))
                .replace("{keyall}", Fmt.time(plugin.keyall().remainingSeconds()))
                .replace("{rank}", rank(p))
                .replace("{land}", land)
                .replace("{team}", land)
                .replace("{land_members}", c != null ? String.valueOf(c.members.size()) : "0")
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        if (papi) s = PlaceholderAPI.setPlaceholders(p, s);
        return s;
    }

    private String rank(Player p) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return "-";
        try {
            var user = net.luckperms.api.LuckPermsProvider.get().getPlayerAdapter(Player.class).getUser(p);
            return user.getPrimaryGroup();
        } catch (Throwable t) {
            return "-";
        }
    }
}

/** Winkel waar je items koopt met geld (Vault), echte diamanten (items) of shards. */
class ShopManager {
    static class Item {
        final String id;
        String name;
        Material icon = Material.CHEST;
        String description = "";
        long price = 100;
        String currency = "money";   // money, diamond of shards
        String permission = "";
        final List<String> commands = new ArrayList<>();

        Item(String id, String name) { this.id = id; this.name = name; }
    }

    private final LandenScore plugin;
    private final File file;
    private final Map<String, Item> items = new LinkedHashMap<>();

    public ShopManager(LandenScore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shop.yml");
        load();
    }

    private void load() {
        if (!file.exists()) { defaults(); return; }
        ConfigurationSection s = YamlConfiguration.loadConfiguration(file).getConfigurationSection("items");
        if (s == null) return;
        for (String id : s.getKeys(false)) {
            Item it = new Item(id, s.getString(id + ".name", id));
            Material m = Material.matchMaterial(s.getString(id + ".icon", "CHEST"));
            it.icon = m == null ? Material.CHEST : m;
            it.description = s.getString(id + ".description", "");
            it.price = s.getLong(id + ".price", 100);
            it.currency = s.getString(id + ".currency", "money");
            it.permission = s.getString(id + ".permission", "");
            it.commands.addAll(s.getStringList(id + ".commands"));
            items.put(id, it);
        }
    }

    private void defaults() {
        Item a = new Item("diamanten", "&b&l10 Diamanten");
        a.icon = Material.DIAMOND;
        a.description = "Tien echte diamanten.";
        a.price = 500;
        a.currency = "money";
        a.commands.add("give {player} diamond 10");
        items.put(a.id, a);
        Item b = new Item("shards", "&d&l50 Shards");
        b.icon = Material.AMETHYST_SHARD;
        b.description = "Ruil 20 diamanten in voor 50 shards.";
        b.price = 20;
        b.currency = "diamond";
        b.commands.add("shards give {player} 50");
        items.put(b.id, b);
        save();
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Item it : items.values()) {
            String k = "items." + it.id;
            y.set(k + ".name", it.name);
            y.set(k + ".icon", it.icon.name());
            y.set(k + ".description", it.description);
            y.set(k + ".price", it.price);
            y.set(k + ".currency", it.currency);
            y.set(k + ".permission", it.permission);
            y.set(k + ".commands", it.commands);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Kon shop.yml niet opslaan: " + e.getMessage());
        }
    }

    public Collection<Item> all() { return items.values(); }

    public Item create(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("&[0-9a-fk-or]", "").replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) base = "item";
        String id = base;
        int n = 2;
        while (items.containsKey(id)) id = base + "_" + n++;
        Item it = new Item(id, name);
        items.put(id, it);
        save();
        return it;
    }

    public void delete(String id) {
        items.remove(id);
        save();
    }

    // ---------- betalen ----------
    private Economy economy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    private int diamonds(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents())
            if (s != null && s.getType() == Material.DIAMOND && !s.hasItemMeta()) n += s.getAmount();
        return n;
    }

    private void takeDiamonds(Player p, int amount) {
        ItemStack[] c = p.getInventory().getStorageContents();
        for (int i = 0; i < c.length && amount > 0; i++) {
            ItemStack s = c[i];
            if (s == null || s.getType() != Material.DIAMOND || s.hasItemMeta()) continue;
            int t = Math.min(amount, s.getAmount());
            s.setAmount(s.getAmount() - t);
            if (s.getAmount() <= 0) c[i] = null;
            amount -= t;
        }
        p.getInventory().setStorageContents(c);
    }

    public double balance(Player p, String cur) {
        return switch (cur) {
            case "diamond" -> diamonds(p);
            case "shards" -> plugin.stats().shards(p.getUniqueId());
            default -> {
                Economy e = economy();
                yield e == null ? 0 : e.getBalance(p);
            }
        };
    }

    public boolean canAfford(Player p, Item it) { return balance(p, it.currency) >= it.price; }

    public String curName(String cur) {
        return switch (cur) {
            case "diamond" -> "Diamanten (items)";
            case "shards" -> plugin.getConfig().getString("currency.name", "Shards");
            default -> "Geld";
        };
    }

    public String priceLabel(Item it) {
        return switch (it.currency) {
            case "diamond" -> "&b" + it.price + " diamant" + (it.price == 1 ? "" : "en");
            case "shards" -> "&d" + it.price + " " + plugin.getConfig().getString("currency.name", "Shards");
            default -> "&a$" + Fmt.compact(it.price);
        };
    }

    public String balanceLabel(Player p, String cur) {
        double b = balance(p, cur);
        return switch (cur) {
            case "diamond" -> "&b" + (long) b;
            case "shards" -> "&d" + Fmt.compact(b);
            default -> "&a$" + Fmt.compact(b);
        };
    }

    /** null = gelukt, anders een foutmelding. */
    public String buy(Player p, Item it) {
        if (!it.permission.isBlank() && !p.hasPermission(it.permission)) return "&cJe hebt hier geen toegang toe.";
        if (it.commands.isEmpty()) return "&cDit item heeft nog geen commando's (admin: /setwinkel).";
        switch (it.currency) {
            case "diamond" -> {
                if (diamonds(p) < it.price) return "&cJe hebt &f" + it.price + " &cdiamanten (items) nodig.";
                takeDiamonds(p, (int) it.price);
            }
            case "shards" -> {
                if (plugin.stats().shards(p.getUniqueId()) < it.price) return "&cJe hebt niet genoeg " + plugin.getConfig().getString("currency.name", "Shards") + ".";
                plugin.stats().addShards(p.getUniqueId(), -it.price);
            }
            default -> {
                Economy eco = economy();
                if (eco == null) return "&cGeen economie gevonden (Vault + EssentialsX).";
                if (!eco.has(p, it.price)) return "&cJe hebt niet genoeg geld.";
                EconomyResponse r = eco.withdrawPlayer(p, it.price);
                if (!r.transactionSuccess()) return "&cBetaling mislukt.";
            }
        }
        run(p.getName(), it);
        return null;
    }

    public void run(String playerName, Item it) {
        for (String cmd : it.commands) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
    }
}

class ShopMenu {
    private static final int PER_PAGE = 28;
    private final LandenScore plugin;

    public ShopMenu(LandenScore plugin) { this.plugin = plugin; }

    public void open(Player p, int page) {
        List<ShopManager.Item> items = new ArrayList<>(plugin.shop().all());
        int pages = Math.max(1, (items.size() + PER_PAGE - 1) / PER_PAGE);
        final int cur = Math.max(0, Math.min(page, pages - 1));
        GuiManager.Page pg = new GuiManager.Page("&8» <gb:#43e97b:#38f9d7>WINKEL</g> &8«", 6, true);
        pg.border();
        for (int i = 0; i < PER_PAGE && cur * PER_PAGE + i < items.size(); i++) {
            ShopManager.Item it = items.get(cur * PER_PAGE + i);
            int slot = (1 + i / 7) * 9 + 1 + i % 7;
            boolean can = plugin.shop().canAfford(p, it);
            List<String> lore = new ArrayList<>();
            if (!it.description.isBlank()) {
                for (String line : it.description.split("\\|")) lore.add("&7" + line.trim());
                lore.add("");
            }
            lore.add("&7Prijs: " + plugin.shop().priceLabel(it));
            lore.add("&7Jij hebt: " + plugin.shop().balanceLabel(p, it.currency));
            lore.add("");
            lore.add(can ? "&eKlik om te kopen" : "&cJe hebt hier te weinig voor");
            pg.set(slot, GuiManager.build(it.icon, "&f" + it.name, lore, can), e -> {
                String err = plugin.shop().buy(p, it);
                if (err != null) {
                    plugin.msg(p, err);
                } else {
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
                    plugin.msg(p, "Gekocht: &f" + it.name + " &7voor " + plugin.shop().priceLabel(it) + "&7.");
                }
                open(p, cur);
            });
        }
        if (items.isEmpty()) pg.set(22, GuiManager.item(Material.BARRIER, "&7De winkel is nog leeg"), null);
        pg.set(4, GuiManager.glow(Material.EMERALD, "&a&lJouw saldo",
                "&7Geld: " + plugin.shop().balanceLabel(p, "money"),
                "&7" + plugin.getConfig().getString("currency.name", "Shards") + ": " + plugin.shop().balanceLabel(p, "shards"),
                "&7Diamanten: " + plugin.shop().balanceLabel(p, "diamond")), null);
        if (cur > 0) pg.set(46, GuiManager.item(Material.SPECTRAL_ARROW, "&e« Vorige pagina"), e -> open(p, cur - 1));
        if (cur < pages - 1) pg.set(52, GuiManager.item(Material.SPECTRAL_ARROW, "&eVolgende pagina »"), e -> open(p, cur + 1));
        pg.set(49, GuiManager.item(Material.ARROW, "&c« Terug", "&7Naar het spelmenu."), e -> plugin.menu().open(p));
        if (p.hasPermission("landen.admin"))
            pg.set(53, GuiManager.item(Material.COMMAND_BLOCK, "&c&lWinkel beheren", "&7Alleen voor admins.", "", "&eKlik"), e -> plugin.admin().shop(p, 0));
        pg.open(p);
    }
}

/** Kills, deaths en diamanten (shards). Playtime komt van de Minecraft-statistieken. */
class StatsManager implements Listener {
    static class Stat { int kills, deaths; long shards; }

    private final LandenScore plugin;
    private final File file;
    private final Map<UUID, Stat> data = new HashMap<>();
    private BukkitTask saver;

    public StatsManager(LandenScore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        ConfigurationSection s = YamlConfiguration.loadConfiguration(file).getConfigurationSection("players");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            try {
                Stat st = new Stat();
                st.kills = s.getInt(k + ".kills");
                st.deaths = s.getInt(k + ".deaths");
                st.shards = s.getLong(k + ".shards");
                data.put(UUID.fromString(k), st);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Stat> e : data.entrySet()) {
            String k = "players." + e.getKey();
            y.set(k + ".kills", e.getValue().kills);
            y.set(k + ".deaths", e.getValue().deaths);
            y.set(k + ".shards", e.getValue().shards);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Kon stats.yml niet opslaan: " + ex.getMessage());
        }
    }

    public void start() { saver = Bukkit.getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L); }

    public void stop() {
        if (saver != null) saver.cancel();
        save();
    }

    private Stat get(UUID id) { return data.computeIfAbsent(id, k -> new Stat()); }

    public int kills(UUID id) { return get(id).kills; }
    public int deaths(UUID id) { return get(id).deaths; }
    public long shards(UUID id) { return get(id).shards; }
    public void addKill(UUID id) { get(id).kills++; }
    public void addDeath(UUID id) { get(id).deaths++; }
    public void addShards(UUID id, long n) { Stat s = get(id); s.shards = Math.max(0, s.shards + n); }
    public void setShards(UUID id, long n) { get(id).shards = Math.max(0, n); }

    public List<Map.Entry<UUID, Long>> top(String type, int limit) {
        List<Map.Entry<UUID, Long>> l = new ArrayList<>();
        for (Map.Entry<UUID, Stat> e : data.entrySet()) {
            long v = switch (type) {
                case "deaths" -> e.getValue().deaths;
                case "shards" -> e.getValue().shards;
                default -> e.getValue().kills;
            };
            if (v > 0) l.add(Map.entry(e.getKey(), v));
        }
        l.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return l.size() > limit ? new ArrayList<>(l.subList(0, limit)) : l;
    }

    public long playtimeSeconds(OfflinePlayer p) {
        try {
            return p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        addDeath(victim.getUniqueId());
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) addKill(killer.getUniqueId());
    }
}

/** Webshop-menu (links naar je CraftingStore) en een aankoopmelding voor de hele server. */
class StoreMenu {
    private final LandenScore plugin;

    public StoreMenu(LandenScore plugin) { this.plugin = plugin; }

    public void open(Player p) {
        FileConfiguration cfg = plugin.getConfig();
        GuiManager.Page pg = new GuiManager.Page("&8» <gb:#ffd200:#f7971e>WEBSHOP</g> &8«", 5, true);
        pg.border();
        pg.set(4, GuiManager.glow(Material.NETHER_STAR, "&6&lWebshop",
                "&7Koop ranks, sleutels en meer.", "", "&7Klik op een pakket voor de link.", "&f" + cfg.getString("store.url", "")), null);
        ConfigurationSection pk = cfg.getConfigurationSection("store.packages");
        if (pk != null) {
            for (String key : pk.getKeys(false)) {
                ConfigurationSection s = pk.getConfigurationSection(key);
                if (s == null) continue;
                int slot = s.getInt("slot", -1);
                if (slot < 0 || slot >= 45) continue;
                Material parsed = Material.matchMaterial(s.getString("material", "CHEST"));
                String url = s.getString("url", cfg.getString("store.url", ""));
                List<String> lore = new ArrayList<>(s.getStringList("lore"));
                lore.add("");
                lore.add("&eKlik voor de link");
                pg.set(slot, GuiManager.build(parsed == null ? Material.CHEST : parsed, s.getString("name", key), lore, s.getBoolean("glow", false)),
                        e -> { p.closeInventory(); sendLink(p, url); });
            }
        }
        pg.set(40, GuiManager.item(Material.ARROW, "&c« Terug", "&7Naar het spelmenu."), e -> plugin.menu().open(p));
        pg.open(p);
    }

    public void sendLink(Player p, String url) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            plugin.msg(p, "&cDe webshop-link is nog niet ingesteld (store.url in config.yml).");
            return;
        }
        p.sendMessage(Text.c(plugin.getConfig().getString("messages.prefix", "") + "&eKlik hier voor de webshop: ")
                .append(Component.text(url).color(NamedTextColor.AQUA).decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(url))));
    }

    /** Wordt door de webshop als console-commando aangeroepen: buyalert <speler> <pakket> */
    public void announce(String player, String pkg) {
        String msg = plugin.getConfig().getString("store.alert-message", "&6&l★ &e{player} &7heeft &f{package} &7gekocht! &6&l★")
                .replace("{player}", player).replace("{package}", pkg);
        Bukkit.broadcast(Text.c(msg));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(Text.c("&6&l" + player), Text.c("&7kocht &f" + pkg),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(600))));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        Player buyer = Bukkit.getPlayerExact(player);
        if (buyer != null) {
            buyer.getWorld().spawn(buyer.getLocation(), Firework.class, fw -> {
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder().withColor(Color.ORANGE, Color.YELLOW).with(FireworkEffect.Type.BALL_LARGE).build());
                fw.setFireworkMeta(meta);
            });
        }
    }
}

/**
 * Tekst met &-kleurcodes, hex (&#ff9900) en kleurverloop:
 * <g:#ff9900:#ff0066>tekst</g>  of  <gb:...>tekst</g> (vet).
 */
final class Text {
    private static final LegacyComponentSerializer L = LegacyComponentSerializer.builder().character('&').hexColors().build();
    private static final Pattern GRAD = Pattern.compile("<g(b?):(#[0-9a-fA-F]{6}):(#[0-9a-fA-F]{6})>(.*?)</g>");

    private Text() {}

    public static Component c(String s) {
        if (s == null) s = "";
        return L.deserialize(gradients(s)).decoration(TextDecoration.ITALIC, false);
    }

    public static String gradients(String s) {
        Matcher m = GRAD.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            boolean bold = !m.group(1).isEmpty();
            m.appendReplacement(out, Matcher.quoteReplacement(gradient(m.group(4), m.group(2), m.group(3), bold)));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String gradient(String text, String from, String to, boolean bold) {
        int r1 = Integer.parseInt(from.substring(1, 3), 16), g1 = Integer.parseInt(from.substring(3, 5), 16), b1 = Integer.parseInt(from.substring(5, 7), 16);
        int r2 = Integer.parseInt(to.substring(1, 3), 16), g2 = Integer.parseInt(to.substring(3, 5), 16), b2 = Integer.parseInt(to.substring(5, 7), 16);
        int n = text.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double t = n <= 1 ? 0 : (double) i / (n - 1);
            int r = (int) Math.round(r1 + (r2 - r1) * t);
            int g = (int) Math.round(g1 + (g2 - g1) * t);
            int b = (int) Math.round(b1 + (b2 - b1) * t);
            sb.append(String.format("&#%02x%02x%02x", r, g, b));
            if (bold) sb.append("&l");
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }
}

class TopMenu {
    private static final int[] SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
    private final LandenScore plugin;

    public TopMenu(LandenScore plugin) { this.plugin = plugin; }

    private void tab(GuiManager.Page pg, Player p, int slot, Material m, String name, String type, String active) {
        pg.set(slot, GuiManager.build(m, name, List.of("&7Klik om te bekijken"), type.equals(active)), e -> open(p, type));
    }

    public void open(Player p, String typeIn) {
        String type = List.of("money", "shards", "kills", "deaths").contains(typeIn.toLowerCase()) ? typeIn.toLowerCase() : "money";
        String cur = plugin.getConfig().getString("currency.name", "Shards");
        GuiManager.Page pg = new GuiManager.Page("&8» <gb:#f6d365:#fda085>TOPLIJST</g> &8«", 6, true);
        pg.border();
        tab(pg, p, 10, Material.GOLD_INGOT, "&a&lMoney", "money", type);
        tab(pg, p, 12, Material.AMETHYST_SHARD, "&d&l" + cur, "shards", type);
        tab(pg, p, 14, Material.IRON_SWORD, "&c&lKills", "kills", type);
        tab(pg, p, 16, Material.SKELETON_SKULL, "&6&lDeaths", "deaths", type);

        List<UUID> ids = new ArrayList<>();
        List<String> vals = new ArrayList<>();
        if (type.equals("money")) {
            for (Map.Entry<UUID, Double> e : plugin.money().top(10)) { ids.add(e.getKey()); vals.add(plugin.money().format(e.getValue())); }
        } else {
            for (Map.Entry<UUID, Long> e : plugin.stats().top(type, 10)) {
                ids.add(e.getKey());
                vals.add(type.equals("shards") ? Fmt.compact(e.getValue()) : String.valueOf(e.getValue()));
            }
        }
        String label = switch (type) {
            case "money" -> "Money";
            case "shards" -> cur;
            case "kills" -> "Kills";
            default -> "Deaths";
        };
        for (int i = 0; i < ids.size() && i < SLOTS.length; i++) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(ids.get(i));
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(op);
            sm.displayName(Text.c("&e#" + (i + 1) + " &f" + Optional.ofNullable(op.getName()).orElse("?")));
            sm.lore(List.of(Text.c("&7" + label + ": &f" + vals.get(i))));
            if (i < 3) sm.setEnchantmentGlintOverride(true);
            head.setItemMeta(sm);
            pg.set(SLOTS[i], head, null);
        }
        if (ids.isEmpty()) pg.set(22, GuiManager.item(Material.BARRIER, "&7Nog niemand in de lijst"), null);
        pg.set(49, GuiManager.item(Material.ARROW, "&c« Terug", "&7Naar het spelmenu."), e -> plugin.menu().open(p));
        pg.open(p);
    }
}

/** Standaard config.yml, wordt bij de eerste start naar de pluginmap geschreven. */
class DefaultConfig {
    static final String TEXT = """
# ================= Placeholders =================
# {player} {money} {money_full} {shards} {currency} {symbol} {kills} {deaths} {kd}
# {playtime} {keyall} {rank} {land} {team} {land_members} {online}
# Ook PlaceholderAPI-placeholders (bv. %luckperms_prefix%) werken.
# Kleuren: &a, hex &#ff9900, kleurverloop <g:#ff9900:#ff0066>tekst</g> (<gb:...> = vet)

currency:
  name: "Shards"
  symbol: "✦"

# Scoreboard zoals op de screenshot. Symbolen hangen van je lettertype/resourcepack af; pas ze gerust aan.
scoreboard:
  enabled: true
  update-ticks: 20
  title: "<gb:#ffb347:#ff5e62>MIJN SERVER</g>"
  lines:
    - "&8&m                      "
    - "&a$ &fMoney &a{money}"
    - "&d{symbol} &f{currency} &d{shards}"
    - "&c⚔ &fKills &c{kills}"
    - "&6☠ &fDeaths &6{deaths}"
    - "&3⌛ &fKeyall &b{keyall}"
    - "&e⏱ &fPlaytime &e{playtime}"
    - "&9⚑ &fTeam &9{land}"
    - "&8&m                      "
    - "&7play.jouwserver.nl"

messages:
  no-land: "Geen land"
  prefix: "&8[&6Server&8] &7"

stats:
  count-duels: true           # kills/deaths in PvP-matches meetellen

land:
  max-name-length: 16
  players-can-create: false   # false = alleen admins (landen.admin) maken landen aan

lobby:
  tp-on-join: true
  give-selector-on-join: true
  selector-slot: 4
  selector-material: COMPASS
  selector-name: "&a&lKies een spel &7(rechtermuisklik)"
  protect: true

afk:
  override-essentials: true
  shards-per-minute: 1        # diamanten per minuut voor spelers in de AFK-zone (0 = uit)
  radius: 20                  # grootte van de AFK-zone rond /setafk

arena:
  marker-block: GOLD_BLOCK
  scan-radius: 60
  remove-markers: false

# ================= Webshop (CraftingStore) =================
# Zet je winkel-link hieronder. Pakketten verschijnen in /store en in het spelmenu.
store:
  url: "https://jouwwinkel.craftingstore.net"
  alert-message: "&6&l★ &e{player} &7heeft &f{package} &7gekocht in de webshop! &6&l★"
  packages:
    vip:
      slot: 20
      material: GOLD_INGOT
      name: "&e&lVIP Rank"
      lore: ["&7Kleur in de chat, extra voordelen", "&7en een eigen rank."]
      glow: true
    mvp:
      slot: 22
      material: DIAMOND
      name: "&b&lMVP Rank"
      lore: ["&7Alles van VIP en meer."]
      glow: true
    keys:
      slot: 24
      material: TRIPWIRE_HOOK
      name: "&6&lCrate-sleutels"
      lore: ["&7Sleutels voor Phoenix Crates."]
    diamonds:
      slot: 31
      material: EMERALD
      name: "&a&lDiamanten"
      lore: ["&7Koop diamanten voor in-game."]

# ================= Willekeurig teleporteren =================
rtp:
  enabled: true
  default-world: survival
  worlds: [survival]
  radius: 5000
  min-radius: 200
  cooldown-seconds: 30

# ================= Beloningen & keyall =================
# Sjablonen voor het beheermenu (/setbeloningen). {player} wordt ingevuld bij het uitvoeren.
# LET OP: controleer het crate-commando in de documentatie van Phoenix Crates en pas het hier aan!
economy:
  mode: auto              # auto = eigen economie alleen als er geen andere is; own = altijd eigen; vault = nooit eigen
  start-balance: 100
  min-pay: 1
  override-commands: true # bij eigen economie: /balance /pay /baltop gaan naar /geld /betaal /geldtop

rewards:
  daily-id: dagelijks     # welke beloning /dagelijks en de NPC-actie 'daily' claimt
  templates:
    crate-key: "crate key give {player} {crate} {amount}"
    lp-rank: "lp user {player} parent add {group}"
    lp-rank-temp: "lp user {player} parent addtemp {group} {duration}"
    diamonds: "shards give {player} {amount}"
    money: "geldbeheer give {player} {amount}"   # werkt met de eigen economie en met EssentialsX
    diamond-item: "give {player} diamond {amount}"

keyall:
  interval-minutes: 15
  message: "&6&lKEYALL! &7Iedereen krijgt een beloning!"
  commands: []        # beter via /beheer -> Keyall

# ================= Menu =================
# Acties: world:<wereld>  queue:1v1|2v2|tower  lobby[:naam]  cmd:<commando>
#         menu rewards daily shop store bank top[:type] rtp[:wereld] afk
menu:
  title: "&8» <gb:#ffb347:#ff5e62>SPELMENU</g> &8«"
  rows: 6
  animated: true
  profile-slot: 13
  profile-lines:
    - "&9⚑ &7Team: &f{land}"
    - "&b{symbol} &7Rank: &f{rank}"
    - "&7K/D: &f{kd}"
    - "&7Online: &f{online}"
  admin-slot: 49
  items:
    money:
      slot: 10
      material: GOLD_INGOT
      name: "&a$ &fMoney"
      lore: ["&a{money}", "", "&eKlik voor de bank"]
      action: "bank"
    shards:
      slot: 11
      material: AMETHYST_SHARD
      name: "&d{symbol} &f{currency}"
      lore: ["&d{shards}", "", "&eKlik voor de winkel"]
      action: "shop"
    kills:
      slot: 12
      material: IRON_SWORD
      name: "&c⚔ &fKills"
      lore: ["&c{kills}", "", "&eKlik voor de toplijst"]
      action: "top:kills"
    deaths:
      slot: 14
      material: SKELETON_SKULL
      name: "&6☠ &fDeaths"
      lore: ["&6{deaths}", "", "&eKlik voor de toplijst"]
      action: "top:deaths"
    keyall:
      slot: 15
      material: TRIPWIRE_HOOK
      name: "&3⌛ &fKeyall"
      lore: ["&b{keyall}", "", "&7Tot de volgende keyall"]
    playtime:
      slot: 16
      material: CLOCK
      name: "&e⏱ &fPlaytime"
      lore: ["&e{playtime}"]
    duel1:
      slot: 20
      material: IRON_SWORD
      name: "&c&lPvP 1v1"
      lore: ["&7Vecht één tegen één.", "", "&7In wachtrij: &f{q_1v1}", "", "&eKlik om mee te doen"]
      action: "queue:1v1"
    duel2:
      slot: 22
      material: DIAMOND_SWORD
      name: "&6&lPvP 2v2"
      lore: ["&7Vecht met een teamgenoot.", "", "&7In wachtrij: &f{q_2v2}", "", "&eKlik om mee te doen"]
      action: "queue:2v2"
    tower:
      slot: 24
      material: BRICKS
      name: "&e&lTower Drop"
      lore: ["&7Sta op je toren en krijg elke paar", "&7seconden een willekeurig item of blok.", "&7De laatste die overblijft wint!", "", "&7In wachtrij: &f{q_tower}", "", "&eKlik om mee te doen"]
      action: "queue:tower"
      glow: true
    survival:
      slot: 28
      material: GRASS_BLOCK
      name: "&a&lSurvival"
      lore: ["&7Teleporteer naar de wereld 'survival'.", "", "&eKlik om te gaan"]
      action: "world:survival"
    dropper:
      slot: 29
      material: DROPPER
      name: "&b&lDropper"
      lore: ["&7Teleporteer naar de wereld 'dropper'.", "", "&eKlik om te gaan"]
      action: "world:dropper"
    rtp:
      slot: 31
      material: ENDER_PEARL
      name: "&5&lRTP"
      lore: ["&7Teleporteer naar een willekeurige plek.", "", "&eKlik om te gaan"]
      action: "rtp"
    afk:
      slot: 33
      material: CLOCK
      name: "&7&lAFK-zone"
      lore: ["&7Ga AFK en verdien shards.", "", "&eKlik om te gaan"]
      action: "afk"
    lobby:
      slot: 34
      material: RED_BED
      name: "&d&lLobby"
      lore: ["&7Terug naar de hoofdlobby.", "", "&eKlik om te gaan"]
      action: "lobby"
    bank:
      slot: 37
      material: GOLD_BLOCK
      name: "&6&lBank"
      lore: ["&7Saldo, betalen en meer.", "", "&eKlik om te openen"]
      action: "bank"
    daily:
      slot: 38
      material: SUNFLOWER
      name: "&e&lDagelijkse beloning"
      lore: ["&7Claim je gratis dagelijkse beloning", "&7(geld, shards en een sleutel).", "", "&eKlik om te claimen"]
      action: "daily"
      glow: true
    rewards:
      slot: 39
      material: ENDER_CHEST
      name: "&6&lBeloningen"
      lore: ["&7Al je beloningen op een rij,", "&7ook die van je rank.", "", "&eKlik om te openen"]
      action: "rewards"
      glow: true
    shop:
      slot: 41
      material: EMERALD
      name: "&a&lWinkel"
      lore: ["&7Koop items met geld, diamanten", "&7of shards.", "", "&eKlik om te openen"]
      action: "shop"
    store:
      slot: 42
      material: NETHER_STAR
      name: "&6&lWebshop"
      lore: ["&7Koop ranks, sleutels en meer.", "", "&eKlik om te openen"]
      action: "store"
      glow: true
    top:
      slot: 43
      material: GOLDEN_HELMET
      name: "&e&lToplijst"
      lore: ["&7De beste spelers van de server.", "", "&eKlik om te openen"]
      action: "top"

duels:
  lobby: main
  countdown: 5
  end-delay: 5
  max-seconds: 600
  kit:
    helmet: IRON_HELMET
    chestplate: IRON_CHESTPLATE
    leggings: IRON_LEGGINGS
    boots: IRON_BOOTS
    items:
      - IRON_SWORD
      - BOW
      - ARROW:16
      - GOLDEN_APPLE:3
      - COOKED_BEEF:16

tower:
  lobby: main
  min-players: 2
  queue-wait: 20
  countdown: 5
  drop-seconds: 10
  kill-below: 30
  max-seconds: 900
  end-delay: 5
  kit:
    items:
      - STONE_SWORD
  # Geen emmers of TNT hier: die laten sporen na in de arena. Dubbel opnemen = grotere kans.
  drops:
    - OAK_PLANKS:16
    - COBBLESTONE:16
    - SANDSTONE:16
    - WHITE_WOOL:12
    - OAK_PLANKS:16
    - COBBLESTONE:16
    - COOKED_BEEF:8
    - ARROW:8
    - SNOWBALL:8
    - IRON_SWORD
    - BOW
    - GOLDEN_APPLE
    - ENDER_PEARL
    - SHIELD
    - IRON_AXE
    - COBWEB:4
    - DIAMOND_SWORD
""";
}
