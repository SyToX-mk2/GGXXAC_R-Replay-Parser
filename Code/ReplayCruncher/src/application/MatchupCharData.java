package application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MatchupCharData {
    private final SimpleStringProperty playerName;
    private final SimpleStringProperty oppChar;
    private final SimpleIntegerProperty wins;
    private final SimpleIntegerProperty total;
    private final SimpleStringProperty winrate;
    public final long steamID;

    public MatchupCharData(String playerName, String oppChar, int wins, int total, long steamID) {
        this.playerName = new SimpleStringProperty(playerName);
        this.oppChar = new SimpleStringProperty(oppChar);
        this.wins = new SimpleIntegerProperty(wins);
        this.total = new SimpleIntegerProperty(total);
        double winratePercent = total > 0 ? (wins * 100.0 / total) : 0;
        this.winrate = new SimpleStringProperty(String.format("%.0f%%", winratePercent));
        this.steamID = steamID;
    }

    public StringProperty playerNameProperty() { return playerName; }
    public StringProperty oppCharProperty() { return oppChar; }
    public IntegerProperty winsProperty() { return wins; }
    public IntegerProperty totalProperty() { return total; }
    public StringProperty winrateProperty() { return winrate; }

    public String getPlayerName() { return playerName.get(); }
    public String getOppChar() { return oppChar.get(); }
    public int getWins() { return wins.get(); }
    public int getTotal() { return total.get(); }
    public String getWinrate() { return winrate.get(); }
}