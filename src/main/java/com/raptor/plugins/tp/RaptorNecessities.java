package com.raptor.plugins.tp;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.permissions.Permissible;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.raptor.plugins.RCommand;
import com.raptor.plugins.RPlayerCommand;
import com.raptor.plugins.RaptorPlugin;
import com.raptor.plugins.util.StringUtils;

public final class RaptorNecessities extends RaptorPlugin {
	private static RaptorNecessities instance;
	private static final String PLAYER_DATA_FOLDER = "playerData",
								WARPS_LIST_FILE = "warps.json",
								NICKNAMES_FILE = "nicknames.json";
	private static final Type WARPS_TYPE = new TypeToken<HashMap<String, Location>>() {}.getType(),
							  NICKNAMES_TYPE = new TypeToken<HashMap<String, UUID>>() {}.getType();
	private final HashMap<OfflinePlayer, PlayerData> playerData = new HashMap<>();
	private final HashMap<String, Location> warps = new HashMap<>();
	final HashMap<String, UUID> nicknames = new HashMap<>();
	private final File playerDataFolder, warpsListFile, nicknamesFile;
	private NecessitiesConfig config;
	
	public RaptorNecessities() {
		super(NecessitiesConfig.class);
		instance = this;
		playerDataFolder = getFile(PLAYER_DATA_FOLDER);
		if(!playerDataFolder.exists() || !playerDataFolder.isDirectory())
			playerDataFolder.mkdir();
		warpsListFile = getFile(WARPS_LIST_FILE);
		nicknamesFile = getFile(NICKNAMES_FILE);
	}
	
	public static RaptorNecessities getInstance() {
		return instance;
	}
	
	@Override
	public NecessitiesConfig getConfigObject() {
		return (NecessitiesConfig)super.getConfigObject();
	}
	
	@Override
	public void reloadConfig() {
		super.reloadConfig();
		config = getConfigObject();
	}
	
	@Override
	public void onLoad() {
		registerCommand("back", new CommandBack());
		registerCommand("tpa", new CommandTpa());
		registerCommand("tpahere", new CommandTpaHere());
		registerCommand("tpaall", new CommandTpaAll());
		registerCommand("tpcancel", new CommandTpCancel());
		registerCommand("tpaccept", new CommandTpAccept());
		registerCommand("tpdeny", new CommandTpDeny());
		registerCommand("tpignore", new CommandTpIgnore());
		registerCommand("tpunignore", new CommandTpUnignore());
		registerCommand("home", new CommandHome());
		registerCommand("sethome", new CommandSetHome());
		registerCommand("delhome", new CommandDelHome());
		registerCommand("warp", new CommandWarp());
		registerCommand("setwarp", new CommandSetWarp());
		registerCommand("delwarp", new CommandDelWarp());
		registerCommand("nickname", new CommandNickname());
		registerCommand("raptornecessities", new CommandRaptorNecessities());
		registerListener(new EventHandlers(this));
	}
	
	@Override
	public void onEnable() {
		super.onEnable();
		loadData();
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		saveData();
		clearData();
	}
	
	private void loadData() {
		for(Player player : getServer().getOnlinePlayers()) {
			loadPlayerDataFor(player);
		}
		try {
			warps.putAll(gson.<HashMap<String, Location>>fromJson(loadJson(warpsListFile), WARPS_TYPE));
		} catch(FileNotFoundException e) {
		} catch(JsonParseException e) {
			getLogger().log(Level.SEVERE, "Json error loading " + warpsListFile, e);
		}
		try {
			nicknames.putAll(gson.<HashMap<String, UUID>>fromJson(loadJson(nicknamesFile), NICKNAMES_TYPE));
		} catch(JsonParseException e) {
		} catch(FileNotFoundException e) {
			getLogger().log(Level.SEVERE, "Json error loading " + nicknamesFile, e);
		}
	}
	
	private void saveData() {
		for(PlayerData playerData : playerData.values()) {
			playerData.save();
		}
		saveJson(warpsListFile, gson.toJsonTree(warps, WARPS_TYPE));
		saveJson(nicknamesFile, gson.toJsonTree(nicknames, NICKNAMES_TYPE));
	}
	
	private void clearData() {
		playerData.clear();
		warps.clear();
		nicknames.clear();
	}
	
	public String getRealName(String nickname) {
		if(nicknames.containsKey(nickname))
			return getServer().getOfflinePlayer(nicknames.get(nickname)).getName();
		else return null;
	}
	
	public boolean hasWarp(String name) {
		return warps.containsKey(name);
	}
	
	public Location getWarp(String name) {
		return warps.get(name);
	}
	
	public void setWarp(String name, Location location) {
		Validate.notNull(name, "warp name may not be null");
		Validate.notNull(location, "warp location may not be null");
		warps.put(name, location);
	}
	
	public Set<String> getWarpNames() {
		return warps.keySet();
	}
	
	public Set<String> getWarpNames(Permissible permissions) {
		return warps.keySet().stream()
				.filter(warp -> permissions.hasPermission("raptortps.warps." + warp))
				.collect(Collectors.toSet());
	}
	
	public boolean deleteWarp(String name) {
		return warps.remove(name) != null;
	}
	
	public File getPlayerDataFolder() {
		return playerDataFolder;
	}
	
	public boolean isNicknameInUseBySomeoneElse(String nickname, OfflinePlayer playerWhoWantsToUseIt) {
		if(nicknames.containsKey(nickname))
			return !nicknames.get(nickname).equals(playerWhoWantsToUseIt.getUniqueId());
		@SuppressWarnings("deprecation")
		OfflinePlayer player2 = getServer().getOfflinePlayer(nickname);
		if(player2.hasPlayedBefore()) {
			Instant lastPlayed = Instant.ofEpochMilli(player2.getLastPlayed());
			// true if the player has been online in the last year
			if(!lastPlayed.isBefore(Instant.now().minus(365, ChronoUnit.DAYS)))
				return true;
		}
		return false;
	}
	
	public void loadPlayerDataFor(OfflinePlayer player) {
		File file = new File(getPlayerDataFolder(), player.getUniqueId().toString() + ".json");
		PlayerData data;
		if(file.exists()) {
			data = gson.fromJson(loadJsonObject(file), PlayerData.class);
			data.uuid = player.getUniqueId();
		} else {
			data = new PlayerData(player);
			data.save();
		}		
		playerData.put(player, data);
		data.lastKnownUsername = player.getName();
	}
	
	public void unloadPlayerDataFor(OfflinePlayer player) {
		PlayerData data = playerData.remove(player);
		if(data != null)
			data.save();
	}
	
	public PlayerData getPlayerDataFor(OfflinePlayer player) {
		if(!playerData.containsKey(player))
			loadPlayerDataFor(player);
		return playerData.get(player);
	}
	
	final HashMap<Player, Teleport> pendingTeleports = new HashMap<>();
	
	public void teleport(Player player, Location destination) {
		if(player.hasPermission("raptortps.instant")) {
			cancelPendingTeleports(player);
			player.teleport(destination, TeleportCause.PLUGIN);
			return;
		}
		Teleport teleport = new Teleport(player, destination);
		Teleport prevTeleport = pendingTeleports.put(player, teleport);
		if(prevTeleport != null)
			prevTeleport.cancel();
		player.spigot().sendMessage(config.getTeleportWaitMessage());
		teleport.runTaskLater(this, config.getTeleportDelayInTicks());
	}
	
	public void teleport(Player playerToTeleport, Player requester, Location destination) {
		if(playerToTeleport.hasPermission("raptortps.instant")) {
			cancelPendingTeleports(playerToTeleport);
			playerToTeleport.teleport(destination, TeleportCause.PLUGIN);
			return;
		}
		Teleport teleport = new Teleport(playerToTeleport, requester, destination);
		Teleport prevTeleport = pendingTeleports.put(playerToTeleport, teleport);
		if(prevTeleport != null)
			prevTeleport.cancel();
		prevTeleport = pendingTeleports.put(requester, teleport);
		if(prevTeleport != null)
			prevTeleport.cancel();
		playerToTeleport.spigot().sendMessage(config.getTeleportWaitMessage());
		teleport.runTaskLater(this, config.getTeleportDelayInTicks());
	}
	
	public Teleport getPendingTeleport(Player player) {
		return pendingTeleports.get(player);
	}
	
	public boolean cancelPendingTeleports(Player player) {
		Teleport teleport = getPendingTeleport(player);
		if(teleport != null) {
			teleport.cancel();
			return true;
		}
		return false;
	}
	
	public class Teleport extends BukkitRunnable {
		Player player, requester;
		Location originalLoc, destination;
		Instant startTime;
		
		Teleport(Player player, Location destination) {
			this.player = player;
			this.originalLoc = player.getLocation();
			this.destination = destination;
			this.startTime = Instant.now();
		}
		
		Teleport(Player player, Player requester, Location destination) {
			this(player, destination);
			this.requester = requester;
		}
		
		private void sendCancelMessage() {
			if(player.isOnline())
				player.spigot().sendMessage(config.getTeleportCancelMessage());
			if(requester != null && requester.isOnGround())
				requester.spigot().sendMessage(config.getTeleportCancelMessage());
		}
		
		private void sendTeleportMessage() {
			player.spigot().sendMessage(config.getTeleportMessage());
		}
		
		private synchronized void removeFromPendingTeleports() {
			pendingTeleports.remove(player, this);
			if(requester != null)
				pendingTeleports.remove(requester, this);
		}
		
		@Override
		public void cancel() {
			try {
				if(isCancelled())
					return;
			} catch(IllegalStateException e) {
				return;
			}
			super.cancel();
			sendCancelMessage();
			removeFromPendingTeleports();
			
		}
		
		@Override
		public void run() {
			if(player.isOnline()) {
				Location currentLoc = player.getLocation();
				if(originalLoc.getBlockX() != currentLoc.getBlockX()
						|| originalLoc.getBlockY() != currentLoc.getBlockY()
						|| originalLoc.getBlockZ() != currentLoc.getBlockZ()) {
					sendCancelMessage();
					removeFromPendingTeleports();
					return;
				}
				sendTeleportMessage();
				removeFromPendingTeleports();
				player.teleport(destination, TeleportCause.PLUGIN);
			} else {
				removeFromPendingTeleports();
			}			
		}

	}
	
	final HashMap<Player, TeleportRequest> teleportRequests = new HashMap<>();
	
	public void newTeleportRequest(Player sender, Player receiver, boolean tpaHere) {
		TeleportRequest request = new TeleportRequest(sender, receiver, tpaHere);
		
		TeleportRequest oldRequest = teleportRequests.put(sender, request);
		if(oldRequest != null)
			oldRequest.cancel();
		
		oldRequest = teleportRequests.put(receiver, request);
		if(oldRequest != null)
			oldRequest.cancel();
		
		sender.spigot().sendMessage(config.getTeleportRequestMessage(receiver));
		
		if(tpaHere) {
			receiver.spigot().sendMessage(config.getTeleportThereMessage(sender));
		} else {
			receiver.spigot().sendMessage(config.getTeleportHereMessage(sender));
		}
		
		request.runTaskLater(this, config.getTeleportRequestTimeoutInTicks());
	}
	
	public TeleportRequest getPendingTeleportRequest(Player player) {
		return teleportRequests.get(player);
	}
	
	public boolean cancelPendingTeleportRequest(Player player) {
		TeleportRequest request = getPendingTeleportRequest(player);
		if(request != null) {
			request.cancel();
			return true;
		}
		return false;
	}
	
	public class TeleportRequest extends BukkitRunnable {
		Player sender, receiver;
		boolean tpaHere;
		Instant startTime;
		
		TeleportRequest(Player sender, Player receiver, boolean tpaHere) {
			this.sender = sender;
			this.receiver = receiver;
			this.tpaHere = tpaHere;
			this.startTime = Instant.now();
		}
		
		private void removeFromTeleportRequests() {
			teleportRequests.remove(sender, this);
			teleportRequests.remove(receiver, this);
		}
		
		public void accept() {
			super.cancel();
			removeFromTeleportRequests();
			sender.spigot().sendMessage(config.getTeleportAcceptedMessage(receiver));
			receiver.spigot().sendMessage(config.getTeleportAcceptMessage(sender));
			if(tpaHere) {
				teleport(receiver, sender, sender.getLocation());
			} else {
				teleport(sender, receiver, receiver.getLocation());
			}
		}
		
		public void deny() {
			super.cancel();
			removeFromTeleportRequests();
			sender.spigot().sendMessage(config.getTeleportDeniedMessage(receiver));
			receiver.spigot().sendMessage(config.getTeleportDenyMessage(sender));
			PlayerData receiverData = getPlayerDataFor(receiver);
			receiverData.getTempIgnoredPlayers().add(sender.getUniqueId());
			getServer().getScheduler().runTaskLater(RaptorNecessities.this, () -> {
				if(receiver.isOnline()) {
					getPlayerDataFor(receiver).getTempIgnoredPlayers().remove(sender.getUniqueId());
				}
			}, config.getAutoIgnoreTimeOnCancelInTicks());
		}
		
		@Override
		public void cancel() {
			try {
				if(isCancelled())
					return;
			} catch(IllegalStateException e) {
				return;
			}
			super.cancel();
			removeFromTeleportRequests();
			sender.spigot().sendMessage(config.getTeleportCancelMessage());
			receiver.spigot().sendMessage(config.getTeleportCancelMessage());
		}
		
		@Override
		public void run() {
			removeFromTeleportRequests();
		}
		
	}

	class CommandBack extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			PlayerData playerData = getPlayerDataFor(sender);
			Location back = playerData.getPreviousLocation();
			if(back == null) {
				sender.spigot().sendMessage(config.getNoPreviousLocationMessage());
			} else {
				teleport(sender, back);
			}
		}
		
	}
	
	class CommandTpa extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			if(args.length != 1) {
				sender.sendMessage(unknownCommand(label, args));
				return;
			}
			
			Player receiver = player(args[0]);
			if(receiver == null) {
				sender.sendMessage(playerNotFound(args[0], label, args, 1));
				return;
			}
			if(receiver.equals(sender)) {
				sender.spigot().sendMessage(config.confusion());
				return;
			}
			
			PlayerData receiverData = getPlayerDataFor(receiver);
			if(receiverData.isIgnoringRequestsFrom(sender)) {
				sender.spigot().sendMessage(config.getTeleportBlockedMessage(receiver));
				return;
			}
			
			TeleportRequest pending = getPendingTeleportRequest(receiver);
			if(pending != null) {
				sender.spigot().sendMessage(config.getPendingTeleportRequestMessage(receiver, Duration.between(pending.startTime, Instant.now())));
				return;
			}
			
			newTeleportRequest(sender, receiver, false);
		}

		@Override
		public List<String> onTabComplete(Player player, String alias, String[] args) {
			if(args.length <= 1)
				return onlinePlayerNamesMatchingLastArg(args);
			else return Collections.emptyList();
		}
		
	}
	
	class CommandTpaHere extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			if(args.length != 1) {
				sender.sendMessage(unknownCommand(label, args));
				return;
			}
			
			Player receiver = player(args[0]);
			if(receiver == null) {
				sender.sendMessage(playerNotFound(args[0], label, args, 1));
				return;
			}
			if(receiver.equals(sender)) {
				sender.spigot().sendMessage(config.confusion());
				return;
			}
			
			PlayerData receiverData = getPlayerDataFor(receiver);
			if(receiverData.isIgnoringRequestsFrom(sender)) {
				sender.spigot().sendMessage(config.getTeleportBlockedMessage(receiver));
				return;
			}
			
			TeleportRequest pending = getPendingTeleportRequest(receiver);
			if(pending != null) {
				sender.spigot().sendMessage(config.getPendingTeleportRequestMessage(receiver, Duration.between(pending.startTime, Instant.now())));
				return;
			}
			
			newTeleportRequest(sender, receiver, true);
		}
		
		@Override
		public List<String> onTabComplete(Player player, String alias, String[] args) {
			if(args.length <= 1)
				return onlinePlayerNamesMatchingLastArg(args);
			else return Collections.emptyList();
		}
		
	}
	
	class CommandTpaAll extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {			
			if(args.length != 0) {
				sender.sendMessage(unknownCommand(label, args));
				return;
			}
			
			int count = 0;
			for(Player receiver : getServer().getOnlinePlayers()) {
				if(!sender.equals(receiver)) {
					TeleportRequest pending = getPendingTeleportRequest(receiver);
					if(pending == null && !getPlayerDataFor(receiver).isIgnoringRequestsFrom(sender)) {
						newTeleportRequest(sender, receiver, true);
						count++;
					}
				}
			}
			
			sender.spigot().sendMessage(config.getTpaAllMessage(count));
		}
		
	}
	
	class CommandTpCancel extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			if(!cancelPendingTeleports(sender)) {
				if(!cancelPendingTeleportRequest(sender)) {
					sender.spigot().sendMessage(config.getNoPendingInvitationsMessage());
				}
			}
		}
		
	}
	
	class CommandTpAccept extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			TeleportRequest request = getPendingTeleportRequest(sender);
			if(request == null || request.receiver != sender) {
				sender.spigot().sendMessage(config.getNoPendingInvitationsMessage());
				return;
			}
			
			request.accept();
		}
		
	}
	
	class CommandTpDeny extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			TeleportRequest request = getPendingTeleportRequest(sender);
			if(request == null || request.receiver != sender) {
				sender.spigot().sendMessage(config.getNoPendingInvitationsMessage());
				return;
			}
			
			request.deny();
		}
		
	}
	
	class CommandTpIgnore extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			if(args.length > 1) {
				sender.sendMessage(unknownCommand(label, args));
				return;
			}
			
			if(args.length == 0) {
				if(sender.hasPermission("raptortps.tpignore.list")) {
					PlayerData playerData = getPlayerDataFor(sender);
					Set<OfflinePlayer> ignoredPlayers = playerData.getIgnoredPlayers().stream()
							.map(RCommand::offlinePlayer)
							.collect(hashSetCollector());
					sender.spigot().sendMessage(config.getIgnoreList(ignoredPlayers));
				} else {
					sender.sendMessage(missingArgument("name", label, args, 1));
				}
			} else {
				OfflinePlayer ignored = offlinePlayer(args[0]);
				if(ignored == null) {
					sender.sendMessage(playerNotFound(args[0], label, args, 1));
					return;
				}
				
				PlayerData playerData = getPlayerDataFor(sender);
				playerData.ignoreRequestsFrom(ignored);
				sender.spigot().sendMessage(config.getTeleportIgnoredMessage(ignored));
			}
		}
		
	}
	
	class CommandTpUnignore extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			if(args.length != 1) {
				sender.sendMessage(unknownCommand(label, args));
				return;
			}
			
			OfflinePlayer ignored = offlinePlayer(args[0]);
			if(ignored == null) {
				sender.sendMessage(playerNotFound(args[0], label, args, 1));
				return;
			}
			
			PlayerData playerData = getPlayerDataFor(sender);
			playerData.stopIgnoringRequestsFrom(ignored);
			sender.spigot().sendMessage(config.getTeleportUnignoredMessage(ignored));
		}
		
	}
	
	class CommandHome extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			PlayerData playerData;
			
			String home;
			/*
			 * if they have only 1 home set, then
			 * 		use that home name as default home name;
			 * otherwise, if they have no homes set, then
			 * 		let them know this and return;
			 * otherwise,
			 * 		list their homes.
			 */
			if(args.length == 0) {
				playerData = getPlayerDataFor(sender);
				switch(playerData.homeCount()) {
				case 0:
					sender.spigot().sendMessage(config.getNoHomesSetMessage());
					return;
				case 1:
					home = playerData.getHomeNames().iterator().next();
					break;
				default:
					sender.spigot().sendMessage(config.getHomesList(playerData.getHomes()));
					return;
				}
			} else {
				home = StringUtils.strip(join(args));
				if(home.isEmpty()) {
					sender.sendMessage(missingArgument("home", label, args, 1));
					return;
				}
				if(sender.hasPermission("raptortps.home.others")) {
					int dotIndex = home.indexOf('.');
					if(dotIndex > 0 && dotIndex < home.length()-1) {
						String name = home.substring(0, dotIndex);
						OfflinePlayer player = offlinePlayer(name);
						if(player == null) {
							sender.sendMessage(playerNotFound(name, label, args, 1));
							return;
						} else {
							playerData = getPlayerDataFor(player);
							home = StringUtils.strip(home.substring(dotIndex+1));
							if(home.isEmpty()) {
								sender.sendMessage(missingArgument("home", label, args, 1));
								return;
							}
						}
					} else {
						playerData = getPlayerDataFor(sender);
					}
				} else {
					playerData = getPlayerDataFor(sender);
				}
			}
			
			Location dest = playerData.getHome(home);
			if(dest == null) {
				sender.spigot().sendMessage(config.getNoSuchHomeMessage(home));
			} else {
				teleport(sender, dest);
			}

			return;
		}

		@Override
		public List<String> onTabComplete(Player sender, String alias, String[] args) {
			if(args.length <= 1) {
				String home = join(args);
				if(sender.hasPermission("raptortps.home.others")) {
					int dotIndex = home.indexOf('.');
					if(dotIndex > 0 && dotIndex < home.length()-1) {
						OfflinePlayer player = offlinePlayer(home.substring(0, dotIndex));
						if(player != null) {
							PlayerData playerData = getPlayerDataFor(player);
							List<String> results = stringsMatching(home.substring(dotIndex+1), playerData.getHomeNames());
							String prefix = home.substring(0, dotIndex+1);
							return results.stream().map(str -> prefix + str).collect(Collectors.toList());
						}
					}
				}
				PlayerData playerData = getPlayerDataFor(sender);
				return stringsMatching(home, playerData.getHomeNames());
			}
			return Collections.emptyList();
		}
		
	}
	
	class CommandSetHome extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			PlayerData playerData;
			
			String home;
			/*
			 *	if their max limit of homes is 1, then:
			 * 		if they have already set a single home, then
			 * 			use that home name;
			 * 		otherwise, if they have set no homes, then
			 * 			use a default home name of "home";
			 * 	otherwise, send error message.
			 */
			if(args.length == 0) {
				playerData = getPlayerDataFor(sender);
				switch(playerData.homeCount()) {
				case 0:
					home = "home";
					break;
				case 1:
					home = playerData.getHomeNames().iterator().next();
					break;
				default:
					sender.sendMessage(missingArgument("name", label, args, 1));
					return;
				}
			} else {
				home = StringUtils.strip(join(args));
				if(home.isEmpty()) {
					sender.sendMessage(missingArgument("name", label, args, 1));
					return;
				}
				if(sender.hasPermission("raptortps.sethome.others")) {
					int dotIndex = home.indexOf('.');
					if(dotIndex > 0 && dotIndex < home.length()-1) {
						String name = home.substring(0, dotIndex);
						OfflinePlayer player = offlinePlayer(name);
						if(player == null) {
							sender.sendMessage(playerNotFound(name, label, args, 1));
							return;
						} else {
							playerData = getPlayerDataFor(player);
							home = StringUtils.strip(home.substring(dotIndex+1));
							if(home.isEmpty()) {
								sender.sendMessage(missingArgument("name", label, args, 1));
								return;
							}
						}
					} else {
						playerData = getPlayerDataFor(sender);
					}
				} else {
					playerData = getPlayerDataFor(sender);
				}
			}
			
			int maxHomes = config.getMaxHomesFor(sender);
			Location loc = sender.getLocation();		
			
			// if they have reached their max limit of homes, and
			// they are not trying to change an already set home,
			// send the 'too many homes' message and return.
			if(!playerData.hasHome(home) && playerData.homeCount() >= maxHomes
					&& !sender.hasPermission("raptortps.sethome.multiple.override")) {
				sender.spigot().sendMessage(config.getHomeMaxLimitReachedMessage(maxHomes));
				return;
			}
			
			if(home.length() > config.getMaxHomeNameLength()) {
				sender.spigot().sendMessage(config.getHomeNameTooLongMessage());
				return;
			}
			
			playerData.setHome(home, loc);
			sender.spigot().sendMessage(config.getHomeSetMessage(home));
		}
		
		@Override
		public List<String> onTabComplete(Player sender, String alias, String[] args) {
			if(args.length <= 1) {
				String home = join(args);
				if(sender.hasPermission("raptortps.sethome.others")) {
					int dotIndex = home.indexOf('.');
					if(dotIndex > 0 && dotIndex < home.length()-1) {
						OfflinePlayer player = offlinePlayer(home.substring(0, dotIndex));
						if(player != null) {
							PlayerData playerData = getPlayerDataFor(player);
							List<String> results = stringsMatching(home.substring(dotIndex+1), playerData.getHomeNames());
							String prefix = home.substring(0, dotIndex+1);
							return results.stream().map(str -> prefix + str).collect(Collectors.toList());
						}
					}
				}
				PlayerData playerData = getPlayerDataFor(sender);
				return stringsMatching(home, playerData.getHomeNames());
			}
			return Collections.emptyList();
		}
		
	}
	
	class CommandDelHome extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			PlayerData playerData;
			
			String home;
			/*
			 *	if their max limit of homes is 1, then:
			 * 		if they have already set a single home, then
			 * 			use that home name;
			 * 		otherwise, if they have set no homes, then
			 * 			use a default home name of "home";
			 * 	otherwise, send error message.
			 */
			if(args.length == 0) {
				playerData = getPlayerDataFor(sender);
				switch(playerData.homeCount()) {
				case 0:
					sender.spigot().sendMessage(config.getNoHomesSetMessage());
					return;
				case 1:
					home = playerData.getHomeNames().iterator().next();
					break;
				default:
					sender.sendMessage(missingArgument("home", label, args, 1));
					return;
				}
			} else {
				home = StringUtils.strip(join(args));
				if(home.isEmpty()) {
					sender.sendMessage(missingArgument("home", label, args, 1));
					return;
				}
				if(sender.hasPermission("raptortps.delhome.others")) {
					int dotIndex = home.indexOf('.');
					if(dotIndex > 0 && dotIndex < home.length()-1) {
						String name = home.substring(0, dotIndex);
						OfflinePlayer player = offlinePlayer(name);
						if(player == null) {
							sender.sendMessage(playerNotFound(name, label, args, 1));
							return;
						} else {
							playerData = getPlayerDataFor(player);
							home = StringUtils.strip(home.substring(dotIndex+1));
							if(home.isEmpty()) {
								sender.sendMessage(missingArgument("home", label, args, 1));
								return;
							}
						}
					} else {
						playerData = getPlayerDataFor(sender);
					}
				} else {
					playerData = getPlayerDataFor(sender);
				}
			}
			
			if(!playerData.hasHome(home)) {
				sender.spigot().sendMessage(config.getNoSuchHomeMessage(home));
				return;
			}
			
			playerData.delHome(home);
			sender.spigot().sendMessage(config.getHomeRemovedMessage(home));
		}
		
		@Override
		public List<String> onTabComplete(Player sender, String alias, String[] args) {
			if(args.length <= 1) {
				String home = join(args);
				if(sender.hasPermission("raptortps.delhome.others")) {
					int dotIndex = home.indexOf('.');
					if(dotIndex > 0 && dotIndex < home.length()-1) {
						OfflinePlayer player = offlinePlayer(home.substring(0, dotIndex));
						if(player != null) {
							PlayerData playerData = getPlayerDataFor(player);
							List<String> results = stringsMatching(home.substring(dotIndex+1), playerData.getHomeNames());
							String prefix = home.substring(0, dotIndex+1);
							return results.stream().map(str -> prefix + str).collect(Collectors.toList());
						}
					}
				}
				PlayerData playerData = getPlayerDataFor(sender);
				return stringsMatching(home, playerData.getHomeNames());
			}
			return Collections.emptyList();
		}
		
	}
	
	class CommandWarp extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			if(args.length == 0) {
				if(sender.hasPermission("raptortps.warp.list")) {
					sender.spigot().sendMessage(config.getWarpsList(warps));
				} else {
					sender.sendMessage(missingArgument("warp", label, args, 1));
				}
			} else {
				String warp = StringUtils.strip(join(args));
				if(warp.isEmpty()) {
					sender.sendMessage(missingArgument("warp", label, args, 1));
					return;
				}
				
				if(!hasWarp(warp) || config.arePermissionBasedWarpsEnabled() && !sender.hasPermission("raptortps.warps." + warp)) {
					sender.spigot().sendMessage(config.getNoSuchWarpMessage(warp));
					return;
				}
				
				teleport(sender, getWarp(warp));
			}
		}

		@Override
		public List<String> onTabComplete(Player player, String label, String[] args) {
			return stringsMatching(join(args), getWarpNames(player));
		}
	
	}
	
	class CommandSetWarp extends RPlayerCommand {

		@Override
		public void onCommand(Player sender, String label, String[] args) {
			if(args.length == 0) {
				sender.sendMessage(missingArgument("warp", label, args, 1));
				return;
			}
			
			String warp = StringUtils.strip(join(args));
			if(warp.isEmpty()) {
				sender.sendMessage(missingArgument("warp", label, args, 1));
				return;
			}
			
			Location loc = sender.getLocation();
			
			setWarp(warp, loc);
			sender.spigot().sendMessage(config.getWarpSetMessage(warp));
		}
		
		@Override
		public List<String> onTabComplete(Player player, String label, String[] args) {
			return stringsMatching(join(args), getWarpNames());
		}
		
	}
	
	class CommandDelWarp extends RCommand {

		@Override
		public void onCommand(CommandSender sender, String label, String[] args) {
			if(args.length == 0) {
				sender.sendMessage(missingArgument("warp", label, args, 1));
				return;
			}
			
			String warp = StringUtils.strip(join(args));
			if(warp.isEmpty()) {
				sender.sendMessage(missingArgument("warp", label, args, 1));
				return;
			}
			
			if(!hasWarp(warp)) {
				sender.spigot().sendMessage(config.getNoSuchWarpMessage(warp));
				return;
			}
			
			deleteWarp(warp);
			sender.spigot().sendMessage(config.getWarpRemovedMessage(warp));
		}
		
		@Override
		public List<String> onTabComplete(Player player, String label, String[] args) {
			return stringsMatching(join(args), getWarpNames());
		}
		
	}
	
	class CommandNickname extends RCommand {

		@Override
		public void onCommand(CommandSender sender, String label, String[] args) {
			Player player;
			String nickname;
			if(args.length == 2 && sender.hasPermission("raptortps.nickname.others")) {
				player = player(args[0]);
				if(player == null) {
					sender.sendMessage(playerNotFound(args[0], label, args, 1));
					return;
				}
				nickname = args[1];
			} else {
				if(args.length != 1) {
					sender.sendMessage(missingArgument("name", label, args, 1));
					return;
				}
				if(!(sender instanceof Player)) {
					sender.sendMessage(mustBePlayerMessage());
					return;
				}
				
				player = (Player)sender;
				nickname = args[0];				
			}
			
			if("-off".equals(nickname) || nickname.equals(player.getName())) {
				nickname = null;
			} else if(nickname.equals(player.getDisplayName())) {
				sender.spigot().sendMessage(config.confusion());
				return;
			} else if(isNicknameInUseBySomeoneElse(nickname, player)) {
				sender.spigot().sendMessage(config.getNicknameAlreadyInUseMessage());
				return;
			} else if(nickname.length() > config.getMaxNicknameLength()) {
				sender.spigot().sendMessage(config.getNicknameTooLongMessage());
				return;
			}
			
			try {
				PlayerData playerData = getPlayerDataFor(player);
				String oldNickname = playerData.getNickname();
				playerData.setNickname(nickname);
				player.setDisplayName(nickname);
				if(nickname == null) {
					if(oldNickname != null) {
						player.spigot().sendMessage(config.getNicknameRemovedMessage());
						nicknames.remove(oldNickname, player.getUniqueId());
					}
				} else {
					if(oldNickname != null) {
						nicknames.remove(oldNickname, player.getUniqueId());
					}
					player.spigot().sendMessage(config.getNicknameSetMessage(nickname));
					nicknames.put(nickname, player.getUniqueId());
				}	
			} catch(IllegalArgumentException e) {
				sender.sendMessage(error("Invalid nickname '" + nickname + "': nicknames can only contain letters, numbers, and underscores.", label, args));
			}
		}
	}
	
	class CommandRaptorNecessities extends RCommand {

		@Override
		public void onCommand(CommandSender sender, String label, String[] args) {
			if(args.length != 1) {
				sender.sendMessage(unknownCommand(label, args));
				return;
			}
			
			switch(args[0]) {
			case "reload":
				sender.sendMessage("\u00a7cReloading RaptorNecessities");
				reload();
				sender.sendMessage("\u00a7aReload complete!");
				break;
			case "load":
				clearData();
				reloadConfig();
				loadData();
				break;
			case "save":
				saveData();
				break;
			default:
				sender.sendMessage(unknownCommand(label, args));
				return;
			}
		}

		@Override
		public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
			if(args.length <= 1) {
				return stringsMatchingLastArg(args, "reload", "load", "save");
			}
			return super.onTabComplete(sender, label, args);
		}
		
	}
}
