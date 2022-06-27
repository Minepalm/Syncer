package com.minepalm.syncer.player.bukkit;

import com.minepalm.arkarangutils.bukkit.SimpleConfig;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PlayerSyncerConf extends SimpleConfig {

    @Getter
    private final String timeoutText, illegalAccessText;

    protected PlayerSyncerConf(JavaPlugin plugin, String fileName) {
        super(plugin, fileName);
        timeoutText = config.getString("Message.KICK_TIMEOUT").replace("&", "§");
        illegalAccessText = config.getString("Message.KICK_ILLEGAL_JOIN").replace("&", "§");
    }

    public List<String> getStrategies(){
        return config.getStringList("flags");
    }

    public long getTimeout(){
        return config.getLong("joinTimeoutMills");
    }

    public long getExtendingTimeoutPeriod(){
        return config.getLong("extendingTimeoutPeriodMills");
    }

    public String getMySQLName(){
        return config.getString("TravelLibrary.mysql");
    }

    public boolean logResults(){
        return config.getBoolean("logResult");
    }
}
