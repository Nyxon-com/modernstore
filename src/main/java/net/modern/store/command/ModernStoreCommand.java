//made with love by nyxon

package net.modern.store.command;

import net.modern.store.ModernStore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ModernStoreCommand implements CommandExecutor, TabCompleter {

  private static final MiniMessage MM = MiniMessage.miniMessage();

  private static final String PERM_OPEN  = "nyxonstore.open";
  private static final String PERM_ADMIN = "nyxonstore.admin";

  private final ModernStore plugin;

  public ModernStoreCommand(ModernStore plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
               @NotNull String label, @NotNull String[] args) {
    if (args.length == 0) {
      return handleRootOpen(sender);
    }
    return switch (args[0].toLowerCase()) {
      case "open"  -> handleOpen(sender, args);
      case "admin" -> handleAdmin(sender, args);
      default -> {
        sendUsage(sender);
        yield true;
      }
    };
  }

  private boolean handleRootOpen(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(MM.deserialize("<red>Only players can open the store.</red>"));
      return true;
    }
    if (!player.hasPermission(PERM_OPEN)) {
      sendNoPermission(player);
      return true;
    }
    plugin.getGuiManager().open(player);
    return true;
  }

  private boolean handleOpen(CommandSender sender, String[] args) {
    if (args.length >= 3 && sender.hasPermission(PERM_ADMIN)) {
      String poolName = args[1];
      if (!poolExists(poolName)) { sendPoolNotFound(sender); return true; }
      Player target = Bukkit.getPlayer(args[2]);
      if (target == null) { sendPlayerNotFound(sender, args[2]); return true; }
      plugin.getGuiManager().open(target, poolName);
      return true;
    }

    if (!(sender instanceof Player player)) {
      sender.sendMessage(MM.deserialize(
          "<red>Usage: /ns open &lt;pool&gt; &lt;player&gt; (requires nyxonstore.admin)</red>"));
      return true;
    }
    if (!player.hasPermission(PERM_OPEN)) { sendNoPermission(player); return true; }

    String poolName = args.length >= 2 ? args[1] : plugin.getConfigManager().getDefaultPool();
    if (!poolExists(poolName)) { sendPoolNotFound(player); return true; }

    plugin.getGuiManager().open(player, poolName);
    return true;
  }

  private boolean handleAdmin(CommandSender sender, String[] args) {
    if (!sender.hasPermission(PERM_ADMIN)) { sendNoPermission(sender); return true; }

    if (args.length < 2) { sendAdminUsage(sender); return true; }

    return switch (args[1].toLowerCase()) {
      case "reload"      -> handleReload(sender);
      case "forcerotate" -> handleForceRotate(sender, args);
      case "edit"        -> handleEdit(sender, args);
      default            -> { sendAdminUsage(sender); yield true; }
    };
  }

  private boolean handleReload(CommandSender sender) {
    boolean anyRegenerated =
        plugin.getConfigManager().load()    |
        plugin.getGuiConfig().load()        |
        plugin.getConfirmationConfig().load()|
        plugin.getPoolConfig().load()       |
        plugin.getDataManager().reload()    |
        plugin.getLogManager().reload();

    plugin.getItemPoolManager().loadAllActiveItems();
    plugin.getGuiManager().invalidateFiller();

    if (anyRegenerated) {
      send(sender, "<yellow>Some config files were missing and have been regenerated.</yellow>");
    } else {
      sender.sendMessage(MM.deserialize(plugin.getConfigManager().getMessage("reloaded")));
    }
    return true;
  }

  private boolean handleForceRotate(CommandSender sender, String[] args) {
    String poolName = args.length >= 3 ? args[2] : plugin.getConfigManager().getDefaultPool();
    if (!poolExists(poolName)) { sendPoolNotFound(sender); return true; }

    plugin.getItemPoolManager().rotatePool(poolName, true);
    sender.sendMessage(MM.deserialize(plugin.getConfigManager().getMessage("rotation-forced")));

    if (sender instanceof Player player) {
      try {
        Sound sound = Sound.valueOf(plugin.getConfigManager().getForceRotateSound().sound());
        player.playSound(player.getLocation(), sound,
            plugin.getConfigManager().getForceRotateSound().volume(),
            plugin.getConfigManager().getForceRotateSound().pitch());
      } catch (IllegalArgumentException e) {
        plugin.getLogger().warning("[ModernStore/Command] Invalid force-rotate sound '"
            + plugin.getConfigManager().getForceRotateSound().sound() + "' in config.");
      }
    }
    return true;
  }

  private boolean handleEdit(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(MM.deserialize("<red>Only players can use the editor.</red>"));
      return true;
    }
    if (args.length >= 3) {
      String poolName = args[2];
      if (!poolExists(poolName)) { sendPoolNotFound(player); return true; }
      plugin.getPoolEditorGUI().openPoolEditor(player, poolName);
    } else {
      plugin.getPoolEditorGUI().openMainMenu(player);
    }
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                        @NotNull String alias, @NotNull String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      if (sender.hasPermission(PERM_OPEN))  completions.add("open");
      if (sender.hasPermission(PERM_ADMIN)) completions.add("admin");

    } else if (args.length == 2) {
      String sub = args[0].toLowerCase();
      if (sub.equals("admin") && sender.hasPermission(PERM_ADMIN)) {
        completions.addAll(List.of("edit", "reload", "forcerotate"));
      } else if (sub.equals("open") && sender.hasPermission(PERM_OPEN)) {
        completions.addAll(plugin.getPoolConfig().getPoolNames());
      }

    } else if (args.length == 3) {
      String sub  = args[0].toLowerCase();
      String sub2 = args[1].toLowerCase();
      if (sub.equals("admin") && sender.hasPermission(PERM_ADMIN)) {
        if (sub2.equals("edit") || sub2.equals("forcerotate")) {
          completions.addAll(plugin.getPoolConfig().getPoolNames());
        }
      } else if (sub.equals("open") && sender.hasPermission(PERM_ADMIN)) {
        Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
      }
    }

    String partial = args[args.length - 1].toLowerCase();
    completions.removeIf(c -> !c.toLowerCase().startsWith(partial));
    return completions;
  }

  private boolean poolExists(String poolName) {
    return plugin.getPoolConfig().getPoolNames().contains(poolName);
  }

  private void sendNoPermission(CommandSender sender) {
    sender.sendMessage(MM.deserialize(plugin.getConfigManager().getMessage("no-permission")));
  }

  private void sendPoolNotFound(CommandSender sender) {
    send(sender, "<red>Pool not found.</red>");
  }

  private void sendPlayerNotFound(CommandSender sender, String name) {
    send(sender, "<red>Player '" + name + "' is not online.</red>");
  }

  private void send(CommandSender sender, String miniMessage) {
    sender.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix() + miniMessage));
  }

  private void sendUsage(CommandSender sender) {
    send(sender, "<gray>Usage: /ns [open|admin]</gray>");
  }

  private void sendAdminUsage(CommandSender sender) {
    if (sender instanceof Player) {
      send(sender, "<green>Admin commands:");
      send(sender, "<gray>/ns admin edit [pool]");
      send(sender, "<gray>/ns admin reload");
      send(sender, "<gray>/ns admin forcerotate [pool]");
    } else {
      send(sender, "<red>Usage: /ns admin [edit|reload|forcerotate [pool]]</red>");
    }
  }
}
