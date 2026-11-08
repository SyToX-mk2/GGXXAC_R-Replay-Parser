package application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchRecord {
    public String filename;
    public long p1SteamID, p2SteamID;
    public String p1Name, p2Name;
    public String p1Char, p2Char;
    public int p1RoundsWon, p2RoundsWon;
    public int p1Score, p2Score;
    public int p1Rank, p2Rank;
    public int winner;
    public long durationFrames;
    public int year, month, day;
    public String time;
    public String matchType, gameVersion, matchFlag;
    public boolean outdated;
    public int ping;
    public int optionsModified;
    public String organizedPath;

    public MatchRecord() {}

    public String formatDate() {
        return String.format("%04d-%02d-%02d %s", year, month, day, time);
    }

    public boolean isMainP1(long mainPlayerID) {
        return p1SteamID == mainPlayerID;
    }

    public boolean didMainWin(long mainPlayerID) {
        boolean mainIsP1 = isMainP1(mainPlayerID);
        if (p1RoundsWon > p2RoundsWon) return mainIsP1;
        if (p2RoundsWon > p1RoundsWon) return !mainIsP1;
        return false;
    }

    public String getWinnerName() {
        if (p1RoundsWon > p2RoundsWon) return p1Name;
        if (p2RoundsWon > p1RoundsWon) return p2Name;
        return null;
    }

    public ReplayDisplay toReplayDisplay(long mainPlayerID) {
        return toReplayDisplay(mainPlayerID, false);
    }

    public ReplayDisplay toReplayDisplay(long mainPlayerID, boolean includeOpponent) {
        boolean mainIsP1 = isMainP1(mainPlayerID);
        String mainChar = mainIsP1 ? p1Char : p2Char;
        String oppChar = mainIsP1 ? p2Char : p1Char;
        String opponentName = includeOpponent ? (mainIsP1 ? p2Name : p1Name) : null;
        return new ReplayDisplay(mainChar, oppChar, opponentName,
            didMainWin(mainPlayerID), gameVersion, matchFlag, outdated,
            formatDate(), organizedPath, getWinnerName(), null, null);
    }

    public ReplayDisplay toSpectatedReplayDisplay() {
        boolean p1Won = p1RoundsWon > p2RoundsWon;
        return new ReplayDisplay(p1Char, p2Char, null, p1Won,
            gameVersion, matchFlag, outdated, formatDate(), organizedPath,
            getWinnerName(), p1Name, p2Name);
    }

    public static class MatchupStats {
        public final Map<String, int[]> matchupStats;
        public final int totalWins;
        public final int totalMatches;

        public MatchupStats(Map<String, int[]> matchupStats, int totalWins, int totalMatches) {
            this.matchupStats = matchupStats;
            this.totalWins = totalWins;
            this.totalMatches = totalMatches;
        }
    }

    public static MatchupStats computeStats(List<MatchRecord> matches, long mainPlayerID) {
        Map<String, int[]> matchupStats = new HashMap<>();
        int totalWins = 0;
        int totalMatches = 0;

        for (MatchRecord m : matches) {
            boolean mainIsP1 = m.isMainP1(mainPlayerID);
            String mainChar = mainIsP1 ? m.p1Char : m.p2Char;
            String oppChar = mainIsP1 ? m.p2Char : m.p1Char;

            String matchupKey = "As " + mainChar + " vs " + oppChar;
            int[] stats = matchupStats.computeIfAbsent(matchupKey, k -> new int[]{0, 0});
            stats[1]++;
            totalMatches++;

            if (m.didMainWin(mainPlayerID)) {
                stats[0]++;
                totalWins++;
            }
        }

        return new MatchupStats(matchupStats, totalWins, totalMatches);
    }
}