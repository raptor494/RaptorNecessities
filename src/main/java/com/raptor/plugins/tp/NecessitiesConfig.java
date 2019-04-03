package com.raptor.plugins.tp;

import static com.raptor.plugins.RaptorPlugin.*;
import static com.raptor.plugins.util.StringUtils.format;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.raptor.plugins.util.TypedMap;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class NecessitiesConfig {
	private Messages messages;
	
	private static class Messages {
		private BaseComponent 
			teleportWait, teleport, teleportCancel,
			teleportRequest, teleportThere, teleportHere, 
			teleportDeny, teleportDenied, 
			pendingTeleportRequest, teleportIgnored, teleportUnignored, 
			notIgnoringTeleport, teleportBlocked, noHomesSet,
			homeSet, homeRemoved, homesListHeader, homeNameTooLong,
			ignoreListHeader, homeNameFormat, homesListSeparator,
			ignoreListSeparator, ignoreNameFormat,
			warpsListHeader, warpsListSeparator, warpNameFormat,
			noPendingInvitations, noPreviousLocation,
			tpaAll, noSuchHome, homeMaxLimitReached,
			teleportAccept, teleportAccepted,
			noSuchWarp, warpSet, warpRemoved,
			confusion, nicknameAlreadyInUse, nicknameRemoved,
			nicknameSet, nicknameTooLong;
		private ChatColor defaultColor = ChatColor.WHITE;
	}
	
	private Duration teleportDelay;
	private Duration teleportRequestTimeout;
	private Duration autoIgnoreTimeOnCancel;
	private TypedMap<String, Integer> maxHomes;
	private boolean permissionBasedWarps;
	private boolean customJoinMessage;
	private int maxHomeNameLength = 15;
	private int maxNicknameLength = 10;
	private boolean saveInstantly = true;
	
	public BaseComponent getTeleportWaitMessage() {
		if(messages.teleportWait == null) {
			messages.teleportWait = new TextComponent("messages.teleportWait {0}");
		}

		return format(updateColor(messages.teleportWait), 
				formatDuration(getTeleportDelay()));
	}
	
	public BaseComponent getTeleportMessage() {
		if(messages.teleport == null) {
			return messages.teleport = new TextComponent("messages.teleport");
		}
		return updateColor(messages.teleport);
	}
	
	public BaseComponent getTeleportCancelMessage() {
		if(messages.teleportCancel == null) {
			return messages.teleportCancel = new TextComponent("messages.teleportCancel");
		}
		return updateColor(messages.teleportCancel);
	}
	
	/**
	 * Sent to the sender upon sending a request to the receiver.
	 */
	public BaseComponent getTeleportRequestMessage(Player receiver) {
		if(messages.teleportRequest == null) {
			messages.teleportRequest = new TextComponent("messages.teleportRequest {0}");
		}
		return format(updateColor(messages.teleportRequest), 
				ChatColor.stripColor(receiver.getDisplayName()));
	}
	
	/**
	 * Sent to the receiver when they deny a tp request.
	 */
	public BaseComponent getTeleportDenyMessage(Player sender) {
		if(messages.teleportDeny == null) {
			messages.teleportDeny = new TextComponent("messages.teleportDeny {0} {1}");
		}
		return format(updateColor(messages.teleportDeny), 
				ChatColor.stripColor(sender.getDisplayName()), 
				formatDuration(getAutoIgnoreTimeOnCancel()));
	}
	
	/**
	 * Sent to the receiver when they accept a tp request.
	 */
	public BaseComponent getTeleportAcceptMessage(Player sender) {
		if(messages.teleportAccept == null) {
			messages.teleportAccept = new TextComponent("messages.teleportAccept {0}");
		}
		return format(updateColor(messages.teleportAccept),
				ChatColor.stripColor(sender.getDisplayName()));
	}
	
	/**
	 * Sent to the receiver when a player wishes to teleport the receiver to themselves.
	 */
	public BaseComponent getTeleportThereMessage(Player sender) {
		if(messages.teleportThere == null) {
			messages.teleportThere = new TextComponent("messages.teleportThere {0} {1}");
		}
		return format(updateColor(messages.teleportThere),
				ChatColor.stripColor(sender.getDisplayName()),
				formatDuration(getTeleportRequestTimeout()));
	}
	
	/**
	 * Sent to the receiver when a player wishes to teleport to the receiver.
	 */
	public BaseComponent getTeleportHereMessage(Player sender) {
		if(messages.teleportHere == null) {
			messages.teleportHere = new TextComponent("messages.teleportHere {0} {1}");
		}
		return format(updateColor(messages.teleportHere),
				ChatColor.stripColor(sender.getDisplayName()),
				formatDuration(getTeleportRequestTimeout()));
	}
	
	/**
	 * Sent to the sender when the receiver denies their tp request.
	 */
	public BaseComponent getTeleportDeniedMessage(Player receiver) {
		if(messages.teleportDenied == null) {
			messages.teleportDenied = new TextComponent("messages.teleportDenied {0}");
		}
		return format(updateColor(messages.teleportDenied),
				ChatColor.stripColor(receiver.getDisplayName()));
	}
	
	/**
	 * Sent to the sender when the receiver accepts their tp request
	 */
	public BaseComponent getTeleportAcceptedMessage(Player receiver) {
		if(messages.teleportAccepted == null) {
			messages.teleportAccepted = new TextComponent("messages.teleportAccepted {0}");
		}
		return format(updateColor(messages.teleportAccepted),
				ChatColor.stripColor(receiver.getDisplayName()));
	}
	
	/**
	 * Sent to the receiver upon ignoring tp requests from someone.
	 */
	public BaseComponent getTeleportIgnoredMessage(OfflinePlayer ignored) {
		if(messages.teleportIgnored == null) {
			messages.teleportIgnored = new TextComponent("messages.teleportIgnored {0}");
		}
		return format(updateColor(messages.teleportIgnored),
				getName(ignored));
	}
	
	/**
	 * Sent to the receiver upon unignoring tp requests from someone.
	 */
	public BaseComponent getTeleportUnignoredMessage(OfflinePlayer unignored) {
		if(messages.teleportUnignored == null) {
			messages.teleportUnignored = new TextComponent("messages.teleportUnignored {0}");
		}
		
		return format(updateColor(messages.teleportUnignored),
				getName(unignored));
	}
	
	/**
	 * Sent to the sender when they aren't currently ignoring tp requests from
	 * a specific player.
	 */
	public BaseComponent getNotCurrentlyIgnoringTeleportMessage(OfflinePlayer notignored) {
		if(messages.notIgnoringTeleport == null) {
			messages.notIgnoringTeleport = new TextComponent("messages.notIgnoringTeleport {0}");
		}
		
		return format(updateColor(messages.notIgnoringTeleport),
				getName(notignored));
	}
	
	/**
	 * Sent to the sender when the intended receiver has blocked teleport requests to them.
	 */
	public BaseComponent getTeleportBlockedMessage(Player blocker) {
		if(messages.teleportBlocked == null) {
			messages.teleportBlocked = new TextComponent("messages.teleportBlocked {0}");
		}
		return format(updateColor(messages.teleportBlocked),
				ChatColor.stripColor(blocker.getDisplayName()));
	}
	
	/**
	 * Sent to the sender when the intended receiver has already received another teleport request.
	 */
	public BaseComponent getPendingTeleportRequestMessage(Player receiver, Duration timeLeft) {
		if(messages.pendingTeleportRequest == null) {
			messages.pendingTeleportRequest = new TextComponent("messages.pendingTeleportRequest {0} {1}");
		}
		return format(updateColor(messages.pendingTeleportRequest),
				ChatColor.stripColor(receiver.getDisplayName()),
				formatDuration(timeLeft));
	}
	
	public BaseComponent getNoHomesSetMessage() {
		if(messages.noHomesSet == null) {
			messages.noHomesSet = new TextComponent("messages.noHomesSet");
		}
		return updateColor(messages.noHomesSet);
	}
	
	public BaseComponent getHomeSetMessage(String homeName) {
		if(messages.homeSet == null) {
			messages.homeSet = new TextComponent("messages.homeSet {0}");
		}
		return format(updateColor(messages.homeSet), homeName);
	}
	
	public BaseComponent getHomeRemovedMessage(String homeName) {
		if(messages.homeRemoved == null) {
			messages.homeRemoved = new TextComponent("messages.homeRemoved {0}");
		}
		return format(updateColor(messages.homeRemoved), homeName);
	}
	
	public BaseComponent getHomesList(Map<String, Location> homes) {
		if(messages.homesListHeader == null) {
			messages.homesListHeader = new TextComponent("messages.homesListHeader");
		}
		updateColor(messages.homesListHeader);
		if(messages.homesListSeparator == null) {
			messages.homesListSeparator = new TextComponent(", ");
			messages.homesListSeparator.setColor(ChatColor.RESET);
		}
		updateColor(messages.homesListSeparator);
		if(messages.homeNameFormat == null) {
			messages.homeNameFormat = new TextComponent("{0} {1} {2$1.2f},{3$1.2f},{4$1.2f} {5$1.2f}/{6$1.2f}");
		}
		updateColor(messages.homeNameFormat);
		BaseComponent result = messages.homesListHeader.duplicate();
		boolean first = true;
		for(Map.Entry<String, Location> entry : homes.entrySet()) {
			if(first) first = false;
			else result.addExtra(messages.homesListSeparator);
			String name = entry.getKey();
			Location loc = entry.getValue();
			String world = loc.getWorld().getName();
			double x = loc.getX();
			double y = loc.getY();
			double z = loc.getZ();
			float pitch = loc.getPitch();
			float yaw = loc.getYaw();
			result.addExtra(format(messages.homeNameFormat, name, world, x, y, z, pitch, yaw));
		}
		return result;
	}
	
	public BaseComponent getIgnoreList(Collection<OfflinePlayer> players) {
		if(messages.ignoreListHeader == null) {
			messages.ignoreListHeader = new TextComponent("messages.ignoreListHeader");
		}
		updateColor(messages.ignoreListHeader);
		if(messages.ignoreListSeparator == null) {
			messages.ignoreListSeparator = new TextComponent(", ");
			messages.ignoreListSeparator.setColor(ChatColor.RESET);
		}
		updateColor(messages.ignoreListSeparator);
		if(messages.ignoreNameFormat == null) {
			messages.ignoreNameFormat = new TextComponent("{0}");
		}
		updateColor(messages.ignoreNameFormat);
		BaseComponent result = messages.ignoreListHeader.duplicate();
		boolean first = true;
		for(OfflinePlayer player : players) {
			if(first) first = false;
			else result.addExtra(messages.ignoreListSeparator);
			result.addExtra(format(messages.ignoreNameFormat, getName(player)));
		}
		return result;
	}
	
	public BaseComponent getWarpsList(Map<String, Location> warps) {
		if(messages.warpsListHeader == null) {
			messages.warpsListHeader = new TextComponent("messages.warpsListHeader");
		}
		updateColor(messages.warpsListHeader);
		if(messages.warpsListSeparator == null) {
			messages.warpsListSeparator = new TextComponent(", ");
			messages.warpsListSeparator.setColor(ChatColor.RESET);
		}
		updateColor(messages.warpsListSeparator);
		if(messages.warpNameFormat == null) {
			messages.warpNameFormat = new TextComponent("{0} {1} {2$1.2f},{3$1.2f},{4$1.2f} {5$1.2f}/{6$1.2f}");
		}
		updateColor(messages.warpNameFormat);
		BaseComponent result = messages.warpsListHeader.duplicate();
		boolean first = true;
		for(Map.Entry<String, Location> entry : warps.entrySet()) {
			if(first) first = false;
			else result.addExtra(messages.warpsListSeparator);
			String name = entry.getKey();
			Location loc = entry.getValue();
			String world = loc.getWorld().getName();
			double x = loc.getX();
			double y = loc.getY();
			double z = loc.getZ();
			float pitch = loc.getPitch();
			float yaw = loc.getYaw();
			result.addExtra(format(messages.warpNameFormat, name, world, x, y, z, pitch, yaw));
		}
		return result;
	}
	
	public BaseComponent getHomeNameTooLongMessage() {
		if(messages.homeNameTooLong == null) {
			return messages.homeNameTooLong = new TextComponent("messages.homeNameTooLong");
		}
		return updateColor(messages.homeNameTooLong);
	}
	
	public BaseComponent getNoPendingInvitationsMessage() {
		if(messages.noPendingInvitations == null) {
			return messages.noPendingInvitations = new TextComponent("messages.noPendingInvitations");
		}
		return updateColor(messages.noPendingInvitations);
	}
	
	public BaseComponent getNoPreviousLocationMessage() {
		if(messages.noPreviousLocation == null) {
			messages.noPreviousLocation = new TextComponent("messages.noPreviousLocation");
		}
		return updateColor(messages.noPreviousLocation);
	}
	
	public BaseComponent getTpaAllMessage(int numPlayersRequested) {
		if(messages.tpaAll == null) {
			messages.tpaAll = new TextComponent("messages.tpaAll {0}");
		}
		return format(updateColor(messages.tpaAll), numPlayersRequested, numPlayersRequested == 1? "" : "s");
	}
	
	public BaseComponent getNoSuchHomeMessage(String home) {
		if(messages.noSuchHome == null) {
			messages.noSuchHome = new TextComponent("messages.noSuchHome {0}");
		}
		return format(updateColor(messages.noSuchHome), home);
	}
	
	public BaseComponent getHomeMaxLimitReachedMessage(int maxHomeCount) {
		if(messages.homeMaxLimitReached == null) {
			messages.homeMaxLimitReached = new TextComponent("messages.homeMaxLimitReached {0}");
		}
		return format(updateColor(messages.homeMaxLimitReached), 
				maxHomeCount, maxHomeCount == 1? "" : "s");
	}
	
	public BaseComponent getNoSuchWarpMessage(String name) {
		if(messages.noSuchWarp == null) {
			messages.noSuchWarp = new TextComponent("messages.noSuchWarp {0}");
		}
		return format(updateColor(messages.noSuchWarp), name);
	}
	
	public BaseComponent getWarpSetMessage(String name) {
		if(messages.warpSet == null) {
			messages.warpSet = new TextComponent("messages.warpSet {0}");
		}
		return format(updateColor(messages.warpSet), name);
	}
	
	public BaseComponent getWarpRemovedMessage(String name) {
		if(messages.warpRemoved == null) {
			messages.warpRemoved = new TextComponent("messages.warpRemoved {0}");
		}
		return format(updateColor(messages.warpRemoved), name);
	}
	
	public BaseComponent confusion() {
		if(messages.confusion == null) {
			messages.confusion = new TextComponent("messages.confusion");
		}
		return updateColor(messages.confusion);
	}
	
	public BaseComponent getNicknameAlreadyInUseMessage() {
		if(messages.nicknameAlreadyInUse == null) {
			messages.nicknameAlreadyInUse = new TextComponent("messages.nicknameAlreadyInUse");
		}
		return updateColor(messages.nicknameAlreadyInUse);
	}
	
	public BaseComponent getNicknameRemovedMessage() {
		if(messages.nicknameRemoved == null) {
			messages.nicknameRemoved = new TextComponent("messages.nicknameRemoved");
		}
		return updateColor(messages.nicknameRemoved);
	}
	
	public BaseComponent getNicknameSetMessage(String nickname) {
		if(messages.nicknameSet == null) {
			messages.nicknameSet = new TextComponent("messages.nicknameSet {0}");
		}
		return format(updateColor(messages.nicknameSet), nickname);
	}
	
	public BaseComponent getNicknameTooLongMessage() {
		if(messages.nicknameTooLong == null) {
			messages.nicknameTooLong = new TextComponent("messages.nicknameTooLong");
		}
		return updateColor(messages.nicknameTooLong);
	}
	
	public int getMaxHomesFor(Player player) {
		if(maxHomes == null) {
			maxHomes = new TypedMap<>(String.class, Integer.class);
			maxHomes.put("default", 1);
		} else if(!maxHomes.containsKey("default")) {
			maxHomes.put("default", 1);
		}
		
		int maxHome = maxHomes.get("default");
		
		if(player.hasPermission("raptortps.sethome.multiple")) {
			for(String key : maxHomes.keySet()) {
				if(player.hasPermission("raptortps.sethome.multiple." + key)) {
					int value = maxHomes.get(key);
					if(value > maxHome) {
						maxHome = value;
					}
				}
			}
		}
		
		return maxHome;
	}
	
	public Duration getTeleportDelay() {
		if(teleportDelay == null) {
			teleportDelay = Duration.ZERO;
		}
		return teleportDelay;
	}
	
	public long getTeleportDelayInTicks() {
		return secondsToTicks(getTeleportDelay().getSeconds());
	}
	
	public boolean isTeleportDelayEnabled() {
		return !Duration.ZERO.equals(getTeleportDelay());
	}
	
	public Duration getAutoIgnoreTimeOnCancel() {
		if(autoIgnoreTimeOnCancel == null) {
			autoIgnoreTimeOnCancel = Duration.ZERO;
		}
		return autoIgnoreTimeOnCancel;
	}
	
	public long getAutoIgnoreTimeOnCancelInTicks() {
		return secondsToTicks(getAutoIgnoreTimeOnCancel().getSeconds());
	}
	
	public boolean isAutoIgnoreOnCancelEnabled() {
		return !Duration.ZERO.equals(getAutoIgnoreTimeOnCancel());
	}
	
	public Duration getTeleportRequestTimeout() {
		if(teleportRequestTimeout == null) {
			teleportRequestTimeout = Duration.ZERO;
		}
		return teleportRequestTimeout;
	}
	
	public long getTeleportRequestTimeoutInTicks() {
		return secondsToTicks(getTeleportRequestTimeout().getSeconds());
	}
	
	public boolean isTeleportRequestTimeoutEnabled() {
		return !Duration.ZERO.equals(getTeleportRequestTimeout());
	}
	
	public boolean arePermissionBasedWarpsEnabled() {
		return permissionBasedWarps;
	}
	
	public boolean isCustomJoinMessageEnabled() {
		return customJoinMessage;
	}
	
	public int getMaxHomeNameLength() {
		return maxHomeNameLength;
	}
	
	public int getMaxNicknameLength() {
		return maxNicknameLength;
	}
	
	public boolean shouldSaveInstantly() {
		return saveInstantly;
	}
	
	private String getName(OfflinePlayer offlinePlayer) {
		Player player = offlinePlayer.getPlayer();
		if(player == null)
			return offlinePlayer.getName();
		else return ChatColor.stripColor(player.getDisplayName());
	}
	
	private BaseComponent updateColor(BaseComponent text) {
		if(text == null) return null;
		if(text.getColorRaw() == null)
			text.setColor(messages.defaultColor);
		return text;
	}
}
