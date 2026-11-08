package application;

public class ReplayDisplay {
    public String mainChar;
    public String oppChar;
    public String opponentName;
    public boolean mainWon;
    public String gameVersion;
    public String matchFlag;
    public boolean outdated;
    public String date;
    public String filePath;
    public String winnerName;
    public String p1Name;
    public String p2Name;

    public ReplayDisplay(
        String mainChar, String oppChar, String opponentName, boolean mainWon,
        String gameVersion, String matchFlag, boolean outdated,
        String date, String filePath, String winnerName,
        String p1Name, String p2Name) {

        this.mainChar = mainChar;
        this.oppChar = oppChar;
        this.opponentName = opponentName;
        this.mainWon = mainWon;
        this.gameVersion = gameVersion;
        this.matchFlag = matchFlag;
        this.outdated = outdated;
        this.date = date;
        this.filePath = filePath;
        this.winnerName = winnerName;
        this.p1Name = p1Name;
        this.p2Name = p2Name;
    }
}