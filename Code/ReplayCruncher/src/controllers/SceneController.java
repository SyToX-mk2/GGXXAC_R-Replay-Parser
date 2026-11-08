package controllers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import application.ConfigManager;
import application.DatabaseManager;
import application.MatchRecord;
import application.MatchupData;
import application.MatchupOpponentData;
import application.OpponentDisplay;
import application.ReplayDisplay;
import application.ReplayMetadata;
import application.ReplayMetadataParser;
import application.ReplayOrganizerService;
import application.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils.CharacterIconUtils;
import utils.WindowUtils;


public class SceneController {
	
    @FXML
    public void initialize() throws Exception {
        ConfigManager.loadConfig();

        String savedTheme = ConfigManager.get("theme", "dark");
        if ("dark".equals(savedTheme)) {
            menuThemeDark.setSelected(true);
        } else {
            menuThemeLight.setSelected(true);
        }

        String savedIconSize = ConfigManager.get("iconSize", "large");
        switch (savedIconSize) {
            case "medium" -> {
                CharacterIconUtils.setIconSize(32);
                menuIconMedium.setSelected(true);
            }
            case "large" -> {
                CharacterIconUtils.setIconSize(48);
                menuIconLarge.setSelected(true);
            }
            default -> {
                CharacterIconUtils.setIconSize(16);
                menuIconSmall.setSelected(true);
            }
        }

        setupPlayerSearch();
        setupMatchupTable();
        setupDateFilters();
        updateFieldStates();
        if (!ConfigManager.get("mainSteamID", "").isEmpty()) {
            loadAllMatchups();
        }
        Platform.runLater(() -> txtPlayerSearch.requestFocus());
        
        colWinrate.setComparator((a, b) -> Double.compare(
        	    Double.parseDouble(a.replace("%", "")),
        	    Double.parseDouble(b.replace("%", ""))));
    }

    private String lastSearchedTerm = "";
    private javafx.animation.Timeline searchTimeline;
    private OpponentDisplay selectedOpponent = null;
    private boolean suppressMatchLoadErrors = false;
    private boolean hasMatchData = false;

    @FXML
    private Button btnParseReplays, btnFindPlayer, btnOrganizeReplays, btnGenerateSteamIDs, btnReplayList, btnSpectatedMatches, btnActivityCalendar, btnParseReplay, btnAllPlayerMatchups;
    @FXML
    private TextField txtPlayerSearch;
    @FXML
    private ListView<OpponentDisplay> lstPlayerResults;
    @FXML
    private PieChart pieWinLoss;
    @FXML
    private DatePicker dpFromDate, dpToDate;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label labelReplays;
    @FXML
    private RadioMenuItem menuThemeLight, menuThemeDark, menuIconSmall, menuIconMedium, menuIconLarge;
    @FXML
    private ToggleGroup themeToggleGroup, iconToggleGroup;

/*  @FXML //UNUSED and unfinished anti-cheat feature, extracts all player inputs from a match.
    private void handleParseReplay() {
        try {
           String path = "";
            GGRReplayParser.ReplayData replay = GGRReplayParser.parse(path);
            System.out.println("Parsed replay: " + replay.metadata.p1Name + " vs " + replay.metadata.p2Name);
            System.out.println("Frames: " + replay.frameCount + ", Winner: P" + replay.metadata.winnerSide);
            GGRReplayParser.printAllInputs(replay);
            GGRReplayParser.writeInputsToFile(replay, path.replace(".ggr", "_inputs.txt"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
*/
    @FXML
    private void handleTheme() {
        Toggle selected = themeToggleGroup.getSelectedToggle();
        ThemeManager.setTheme((String) selected.getUserData());
    }

    @FXML
    private void handleIconSize() {
        Toggle selected = iconToggleGroup.getSelectedToggle();
        if (selected == null) return;
        String[] parts = ((String) selected.getUserData()).split(":");
        int size = Integer.parseInt(parts[0]);
        String name = parts[1];
        CharacterIconUtils.setIconSize(size);
        ConfigManager.set("iconSize", name);
        ConfigManager.saveConfig();
        refreshTableView();
    }

    private void refreshTableView() {
        if (selectedOpponent != null) {
            try {
                handleFindPlayer();
            } catch (Exception e) {
                showError("Failed to refresh: " + e.getMessage());
            }
        } else {
            loadAllMatchups();
        }
    }

    @FXML
    private void handleSetup() throws Exception {
        String oldSteamID = ConfigManager.get("mainSteamID", "");
        WindowUtils.openModal("ConfigScene.fxml", "Setup", false, null, false);
        String newSteamID = ConfigManager.get("mainSteamID", "");
        if (!oldSteamID.equals(newSteamID)) {
            suppressMatchLoadErrors = true;
            try {
                resetPlayerView();
                loadAllMatchups();
            } finally {
                suppressMatchLoadErrors = false;
            }
        }
        updateFieldStates();
    }

    private void resetPlayerView() {
        if (searchTimeline != null) {
            searchTimeline.stop();
        }
        selectedOpponent = null;
        lastSearchedTerm = "";
        lstPlayerResults.getItems().clear();
        txtPlayerSearch.setText("");
        clearWinLossChart();
    }

    private long getMainPlayerID() {
        String text = ConfigManager.get("mainSteamID", "");
        if (text.isEmpty()) {
            showError("Please set your SteamID in Setup");
            return 0;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            showError("Invalid SteamID format. Must be a number.");
            return 0;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateFieldStates() {
        String steamID = ConfigManager.get("mainSteamID", "");
        boolean hasSteamID = !steamID.trim().isEmpty();
        boolean hasData = hasSteamID && DatabaseManager.hasAnyMatches();
        hasMatchData = hasData;

        btnFindPlayer.setDisable(!hasData || txtPlayerSearch.getText().trim().isEmpty());
        btnOrganizeReplays.setDisable(!hasSteamID);
        btnGenerateSteamIDs.setDisable(!hasSteamID);
        btnReplayList.setDisable(!hasData || selectedOpponent == null);
        txtPlayerSearch.setDisable(!hasData);
        btnSpectatedMatches.setDisable(!hasData);
        dpToDate.setDisable(!hasData);
        dpFromDate.setDisable(!hasData);
        btnAllPlayerMatchups.setDisable(!hasData);
        btnActivityCalendar.setDisable(!hasData);

        if (btnParseReplays != null) {
            btnParseReplays.setDisable(!hasSteamID);
        }
    }

    public void refresh() {
        updateFieldStates();
        if (!ConfigManager.get("mainSteamID", "").isEmpty()) {
            loadAllMatchups();
        }
    }

    private void setupDateFilters() {
        Runnable onDateChanged = () -> {
            try {
                if (selectedOpponent != null) {
                    handleFindPlayer();
                } else {
                    loadAllMatchups();
                }
            } catch (Exception e) {
                showError("Failed to apply date filter: " + e.getMessage());
            }
        };
        dpFromDate.valueProperty().addListener((obs, oldVal, newVal) -> onDateChanged.run());
        dpToDate.valueProperty().addListener((obs, oldVal, newVal) -> onDateChanged.run());
    }

    @FXML
    private void handleClearDateFilter() {
        dpFromDate.setValue(null);
        dpToDate.setValue(null);
    }

    private void updateWinLossChart(int wins, int losses) {
        int total = wins + losses;
        if (total == 0) {
            clearWinLossChart();
            return;
        }

        ObservableList<PieChart.Data> pieChartData = javafx.collections.FXCollections.observableArrayList(
            new PieChart.Data("Wins", wins),
            new PieChart.Data("Losses", losses)
        );

        pieWinLoss.setData(pieChartData);

        String[] styleClasses = {"pie-slice-win", "pie-slice-loss"};
        String[] labels = {
            "Wins: " + wins + " (" + String.format("%.2f", (double) wins / total * 100) + "%)",
            "Losses: " + losses + " (" + String.format("%.2f", (double) losses / total * 100) + "%)"
        };

        for (int i = 0; i < pieWinLoss.getData().size(); i++) {
            PieChart.Data slice = pieWinLoss.getData().get(i);
            slice.getNode().getStyleClass().add(styleClasses[i]);

            Tooltip tooltip = new Tooltip(labels[i]);
            Tooltip.install(slice.getNode(), tooltip);
            tooltip.setShowDelay(Duration.ZERO);
            tooltip.setHideDelay(Duration.ZERO);

            slice.getNode().setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(200), slice.getNode());
                st.setToX(1.1);
                st.setToY(1.1);
                st.play();
                slice.getNode().setEffect(new javafx.scene.effect.DropShadow(10, javafx.scene.paint.Color.GRAY));
            });

            slice.getNode().setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(200), slice.getNode());
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
                slice.getNode().setEffect(null);
            });
        }
    }

    private void clearWinLossChart() {
        pieWinLoss.setData(javafx.collections.FXCollections.observableArrayList());
        tblMatchups.getItems().clear();
    }

    private void setupPlayerSearch() {
    	lstPlayerResults.setFixedCellSize(28);
        lstPlayerResults.setCellFactory(listView -> new javafx.scene.control.ListCell<OpponentDisplay>() {
            @Override
            protected void updateItem(OpponentDisplay item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    TextFlow flow = new TextFlow();
                    flow.setMaxWidth(Control.USE_COMPUTED_SIZE);
                    flow.setMinWidth(0);

                    Text nameText = new Text(item.name);
                    nameText.getStyleClass().add("player-name");
                    flow.getChildren().add(nameText);

                    if (item.aliases != null && !item.aliases.isEmpty()) {
                        Text aliasesText = new Text(" (" + item.aliases + ")");
                        aliasesText.getStyleClass().add("player-aliases");
                        flow.getChildren().add(aliasesText);
                    }

                    Text steamIdText = new Text(" [" + item.steamID + "]");
                    steamIdText.getStyleClass().add("player-steamid");
                    flow.getChildren().add(steamIdText);

                    setGraphic(flow);
                    setText(null);
                }
            }
        });

        Button btnClearSearch = new Button("x");
        btnClearSearch.setStyle("-fx-background-color: transparent; -fx-text-fill: gray; -fx-font-size: 12px; -fx-padding: 0 0 0 0; -fx-cursor: hand;");
        btnClearSearch.setVisible(false);
        HBox.setMargin(btnClearSearch, new Insets(2, 0, 0, 0));

        HBox parent = (HBox) txtPlayerSearch.getParent();
        parent.getChildren().add(btnClearSearch);

        btnClearSearch.visibleProperty().bind(txtPlayerSearch.textProperty().isEmpty().not());
        btnClearSearch.setOnAction(e -> {txtPlayerSearch.clear();
        								 txtPlayerSearch.requestFocus();
        });

        txtPlayerSearch.setOnAction(e -> {
            if (!lstPlayerResults.getItems().isEmpty()) {
                selectedOpponent = lstPlayerResults.getItems().get(0);
                lstPlayerResults.getSelectionModel().select(0);
                try {
                    handleFindPlayer();
                } catch (Exception ex) {
                    showError("Search failed: " + ex.getMessage());
                }
            }
        	
        });

        lstPlayerResults.setOnMouseClicked(event -> {
            OpponentDisplay selected = lstPlayerResults.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedOpponent = selected;
                btnReplayList.setDisable(false);
                try {
                    handleFindPlayer();
                } catch (Exception e) {
                    showError("Failed to load opponent data: " + e.getMessage());
                }
            }
        });

        txtPlayerSearch.textProperty().addListener((observable, oldValue, newValue) -> {
        	btnFindPlayer.setDisable(!hasMatchData || newValue == null || newValue.trim().isEmpty());
        	
            if (searchTimeline != null) {
                searchTimeline.stop();
            }

            if (newValue == null || newValue.trim().isEmpty()) {
                lstPlayerResults.getItems().clear();
                lastSearchedTerm = "";
                selectedOpponent = null;
                btnReplayList.setDisable(true);
                loadAllMatchups();
                return;
            }

            String trimmedNewValue = newValue.trim();

            if (trimmedNewValue.equalsIgnoreCase(lastSearchedTerm)) {
                return;
            }
            
            if (selectedOpponent != null && !selectedOpponent.name.equalsIgnoreCase(newValue)) {
                selectedOpponent = null;
            }

            lastSearchedTerm = trimmedNewValue;

            searchTimeline = new javafx.animation.Timeline(
                new KeyFrame(Duration.millis(150),
                    event -> {
                        if (trimmedNewValue.length() >= 1) {
                            try {
                            	List<OpponentDisplay> matches = DatabaseManager.searchOpponentNames(trimmedNewValue, 10);
                            	lstPlayerResults.getItems().setAll(matches);
                            	if (!matches.isEmpty()) {
                            	    lstPlayerResults.getSelectionModel().select(0);
                            	    selectedOpponent = matches.get(0);
                            	    btnReplayList.setDisable(false);
                            	    handleFindPlayer();
                            	} else {
                            	    selectedOpponent = null;
                            	    btnReplayList.setDisable(true);
                            	    clearWinLossChart();
                            	}
                            } catch (Exception e) {
                                showError("Search failed: " + e.getMessage());
                            }
                        }
                    }
                )
            );
            searchTimeline.play();
        });
    }

    @FXML
    private TableView<MatchupData> tblMatchups;
    @FXML
    private TableColumn<MatchupData, String> colMainChar;
    @FXML
    private TableColumn<MatchupData, String> colOppChar;
    @FXML
    private TableColumn<MatchupData, Integer> colWins;
    @FXML
    private TableColumn<MatchupData, Integer> colTotal;
    @FXML
    private TableColumn<MatchupData, String> colWinrate;

    private void setupMatchupTable() {
        colMainChar.setCellValueFactory(new PropertyValueFactory<>("mainChar"));
        colOppChar.setCellValueFactory(new PropertyValueFactory<>("oppChar"));
        colWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colWinrate.setCellValueFactory(new PropertyValueFactory<>("winrate"));

        colMainChar.setCellFactory(column -> new TableCell<MatchupData, String>() {
            @Override
            protected void updateItem(String charCode, boolean empty) {
                super.updateItem(charCode, empty);
                if (empty || charCode == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox hbox = new HBox(5);
                    ImageView icon = CharacterIconUtils.createCharacterIcon(charCode);
                    hbox.getChildren().addAll(icon);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });

        colOppChar.setCellFactory(column -> new TableCell<MatchupData, String>() {
            @Override
            protected void updateItem(String charCode, boolean empty) {
                super.updateItem(charCode, empty);
                if (empty || charCode == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox hbox = new HBox(5);
                    ImageView icon = CharacterIconUtils.createCharacterIcon(charCode);
                    hbox.getChildren().addAll(icon);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });
        
        tblMatchups.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (tblMatchups.getSelectionModel().getSelectedItem() != null) {
                    try {
                        handleShowReplays();
                    } catch (Exception e) {
                        showError("Failed to open matchup: " + e.getMessage());
                    }
                }
            }
        });
    }

    @FXML
    private void handleShowReplays() throws Exception {
        long mainPlayerID = getMainPlayerID();
        if (mainPlayerID == 0) return;

        LocalDate fromDate = dpFromDate.getValue();
        LocalDate toDate = dpToDate.getValue();
        if (fromDate != null && toDate == null) {
            toDate = LocalDate.now();
        }

        if (selectedOpponent != null) {
            List<MatchRecord> matches = DatabaseManager.getDetailedMatchesVsOpponent(
                    mainPlayerID, selectedOpponent.steamID, fromDate, toDate);

            List<ReplayDisplay> replays = new ArrayList<>();
            for (MatchRecord m : matches) {
                replays.add(m.toReplayDisplay(mainPlayerID));
            }

            WindowUtils.openModal("ReplayList.fxml",
                "Replays vs " + selectedOpponent.name, true,
                c -> { ReplayList ctrl = (ReplayList) c; ctrl.setSpectatedMode(false); ctrl.setReplays(replays); },
                true);
        } else {
            MatchupData selectedMatchup = tblMatchups.getSelectionModel().getSelectedItem();
            if (selectedMatchup == null) return;

            String mainChar = selectedMatchup.getMainChar();
            String oppChar = selectedMatchup.getOppChar();

            List<MatchRecord> matches = DatabaseManager.getDetailedMatchesForMatchup(
                    mainPlayerID, mainChar, oppChar, fromDate, toDate);

            Map<Long, int[]> opponentStats = new HashMap<>();
            Map<Long, String> opponentNames = new HashMap<>();

            for (MatchRecord m : matches) {
                boolean mainIsP1 = m.isMainP1(mainPlayerID);
                long oppID = mainIsP1 ? m.p2SteamID : m.p1SteamID;
                String oppName = mainIsP1 ? m.p2Name : m.p1Name;

                opponentNames.putIfAbsent(oppID, oppName);
                int[] stats = opponentStats.computeIfAbsent(oppID, k -> new int[]{0, 0});
                stats[1]++;
                if (m.didMainWin(mainPlayerID)) {
                    stats[0]++;
                }
            }

            List<MatchupOpponentData> opponents = new ArrayList<>();
            for (Long oppID : opponentStats.keySet()) {
                int[] stats = opponentStats.get(oppID);
                opponents.add(new MatchupOpponentData(opponentNames.get(oppID), stats[0], stats[1], oppID));
            }

            WindowUtils.openModal("ReplayList.fxml",
                mainChar + " vs " + oppChar, true,
                c -> { ReplayList ctrl = (ReplayList) c; ctrl.setMatchupMode(mainChar, oppChar, opponents); },
                true);
        }
    }
    
    @FXML
    private void handleAllPlayerMatchups() throws Exception {
        long mainPlayerID = getMainPlayerID();
        if (mainPlayerID == 0) return;

        LocalDate fromDate = dpFromDate.getValue();
        LocalDate toDate = dpToDate.getValue();
        if (fromDate != null && toDate == null) {
            toDate = LocalDate.now();
        }
        final LocalDate finalToDate = toDate;

        Set<String> playedChars = new HashSet<>();
        String defaultChar = null;
        Map<String, Integer> charCount = new HashMap<>();
        try {
            List<MatchRecord> matches = DatabaseManager.getAllMatchesForPlayer(mainPlayerID, fromDate, toDate);
            for (MatchRecord m : matches) {
                boolean mainIsP1 = m.isMainP1(mainPlayerID);
                String mainChar = mainIsP1 ? m.p1Char : m.p2Char;
                playedChars.add(mainChar);
                charCount.merge(mainChar, 1, Integer::sum);
            }
            defaultChar = charCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        } catch (Exception e) {
            showError("Failed to load character data: " + e.getMessage());
        }

        final String finalDefaultChar = defaultChar;
        WindowUtils.openModal("ReplayList.fxml",
                "All Player Matchups", true,
                c -> { ReplayList ctrl = (ReplayList) c; ctrl.setAllPlayerMatchupsMode(playedChars, finalDefaultChar, mainPlayerID, fromDate, finalToDate); },
                true);
    }

    @FXML
    private void handleSpectatedMatches() throws Exception {
        long mainPlayerID = getMainPlayerID();
        if (mainPlayerID == 0) return;

        LocalDate fromDate = dpFromDate.getValue();
        LocalDate toDate = dpToDate.getValue();
        if (fromDate != null && toDate == null) {
            toDate = LocalDate.now();
        }

        List<MatchRecord> matches = DatabaseManager.getSpectatedMatches(mainPlayerID, fromDate, toDate);

        List<ReplayDisplay> replays = new ArrayList<>();
        for (MatchRecord m : matches) {
            replays.add(m.toSpectatedReplayDisplay());
        }

        WindowUtils.openModal("ReplayList.fxml", "Spectated Matches", true,
            c -> { ReplayList ctrl = (ReplayList) c; ctrl.setSpectatedMode(true); ctrl.setReplays(replays); },
            false);
    }

    @FXML
    private void handleActivityCalendar() throws Exception {
        long mainPlayerID = getMainPlayerID();
        if (mainPlayerID == 0) return;

        WindowUtils.openModal("ActivityCalendar.fxml", "Activity Calendar", false,
            c -> ((ActivityCalendar) c).setMainPlayer(mainPlayerID), false);
    }

    private void updateMatchupTable(Map<String, int[]> matchupStats) {
        List<MatchupData> matchups = new ArrayList<>();

        List<String> sortedMatchups = new ArrayList<>(matchupStats.keySet());
        Collections.sort(sortedMatchups);

        for (String matchup : sortedMatchups) {
            int[] stats = matchupStats.get(matchup);

            String mainChar = "";
            String oppChar = "";
            if (matchup.startsWith("As ") && matchup.contains(" vs ")) {
                String[] parts = matchup.substring(3).split(" vs ");
                if (parts.length == 2) {
                    mainChar = parts[0];
                    oppChar = parts[1];
                }
            }

            matchups.add(new MatchupData(mainChar, oppChar, stats[0], stats[1]));
        }

        tblMatchups.getItems().setAll(matchups);
    }

    private void loadAllMatchups() {
        loadAllMatchups(suppressMatchLoadErrors);
    }

    private void loadAllMatchups(boolean silent) {
        long mainPlayerID = getMainPlayerID();
        if (mainPlayerID == 0) {
            clearWinLossChart();
            return;
        }

        LocalDate fromDate = dpFromDate.getValue();
        LocalDate toDate = dpToDate.getValue();
        if (fromDate != null && toDate == null) {
            toDate = LocalDate.now();
        }

        try {
            List<MatchRecord> matches = DatabaseManager.getAllMatchesForPlayer(mainPlayerID, fromDate, toDate);
            if (matches.isEmpty()) {
                clearWinLossChart();
                return;
            }
            MatchRecord.MatchupStats stats = MatchRecord.computeStats(matches, mainPlayerID);
            updateMatchupTable(stats.matchupStats);
            updateWinLossChart(stats.totalWins, stats.totalMatches - stats.totalWins);
        } catch (Exception e) {
            if (!silent) {
                showError("Failed to load matchups: " + e.getMessage());
            }
            clearWinLossChart();
        }
    }

    @FXML
    private void handleUpdateOutdated() throws Exception {
        List<Object[]> outdatedReplays = DatabaseManager.getOutdatedReplays();

        if (outdatedReplays.isEmpty()) {
            System.out.println("No outdated replays found.");
            return;
        }

        int updated = 0;
        int missing = 0;

        for (Object[] replay : outdatedReplays) {
            String filename = (String) replay[0];
            String organizedPath = (String) replay[1];

            Path filePath = Path.of(organizedPath);
            if (!Files.exists(filePath)) {
                missing++;
                continue;
            }

            try {
                ReplayMetadata meta = ReplayMetadataParser.parse(filePath.toString());

                if (!meta.isOutdated) {
                    DatabaseManager.updateReplayMetadata(
                        filename,
                        meta.p1RoundsWon, meta.p2RoundsWon,
                        meta.p1Score, meta.p2Score,
                        meta.p1Rank, meta.p2Rank,
                        meta.winnerSide, meta.durationFrames,
                        ReplayMetadataParser.getMatchType(meta.matchType),
                        ReplayMetadataParser.getGameVersion(meta.gameVersion),
                        ReplayMetadataParser.getMatchFlag(meta.matchFlags),
                        meta.ping, meta.optionsModified,
                        false
                    );
                    updated++;
                }
            } catch (Exception e) {
                System.out.println("Failed to parse: " + filename);
                showError("Failed to parse replay: " + filename);
            }
        }

        System.out.println("Updated " + updated + " replays. " + missing + " files not found.");
    }

    @FXML
    private void handleGenerateSteamIDs() throws Exception {
        Path sourceDir = Path.of(ConfigManager.get("rootFolder", ""));
        Map<Long, Set<String>> steamIdToNames = new java.util.TreeMap<>();
        long mainPlayerID = getMainPlayerID();
        if (mainPlayerID == 0) return;

        try (var stream = Files.list(sourceDir)) {
            stream.filter(p -> p.toString().endsWith(".ggr"))
                  .forEach(replayPath -> {
                      try {
                          ReplayMetadata meta = ReplayMetadataParser.parse(replayPath.toString());

                          long opponentSteamId;
                          String opponentName;

                          if (meta.p1SteamID == mainPlayerID) {
                              opponentSteamId = meta.p2SteamID;
                              opponentName = meta.p2Name;
                          } else if (meta.p2SteamID == mainPlayerID) {
                              opponentSteamId = meta.p1SteamID;
                              opponentName = meta.p1Name;
                          } else {
                              return;
                          }

                          steamIdToNames.computeIfAbsent(opponentSteamId, k -> new java.util.LinkedHashSet<>())
                                        .add(opponentName);

                      } catch (Exception e) {
                          e.printStackTrace();
                      }
                  });
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Long, Set<String>> entry : steamIdToNames.entrySet()) {
            sb.append(entry.getKey())
              .append("=")
              .append(String.join(" -|- ", entry.getValue()))
              .append("\n");
        }

        Files.writeString(Path.of("SteamIDs.txt"), sb.toString());
    }

    @FXML
    private void handleOrganizeReplays() {
        if (ReplayOrganizerService.isRunning()) return;

        long mainPlayerID = getMainPlayerID();
        if (mainPlayerID == 0) return;

        progressBar.setVisible(true);
        labelReplays.setVisible(true);
        progressBar.setProgress(0.0);
        btnOrganizeReplays.setDisable(true);

        ReplayOrganizerService.organizeReplays(mainPlayerID, new ReplayOrganizerService.ProgressCallback() {
            @Override
            public void update(int current, int total, String message) {
                progressBar.setProgress((double) current / total);
                labelReplays.setText(message);
            }

            @Override
            public void complete(String message) {
                progressBar.setVisible(false);
                labelReplays.setVisible(false);
                btnOrganizeReplays.setDisable(false);
                updateFieldStates();
                loadAllMatchups();
            }

            @Override
            public void error(String message) {
                progressBar.setVisible(false);
                labelReplays.setVisible(false);
                btnOrganizeReplays.setDisable(false);
                updateFieldStates();
                showError(message);
            }
        });
    }

    @FXML
    private void handleFindPlayer() throws Exception {
        long opponentID;
        String opponentName;
        String aliases;

        if (!lstPlayerResults.getItems().isEmpty() && selectedOpponent == null) {
            lstPlayerResults.getSelectionModel().select(0);
            selectedOpponent = lstPlayerResults.getItems().get(0);
        }

        if (selectedOpponent != null) {
            opponentID = selectedOpponent.steamID;
            opponentName = selectedOpponent.name;
            aliases = selectedOpponent.aliases;
        } else {
            String searchText = txtPlayerSearch.getText().trim();
            if (searchText.isEmpty()) {
                loadAllMatchups();
                return;
            }

            List<OpponentDisplay> matches = DatabaseManager.searchOpponentNames(searchText, 1);
            if (matches.isEmpty()) {
                System.out.println("No opponent found with name: " + searchText);
                clearWinLossChart();
                return;
            }

            opponentID = matches.get(0).steamID;
            opponentName = matches.get(0).name;
            aliases = matches.get(0).aliases;
        }

        LocalDate fromDate = dpFromDate.getValue();
        LocalDate toDate = dpToDate.getValue();

        if (fromDate != null && toDate == null) {
            toDate = LocalDate.now();
        }

        System.out.println("\n========================================");
        System.out.println("Opponent: " + opponentName + " (SteamID: " + opponentID + ")");
        if (aliases != null && !aliases.isEmpty()) {
            System.out.println("Aliases: " + aliases);
        }
        if (fromDate != null || toDate != null) {
            System.out.println("Date Filter: " +
                (fromDate != null ? fromDate : "any") + " to " +
                (toDate != null ? toDate : "any"));
        }
        System.out.println();

        long mainPlayerID = getMainPlayerID();
        List<MatchRecord> matches = DatabaseManager.getMatchesVsOpponent(mainPlayerID, opponentID, fromDate, toDate);

        if (matches.isEmpty()) {
            System.out.println("No matches found.");
            System.out.println("\n========================================");
            clearWinLossChart();
            return;
        }

        MatchRecord.MatchupStats stats = MatchRecord.computeStats(matches, mainPlayerID);
        updateMatchupTable(stats.matchupStats);
        updateWinLossChart(stats.totalWins, stats.totalMatches - stats.totalWins);
    }

    @FXML
    private void handleRestoreReplays() {
        if (ReplayOrganizerService.isRunning()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        ((Stage) confirm.getDialogPane().getScene().getWindow()).getIcons()
            .add(new Image(getClass().getResource(WindowUtils.ICON_PATH).toExternalForm()));
        confirm.setTitle("Restore Replays");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to restore all replays to the root folder?");
        if (confirm.showAndWait().orElse(null) != ButtonType.OK) {
            return;
        }

        progressBar.setVisible(true);
        labelReplays.setVisible(true);
        progressBar.setProgress(0.0);
        btnOrganizeReplays.setDisable(true);

        ReplayOrganizerService.restoreReplays(new ReplayOrganizerService.ProgressCallback() {
            @Override
            public void update(int current, int total, String message) {
                progressBar.setProgress((double) current / total);
                labelReplays.setText(message);
            }

            @Override
            public void complete(String message) {
                progressBar.setVisible(false);
                labelReplays.setVisible(false);
                btnOrganizeReplays.setDisable(false);
                updateFieldStates();
                loadAllMatchups();

                if (message != null) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Restore Complete");
                    alert.setHeaderText(null);
                    alert.setContentText(message);
                    alert.showAndWait();
                }
            }

            @Override
            public void error(String message) {
                progressBar.setVisible(false);
                labelReplays.setVisible(false);
                btnOrganizeReplays.setDisable(false);
                updateFieldStates();
                showError(message);
            }
        });
    }
    
    @FXML
    private void handleAbout() {
    	Alert alert = new Alert(Alert.AlertType.INFORMATION);
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons()
        .add(new Image(getClass().getResource(WindowUtils.ICON_PATH).toExternalForm()));
    	alert.setTitle("About");
    	alert.setHeaderText(null);
    	alert.setContentText("Guilty Gear XX Accent Core +R replay parser."
    					   + "\nVersion: 1.0."
    					   + "\n\nMade by SyToX.");
    	alert.showAndWait();
    }
}