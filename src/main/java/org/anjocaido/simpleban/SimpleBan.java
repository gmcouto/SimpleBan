/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.simpleban;

import com.nijikokun.bukkit.Permissions.Permissions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

/**
 *
 * @author gabrielcouto
 */
public class SimpleBan extends JavaPlugin {

    private Map<String, Boolean> playerExists;
    private SimpleBanPlayerListener listener;
    private Configuration banList = null;
    private Configuration ipHistory = null;
    private Plugin perm = null;

    @Override
    public void onDisable() {
        saveBans();
        saveHistory();
        System.out.println(this.getDescription().getName() + " " + this.getDescription().getVersion() + " was disabled!");
    }

    @Override
    public void onEnable() {
        Plugin p = this.getServer().getPluginManager().getPlugin("Permissions");
        if (p != null) {
            if (!this.getServer().getPluginManager().isPluginEnabled(p)) {
                this.getServer().getPluginManager().enablePlugin(p);
            }
            perm = (Permissions) p;
        } else {
            System.out.println("Roles plugin could not load the Permissions data!");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        playerExists = new HashMap<String, Boolean>();

        ipHistory = new Configuration(new File(this.getDataFolder(), "iphistory.yml"));
        banList = new Configuration(new File(this.getDataFolder(), "banlist.yml"));

        loadBans();
        loadHistory();

        listener = new SimpleBanPlayerListener(this);
        this.getServer().getPluginManager().registerEvent(Type.PLAYER_LOGIN, listener, Priority.Low, this);
        this.getServer().getPluginManager().registerEvent(Type.PLAYER_JOIN, listener, Priority.Low, this);
        System.out.println(this.getDescription().getName() + " " + this.getDescription().getVersion() + " was loaded sucessfully!");
    }

    private void loadBans() {
        try {
            banList.load();
        } catch (Exception e) {
        }
        if (banList.getKeys("bans") == null) {
            banList.setProperty("bans", new HashMap<String, List<String>>());
        }
    }

    private void saveBans() {
        if (banList != null) {
            try {
                banList.save();
            } catch (Exception e) {
            }
        }
    }

    private void loadHistory() {
        try {
            ipHistory.load();
        } catch (Exception e) {
        }
        if (ipHistory.getKeys("history") == null) {
            ipHistory.setProperty("history", new HashMap<String, String>());
        }
    }

    private void saveHistory() {
        if (ipHistory != null) {
            try {
                ipHistory.save();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean hasPerm = false;
        label = command.getName().toLowerCase();
        if (sender instanceof ConsoleCommandSender) {
            hasPerm = true;
        } else if (sender instanceof Player) {
            Player sending = (Player) sender;
            hasPerm = ((Permissions) perm).getHandler().permission(sending, "simpleban." + label);
        }
        if (hasPerm) {
            if (label.equals("sban")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Review your arguments!");
                    return false;
                }
                List<Player> matchPlayer = this.getServer().matchPlayer(args[0]);
                if (matchPlayer.size() != 1) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return false;
                }
                banPlayer(matchPlayer.get(0));
                kickBanWarning(matchPlayer.get(0));
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Player " + matchPlayer.get(0).getName() + " has been banned sucessfully!");
                return true;
            } else if (label.equals("sbanhistory")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Review your arguments!");
                    return false;
                }
                String ip = getHistory(args[0]);
                if (ip == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found in history!");
                    return false;
                }
                banWithNameAndAddress(args[0], ip);
                for (Player p : this.getServer().getOnlinePlayers()) {
                    generalBanCheckAction(p);
                }
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Player " + args[0] + " has been banned sucessfully!");
                return true;
            } else if (label.equals("sbanip")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Review your arguments!");
                    return false;
                }
                if (!args[0].contains(".")) {
                    sender.sendMessage(ChatColor.RED + "Is that an IP? Don't think so.");
                    return false;
                }
                if (args[0].length() < 7) {
                    sender.sendMessage(ChatColor.RED + "Don't you think that IP is too short? I think.");
                }
                for (Player p : this.getServer().getOnlinePlayers()) {
                    generalBanCheckAction(p);
                }
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Ip " + args[0] + " has been added to banned list sucessfully!");
                return true;
            } else if (label.equals("sbanipfromname")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Review your arguments!");
                    return false;
                }
                List<Player> matchPlayer = this.getServer().matchPlayer(args[0]);
                if (matchPlayer.size() != 1) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return false;
                }
                if (!args[0].contains(".")) {
                    sender.sendMessage(ChatColor.RED + "Is that an IP? Don't think so.");
                    return false;
                }
                if (args[0].length() < 7) {
                    sender.sendMessage(ChatColor.RED + "Don't you think that IP is too short? I think.");
                }
                banOnlyIp(matchPlayer.get(0));
                kickBanWarning(matchPlayer.get(0));
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "The IP from player " + matchPlayer.get(0).getName() + " has been added to banned list sucessfully!");
                return true;
            } else if (label.equals("sunban")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Review your arguments!");
                    return false;
                }
                String name = args[0];
                unbanPlayer(name);
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Player " + name + " has been unbanned sucessfully!");
                return true;
            } else if (label.equals("sunbanip")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Review your arguments!");
                    return false;
                }
                unbanIp(args[0]);
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Ip " + args[0] + " has been unbanned sucessfully!");
                return true;
            } else if (label.equals("sreload")) {
                loadBans();
                saveHistory();
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "The file banlist.yml was reloaded!");
                return true;
            }
            return false;
        } else {
            sender.sendMessage(ChatColor.RED+"You don't have permissions to do this.");
        }
        return false;
    }

    public boolean isNameBanned(Player player) {
        if (isGetNameFailing(player)) {
            return false;
        }
        return isNameBanned(player.getName());
    }

    public boolean isNameBanned(String playerName) {
        if (banList.getKeys("bans").contains(playerName.toLowerCase())) {
            System.out.println(ChatColor.RED + "Player " + playerName + " has been detected by name during game.");
            return true;
        }
        return false;
    }

    public boolean isAnyBanned(Player player) {
        if (isGetAddressFailing(player)) {
            return false;
        }
        return isAnyBanned(player.getName(), player.getAddress().getAddress().getHostAddress());
    }

    public boolean isAnyBanned(String playerName, String ip) {
        if (banList.getKeys("bans").contains(playerName.toLowerCase())) {
            System.out.println(ChatColor.RED + "Player " + playerName + " has been detected by name.");
            System.out.println(ChatColor.RED + "His current IP: " + ip);
            return true;
        }
        for (String key : banList.getKeys("bans")) {
            List<String> stringList = banList.getStringList("bans." + key, null);
            if (stringList != null) {
                for (String fullAddress : stringList) {
                    if (fullAddress.equalsIgnoreCase(ip)) {
                        System.out.println(ChatColor.RED + "Player " + playerName + " is considered banned by IP address.");
                        System.out.println(ChatColor.RED + "Matched banned entry: " + key + " Matched IP: " + ip);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void banPlayer(Player player) {
        if (isGetAddressFailing(player)) {
            return;
        }
        banWithNameAndAddress(player.getName(), player.getAddress().getAddress().getHostAddress());
    }

    public void banWithNameAndAddress(String playerName, String address) {
        if (banList.getKeys("bans").contains(playerName.toLowerCase())) {
            List<String> stringList = banList.getStringList("bans." + playerName.toLowerCase(), null);
            if (stringList != null) {
                if (!stringList.contains(address)) {
                    stringList.add(address);
                    banList.setProperty("bans." + playerName.toLowerCase(), stringList);
                    banList.save();
                    //kickBanWarning(playerName);
                }
            } else {
                stringList = new ArrayList<String>();
                stringList.add(address);
                banList.setProperty("bans." + playerName.toLowerCase(), stringList);
                banList.save();
                //kickBanWarning(playerName);
            }
        } else {
            List<String> stringList = new ArrayList<String>();
            stringList.add(address);
            banList.setProperty("bans." + playerName.toLowerCase(), stringList);
            banList.save();
            //kickBanWarning(playerName);
        }
    }

    public void banOnlyIp(Player player) {
        if (isGetAddressFailing(player)) {
            return;
        }
        banOnlyIp(player.getAddress().getAddress().getHostAddress());
    }

    public void banOnlyIp(String address) {
        banWithNameAndAddress("!ips-only!", address);
    }

    public void kickBanWarning(Player player) {
        try {
            player.kickPlayer("You got hit by the Ban Hammer!");
            System.out.println(ChatColor.RED + "The player " + player.getName() + " has been kicked because is banned!");
        } catch (Exception e) {
        }
    }

    public void unbanPlayer(String playerName) {
        if (banList.getKeys("bans").contains(playerName.toLowerCase())) {
            banList.removeProperty("bans." + playerName.toLowerCase());
            banList.save();
        }
    }

    public void unbanIp(String ip) {
        for (String key : banList.getKeys("bans")) {
            List<String> stringList = banList.getStringList("bans." + key, null);
            List<String> toRemove = new ArrayList<String>();
            if (stringList != null) {

                for (String fullAddress : stringList) {
                    if (fullAddress.toLowerCase().contains(ip.toLowerCase())) {
                        toRemove.add(fullAddress);
                    }
                }
            }
            if (!toRemove.isEmpty()) {
                stringList.removeAll(toRemove);
                banList.setProperty(key, stringList);
            }
        }
        banList.save();
    }

    public void addHistory(Player player) {
        if (isGetAddressFailing(player)) {
            return;
        }
        ipHistory.setProperty("history." + player.getName().toLowerCase(), player.getAddress().getAddress().getHostAddress());
    }

    public String getHistory(String playerName) {
        return ipHistory.getString("history." + playerName.toLowerCase());
    }

    public boolean isGetNameFailing(Player player) {
        try {
            if (player == null || player.getName() == null) {
                return true;
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    /**
     * Unfortunately CB is buggy. And sometimes a player doesnt have an IP assigned.
     * @param player
     * @return
     */
    public boolean isGetAddressFailing(Player player) {
        try {
            if (player == null || player.getName() == null || player.getAddress() == null || player.getAddress().getHostName() == null || player.getAddress().getAddress() == null || player.getAddress().getAddress().getHostAddress() == null) {
                return true;
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    public boolean generalBanCheckAction(Player player) {
        if (isGetAddressFailing(player)) {
            if (isGetNameFailing(player)) {
                System.out.println("There is a problem with player: " + player);
            } else {
                if (isNameBanned(player)) {
                    kickBanWarning(player);
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            if (isAnyBanned(player)) {
                if (isNameBanned(player)) {
                    //IF HIS NAME IS BANNED TOO, ACT NORMALLY
                    banPlayer(player);
                    kickBanWarning(player);
                } else {
                    //IF NAME IS NOT BANNED TOO, CHECK IF THAT NAME IS FIRST TIME
                    if (doesPlayerExist(player.getName())) {
                        //IF PLAYER EXISTS, JUST KICK THE PLAYER OUT
                        //WE DONT WANT AN EXISTING PLAYER GETTING BANNED
                        kickBanWarning(player);
                    } else {
                        //IF PLAYER NAME DOES NOT EXISTS, BAN THE NEW NAME TOO
                        banPlayer(player);
                        kickBanWarning(player);
                    }
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean doesPlayerExist(String playerName) {
        if (playerExists.containsKey(playerName.toLowerCase())) {
            System.out.println("Does " + playerName + " exists? " + playerExists.get(playerName.toLowerCase()));
            return playerExists.get(playerName.toLowerCase());
        }
        return false;
    }

    public void mapPlayerExistance(String playerName) {
        if (this.getServer().getPlayer(playerName) != null) {
            playerExists.put(playerName.toLowerCase(), Boolean.TRUE);
        }
        boolean found = false;
        PlayerNameFilter fnf = new PlayerNameFilter(playerName);
        for (World w : getServer().getWorlds()) {
            File playersFolder = new File(w.getName() + File.separator + "players");
            File[] listFiles = playersFolder.listFiles(fnf);
            if (listFiles.length > 0) {
                found = true;
                break;
            }
        }
        playerExists.put(playerName.toLowerCase(), found);
    }

    public class PlayerNameFilter implements FilenameFilter {

        String name;

        public PlayerNameFilter(String playerName) {
            this.name = playerName + ".dat";
        }

        @Override
        public boolean accept(File file, String string) {
            return string.equalsIgnoreCase(name);
        }
    }
}
