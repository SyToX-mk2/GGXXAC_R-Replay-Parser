package application;

public class ReplayMetadata {
    public String filename;
    public String signature;
    public int metadataSize;
    public int year, month, day, hour, minute, second;
    public long p1SteamID, p2SteamID;
    public String p1Name, p2Name;
    public int p1Character, p2Character;
    public int optionsModified;
    public int matchType;
    public int gameVersion;
    public int timezoneBias;
    public int p1RoundsWon, p2RoundsWon;
    public int matchFlags;
    public int ping;
    public long durationFrames;
    public int p1Score, p2Score;
    public int p1Rank, p2Rank;
    public int winnerSide;
    public boolean isOutdated;
}