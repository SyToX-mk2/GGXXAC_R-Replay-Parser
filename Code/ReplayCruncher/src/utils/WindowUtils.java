package utils;

import java.util.function.Consumer;

import application.ThemeManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class WindowUtils {
    public static final String ICON_PATH = "/resources/icon.png";

    public static Stage openModal(String fxml, String title, boolean resizable,
            Consumer<Object> configurer, boolean sizeToScene) throws Exception {
        return openModal(fxml, title, resizable, null, configurer, sizeToScene);
    }

    public static Stage openModal(String fxml, String title, boolean resizable,
            Stage owner, Consumer<Object> configurer, boolean sizeToScene) throws Exception {
        FXMLLoader loader = new FXMLLoader(WindowUtils.class.getResource("/controllers/" + fxml));
        Parent root = loader.load();
        if (configurer != null) configurer.accept(loader.getController());
        Scene scene = new Scene(root);
        ThemeManager.registerScene(scene);
        Stage stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setResizable(resizable);
        if (sizeToScene) stage.sizeToScene();
        stage.getIcons().add(new Image(WindowUtils.class.getResource(ICON_PATH).toExternalForm()));
        stage.showAndWait();
        return stage;
    }
}