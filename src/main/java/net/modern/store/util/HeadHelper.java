//made with love by nyxon

package net.modern.store.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HeadHelper {

  private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"(.*?)\"");

  private static final ConcurrentHashMap<String, PlayerProfile> TEXTURE_CACHE = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<String, PlayerProfile> OWNER_CACHE = new ConcurrentHashMap<>();

  private HeadHelper() {}

  public static void applyTexture(ItemStack head, String base64) {
    if (base64 == null || base64.isBlank()) return;
    if (head == null || !(head.getItemMeta() instanceof SkullMeta meta)) return;

    PlayerProfile profile = TEXTURE_CACHE.computeIfAbsent(base64, HeadHelper::buildTextureProfile);
    if (profile == null) return;

    meta.setOwnerProfile(profile);
    head.setItemMeta(meta);
  }

  @SuppressWarnings("deprecation")
  public static void applyOwner(ItemStack head, String playerName) {
    if (playerName == null || playerName.isBlank()) return;
    if (head == null || !(head.getItemMeta() instanceof SkullMeta meta)) return;

    try {
      meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
      head.setItemMeta(meta);
    } catch (Exception ignored) {
    }
  }

  public static void clearCaches() {
    TEXTURE_CACHE.clear();
    OWNER_CACHE.clear();
  }

  private static PlayerProfile buildTextureProfile(String base64) {
    try {
      String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
      Matcher matcher = URL_PATTERN.matcher(decoded);
      if (!matcher.find()) return null;

      URL url = URI.create(matcher.group(1)).toURL();

      PlayerProfile profile  = Bukkit.createPlayerProfile(UUID.randomUUID());
      PlayerTextures textures = profile.getTextures();
      textures.setSkin(url);
      profile.setTextures(textures);
      return profile;
    } catch (Exception e) {
      return null;
    }
  }
}
