//made with ❤ by nyxon.
package net.nyxon.store.config;

import net.nyxon.store.NyxonStore;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final NyxonStore plugin;
    private final File logFile;

    public LogManager(NyxonStore plugin) {
        this.plugin  = plugin;
        this.logFile = new File(plugin.getDataFolder(), "log.txt");
        ensureExists();
    }

    public void logPurchase(Player player, String poolName, String itemId, String itemLabel, double price) {
        LocalDateTime now  = LocalDateTime.now();
        String date = now.format(DATE_FMT);
        String time = now.format(TIME_FMT);

        String entry = String.format(
                "date=%s time=%s player=%s pool=%s item=%s label=%s price=%.2f",
                date, time, player.getName(), poolName, itemId, itemLabel, price);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> writeEntry(entry));
    }

    public boolean reload() {
        return ensureExists();
    }

    private boolean ensureExists() {
        if (logFile.exists()) return false;

        File dir = plugin.getDataFolder();
        if (!dir.exists()) dir.mkdirs();

        try {
            logFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("[NyxonStore/Log] Could not create log.txt: " + e.getMessage());
        }
        return true;
    }

    private void writeEntry(String entry) {
        try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
            out.println(entry);
        } catch (IOException e) {
            plugin.getLogger().severe("[NyxonStore/Log] Could not write to log.txt: " + e.getMessage());
        }
    }
}
