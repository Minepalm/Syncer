package com.minepalm.syncer.player.bukkit.strategies;

import com.minepalm.syncer.player.bukkit.PlayerData;
import org.bukkit.entity.Player;

public class SetFly implements ApplyStrategy {

    @Override
    public void applyPlayer(Player player, PlayerData data) {
        if(data.getValues() == null){
            return;
        }

        boolean isFly;
        if(data.getValues().getGamemode() == 1){
            isFly = true;
        }else{
            isFly = data.getValues().isFly();
        }

        player.setAllowFlight(isFly);
        player.setFlying(isFly);
    }

}
