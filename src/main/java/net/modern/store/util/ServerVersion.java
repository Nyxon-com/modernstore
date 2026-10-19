package net.modern.store.util;

import org.bukkit.Bukkit;

public final class ServerVersion {

    private static volatile ServerVersion current;

    private final int major;
    private final int minor;
    private final String display;
    private final boolean parsed;

    private ServerVersion(int major, int minor, String display, boolean parsed) {
        this.major   = major;
        this.minor   = minor;
        this.display = display;
        this.parsed  = parsed;
    }

    public static ServerVersion getCurrent() {
        if (current == null) {
            synchronized (ServerVersion.class) {
                if (current == null) current = detect();
            }
        }
        return current;
    }

    static void reset() { current = null; }

    private static ServerVersion detect() {
        String raw = Bukkit.getBukkitVersion();
        try {
            String versionPart = raw.split("-")[0];
            String[] parts     = versionPart.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return new ServerVersion(major, minor, versionPart, true);
        } catch (Exception e) {
            return new ServerVersion(-1, -1, raw, false);
        }
    }

    public boolean isSupported() {
        if (!parsed) return true;
        if (major == 1) return minor >= 19;
        return major >= 2;
    }

    public boolean isAtLeast1_20_5() {
        if (!parsed) return true;
        if (major > 1) return true;
        if (minor > 20) return true;
        if (minor == 20) {
            return true;
        }
        return false;
    }

    public int getMajor()           { return major;   }
    public int getMinor()           { return minor;   }
    public String getDisplayVersion() { return display; }
    public boolean isParsed()       { return parsed;  }
}
