package application;

public class OpponentDisplay {
    public String name;
    public String aliases;
    public long steamID;
    
    public OpponentDisplay(String name, String aliases, long steamID) {
        this.name = name;
        this.aliases = aliases != null ? aliases : "";
        this.steamID = steamID;
    }
    
    @Override
    public String toString() {
        return name + " [" + steamID + "]";
    }
}
