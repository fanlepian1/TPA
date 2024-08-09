package top.craft_hello.tpa;

import cn.handyplus.lib.adapter.HandySchedulerUtil;
import cn.handyplus.lib.adapter.PlayerSchedulerUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.*;

public class Request {
    private static final Map<Player, List<Player>> tpa = new HashMap<>();
    private final Player executor;
    private Player target;
    private boolean commandError;
    private String playerName;
    private String warpName;
    private final String label;
    private boolean offlineOrNull;

    private boolean youToYou;
    private static boolean isTpHere;
    private final FileConfiguration config = TPA.getPlugin(TPA.class).getConfig();

    private final FileConfiguration warpConfig = TPA.getPlugin(TPA.class).getWarpConfig();
    private Location deathlocation;

    private final long acceptDelay = config.getLong("accept_delay") < 0L ? 30000L : config.getLong("accept_delay") * 1000L;
    private final long teleportDelay = config.getLong("teleport_delay") < 0L ? 3000L : config.getLong("teleport_delay") * 1000L;

    public Request(CommandSender sender, String label, String[] args) {
        this.executor = (Player) sender;
        if (args != null){
            this.commandError = args.length != 1;

            if (!this.commandError) {
                this.warpName = args[args.length - 1];
                this.playerName = args[(args.length - 1)];
                this.target = this.executor.getServer().getPlayerExact(this.playerName);
                this.offlineOrNull = this.target == null;
                this.youToYou = this.executor == this.target;
            }
        }
        this.label = label;
    }
    public void getDeathLocation(Location l){
        deathlocation = l;
    }
    private boolean errorCheck() {
        if (this.label.equals("tpa") || this.label.equals("tpa:tpa") || this.label.equals("tphere") || this.label.equals("tpa:tpahere")) {
            if (this.commandError) {
                Messages.commandError(this.executor, this.label);
                return true;
            }

            if (this.offlineOrNull) {
                Messages.offlineOrNull(this.executor, this.playerName);
                return true;
            }

            if (this.youToYou) {
                Messages.requestYou(this.executor);
                return true;
            }

            List<Player> targetList = tpa.get(this.executor);

            if (tpa.containsKey(this.executor) || tpa.containsKey(this.target)) {
                Messages.requestLock(this.executor);
                return true;
            }

            if (targetList !=  null) {
                Player target = targetList.get(targetList.size() - 1);
                if (tpa.containsKey(target))
                    Messages.requestLock(this.executor);
                return true;
            }
            return false;
        }

        if (this.label.equals("tpaccept") || this.label.equals("tpa:tpaccept")) {
            boolean flag = tpa.containsKey(this.executor);
            List<Player> targetList = tpa.get(this.executor);

            if (!flag) {
                Messages.noRequestAccept(this.executor);
                return true;
            }

            if (targetList == null) {
                Messages.noRequestAccept(this.executor);
                return true;
            }

            Player target = targetList.get(targetList.size() - 1);
            if (!target.isOnline()) {
                Messages.offlineOrNull(this.executor, target.getName());
                tpa.remove(this.executor);
                HandySchedulerUtil.cancelTask();
                return true;
            }
            this.target = target;
            return false;
        }


        if (this.label.equals("tpdeny") || this.label.equals("tpa:tpdeny")) {
            boolean flag = tpa.containsKey(this.executor);
            List<Player> targetList = tpa.get(this.executor);

            if (!flag) {
                Messages.noRequestDeny(this.executor);
                return true;
            }

            if (targetList == null) {
                Messages.noRequestDeny(this.executor);
                return true;
            }

            Player target = targetList.get(targetList.size() - 1);
            if (!target.isOnline()) {
                Messages.offlineOrNull(this.executor, target.getName());
                tpa.remove(this.executor);
                HandySchedulerUtil.cancelTask();
                return true;
            }
            this.target = target;
        }

        if (this.label.equals("warp") || this.label.equals("tpa:warp") || this.label.equals("setwarp") || this.label.equals("tpa:setwarp")) {
            if (this.commandError){
                Messages.warpCommandError(this.executor, label);
                return true;
            }
            try {
                warpConfig.save(TPA.getPlugin(TPA.class).getWarpFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (this.label.equals("warp") | this.label.equals("tpa:warp") && warpConfig.getLocation(this.warpName) == null){
                Messages.warpNull(this.executor, this.warpName);
                return true;
            }
        }
        if (this.label.equals("back") || this.label.equals("tpa:back")){
            if (this.deathlocation == null){
                Messages.deathNot(this.executor);
                return true;
            }
        }
        return false;
    }


    private void setTimer(long delay, String TimerName) {

        HandySchedulerUtil.runTaskLaterAsynchronously(() ->{
            switch (TimerName){
                case "timeOverWarpTp":
                    back(true);
                    break;
                case "timeOverBack":
                    warp(true);
                    break;
                case "timeOverTp":
                    tpaccept(true);
                    break;
                case "timeOverDeny":
                    tpdeny(true);
                    break;
                default:
                    break;
            }
        }, delay / 50L);
    }

    public void tpa() {
        if (this.errorCheck()) {
            return;
        }
        List<Player> executorList = new ArrayList<>();
        executorList.add(this.executor);
        tpa.put(this.target, executorList);
        isTpHere = false;
        Messages.targetToYou(this.executor, this.target, acceptDelay / 1000L);
        setTimer(acceptDelay, "timeOverDeny");
    }

    public void tphere() {
        if (this.errorCheck()) {
            return;
        }
        List<Player> executorList = new ArrayList<>();
        executorList.add(this.executor);
        tpa.put(this.target, executorList);
        isTpHere = true;
        Messages.youToTarget(this.executor, this.target, acceptDelay / 1000L);
        setTimer(acceptDelay, "timeOverDeny");
    }

    public void tpaccept(boolean isTimeOverTp) {
        if (this.errorCheck()) {
            return;
        }

        if (!isTimeOverTp) {
            HandySchedulerUtil.cancelTask();
            Messages.acceptMessage(this.executor, this.target, teleportDelay / 1000L, isTpHere);

            if (isTpHere) {
                Location location = this.executor.getLocation();
                HandySchedulerUtil.runTaskTimerAsynchronously(() -> isMove(location, this.executor, this.target), teleportDelay / 1000L, teleportDelay / 1000L);
                setTimer(teleportDelay, "timeOverTp");
                return;
            }

            Location location = this.target.getLocation();
            HandySchedulerUtil.runTaskTimerAsynchronously(() ->
                isMove(location, this.target, this.executor), teleportDelay / 1000L, teleportDelay / 1000L);
            setTimer(teleportDelay, "timeOverTp");
            return;
        }

        HandySchedulerUtil.cancelTask();
        tpa.remove(this.executor);
        tpa.remove(this.target);
        this.tpToPlayer(this.target, this.executor, isTpHere);
    }

    public void tpdeny(Boolean isTimeOverDeny) {
        if (isTimeOverDeny) {
            Messages.timeOverDeny(this.executor, this.target);
            tpa.remove(this.executor);
            tpa.remove(this.target);
            HandySchedulerUtil.cancelTask();
            return;
        }

        if (this.errorCheck()) {
            return;
        }

        Messages.deny(this.executor, this.target);
        tpa.remove(this.executor);
        tpa.remove(this.target);
        HandySchedulerUtil.cancelTask();
    }

    public void warp(boolean isTimeOverTp) {
        if (this.errorCheck()) {
            return;
        }
        if(!isTimeOverTp) {
            Messages.tpTimeMessage(this.executor, this.warpName, teleportDelay / 1000L);

            Location location = this.executor.getLocation();
            HandySchedulerUtil.runTaskTimerAsynchronously(() ->
                isMove(location, this.executor, this.executor), teleportDelay / 1000L, teleportDelay / 1000L);
            setTimer(teleportDelay, "timeOverWarpTp");
            return;
        }

        HandySchedulerUtil.cancelTask();
        Location location = warpConfig.getLocation(this.warpName);
        this.tp(this.executor, location);
        Messages.warpMessage(this.executor, this.warpName);
    }

    public void setwarp(){
        if (this.errorCheck()) {
            return;
        }
        Location loc = this.executor.getLocation();
        warpConfig.set(this.warpName, loc);
        try {
            warpConfig.save(TPA.getPlugin(TPA.class).getWarpFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Messages.setWarp(this.executor, this.warpName);
    }

    public void isMove(Location location, Player executor, Player target) {
        if (executor.getLocation().getX() != location.getX() || executor.getLocation().getY() != location.getY() || executor.getLocation().getZ() != location.getZ()){
            HandySchedulerUtil.cancelTask();
            Messages.move(executor, target);
            if (executor != target){
                tpa.remove(this.executor);
                tpa.remove(this.target);
            }
        }
    }

    private void tpToPlayer(Player executor, Player target, boolean isTphere){
        if (isTphere) {
            tp(target, executor.getLocation());
            return;
        }
        tp(executor, target.getLocation());
    }
    public void back(boolean isTimeOverTp){
        Player player = executor;
        if(!isTimeOverTp) {
            Messages.tpTimeMessage(this.executor,"死亡地点",teleportDelay / 1000L);

            Location location = this.executor.getLocation();
            HandySchedulerUtil.runTaskTimerAsynchronously(() ->
                    isMove(location, this.executor, this.executor), teleportDelay / 1000L, teleportDelay / 1000L);
            setTimer(teleportDelay, "timeOverWarpTp");
            return;
        }

        HandySchedulerUtil.cancelTask();
        this.tp(this.executor, deathlocation);
    }

    public void tp(Player executor, Location location) {
        PlayerSchedulerUtil.syncTeleport(executor, location);
    }
}
