package application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javafx.application.Platform;

public class ReplayOrganizerService {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ReplayOrganizer");
        t.setDaemon(true);
        return t;
    });

    private static Future<?> currentTask = null;

    public interface ProgressCallback {
        void update(int current, int total, String message);
        void complete(String message);
        void error(String message);
    }

    public static synchronized void cancel() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    public static synchronized boolean isRunning() {
        return currentTask != null && !currentTask.isDone();
    }

    public static synchronized void organizeReplays(long mainPlayerID, ProgressCallback callback) {
        if (isRunning()) return;

        currentTask = executor.submit(() -> {
            try {
                doOrganize(mainPlayerID, callback);
            } catch (Exception e) {
                Platform.runLater(() -> callback.error("Failed to organize replays: " + e.getMessage()));
            }
        });
    }

    public static synchronized void restoreReplays(ProgressCallback callback) {
        if (isRunning()) return;

        currentTask = executor.submit(() -> {
            try {
                doRestore(callback);
            } catch (Exception e) {
                Platform.runLater(() -> callback.error("Failed to restore replays: " + e.getMessage()));
            }
        });
    }
    
    

    private static void doOrganize(long mainPlayerID, ProgressCallback callback) throws Exception {
        Path sourceDir = Path.of(ConfigManager.get("rootFolder", ""));

        Set<String> existingFilenames = DatabaseManager.loadAllFilenames();
        Map<Long, String[]> existingOpponents = DatabaseManager.loadAllOpponents();

        List<MatchRecord> matchBatch = Collections.synchronizedList(new ArrayList<>());
        Map<Long, Set<String>> newOpponents = new ConcurrentHashMap<>();
        Map<Long, String> lastSeenMap = new ConcurrentHashMap<>();
        Map<Long, String> canonicalOpponentName = new ConcurrentHashMap<>();

        List<Path> replayFiles = Files.list(sourceDir)
            .filter(p -> p.toString().endsWith(".ggr"))
            .collect(Collectors.toList());

        int totalFiles = replayFiles.size();
        AtomicInteger processed = new AtomicInteger(0);

        Platform.runLater(() -> callback.update(0, totalFiles, "Organizing: 0/" + totalFiles));

        for (Path replayPath : replayFiles) {
            if (Thread.currentThread().isInterrupted()) return;

            try {
                String filename = replayPath.getFileName().toString();

                int current = processed.incrementAndGet();
                Platform.runLater(() -> callback.update(current, totalFiles, "Organizing: " + current + "/" + totalFiles));

                if (existingFilenames.contains(filename)) {
                    continue;
                }

                ReplayMetadata meta = ReplayMetadataParser.parse(replayPath.toString());
                
                if (meta.p2SteamID == 0) {
                    Path vs2pDir = sourceDir.resolve("VS 2P");
                    Files.createDirectories(vs2pDir);
                    String name = filename.substring(0, filename.lastIndexOf('.'));
                    String ext = filename.substring(filename.lastIndexOf('.'));
                    String sanitizedFilename = sanitizeFileName(name) + ext;
                    Path destFile = getUniquePath(vs2pDir, sanitizedFilename);
                    Files.move(replayPath, destFile, StandardCopyOption.REPLACE_EXISTING);
                    continue;
                }
                
                String rawFilename = buildRawFilename(filename, meta);
                if (!rawFilename.equals(filename) && existingFilenames.contains(rawFilename)) {
                    continue;
                }

                Path destDir;
                long opponentSteamId = 0;
                String opponentName = null;

                if (meta.p1SteamID == mainPlayerID) {
                    String mainChar = CharacterConstants.getCharacterName(meta.p1Character);
                    String oppChar = CharacterConstants.getCharacterName(meta.p2Character);
                    opponentSteamId = meta.p2SteamID;
                    opponentName = meta.p2Name;
                    String canonical = resolveCanonicalName(opponentSteamId, opponentName, existingOpponents, canonicalOpponentName);
                    destDir = sourceDir.resolve("As " + mainChar)
                                      .resolve("vs " + oppChar)
                                      .resolve(sanitizeFileName(canonical));
                } else if (meta.p2SteamID == mainPlayerID) {
                    String mainChar = CharacterConstants.getCharacterName(meta.p2Character);
                    String oppChar = CharacterConstants.getCharacterName(meta.p1Character);
                    opponentSteamId = meta.p1SteamID;
                    opponentName = meta.p1Name;
                    String canonical = resolveCanonicalName(opponentSteamId, opponentName, existingOpponents, canonicalOpponentName);
                    destDir = sourceDir.resolve("As " + mainChar)
                                      .resolve("vs " + oppChar)
                                      .resolve(sanitizeFileName(canonical));
                } else {
                    destDir = sourceDir.resolve("Spectated Matches");
                }

                Files.createDirectories(destDir);

                String name = filename.substring(0, filename.lastIndexOf('.'));
                String ext = filename.substring(filename.lastIndexOf('.'));
                String sanitizedFilename = sanitizeFileName(name) + ext;

                Path destFile = getUniquePath(destDir, sanitizedFilename);
                Files.move(replayPath, destFile, StandardCopyOption.REPLACE_EXISTING);

                String organizedPath = destFile.toString();

                MatchRecord mr = new MatchRecord();
                mr.filename = rawFilename;
                mr.year = meta.year;
                mr.month = meta.month;
                mr.day = meta.day;
                mr.time = String.format("%02d:%02d:%02d", meta.hour, meta.minute, meta.second);
                mr.p1SteamID = meta.p1SteamID;
                mr.p2SteamID = meta.p2SteamID;
                mr.p1Name = meta.p1Name;
                mr.p2Name = meta.p2Name;
                mr.p1Char = CharacterConstants.getCharacterName(meta.p1Character);
                mr.p2Char = CharacterConstants.getCharacterName(meta.p2Character);
                mr.p1RoundsWon = meta.p1RoundsWon;
                mr.p2RoundsWon = meta.p2RoundsWon;
                mr.p1Score = meta.p1Score;
                mr.p2Score = meta.p2Score;
                mr.p1Rank = meta.p1Rank;
                mr.p2Rank = meta.p2Rank;
                mr.winner = meta.winnerSide;
                mr.durationFrames = meta.durationFrames;
                mr.matchType = ReplayMetadataParser.getMatchType(meta.matchType);
                mr.gameVersion = ReplayMetadataParser.getGameVersion(meta.gameVersion);
                mr.matchFlag = ReplayMetadataParser.getMatchFlag(meta.matchFlags);
                mr.ping = meta.ping;
                mr.optionsModified = meta.optionsModified;
                mr.outdated = meta.isOutdated;
                mr.organizedPath = organizedPath;

                matchBatch.add(mr);

                if (opponentSteamId != 0 && opponentName != null) {
                    String timestamp = String.format("%04d-%02d-%02d %02d:%02d:%02d",
                        meta.year, meta.month, meta.day, meta.hour, meta.minute, meta.second);
                    newOpponents.computeIfAbsent(opponentSteamId, k -> ConcurrentHashMap.newKeySet())
                                .add(opponentName);
                    lastSeenMap.put(opponentSteamId, timestamp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!matchBatch.isEmpty()) {
            DatabaseManager.batchInsertMatches(matchBatch);
            DatabaseManager.batchUpdateOpponents(existingOpponents, newOpponents, lastSeenMap);
        }

        Platform.runLater(() -> callback.complete(null));
    }

    private static void doRestore(ProgressCallback callback) throws Exception {
        Path rootDir = Path.of(ConfigManager.get("rootFolder", ""));
        if (!Files.exists(rootDir)) {
            Platform.runLater(() -> callback.error("Root folder does not exist"));
            return;
        }

        List<Object[]> replays = DatabaseManager.getAllOrganizedReplays();
        if (replays.isEmpty()) {
            Platform.runLater(() -> callback.complete("No organized replays found to restore."));
            return;
        }

        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger restored = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        Map<String, String> pathUpdates = new HashMap<>();

        int totalFiles = replays.size();

        Platform.runLater(() -> callback.update(0, totalFiles, "Restoring: 0/" + totalFiles));

        for (Object[] replay : replays) {
            if (Thread.currentThread().isInterrupted()) return;

            String filename = (String) replay[0];
            String organizedPath = (String) replay[1];

            try {
                Path sourcePath = Path.of(organizedPath);

                if (!Files.exists(sourcePath)) {
                    skipped.incrementAndGet();
                    continue;
                }

                if (sourcePath.getParent().equals(rootDir)) {
                    skipped.incrementAndGet();
                    continue;
                }

                Path destPath = getUniquePath(rootDir, sanitizeForFilesystem(filename));
                Files.move(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);

                pathUpdates.put(filename, destPath.toString());
                restored.incrementAndGet();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                int current = processed.incrementAndGet();
                Platform.runLater(() -> callback.update(current, totalFiles, "Restoring: " + current + "/" + totalFiles));
            }
        }

        if (!pathUpdates.isEmpty()) {
            DatabaseManager.batchUpdateOrganizedPaths(pathUpdates);
        }

        cleanupEmptyDirectories(rootDir);

        String summary = "Restored: " + restored.get() + "\nSkipped: " + skipped.get();
        Platform.runLater(() -> callback.complete(summary));
    }

    static String resolveCanonicalName(long steamId, String replayName,
            Map<Long, String[]> existingOpponents, Map<Long, String> newOpponentNames) {
        String[] existing = existingOpponents.get(steamId);
        if (existing != null) {
            return existing[0];
        }
        return newOpponentNames.computeIfAbsent(steamId, k -> replayName);
    }

    static String sanitizeFileName(String name) {
        if (name == null) return "Unknown";
        String sanitized = name.trim();
        sanitized = sanitized.replaceAll("[<>:\"/\\\\|?*]", "_");
        sanitized = sanitized.replaceAll("[\\x00-\\x1f]", "_");
        sanitized = sanitized.replaceAll("\\.{2,}", "_");
        sanitized = sanitized.replaceAll("\\.$", "");
        sanitized = sanitized.replaceAll("^\\.+$", "_");
        sanitized = sanitized.replaceAll("\u2122", "(tm)");
        sanitized = sanitized.replaceAll("ฅᐠ｡ᆽ｡ᐟ", "");
        sanitized = sanitized.replaceAll("\uD83C\uDFF3\uFE0F?\u200D\uD83C\uDF08", "");
        sanitized = sanitized.replaceAll("[\uD83C\uDF00-\uD83E\uDDFF]", "");
        sanitized = sanitized.trim();
        return sanitized.isEmpty() ? "Unknown" : sanitized;
    }
    
    static String sanitizeForFilesystem(String name) {
        return name.replaceAll("[<>:\"/\\\\|?*]", "_");
    }
    
    static String buildRawFilename(String filename, ReplayMetadata meta) {
        String raw = filename;
        String p1CharName = CharacterConstants.getCharacterName(meta.p1Character);
        String p2CharName = CharacterConstants.getCharacterName(meta.p2Character);
        if (meta.p1Name != null) {
            String sanitized = sanitizeFileName(meta.p1Name) + "(" + p1CharName + ")";
            String rawSegment = meta.p1Name + "(" + p1CharName + ")";
            raw = raw.replace(sanitized, rawSegment);
        }
        if (meta.p2Name != null) {
            String sanitized = sanitizeFileName(meta.p2Name) + "(" + p2CharName + ")";
            String rawSegment = meta.p2Name + "(" + p2CharName + ")";
            raw = raw.replace(sanitized, rawSegment);
        }
        return raw;
    }

    static Path getUniquePath(Path dir, String fileName) {
        Path path = dir.resolve(fileName);
        if (!Files.exists(path)) return path;

        String name = fileName.substring(0, fileName.lastIndexOf('.'));
        String ext = fileName.substring(fileName.lastIndexOf('.'));
        int count = 1;

        while (Files.exists(path)) {
            path = dir.resolve(name + " (" + count + ")" + ext);
            count++;
        }
        return path;
    }

    private static void cleanupEmptyDirectories(Path rootDir) {
        try {
            Files.walk(rootDir)
                .filter(Files::isDirectory)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(dir -> {
                    if (!dir.equals(rootDir)) {
                        try (var stream = Files.list(dir)) {
                            if (stream.findFirst().isEmpty()) {
                                Files.delete(dir);
                            }
                        } catch (Exception e) {
                        }
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}