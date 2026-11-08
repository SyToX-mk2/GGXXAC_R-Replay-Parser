package utils;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CharacterIconUtils {
    private static int iconSize = 16;
    private static final Map<String, Image> imageCache = new HashMap<>();

    public static void setIconSize(int size) {
        iconSize = size;
    }

    public static int getIconSize() {
        return iconSize;
    }

    public static ImageView createCharacterIcon(String charCode) {
        return createCharacterIcon(charCode, iconSize);
    }

    public static ImageView createCharacterIcon(String charCode, int size) {
        try {
            Image image = imageCache.computeIfAbsent(charCode,
                code -> new Image(CharacterIconUtils.class.getResource("/resources/" + code + ".png").toExternalForm()));
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(size);
            imageView.setFitWidth(size);
            return imageView;
        } catch (Exception e) {
            return new ImageView();
        }
    }
}