//made with ❤ by nyxon
package net.nyxon.store.task;

import net.nyxon.store.NyxonStore;
import org.bukkit.scheduler.BukkitRunnable;

public class RotationTask extends BukkitRunnable {

    private final NyxonStore plugin;

    public RotationTask(NyxonStore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getItemPoolManager().checkTimer();
    }
}
