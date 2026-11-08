package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseManager {

    public static String getDatabaseName() {
        String steamID = ConfigManager.get("mainSteamID", "");
        if (steamID.isEmpty()) {
            return "replays.db";
        }
        return steamID + ".db";
    }

    public static Connection connect() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + getDatabaseName());
    }

    public static void initializeDatabase() throws Exception {
        try (var conn = connect(); var stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS matches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    filename TEXT UNIQUE,
                    year INTEGER, month INTEGER, day INTEGER, time TEXT,
                    p1SteamID INTEGER, p2SteamID INTEGER,
                    p1Name TEXT, p2Name TEXT,
                    p1Char TEXT, p2Char TEXT,
                    p1RoundsWon INTEGER, p2RoundsWon INTEGER,
                    p1Score INTEGER, p2Score INTEGER,
                    p1Rank INTEGER, p2Rank INTEGER,
                    winner INTEGER,
                    durationFrames INTEGER,
                    matchType TEXT,
                    gameVersion TEXT,
                    matchFlag TEXT,
                    ping INTEGER,
                    optionsModified INTEGER,
                    outdated INTEGER DEFAULT 0,
                    organizedPath TEXT
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS opponents (
                    id INTEGER PRIMARY KEY,
                    steamID INTEGER UNIQUE,
                    name TEXT,
                    aliases TEXT,
                    lastSeen TEXT
                )
            """);
        }
    }
    
    public static boolean hasAnyMatches() {
        try (var conn = connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT EXISTS(SELECT 1 FROM matches LIMIT 1)")) {
            return rs.next() && rs.getInt(1) == 1;
        } catch (Exception e) {
            return false;   // fresh install: no .db / no table yet
        }
    }

    public static Set<String> loadAllFilenames() throws Exception {
        Set<String> filenames = new HashSet<>();
        try (var conn = connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT filename FROM matches")) {
            while (rs.next()) {
                filenames.add(rs.getString("filename"));
            }
        }
        return filenames;
    }

    public static Map<Long, String[]> loadAllOpponents() throws Exception {
        Map<Long, String[]> opponents = new HashMap<>();
        try (var conn = connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT steamID, name, aliases FROM opponents")) {
            while (rs.next()) {
                long steamID = rs.getLong("steamID");
                String name = rs.getString("name");
                String aliases = rs.getString("aliases");
                opponents.put(steamID, new String[]{name, aliases != null ? aliases : ""});
            }
        }
        return opponents;
    }

    public static void batchInsertMatches(List<MatchRecord> matches) throws Exception {
        try (var conn = connect()) {
            conn.setAutoCommit(false);
            try (var stmt = conn.prepareStatement("""
                INSERT OR IGNORE INTO matches (
                    filename, year, month, day, time,
                    p1SteamID, p2SteamID, p1Name, p2Name,
                    p1Char, p2Char, p1RoundsWon, p2RoundsWon,
                    p1Score, p2Score, p1Rank, p2Rank, winner,
                    durationFrames, matchType, gameVersion, matchFlag,
                    ping, optionsModified, outdated, organizedPath
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
                for (MatchRecord m : matches) {
                    stmt.setString(1, m.filename);
                    stmt.setInt(2, m.year);
                    stmt.setInt(3, m.month);
                    stmt.setInt(4, m.day);
                    stmt.setString(5, m.time);
                    stmt.setLong(6, m.p1SteamID);
                    stmt.setLong(7, m.p2SteamID);
                    stmt.setString(8, m.p1Name);
                    stmt.setString(9, m.p2Name);
                    stmt.setString(10, m.p1Char);
                    stmt.setString(11, m.p2Char);
                    stmt.setInt(12, m.p1RoundsWon);
                    stmt.setInt(13, m.p2RoundsWon);
                    stmt.setInt(14, m.p1Score);
                    stmt.setInt(15, m.p2Score);
                    stmt.setInt(16, m.p1Rank);
                    stmt.setInt(17, m.p2Rank);
                    stmt.setInt(18, m.winner);
                    stmt.setLong(19, m.durationFrames);
                    stmt.setString(20, m.matchType);
                    stmt.setString(21, m.gameVersion);
                    stmt.setString(22, m.matchFlag);
                    stmt.setInt(23, m.ping);
                    stmt.setInt(24, m.optionsModified);
                    stmt.setInt(25, m.outdated ? 1 : 0);
                    stmt.setString(26, m.organizedPath);
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
            }
        }
    }

    public static void batchUpdateOpponents(Map<Long, String[]> opponents,
            Map<Long, Set<String>> newOpponents,
            Map<Long, String> lastSeenMap) throws Exception {
        try (var conn = connect()) {
            conn.setAutoCommit(false);

            for (Map.Entry<Long, Set<String>> entry : newOpponents.entrySet()) {
                long steamID = entry.getKey();
                Set<String> names = entry.getValue();
                String lastSeen = lastSeenMap.get(steamID);

                if (opponents.containsKey(steamID)) {
                    String[] existing = opponents.get(steamID);
                    String existingName = existing[0];
                    String existingAliases = existing[1];

                    List<String> newAliasList = new java.util.ArrayList<>();

                    for (String name : names) {
                        if (name.equals(existingName)) continue;
                        if (existingAliases.contains(name)) continue;
                        newAliasList.add(name);
                    }

                    if (!newAliasList.isEmpty()) {
                        String aliasesToAdd = String.join(" -|- ", newAliasList);
                        String newAliases = existingAliases.isEmpty()
                            ? aliasesToAdd
                            : existingAliases + " -|- " + aliasesToAdd;
                        existing[1] = newAliases;

                        try (var stmt = conn.prepareStatement(
                            "UPDATE opponents SET aliases = ?, lastSeen = ? WHERE steamID = ?")) {
                            stmt.setString(1, newAliases);
                            stmt.setString(2, lastSeen);
                            stmt.setLong(3, steamID);
                            stmt.executeUpdate();
                        }
                    }
                } else {
                    Iterator<String> it = names.iterator();
                    String primaryName = it.next();
                    StringBuilder aliases = new StringBuilder();
                    while (it.hasNext()) {
                        if (aliases.length() > 0) aliases.append(" -|- ");
                        aliases.append(it.next());
                    }

                    String aliasesStr = aliases.length() > 0 ? aliases.toString() : null;
                    opponents.put(steamID, new String[]{primaryName, aliasesStr != null ? aliasesStr : ""});

                    try (var stmt = conn.prepareStatement(
                        "INSERT INTO opponents (steamID, name, aliases, lastSeen) VALUES (?, ?, ?, ?)")) {
                        stmt.setLong(1, steamID);
                        stmt.setString(2, primaryName);
                        stmt.setString(3, aliasesStr);
                        stmt.setString(4, lastSeen);
                        stmt.executeUpdate();
                    }
                }
            }
            conn.commit();
        }
    }

    public static List<OpponentDisplay> searchOpponentNames(String partialName, int limit) throws Exception {
        List<OpponentDisplay> results = new java.util.ArrayList<>();
        String searchPattern = "%" + partialName.toLowerCase() + "%";

        try (var conn = connect();
             var stmt = conn.prepareStatement(
                "SELECT steamID, name, aliases FROM opponents WHERE LOWER(name) LIKE ? OR LOWER(aliases) LIKE ? LIMIT ?")) {
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setInt(3, limit);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String aliases = rs.getString("aliases");
                long steamID = rs.getLong("steamID");
                results.add(new OpponentDisplay(name, aliases, steamID));
            }
        }
        return results;
    }

    public static String[] getOpponentInfo(long steamID) throws Exception {
        try (var conn = connect();
             var stmt = conn.prepareStatement("SELECT name, aliases FROM opponents WHERE steamID = ?")) {
            stmt.setLong(1, steamID);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                String aliases = rs.getString("aliases");
                return new String[]{name, aliases != null ? aliases : ""};
            }
        }
        return null;
    }

    private static void appendDateFilter(StringBuilder sql, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) {
            sql.append(" AND date(printf('%04d-%02d-%02d', year, month, day)) >= date(?)");
        }
        if (toDate != null) {
            sql.append(" AND date(printf('%04d-%02d-%02d', year, month, day)) <= date(?)");
        }
    }

    private static int bindDateParams(java.sql.PreparedStatement stmt, int startIndex,
            LocalDate fromDate, LocalDate toDate) throws Exception {
        if (fromDate != null) {
            stmt.setString(startIndex++, fromDate.toString());
        }
        if (toDate != null) {
            stmt.setString(startIndex++, toDate.toString());
        }
        return startIndex;
    }

    private static MatchRecord readSimpleMatch(java.sql.ResultSet rs) throws Exception {
        MatchRecord m = new MatchRecord();
        m.p1SteamID = rs.getLong("p1SteamID");
        m.p2SteamID = rs.getLong("p2SteamID");
        m.p1Char = rs.getString("p1Char");
        m.p2Char = rs.getString("p2Char");
        m.p1RoundsWon = rs.getInt("p1RoundsWon");
        m.p2RoundsWon = rs.getInt("p2RoundsWon");
        return m;
    }

    private static MatchRecord readDetailedMatch(java.sql.ResultSet rs) throws Exception {
        MatchRecord m = readSimpleMatch(rs);
        m.year = rs.getInt("year");
        m.month = rs.getInt("month");
        m.day = rs.getInt("day");
        m.time = rs.getString("time");
        m.gameVersion = rs.getString("gameVersion");
        m.matchFlag = rs.getString("matchFlag");
        m.outdated = rs.getInt("outdated") == 1;
        m.winner = rs.getInt("winner");
        m.organizedPath = rs.getString("organizedPath");
        m.p1Name = rs.getString("p1Name");
        m.p2Name = rs.getString("p2Name");
        return m;
    }

    public static List<MatchRecord> getMatchesVsOpponent(long mainPlayerID, long opponentID,
            LocalDate fromDate, LocalDate toDate) throws Exception {
        List<MatchRecord> matches = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT p1SteamID, p2SteamID, p1Char, p2Char, p1RoundsWon, p2RoundsWon
            FROM matches
            WHERE ((p1SteamID = ? AND p2SteamID = ?) OR (p1SteamID = ? AND p2SteamID = ?))
            """);
        appendDateFilter(sql, fromDate, toDate);

        try (var conn = connect();
             var stmt = conn.prepareStatement(sql.toString())) {
            stmt.setLong(1, mainPlayerID);
            stmt.setLong(2, opponentID);
            stmt.setLong(3, opponentID);
            stmt.setLong(4, mainPlayerID);
            bindDateParams(stmt, 5, fromDate, toDate);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                matches.add(readSimpleMatch(rs));
            }
        }
        return matches;
    }

    public static List<MatchRecord> getAllMatchesForPlayer(long mainPlayerID,
            LocalDate fromDate, LocalDate toDate) throws Exception {
        List<MatchRecord> matches = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT p1SteamID, p2SteamID, p1Char, p2Char, p1RoundsWon, p2RoundsWon
            FROM matches
            WHERE (p1SteamID = ? OR p2SteamID = ?)
            """);
        appendDateFilter(sql, fromDate, toDate);

        try (var conn = connect();
             var stmt = conn.prepareStatement(sql.toString())) {
            stmt.setLong(1, mainPlayerID);
            stmt.setLong(2, mainPlayerID);
            bindDateParams(stmt, 3, fromDate, toDate);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                matches.add(readSimpleMatch(rs));
            }
        }
        return matches;
    }

    public static List<MatchRecord> getDetailedMatchesVsOpponent(long mainPlayerID, long opponentID,
            LocalDate fromDate, LocalDate toDate) throws Exception {
        List<MatchRecord> matches = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT p1SteamID, p2SteamID, p1Char, p2Char, p1RoundsWon, p2RoundsWon,
            year, month, day, time, gameVersion, matchFlag, outdated, winner, organizedPath, p1Name, p2Name
            FROM matches
            WHERE ((p1SteamID = ? AND p2SteamID = ?) OR (p1SteamID = ? AND p2SteamID = ?))
            """);
        appendDateFilter(sql, fromDate, toDate);
        sql.append(" ORDER BY year DESC, month DESC, day DESC, time DESC");

        try (var conn = connect();
             var stmt = conn.prepareStatement(sql.toString())) {
            stmt.setLong(1, mainPlayerID);
            stmt.setLong(2, opponentID);
            stmt.setLong(3, opponentID);
            stmt.setLong(4, mainPlayerID);
            bindDateParams(stmt, 5, fromDate, toDate);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                matches.add(readDetailedMatch(rs));
            }
        }
        return matches;
    }

    public static List<MatchRecord> getSpectatedMatches(long mainPlayerID,
            LocalDate fromDate, LocalDate toDate) throws Exception {
        List<MatchRecord> matches = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT p1SteamID, p2SteamID, p1Char, p2Char, p1RoundsWon, p2RoundsWon,
                   year, month, day, time, gameVersion, matchFlag, outdated, winner, organizedPath,
                   p1Name, p2Name
            FROM matches
            WHERE p1SteamID != ? AND p2SteamID != ?
            """);
        appendDateFilter(sql, fromDate, toDate);
        sql.append(" ORDER BY year DESC, month DESC, day DESC, time DESC");

        try (var conn = connect();
             var stmt = conn.prepareStatement(sql.toString())) {
            stmt.setLong(1, mainPlayerID);
            stmt.setLong(2, mainPlayerID);
            bindDateParams(stmt, 3, fromDate, toDate);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                matches.add(readDetailedMatch(rs));
            }
        }
        return matches;
    }

    public static List<Object[]> getOutdatedReplays() throws Exception {
        List<Object[]> replays = new java.util.ArrayList<>();
        try (var conn = connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT filename, organizedPath FROM matches WHERE outdated = 1")) {
            while (rs.next()) {
                replays.add(new Object[]{
                    rs.getString("filename"),
                    rs.getString("organizedPath")
                });
            }
        }
        return replays;
    }

    public static Map<Integer, int[]> getYearlyActivity(long mainPlayerID, int year) throws Exception {
        Map<Integer, int[]> activity = new HashMap<>();
        String sql = """
            SELECT month, day, COUNT(*) as total,
                   SUM(CASE WHEN (p1SteamID = ? AND p1RoundsWon > p2RoundsWon)
                             OR (p2SteamID = ? AND p2RoundsWon > p1RoundsWon) THEN 1 ELSE 0 END) as wins
            FROM matches
            WHERE (p1SteamID = ? OR p2SteamID = ?) AND year = ?
            GROUP BY month, day
            """;
        try (var conn = connect();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mainPlayerID);
            stmt.setLong(2, mainPlayerID);
            stmt.setLong(3, mainPlayerID);
            stmt.setLong(4, mainPlayerID);
            stmt.setInt(5, year);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                int month = rs.getInt("month");
                int day = rs.getInt("day");
                int total = rs.getInt("total");
                int wins = rs.getInt("wins");
                int dayOfYear = LocalDate.of(year, month, day).getDayOfYear();
                activity.put(dayOfYear, new int[]{total, wins});
            }
        }
        return activity;
    }

    public static void updateReplayMetadata(String filename,
            int p1RoundsWon, int p2RoundsWon,
            int p1Score, int p2Score,
            int p1Rank, int p2Rank,
            int winner, long durationFrames,
            String matchType, String gameVersion, String matchFlag,
            int ping, int optionsModified,
            boolean outdated) throws Exception {
        try (var conn = connect();
             var stmt = conn.prepareStatement("""
                UPDATE matches SET
                    p1RoundsWon = ?, p2RoundsWon = ?,
                    p1Score = ?, p2Score = ?,
                    p1Rank = ?, p2Rank = ?,
                    winner = ?, durationFrames = ?,
                    matchType = ?, gameVersion = ?, matchFlag = ?,
                    ping = ?, optionsModified = ?, outdated = ?
                WHERE filename = ?
                """)) {
            stmt.setInt(1, p1RoundsWon);
            stmt.setInt(2, p2RoundsWon);
            stmt.setInt(3, p1Score);
            stmt.setInt(4, p2Score);
            stmt.setInt(5, p1Rank);
            stmt.setInt(6, p2Rank);
            stmt.setInt(7, winner);
            stmt.setLong(8, durationFrames);
            stmt.setString(9, matchType);
            stmt.setString(10, gameVersion);
            stmt.setString(11, matchFlag);
            stmt.setInt(12, ping);
            stmt.setInt(13, optionsModified);
            stmt.setInt(14, outdated ? 1 : 0);
            stmt.setString(15, filename);
            stmt.executeUpdate();
        }
    }

    public static List<Object[]> getAllOrganizedReplays() throws Exception {
        List<Object[]> replays = new java.util.ArrayList<>();
        try (var conn = connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT filename, organizedPath FROM matches WHERE organizedPath IS NOT NULL")) {
            while (rs.next()) {
                replays.add(new Object[]{
                    rs.getString("filename"),
                    rs.getString("organizedPath")
                });
            }
        }
        return replays;
    }

    public static void batchUpdateOrganizedPaths(Map<String, String> pathUpdates) throws Exception {
        try (var conn = connect()) {
            conn.setAutoCommit(false);
            try (var stmt = conn.prepareStatement(
                    "UPDATE matches SET organizedPath = ? WHERE filename = ?")) {
                for (Map.Entry<String, String> entry : pathUpdates.entrySet()) {
                    stmt.setString(1, entry.getValue());
                    stmt.setString(2, entry.getKey());
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
            }
        }
    }

    public static List<MatchRecord> getDetailedMatchesForDate(long mainPlayerID, LocalDate date) throws Exception {
        List<MatchRecord> matches = new java.util.ArrayList<>();
        String sql = """
            SELECT p1SteamID, p2SteamID, p1Char, p2Char, p1RoundsWon, p2RoundsWon,
                   year, month, day, time, gameVersion, matchFlag, outdated, winner, organizedPath,
                   p1Name, p2Name
            FROM matches
            WHERE (p1SteamID = ? OR p2SteamID = ?)
              AND year = ? AND month = ? AND day = ?
            ORDER BY time DESC
            """;
        try (var conn = connect();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mainPlayerID);
            stmt.setLong(2, mainPlayerID);
            stmt.setInt(3, date.getYear());
            stmt.setInt(4, date.getMonthValue());
            stmt.setInt(5, date.getDayOfMonth());
            var rs = stmt.executeQuery();
            while (rs.next()) {
                matches.add(readDetailedMatch(rs));
            }
        }
        return matches;
    }
    
    public static List<MatchRecord> getDetailedMatchesForMatchup(long mainPlayerID, String mainChar, String oppChar,
            LocalDate fromDate, LocalDate toDate) throws Exception {
        List<MatchRecord> matches = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT p1SteamID, p2SteamID, p1Char, p2Char, p1RoundsWon, p2RoundsWon,
            year, month, day, time, gameVersion, matchFlag, outdated, winner, organizedPath, p1Name, p2Name
            FROM matches
            WHERE ((p1SteamID = ? AND p1Char = ? AND p2Char = ?)
               OR (p2SteamID = ? AND p2Char = ? AND p1Char = ?))
            """);
        appendDateFilter(sql, fromDate, toDate);
        sql.append(" ORDER BY year DESC, month DESC, day DESC, time DESC");

        try (var conn = connect();
             var stmt = conn.prepareStatement(sql.toString())) {
            stmt.setLong(1, mainPlayerID);
            stmt.setString(2, mainChar);
            stmt.setString(3, oppChar);
            stmt.setLong(4, mainPlayerID);
            stmt.setString(5, mainChar);
            stmt.setString(6, oppChar);
            bindDateParams(stmt, 7, fromDate, toDate);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                matches.add(readDetailedMatch(rs));
            }
        }
        return matches;
    }
    
    public static List<MatchRecord> getDetailedMatchesForPlayerChar(long mainPlayerID, String mainChar,
            LocalDate fromDate, LocalDate toDate) throws Exception {
        List<MatchRecord> matches = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT p1SteamID, p2SteamID, p1Char, p2Char, p1RoundsWon, p2RoundsWon,
            year, month, day, time, gameVersion, matchFlag, outdated, winner, organizedPath, p1Name, p2Name
            FROM matches
            WHERE ((p1SteamID = ? AND p1Char = ?) OR (p2SteamID = ? AND p2Char = ?))
            """);
        appendDateFilter(sql, fromDate, toDate);
        sql.append(" ORDER BY year DESC, month DESC, day DESC, time DESC");

        try (var conn = connect();
             var stmt = conn.prepareStatement(sql.toString())) {
            stmt.setLong(1, mainPlayerID);
            stmt.setString(2, mainChar);
            stmt.setLong(3, mainPlayerID);
            stmt.setString(4, mainChar);
            bindDateParams(stmt, 5, fromDate, toDate);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                matches.add(readDetailedMatch(rs));
            }
        }
        return matches;
    }
}