package application;

import controllers.SceneController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.WindowUtils;

public class Main extends Application {
	
	@Override
	public void start(Stage stage) throws Exception {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/controllers/Scene.fxml"));
			Parent root = loader.load();
			SceneController controller = loader.getController();
			Scene scene = new Scene(root, Color.GRAY);
			
			stage.setTitle("Replay CruncheR");
			Image image = new Image(getClass().getResource(WindowUtils.ICON_PATH).toExternalForm());
			stage.getIcons().add(image);
			stage.setResizable(true);
			double prefWidth = ((javafx.scene.layout.Region) root).getPrefWidth();
			double prefHeight = ((javafx.scene.layout.Region) root).getPrefHeight();
			stage.setMinWidth(prefWidth);
			stage.setMaxWidth(prefWidth);
			stage.setMinHeight(prefHeight + 40);
			stage.setScene(scene);
			
			ThemeManager.init(scene);
			stage.show();

			ConfigManager.loadConfig();
			String steamID = ConfigManager.get("mainSteamID", "");
			String rootFolder = ConfigManager.get("rootFolder", "");
	        
			if (steamID.isEmpty() || rootFolder.isEmpty()) {
			    FXMLLoader configLoader = new FXMLLoader(getClass().getResource("/controllers/ConfigScene.fxml"));
			    Parent configRoot = configLoader.load();
			    Scene configScene = new Scene(configRoot);
			    ThemeManager.registerScene(configScene);
			    
			    Stage configStage = new Stage();
			    configStage.initModality(Modality.APPLICATION_MODAL);
			    configStage.initOwner(stage);
			    configStage.setTitle("Setup");
				configStage.getIcons().add(image);
			    configStage.setScene(configScene);
			    configStage.showAndWait();
			}
			controller.refresh();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	

	public static void main(String[] args) {
    	launch(args);
    }
}