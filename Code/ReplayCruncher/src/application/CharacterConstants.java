package application;

public final class CharacterConstants {
    public static final String[] CHARACTERS = {
        "SO", "KY", "MA", "MI", "AX",
        "PO", "CH", "ED", "BA", "FA",
        "TE", "JA", "AN", "JO", "VE",
        "DI", "SL", "IN", "ZA", "BR",
        "RO", "AB", "OS", "KL", "JU"
    };

    private CharacterConstants() {}

    public static String getCharacterName(int charIndex) {
        if (charIndex >= 1 && charIndex <= CHARACTERS.length) {
            return CHARACTERS[charIndex - 1];
        }
        return "UNK";
    }
}