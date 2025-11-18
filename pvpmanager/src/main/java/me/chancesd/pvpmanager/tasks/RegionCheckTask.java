package me.chancesd.pvpmanager.tasks;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import me.chancesd.pvpmanager.event.PlayerTagEvent;
import me.chancesd.pvpmanager.event.PlayerUntagEvent;
import me.chancesd.pvpmanager.manager.DependencyManager;
import me.chancesd.pvpmanager.manager.PlayerManager;
import me.chancesd.pvpmanager.player.CombatPlayer;
import me.chancesd.pvpmanager.setting.Lang;
import me.chancesd.sdutils.scheduler.ScheduleUtils;

public class RegionCheckTask extends BukkitRunnable implements Listener {

	private final PlayerManager playerHandler;
	private final Map<CombatPlayer, Location> lastLocations = new HashMap<>();
	private final DependencyManager dependencyManager;

	public RegionCheckTask(final PlayerManager playerHandler, final DependencyManager dependencyManager) {
		this.playerHandler = playerHandler;
		this.dependencyManager = dependencyManager;
	}

	@SuppressWarnings("null") // playerLocation can't be null
	@Override
	public void run() {
		for (final CombatPlayer pvPlayer : playerHandler.getPlayersInCombat()) {
			final Player player = pvPlayer.getPlayer();
			final Location playerLocation = player.getLocation();
			if (!dependencyManager.canAttackAt(player, playerLocation)) {
				final Location lastLocation = lastLocations.get(pvPlayer);
				if (lastLocation == null)
					continue;
				lastLocation.setPitch(playerLocation.getPitch());
				lastLocation.setYaw(playerLocation.getYaw());
				safeTeleportWithVehicle(player, lastLocation);
				//ScheduleUtils.teleport(player, lastLocation, "Failed to teleport player out of non-PvP region");
				pvPlayer.message(Lang.PUSHBACK_WARNING);
			} else {
				lastLocations.put(pvPlayer, playerLocation);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTag(final PlayerTagEvent event) {
		lastLocations.put(event.getCombatPlayer(), event.getPlayer().getLocation());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTag(final PlayerUntagEvent event) {
		lastLocations.remove(event.getCombatPlayer());
	}
	
	private void safeTeleportWithVehicle(Player player, Location loc) {
        var vehicle = player.getVehicle();

        if (vehicle != null) {
            vehicle.eject();

            // Teleport vehicle
            ScheduleUtils.teleport(vehicle, loc, "Failed to teleport vehicle out of non-PvP region");

            // Teleport player
            ScheduleUtils.teleport(player, loc, "Failed to teleport player out of non-PvP region");

            // Add player back
            vehicle.addPassenger(player);
        } else {
            // No vehicle, normal teleport
            ScheduleUtils.teleport(player, loc, "Failed to teleport player out of non-PvP region");
        }
    }

}
