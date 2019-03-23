package com.raptor.plugins.tp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.raptor.plugins.util.TypedSet;

public class PlayerData {
	public static final Pattern NAME_REGEX = Pattern.compile("^[\\w_]+$");
	
	Location previousLocation, lastLogoutLocation;
	String lastKnownUsername, ipAddress;
	transient UUID uuid;
	HashMap<String, Location> homes;
	HashSet<UUID> ignoredPlayers;
	String nickname;
	ChatColor nicknameColor;
	transient TypedSet<UUID> tempIgnoredPlayers = new TypedSet<>(UUID.class);

	PlayerData() {}
	
	PlayerData(OfflinePlayer player) {
		lastKnownUsername = player.getName();
		uuid = player.getUniqueId();
		homes = new HashMap<>();
	}
	
	public String getNickname() {
		return nickname;
	}
	
	public void setNickname(String newnickname) {
		if(newnickname == null) {
			nickname = null;
		} else {
			if(!NAME_REGEX.matcher(newnickname).matches())
				throw new IllegalArgumentException("Invalid nickname: '" + newnickname + "'");
			nickname = newnickname;
		}
		autoSave();
	}
	
	public ChatColor getNicknameColor() {
		return nicknameColor;
	}
	
	public Location getPreviousLocation() {
		return previousLocation;
	}
	
	public void setPreviousLocation(Location location) {
		previousLocation = location;
	}
	
	public Location getLastLogoutLocation() {
		return lastLogoutLocation;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	
	public Set<UUID> getIgnoredPlayers() {
		if(ignoredPlayers == null) {
			ignoredPlayers = new HashSet<>();
		}
		return ignoredPlayers;
	}
	
	public Set<UUID> getTempIgnoredPlayers() {
		return tempIgnoredPlayers;
	}
	
	public boolean isIgnoringRequestsFrom(OfflinePlayer player) {
		return getIgnoredPlayers().contains(player.getUniqueId())
				|| getTempIgnoredPlayers().contains(player.getUniqueId());
	}
	
	public boolean ignoreRequestsFrom(OfflinePlayer player) {
		if(getIgnoredPlayers().add(player.getUniqueId())) {
			autoSave();
			return true;
		} else return false;
	}
	
	public boolean stopIgnoringRequestsFrom(OfflinePlayer player) {
		if(getIgnoredPlayers().remove(player.getUniqueId())) {
			autoSave();
			return true;
		} else return false;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public String getLastKnownUsername() {
		return lastKnownUsername;
	}
	
	public OfflinePlayer getPlayer() {
		return Bukkit.getServer().getOfflinePlayer(uuid);
	}
	
	public Map<String, Location> getHomes() {
		if(homes == null) {
			homes = new HashMap<>();
		}
		return homes;
	}
	
	public void setHomes(Map<String, Location> newHomes) {
		for(Map.Entry<String, Location> entry : newHomes.entrySet()) {
			Validate.notNull(entry.getKey(), "Home names may not be null");
			Validate.notNull(entry.getValue(), "Homes may not be null");
		}
		if(homes == null) {
			homes = new HashMap<>();
		} else {
			homes.clear();
			homes.putAll(newHomes);
			autoSave();
		}
	}
	
	public Set<String> getHomeNames() {
		return getHomes().keySet();
	}

	public Location getHome(String home) {
		return getHomes().get(home);
	}

	public void setHome(String home, Location loc) {
		Validate.notEmpty(home, "Home name may not be null or empty");
		Validate.notNull(loc, "Location of home may not be null");
		getHomes().put(home, loc);
		autoSave();
	}
	
	public int homeCount() {
		return getHomes().size();
	}
	
	public boolean hasHome(String home) {
		return getHomes().containsKey(home);
	}
	
	public void delHome(String home) {
		if(homes != null) {
			if(homes.remove(home) != null) {
				autoSave();
			}
		}
	}
	
	public void save() {
		RaptorNecessities plugin = RaptorNecessities.getInstance();
		FileWriter writer;
		JsonWriter jsonWriter;
		try {
			writer = new FileWriter(new File(plugin.getPlayerDataFolder(), uuid + ".json"));
			jsonWriter = plugin.getGson().newJsonWriter(writer);
		} catch(IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Error creating writer while saving player data for " + uuid, e);
			return;
		}
		try {
			Streams.write(plugin.getGson().toJsonTree(this), jsonWriter);
			jsonWriter.close();
		} catch(IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Error while saving player data for " + uuid, e);
		}
	}
	
	void merge(PlayerData other) {
		if(this == other) return;
		
		previousLocation = other.previousLocation;
		lastLogoutLocation = other.lastLogoutLocation;
		lastKnownUsername = other.lastKnownUsername;
		ipAddress = other.ipAddress;
		uuid = other.uuid;
		if(homes == null) {
			homes = new HashMap<>();
		} else {
			homes.clear();
			homes.putAll(other.getHomes());
		}
		if(ignoredPlayers == null) {
			ignoredPlayers = new HashSet<>();
		} else {
			ignoredPlayers.clear();
			ignoredPlayers.addAll(other.getIgnoredPlayers());
		}
	}
	
	@Override
	public String toString() {
		return String.format("PlayerData{uuid=%s,name=%s,ip=%s,lastLoc=%s,logoutLoc=%s,nickname=%s,homes=%s,ignoredPlayers=%s}", uuid, lastKnownUsername, ipAddress, previousLocation, lastLogoutLocation, nickname, getHomes(), getIgnoredPlayers());
	}
	
	private void autoSave() {
		if(RaptorNecessities.getInstance().getConfigObject().shouldSaveInstantly()) {
			save();
		}
	}
}