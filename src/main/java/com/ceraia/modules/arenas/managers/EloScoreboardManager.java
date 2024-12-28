package com.ceraia.modules.arenas.managers;

import com.ceraia.modules.arenas.Double;
import com.ceraia.modules.arenas.types.DoublePlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class EloScoreboardManager {

    private final Double plugin;

    public EloScoreboardManager(Double plugin) {
        this.plugin = plugin;
    }

    public void updateScoreboard() {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboardDefault = scoreboardManager.getNewScoreboard();
        Objective objectivePlayerList = scoreboardDefault.registerNewObjective("eloObjectivePlayerList", "dummy", MiniMessage.miniMessage().deserialize("Top Arena Players"));
        Objective objectiveBelowName = scoreboardDefault.registerNewObjective("eloObjectiveBelowName", "dummy", MiniMessage.miniMessage().deserialize("<green>ELO"));

        // Get all online players and set their score to their Elo rating
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(onlinePlayer.getUniqueId());

            objectivePlayerList.getScore(onlinePlayer.getName()).setScore(doublePlayer.getElo());
            objectiveBelowName.getScore(onlinePlayer.getName()).setScore(doublePlayer.getElo());
            objectivePlayerList.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            objectiveBelowName.setDisplaySlot(DisplaySlot.BELOW_NAME);

            onlinePlayer.setScoreboard(scoreboardDefault);
        }
    }
}
