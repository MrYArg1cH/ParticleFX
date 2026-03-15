package xyz.MrYArg1cH.ParticleFX.listener;

import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import xyz.MrYArg1cH.ParticleFX.ParticleFX;
import xyz.MrYArg1cH.ParticleFX.effect.EffectTrigger;

public final class PlayerEffectListener implements Listener {
    private static final double MOVE_THRESHOLD_SQUARED = 0.04D;

    private final ParticleFX plugin;

    public PlayerEffectListener(ParticleFX plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() != Statistic.JUMP) {
            return;
        }

        plugin.getEffectService().play(event.getPlayer(), EffectTrigger.JUMP, event.getPlayer().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getEffectService().play(player, EffectTrigger.DEATH, player.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getEffectService().play(event.getPlayer(), EffectTrigger.RESPAWN, event.getRespawnLocation().clone());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getEffectService().play(event.getPlayer(), EffectTrigger.JOIN, event.getPlayer().getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getEffectService().play(player, EffectTrigger.QUIT, player.getLocation());
        plugin.getPlayerProfileManager().unload(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() != to.getWorld() || from.distanceSquared(to) < MOVE_THRESHOLD_SQUARED) {
            return;
        }

        plugin.getEffectService().play(event.getPlayer(), EffectTrigger.MOVE, to.clone());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSprintToggle(PlayerToggleSprintEvent event) {
        plugin.getEffectService().play(
                event.getPlayer(),
                event.isSprinting() ? EffectTrigger.SPRINT_START : EffectTrigger.SPRINT_STOP,
                event.getPlayer().getLocation()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        plugin.getEffectService().play(
                event.getPlayer(),
                event.isSneaking() ? EffectTrigger.SNEAK_START : EffectTrigger.SNEAK_STOP,
                event.getPlayer().getLocation()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttackOrHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            plugin.getEffectService().play(attacker, EffectTrigger.ATTACK, attacker.getLocation());
        }

        if (event.getEntity() instanceof Player victim) {
            plugin.getEffectService().play(victim, EffectTrigger.HIT, victim.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        plugin.getEffectService().play(event.getPlayer(), EffectTrigger.CONSUME, event.getPlayer().getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) {
            return;
        }

        plugin.getEffectService().play(event.getPlayer(), EffectTrigger.TELEPORT, event.getTo().clone());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.getEffectService().play(
                event.getPlayer(),
                EffectTrigger.BLOCK_BREAK,
                event.getBlock().getLocation().add(0.5D, 0.5D, 0.5D)
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        plugin.getEffectService().play(
                event.getPlayer(),
                EffectTrigger.BLOCK_PLACE,
                event.getBlockPlaced().getLocation().add(0.5D, 0.5D, 0.5D)
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.getEffectService().play(player, EffectTrigger.SHOOT_BOW, player.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLevelUp(PlayerLevelChangeEvent event) {
        if (event.getNewLevel() > event.getOldLevel()) {
            plugin.getEffectService().play(event.getPlayer(), EffectTrigger.LEVEL_UP, event.getPlayer().getLocation());
        }
    }
}
