package com.minepalm.syncer.player.mysql;


import com.minepalm.syncer.player.bukkit.PlayerDataPotion;
import kr.msleague.mslibrary.database.impl.internal.MySQLDatabase;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class MySQLPlayerPotionDatabase {

    private final String table;
    private final MySQLDatabase database;

    public void init(){
        database.execute(connection ->{
            PreparedStatement ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + table + " ( " +
                    "`row_id` BIGINT AUTO_INCREMENT UNIQUE, " +
                    "`uuid` VARCHAR(36), " +
                    "`data` TEXT, " +
                    "PRIMARY KEY(`uuid`)) " +
                    "charset=utf8mb4");
            ps.execute();
        });
    }

    public CompletableFuture<Void> save(UUID uuid, PlayerDataPotion data){
        return database.executeAsync(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO "+table+" (`uuid`, `data`) VALUES(?, ?) ON DUPLICATE KEY UPDATE `data`=VALUES(`data`)");
            ps.setString(1, uuid.toString());
            ps.setString(2, data.toJson());
            ps.execute();
            return null;
        });
    }

    public CompletableFuture<PlayerDataPotion> load(UUID uuid){
        return database.executeAsync(connection -> {
            PreparedStatement ps = connection.prepareStatement("SELECT `data` FROM "+table+" WHERE `uuid`=?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return PlayerDataPotion.from(rs.getString(1));
            }

            return PlayerDataPotion.empty();

        });
    }

}
