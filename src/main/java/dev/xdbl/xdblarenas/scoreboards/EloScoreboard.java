package dev.xdbl.xdblarenas.scoreboards;

import dev.xdbl.xdblarenas.players.ArenaPlayer;
import dev.xdbl.xdblarenas.XDBLArena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EloScoreboard {

    private final XDBLArena plugin;

    public EloScoreboard(XDBLArena plugin) {
        this.plugin = plugin;
    }

    public void updateScoreboard() {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboardDefault = scoreboardManager.getNewScoreboard();
        Objective objectivePlayerList = scoreboardDefault.registerNewObjective("eloObjectivePlayerList", "dummy", ChatColor.GREEN + "Top Elo Players");
        Objective objectiveBelowName = scoreboardDefault.registerNewObjective("eloObjectiveBelowName", "dummy", ChatColor.GREEN + "ELO");

        // Get all online players and set their score to their Elo rating
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            ArenaPlayer arenaPlayer = plugin.getPlayerManager().getArenaPlayer(onlinePlayer.getUniqueId());

            objectivePlayerList.getScore(onlinePlayer.getName()).setScore(arenaPlayer.getElo());
            objectiveBelowName.getScore(onlinePlayer.getName()).setScore(arenaPlayer.getElo());
            objectivePlayerList.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            objectiveBelowName.setDisplaySlot(DisplaySlot.BELOW_NAME);

            onlinePlayer.setScoreboard(scoreboardDefault);
        }
    }
}
