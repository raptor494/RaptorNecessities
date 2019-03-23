package com.raptor.plugins.tp;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.raptor.plugins.tp.RaptorNecessities.TeleportRequest;

public class EventHandlers implements Listener {
	private RaptorNecessities plugin;
	
	public EventHandlers(RaptorNecessities plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Location from = event.getFrom(), to = event.getTo();
		if(from.getBlockX() != to.getBlockX() 
				|| from.getBlockY() != to.getBlockY()
				|| from.getBlockZ() != to.getBlockZ())
			plugin.cancelPendingTeleports(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerHurt(EntityDamageEvent event) {
		if(event.getEntityType() == EntityType.PLAYER) {
			plugin.cancelPendingTeleports((Player)event.getEntity());
		}
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		PlayerData data = plugin.getPlayerDataFor(event.getPlayer());
		data.ipAddress = event.getRealAddress().toString();
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(PlayerJoinEvent event) {
		// ensures the data is loaded
		Player player = event.getPlayer();
		PlayerData data = plugin.getPlayerDataFor(player);
		if(plugin.nicknames.containsKey(player.getName())) {
			UUID otherUUID = plugin.nicknames.get(player.getName());
			if(!otherUUID.equals(player.getUniqueId())) {
				OfflinePlayer player2 = plugin.getServer().getOfflinePlayer(otherUUID);
				PlayerData data2 = plugin.getPlayerDataFor(player2);
				data2.setNickname(null);
				data2.save();
				plugin.nicknames.remove(player.getName());
				if(player2.isOnline())
					player2.getPlayer().spigot().sendMessage(plugin.getConfigObject().getNicknameRemovedMessage());
			}
		}
		if(data.nickname != null) {
			if(data.nicknameColor == null)
				player.setDisplayName(data.nickname);
			else
				player.setDisplayName(data.nicknameColor + data.nickname);
			if(plugin.isNicknameInUseBySomeoneElse(data.nickname, player)) {
				player.setDisplayName(null);
				player.spigot().sendMessage(plugin.getConfigObject().getNicknameRemovedMessage());
				data.setNickname(null);
			} else if(plugin.getConfigObject().isCustomJoinMessageEnabled()) {
				event.setJoinMessage("\u00a7e" + player.getName() + " (aka " + ChatColor.stripColor(data.nickname) + ") joined the game");
			}
		}
		
	}
	
	@EventHandler
	public void onPlayerKicked(PlayerKickEvent event) {
		onPlayerLogout(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		onPlayerLogout(event.getPlayer());
	}
	
	public void onPlayerLogout(Player player) {
		PlayerData playerData = plugin.getPlayerDataFor(player);
		playerData.lastLogoutLocation = player.getLocation();
		plugin.unloadPlayerDataFor(player);
		plugin.cancelPendingTeleports(player);
		plugin.cancelPendingTeleportRequest(player);
	}
	
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		PlayerData playerData = plugin.getPlayerDataFor(player);
		playerData.setPreviousLocation(event.getFrom());
		plugin.cancelPendingTeleports(player);
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		PlayerData playerData = plugin.getPlayerDataFor(player);
		playerData.setPreviousLocation(player.getLocation());
		plugin.cancelPendingTeleports(player);
		TeleportRequest request = plugin.getPendingTeleportRequest(player);
		if(request != null && request.sender == player) {
			request.cancel();
		}
	}
}
