package supermemnon.simpleunmount;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import jdk.nashorn.internal.objects.annotations.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class SimpleUnmount extends JavaPlugin {
    static final String bypassPermission = "simpleunmount.bypass";
    static final String ejectMessage = "You can't ride that here!";
    static final StateFlag unmountFlag = new StateFlag("unmount", false);
    WorldEditPlugin worldEditPlugin;
    WorldGuardPlugin worldGuardPlugin;
    static WorldGuard worldGuard;

   @Override
   public void onLoad() {
       this.worldEditPlugin = (WorldEditPlugin) this.getServer().getPluginManager().getPlugin("WorldEdit");
       this.worldGuardPlugin = (WorldGuardPlugin) this.getServer().getPluginManager().getPlugin("WorldGuard");
       this.worldGuard = WorldGuard.getInstance();

       try {
           FlagRegistry flagRegistry = this.worldGuard.getFlagRegistry();
           flagRegistry.register(unmountFlag);

       }
       catch (Exception e) {
           this.getServer().getPluginManager().disablePlugin(this);
       }

   }
    @Override
    public void onEnable() {
       getServer().getPluginManager().registerEvents(new EventListener(), this);
    }

    @Override
    public void onDisable() {

    }

    public static boolean inUnmountRegion(Location location) {
        ProtectedRegion globalRegion = worldGuard.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld())).getRegion(ProtectedRegion.GLOBAL_REGION);
        boolean flag = globalRegion.getFlag(unmountFlag) == StateFlag.State.ALLOW;
        int highest = 0;
       final ApplicableRegionSet regions = worldGuard.getPlatform().getRegionContainer().createQuery().getApplicableRegions(BukkitAdapter.adapt(Objects.requireNonNull(location)));
        for (ProtectedRegion region : regions) {
            if (region.getFlag(unmountFlag) != null && region.getPriority() >= highest) {
                flag = region.getFlag(unmountFlag) == StateFlag.State.ALLOW;
                highest = region.getPriority();
            }
        }
       return flag;
    }

    public class EventListener implements Listener {
       @EventHandler
       public void onVehicleEnter(VehicleEnterEvent e) {
           if (!(e.getEntered() instanceof Player) || e.getEntered().hasPermission(bypassPermission) || !inUnmountRegion(e.getVehicle().getLocation())) {
               return;
           }
           e.setCancelled(true);
           e.getEntered().sendMessage(ejectMessage);
       }
       @EventHandler
        public void onPlayerMove(PlayerMoveEvent e) {
            if (e.getPlayer().getVehicle() == null || e.getPlayer().hasPermission(bypassPermission) || !inUnmountRegion(e.getTo())) {
               return;
            }
            e.getPlayer().getVehicle().eject();
            e.getPlayer().sendMessage(ejectMessage);
           }
       }
    }
