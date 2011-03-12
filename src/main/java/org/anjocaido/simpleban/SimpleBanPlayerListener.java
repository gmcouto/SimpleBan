/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.anjocaido.simpleban;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 *
 * @author gabrielcouto
 */
public class SimpleBanPlayerListener extends PlayerListener {
    private SimpleBan plugin;
    public SimpleBanPlayerListener(SimpleBan plugin){
        this.plugin = plugin;
    }
    @Override
    public void onPlayerLogin(PlayerLoginEvent event) {
        plugin.mapPlayerExistance(event.getPlayer().getName());
    }
    @Override
    public void onPlayerJoin(PlayerEvent event) {
        plugin.addHistory(event.getPlayer());
        plugin.generalBanCheckAction(event.getPlayer());
    }
    @Override
    public void onPlayerChat(PlayerChatEvent event) {
        plugin.generalBanCheckAction(event.getPlayer());
    }
    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        plugin.generalBanCheckAction(event.getPlayer());
    }
}
