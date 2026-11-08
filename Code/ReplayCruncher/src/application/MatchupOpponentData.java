package application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MatchupOpponentData {
    private final SimpleStringProperty playerName;
    private final SimpleIntegerProperty wins;
    private final SimpleIntegerProperty total;
    private final SimpleStringProperty winrate;
    private final SimpleBooleanProperty include;
    public final long steamID;

    public MatchupOpponentData(String playerName, int wins, int total, long steamID) {
        this.playerName = new SimpleStringProperty(playerName);
        this.wins = new SimpleIntegerProperty(wins);
        this.total = new SimpleIntegerProperty(total);
        double winratePercent = total > 0 ? (wins * 100.0 / total) : 0;
        this.winrate = new SimpleStringProperty(String.format("%.0f%%", winratePercent));
        this.include = new SimpleBooleanProperty(true);
        this.steamID = steamID;
    }

    public StringProperty playerNameProperty() { return playerName; }
    public IntegerProperty winsProperty() { return wins; }
    public IntegerProperty totalProperty() { return total; }
    public StringProperty winrateProperty() { return winrate; }
    public BooleanProperty includeProperty() { return include; }

    public String getPlayerName() { return playerName.get(); }
    public int getWins() { return wins.get(); }
    public int getTotal() { return total.get(); }
    public String getWinrate() { return winrate.get(); }
    public boolean getInclude() { return include.get(); }
}