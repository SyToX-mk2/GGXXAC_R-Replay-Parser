package controllers;

import java.io.File;

import application.ConfigManager;
import application.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class ConfigSceneController {

    @FXML
    private TextField txtMainSteamID;
    @FXML
    private TextField txtRootFolder;
    @FXML
    private Button btnSaveConfig;

    @FXML
    public void initialize() {
        txtMainSteamID.setText(ConfigManager.get("mainSteamID", ""));
        txtRootFolder.setText(ConfigManager.get("rootFolder", ""));
        
        txtMainSteamID.setTextFormatter(new TextFormatter<>(change ->
        change.getControlNewText().matches("\\d*") ? change : null));
        
        // Add listeners to validate fields
        txtMainSteamID.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
        txtRootFolder.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
        
        // Initial validation
        validateFields();
    }
    
    private void validateFields() {
        boolean isValid = !txtMainSteamID.getText().trim().isEmpty() 
                       && !txtRootFolder.getText().trim().isEmpty();
        btnSaveConfig.setDisable(!isValid);
    }
    
    @FXML
    private void handleBrowseFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Replay Folder");
        
        File initialDir = new File(txtRootFolder.getText());
        if (initialDir.exists()) {
            directoryChooser.setInitialDirectory(initialDir);
        }
        
        File selectedDir = directoryChooser.showDialog(txtRootFolder.getScene().getWindow());
        if (selectedDir != null) {
            txtRootFolder.setText(selectedDir.getAbsolutePath());
        }
    }

    @FXML
    private void handleSave() {
        String oldSteamID = ConfigManager.get("mainSteamID", "");
        String newSteamID = txtMainSteamID.getText().trim();
        
        ConfigManager.set("mainSteamID", newSteamID);
        ConfigManager.set("rootFolder", txtRootFolder.getText().trim());
        ConfigManager.saveConfig();
        
        try {
        	DatabaseManager.initializeDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
        Stage stage = (Stage) txtMainSteamID.getScene().getWindow();
        stage.close();
    }

    
    @FXML
    private void handleCancelSetup() {
        Stage stage = (Stage) txtMainSteamID.getScene().getWindow();
        stage.close();
    }
}
