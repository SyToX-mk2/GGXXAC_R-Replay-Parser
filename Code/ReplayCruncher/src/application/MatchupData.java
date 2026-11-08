package application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MatchupData {
    private final SimpleStringProperty mainChar;
    private final SimpleStringProperty oppChar;
    private final SimpleIntegerProperty wins;
    private final SimpleIntegerProperty total;
    private final SimpleStringProperty winrate;
    
    public MatchupData(String mainChar, String oppChar, int wins, int total) {
        this.mainChar = new SimpleStringProperty(mainChar);
        this.oppChar = new SimpleStringProperty(oppChar);
        this.wins = new SimpleIntegerProperty(wins);
        this.total = new SimpleIntegerProperty(total);
        double winratePercent = total > 0 ? (wins * 100.0 / total) : 0;
        this.winrate = new SimpleStringProperty(String.format("%.0f%%", winratePercent));
    }
    
    public StringProperty mainCharProperty() { return mainChar; }
    public StringProperty oppCharProperty() { return oppChar; }
    public IntegerProperty winsProperty() { return wins; }
    public IntegerProperty totalProperty() { return total; }
    public StringProperty winrateProperty() { return winrate; }
    
    // Keep regular getters for convenience
    public String getMainChar() { return mainChar.get(); }
    public String getOppChar() { return oppChar.get(); }
    public int getWins() { return wins.get(); }
    public int getTotal() { return total.get(); }
    public String getWinrate() { return winrate.get(); }
}
