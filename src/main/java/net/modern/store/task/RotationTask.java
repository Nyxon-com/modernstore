package net.modern.store.task;

import net.modern.store.ModernStore;
import org.bukkit.scheduler.BukkitRunnable;

public class RotationTask extends BukkitRunnable {

    private final ModernStore plugin;

    public RotationTask(ModernStore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getItemPoolManager().checkTimer();
    }
}
