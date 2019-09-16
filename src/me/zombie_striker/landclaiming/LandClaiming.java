package me.zombie_striker.landclaiming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.UUID;

import me.zombie_striker.landclaiming.claimedobjects.ClaimedBlock;
import me.zombie_striker.landclaiming.claimedobjects.ClaimedLand;
import me.zombie_striker.landclaiming.commands.ClaimCommand;
import me.zombie_striker.landclaiming.commands.LockCommand;
import me.zombie_striker.landclaiming.commands.UnclaimCommand;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

public class LandClaiming extends JavaPlugin {

	public List<ClaimedLand> claimedLand = new ArrayList<ClaimedLand>();
	public List<ClaimedBlock> claimedBlock = new ArrayList<ClaimedBlock>();

	public final String INTERACTEVENT = "landclaiming.message.interactblock";
	public final String CLAIMEDLAND = "landclaiming.message.claimland";

	public final String LOCKBLOCK = "landclaiming.message.lockchest";
	public final String UNLOCKBLOCK = "landclaiming.message.unlockchest";
	public final String ALREADYLOCKBLOCK = "landclaiming.message.alreadylockchest";
	public final String NOTLOCKEDBLOCK = "landclaiming.message.notlockedchest";
	public final String LOCKEDCHEST = "landclaiming.message.lockedchest";
	public final String ClaimedCHEST = "landclaiming.message.claimedchest";

	public final String UNCLAIMLAND = "landclaiming.message.unclaimland";
	public final String CLAIMCORNER = "landclaiming.message.claimcorner";
	public final String NOTCLAIM = "landclaiming.message.notclaim";
	public final String MAXCLAIM = "landclaiming.message.maxclaims";
	public final String MAXCLAIMINT = "landclaiming.options.maxclaimedblocks";
	public final String MAXCLAIMINT2 = "landclaiming.options.maxclaimedblocksDEFAULT";
	public final String PERMISSION = "landclaiming.message.permission";

	public final String ADDGUEST = "landclaiming.message.addguest";
	public final String REMOVEGUEST = "landclaiming.message.removeguest";

	public final String PREFIX = ChatColor.GREEN + "[Land-Claiming]";

	public final Permission PERM_DEFAULT = new Permission("landclaim.default");
	public final Permission PERM_ADMIN = new Permission("landclaim.admin");

	public static List<String> interactAbleMaterials = Arrays.asList(new String[]{ "LEVER", "CHEST", "TRAPPED_CHEST",
			"WOOD_DOOR", "ACACIA_DOOR", "SPRUCE_DOOR", "BIRCH_DOOR", "DARK_OAK_DOOR",
			"JUNGLE_DOOR", "TRAP_DOOR", "STONE_BUTTON","WOOD_BUTTON","ENDER_CHEST",
			"DISPENSER", "DROPPER", "FURNACE" });
	public final String iwc = "landclaiming.options.enableInteractWithinClaims";
	public final String iwce = "landclaiming.options.InteractWithinClaimsExceptions";
	
	public static boolean ENABLEINTERACTABLEBLOCKS = false;

	public static final Permission PERM_ALL = new Permission("landclaim.*");

	public HashMap<String, Integer> maxLands = new HashMap<>();


	@SuppressWarnings("unchecked")
	public void onEnable() {
		saveDefaultConfig();
		FileConfiguration config = getConfig();

		ENABLEINTERACTABLEBLOCKS = config.getBoolean(iwc);
		interactAbleMaterials = config.getStringList(iwce);

		for (String g : config.getConfigurationSection(MAXCLAIMINT + ".group").getKeys(false)) {
			maxLands.put("group." + g, config.getInt(MAXCLAIMINT + ".group." + g));
		}

		if (config.contains("landclaiming.claimed"))
			for (String s : config.getStringList("landclaiming.claimed")) {
				ClaimedLand cl = new ClaimedLand(s);
				claimedLand.add(cl);
			}
		if (config.contains("landclaiming.locked"))
			for (String s : config.getStringList("landclaiming.locked")) {
				ClaimedBlock cl = new ClaimedBlock(s);
				claimedBlock.add(cl);
			}

		Bukkit.getPluginManager().registerEvents(new LandProtecter(this), this);
		Bukkit.getPluginManager().registerEvents(new LandClaimer(this), this);

		LockCommand lc = new LockCommand(this);
		ClaimCommand cc = new ClaimCommand(this);
		UnclaimCommand uc = new UnclaimCommand(this);

		this.getCommand("lock").setExecutor(lc);
		this.getCommand("unlock").setExecutor(lc);
		this.getCommand("unlock").setTabCompleter(lc);

		this.getCommand("claim").setExecutor(cc);
		this.getCommand("unclaim").setExecutor(uc);
		
		this.getCommand("claim").setTabCompleter(cc);
		this.getCommand("unclaim").setTabCompleter(uc);
	}

	@Override
	public void onDisable() {
		save();
	}

	public void save() {
		List<String> lips = new ArrayList<>();
		for (ClaimedLand cl : this.claimedLand) {
			lips.add(cl.serialize());
		}
		getConfig().set("landclaiming.claimed", lips);

		List<String> lips2 = new ArrayList<>();
		for (ClaimedBlock cl2 : this.claimedBlock) {
			lips2.add(cl2.serialize());
		}
		getConfig().set("landclaiming.locked", lips2);
		saveConfig();
	}

	public String getMessage(String message) {
		return ChatColor.translateAlternateColorCodes('&', getConfig().getString(message));
	}

	public boolean isInArea(Location l) {
		for (ClaimedLand cl : claimedLand) {
			if ((l.getBlockX() >= cl.getMinLoc().getBlockX() && l.getBlockX() <= cl.getMaxLoc().getBlockX())
					&& (l.getBlockZ() >= cl.getMinLoc().getBlockZ() && l.getBlockZ() <= cl.getMaxLoc().getBlockZ()))
				if (l.getWorld() == cl.getMaxLoc().getWorld())
					return true;
		}
		return false;
	}

	public ClaimedLand getArea(Location l) {
		for (ClaimedLand cl : claimedLand) {
			if ((l.getBlockX() >= cl.getMinLoc().getBlockX() && l.getBlockX() <= cl.getMaxLoc().getBlockX())
					&& (l.getBlockZ() >= cl.getMinLoc().getBlockZ() && l.getBlockZ() <= cl.getMaxLoc().getBlockZ())) {
				if (l.getWorld() == cl.getMaxLoc().getWorld())
					return cl;
			}
		}
		return null;
	}

	public boolean isIntersecting(World w, int xmin, int zmin, int xmax, int zmax) {
		for (ClaimedLand cl : claimedLand) {
			if (cl.getMinLoc().getBlockX() > xmax || cl.getMaxLoc().getBlockX() < xmin
					|| cl.getMinLoc().getBlockZ() > zmax || cl.getMaxLoc().getBlockZ() < zmin)
				continue;
			if (cl.getMaxLoc().getWorld() == w)
				return true;
		}
		return false;
	}

	public int getTotalClaimedBlocks(UUID uuid) {
		int amount = 0;
		for (ClaimedLand cl : this.claimedLand) {
			if (cl.getOwner().equals(uuid)) {
				amount += (cl.getMaxLoc().getBlockX() - cl.getMinLoc().getBlockX())
						* (cl.getMaxLoc().getBlockZ() - cl.getMinLoc().getBlockZ());
			}
		}
		return amount;
	}

	public ClaimedBlock getLockedBlock(Location l) {
		for (ClaimedBlock cb : this.claimedBlock) {
			if (cb.getLocation().equals(l))
				return cb;
		}
		return null;
	}

	public boolean isNearLockedBlock(Location l) {
		for (ClaimedBlock cb : this.claimedBlock) {
			if (cb.getLocation().distance(l) < 5) {
				return true;
			}
		}
		return false;
	}
}
