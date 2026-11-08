package controllers;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import application.CharacterConstants;
import application.DatabaseManager;
import application.MatchRecord;
import application.MatchupCharData;
import application.MatchupOpponentData;
import application.ReplayDisplay;
import application.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;
import utils.CharacterIconUtils;

public class ReplayList {

    @FXML
    private TableView<ReplayDisplay> tblReplays;
    @FXML
    private TableView<MatchupOpponentData> tblChars;
    @FXML
    private TableColumn<ReplayDisplay, String> colDate;
    @FXML
    private TableColumn<ReplayDisplay, String> colMatchup;
    @FXML
    private TableColumn<ReplayDisplay, String> colOpponent;
    @FXML
    private TableColumn<ReplayDisplay, String> colPlayer1;
    @FXML
    private TableColumn<ReplayDisplay, String> colPlayer2;
    @FXML
    private TableColumn<ReplayDisplay, String> colWinner;
    @FXML
    private TableColumn<ReplayDisplay, String> colVersion;
    @FXML
    private TableColumn<ReplayDisplay, String> colFlag;
    @FXML
    private TableColumn<ReplayDisplay, String> colOutdated;
    @FXML
    private TableColumn<ReplayDisplay, Void> colOpen;
    @FXML
    private TableColumn<MatchupOpponentData, String> colMatchupPlayer;
    @FXML
    private TableColumn<MatchupOpponentData, Integer> colMatchupWins;
    @FXML
    private TableColumn<MatchupOpponentData, Integer> colMatchupTotal;
    @FXML
    private TableColumn<MatchupOpponentData, String> colMatchupWinrate;
    @FXML
    private TableColumn<MatchupOpponentData, Boolean> colMatchupInclude;
    @FXML
    private ImageView imgYou;
    @FXML
    private ImageView imgOpp;
    @FXML
    private PieChart pieWinLoss;
    @FXML
    private CheckBox cbAll;
    @FXML
    private Text txtWinsSum, txtTotalSum, txtWinrateSum;
    @FXML
    private GridPane gridAsChar, gridVsChar, gridStats;
    @FXML
    private Text txtAs, txtMinMatch;
    @FXML
    private ImageView imgAsChar;
    @FXML private TableColumn<MatchupCharData, String> colAllMatchupOpponent;
    @FXML private TableColumn<MatchupCharData, String> colAllMatchupChar;
    @FXML private TableColumn<MatchupCharData, Integer> colAllMatchupWins;
    @FXML private TableColumn<MatchupCharData, Integer> colAllMatchupTotal;
    @FXML private TableColumn<MatchupCharData, String> colAllMatchupWinrate;
    @FXML
    private TableView<MatchupCharData> tbAllMachups;
    @FXML
    private Spinner<Integer> spinnerGamesLimit;
    private List<MatchupCharData> allMatchupRows = null;
    private String selectedAsChar = null;
    private Set<String> playedChars = null;
    private long mainPlayerID = 0;
    private LocalDate fromDate = null;
    private LocalDate toDate = null;

    private static final float PADDING = 28;
    private static final float MODE_WIDTH = 428;
    
    private static final float GRID_PANE_ALL_MATCHUP_DEFAULT_X = 177;
    private static final float PIE_ALL_MATCHUP_DEFAULT_X = 259;
    
    private static final float GRID_PANE_ALL_MATCHUP_X = GRID_PANE_ALL_MATCHUP_DEFAULT_X - 50;
    private static final float PIE_ALL_MATCHUP_X = PIE_ALL_MATCHUP_DEFAULT_X - 50;
   

    private boolean spectatedMode = false;
    private List<MatchupOpponentData> matchupOpponents;

    private TableCell<ReplayDisplay, String> createTextCell(
            Function<ReplayDisplay, String> extractor,
            Function<ReplayDisplay, String> styleClassFunc) {
        return new TableCell<ReplayDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    ReplayDisplay rec = getTableRow().getItem();
                    String text = extractor.apply(rec);
                    if (text != null && !text.isEmpty()) {
                        Text t = new Text(text);
                        t.getStyleClass().add(styleClassFunc.apply(rec));
                        setGraphic(t);
                    } else {
                        setGraphic(null);
                    }
                    setText(null);
                }
            }
        };
    }

    public void setSpectatedMode(boolean spectated) {
        this.spectatedMode = spectated;
        colPlayer1.setVisible(spectated);
        colPlayer2.setVisible(spectated);
        colWinner.setVisible(!spectated);
        spinnerGamesLimit.setVisible(false);
        spinnerGamesLimit.setManaged(false);
    }

    public void setShowOpponent(boolean show) {
        colOpponent.setVisible(show);
    }

    public void setMatchupMode(String mainChar, String oppChar, List<MatchupOpponentData> opponents) {
        this.matchupOpponents = opponents;
        
        gridStats.setLayoutX(GRID_PANE_ALL_MATCHUP_DEFAULT_X);
        pieWinLoss.setLayoutY(0.0);

        gridAsChar.setVisible(false);
        gridAsChar.setManaged(false);
        gridVsChar.setVisible(true);
        gridVsChar.setManaged(true);
        gridStats.setVisible(true);
        gridStats.setManaged(true);
        pieWinLoss.setVisible(true);
        pieWinLoss.setManaged(true);
        cbAll.setVisible(true);
        cbAll.setManaged(true);
        tblChars.setVisible(true);
        tblChars.setManaged(true);
        tbAllMachups.setVisible(false);
        tbAllMachups.setManaged(false);
        tblReplays.setVisible(false);
        tblReplays.setManaged(false);
        spinnerGamesLimit.setVisible(false);
        spinnerGamesLimit.setManaged(false);
        txtMinMatch.setVisible(false);
        txtMinMatch.setManaged(false);

        imgYou.setImage(CharacterIconUtils.createCharacterIcon(mainChar, 64).getImage());
        imgOpp.setImage(CharacterIconUtils.createCharacterIcon(oppChar, 64).getImage());

        for (MatchupOpponentData opp : opponents) {
            opp.includeProperty().addListener((obs, oldVal, newVal) -> {
                updateMatchupPieChart();
                boolean allChecked = true;
                for (MatchupOpponentData o : opponents) {
                    if (!o.getInclude()) { allChecked = false; break; }
                }
                cbAll.setSelected(allChecked);
            });
        }
        cbAll.setSelected(true);

        tblChars.getItems().setAll(opponents);
        updateMatchupPieChart();

        Platform.runLater(() -> {
            Stage stage = (Stage) tblChars.getScene().getWindow();
            stage.setWidth(MODE_WIDTH);
            stage.setMinWidth(MODE_WIDTH);
            stage.setMaxWidth(MODE_WIDTH);
            stage.centerOnScreen();
        });
    }
    
    public void setAllPlayerMatchupsMode(Set<String> playedChars, String defaultChar, long mainPlayerID, LocalDate fromDate, LocalDate toDate) {
        this.matchupOpponents = null;
        this.playedChars = playedChars;
        this.mainPlayerID = mainPlayerID;
        this.fromDate = fromDate;
        this.toDate = toDate;
        gridStats.setLayoutX(GRID_PANE_ALL_MATCHUP_X);
        pieWinLoss.setLayoutX(PIE_ALL_MATCHUP_X);

        gridAsChar.setVisible(true);
        gridAsChar.setManaged(true);
        gridVsChar.setVisible(false);
        gridVsChar.setManaged(false);
        gridStats.setVisible(true);
        gridStats.setManaged(true);
        pieWinLoss.setVisible(true);
        pieWinLoss.setManaged(true);
        cbAll.setVisible(false);
        cbAll.setManaged(false);
        tblChars.setVisible(false);
        tblChars.setManaged(false);
        tbAllMachups.setVisible(true);
        tbAllMachups.setManaged(true);
        spinnerGamesLimit.setVisible(true);
        spinnerGamesLimit.setManaged(true);
        txtMinMatch.setVisible(true);
        txtMinMatch.setManaged(true);
        tblReplays.setVisible(false);
        tblReplays.setManaged(false);

        imgAsChar.setOnMouseClicked(e -> showCharacterPopup());
        imgAsChar.getStyleClass().add("activity-calendar-cell-active");
        spinnerGamesLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1));
        spinnerGamesLimit.getValueFactory().setConverter(new IntegerStringConverter());
        
        TextFormatter<Integer> formatter = new TextFormatter<>(
        	    new IntegerStringConverter(),
        	    spinnerGamesLimit.getValue(),
        	    change -> {
        	        return change.getControlNewText().matches("\\d+")
        	                ? change
        	                : null;
        	    }
        	);
        
        
        spinnerGamesLimit.getEditor().setTextFormatter(formatter);

        if (defaultChar != null) {
            selectedAsChar = defaultChar;
            imgAsChar.setImage(CharacterIconUtils.createCharacterIcon(defaultChar, 64).getImage());
            loadAllMatchupData(defaultChar);
        } else {
            txtWinsSum.setText("0");
            txtTotalSum.setText("0");
            txtWinrateSum.setText("0%");
            pieWinLoss.setData(FXCollections.observableArrayList());
        }

        Platform.runLater(() -> {
            Stage stage = (Stage) tbAllMachups.getScene().getWindow();
            stage.setWidth(MODE_WIDTH);
            stage.setMinWidth(MODE_WIDTH);
            stage.setMaxWidth(MODE_WIDTH);
            stage.centerOnScreen();
        });
    }

    private void updateMatchupPieChart() {
        if (matchupOpponents == null) return;

        int totalWins = 0;
        int totalLosses = 0;
        for (MatchupOpponentData opp : matchupOpponents) {
            if (opp.getInclude()) {
                totalWins += opp.getWins();
                totalLosses += opp.getTotal() - opp.getWins();
            }
        }

        if (totalWins + totalLosses == 0) {
            pieWinLoss.setData(FXCollections.observableArrayList());
            txtWinsSum.setText("0");
            txtTotalSum.setText("0");
            txtWinrateSum.setText("0%");
            return;
        }

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Wins", totalWins),
            new PieChart.Data("Losses", totalLosses)
        );
        pieWinLoss.setData(pieChartData);

        int total = totalWins + totalLosses;
        txtWinsSum.setText(String.valueOf(totalWins));
        txtTotalSum.setText(String.valueOf(total));
        txtWinrateSum.setText(String.format("%.1f%%", (double) totalWins / total * 100));
        String[] styleClasses = {"pie-slice-win", "pie-slice-loss"};
        String[] labels = {
            "Wins: " + totalWins + " (" + String.format("%.1f%%", (double) totalWins / total * 100) + ")",
            "Losses: " + totalLosses + " (" + String.format("%.1f%%", (double) totalLosses / total * 100) + ")"
        };
        

        for (int i = 0; i < pieWinLoss.getData().size(); i++) {
            PieChart.Data slice = pieWinLoss.getData().get(i);
            slice.getNode().getStyleClass().add(styleClasses[i]);

            Tooltip tooltip = new Tooltip(labels[i]);
            Tooltip.install(slice.getNode(), tooltip);
            tooltip.setShowDelay(Duration.ZERO);
            tooltip.setHideDelay(Duration.ZERO);
        }
    }

    public void setReplays(List<ReplayDisplay> replays) {
        gridAsChar.setVisible(false);
        gridAsChar.setManaged(false);
        gridVsChar.setVisible(false);
        gridVsChar.setManaged(false);
        gridStats.setVisible(false);
        gridStats.setManaged(false);
        pieWinLoss.setVisible(false);
        pieWinLoss.setManaged(false);
        cbAll.setVisible(false);
        cbAll.setManaged(false);
        tblChars.setVisible(false);
        tblChars.setManaged(false);
        tbAllMachups.setVisible(false);
        tbAllMachups.setManaged(false);
        tblReplays.setVisible(true);
        tblReplays.setManaged(true);
        txtMinMatch.setVisible(false);
        txtMinMatch.setManaged(false);

        tblReplays.getItems().setAll(replays);

        double maxMatchupWidth = 180.0;
        for (ReplayDisplay replay : replays) {
            double charCodesWidth = (replay.mainChar.length() + replay.oppChar.length()) * 7.0;
            double estimatedWidth = 149.0 + charCodesWidth;
            maxMatchupWidth = Math.max(maxMatchupWidth, estimatedWidth);
        }
        colMatchup.setPrefWidth(maxMatchupWidth);

        if (spectatedMode) {
            double maxP1Width = 80.0;
            double maxP2Width = 80.0;
            for (ReplayDisplay replay : replays) {
                if (replay.p1Name != null) {
                    maxP1Width = Math.max(maxP1Width, replay.p1Name.length() * 7.0);
                }
                if (replay.p2Name != null) {
                    maxP2Width = Math.max(maxP2Width, replay.p2Name.length() * 7.0);
                }
            }
            colPlayer1.setPrefWidth(maxP1Width);
            colPlayer2.setPrefWidth(maxP2Width);
        } else {
            double maxWidth = 80.0;
            for (ReplayDisplay replay : replays) {
                if (replay.winnerName != null) {
                    maxWidth = Math.max(maxWidth, replay.winnerName.length() * 7.0 + 16);
                }
            }
            colWinner.setPrefWidth(maxWidth);
        }

        if (colOpponent.isVisible()) {
            double maxOppWidth = 80.0;
            for (ReplayDisplay replay : replays) {
                if (replay.opponentName != null) {
                    maxOppWidth = Math.max(maxOppWidth, replay.opponentName.length() * 7.0);
                }
            }
            colOpponent.setPrefWidth(maxOppWidth);
        }

        Platform.runLater(() -> {
            double totalWidth = colDate.getWidth() + colMatchup.getWidth();
            if (spectatedMode) {
                totalWidth += colPlayer1.getWidth() + colPlayer2.getWidth();
            } else {
                totalWidth += colWinner.getWidth();
            }
            if (colOpponent.isVisible()) {
                totalWidth += colOpponent.getWidth();
            }
            totalWidth += colVersion.getWidth() + colFlag.getWidth()
                        + colOutdated.getWidth() + colOpen.getWidth();

            tblReplays.setPrefWidth(totalWidth);
            Stage stage = (Stage) tblReplays.getScene().getWindow();
            stage.setWidth(totalWidth + (PADDING * 2));
            stage.setMinWidth(totalWidth + (PADDING * 2));
            stage.setMaxWidth(totalWidth + (PADDING * 2));
            stage.centerOnScreen();
        });
    }
    
    private void showCharacterPopup() {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(8));
        String bgColor = ThemeManager.isDark() ? "#2b2b2b" : "#ffffff";
        grid.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: #555; -fx-border-width: 1;");

        String[] characters = CharacterConstants.CHARACTERS;
        int cols = 5;
        ColorAdjust grayscale = new ColorAdjust();
        grayscale.setSaturation(-1);

        for (int i = 0; i < characters.length; i++) {
            String code = characters[i];
            ImageView icon = CharacterIconUtils.createCharacterIcon(code, 48);
            icon.setPickOnBounds(true);

            boolean played = playedChars != null && playedChars.contains(code);

            if (!played) {
                icon.setEffect(grayscale);
                icon.setCursor(javafx.scene.Cursor.DEFAULT);
            } else {
                icon.setCursor(javafx.scene.Cursor.HAND);
                icon.getStyleClass().add("activity-calendar-cell-active");


                if (code.equals(selectedAsChar)) {
                    icon.setStyle("-fx-border-color: #4a90d9; -fx-border-width: 2;");
                } else {
                    icon.setOnMouseEntered(e -> icon.setStyle("-fx-border-color: #888; -fx-border-width: 1;"));
                    icon.setOnMouseExited(e -> icon.setStyle(null));
                }

                icon.setOnMouseClicked(ev -> {
                    selectedAsChar = code;
                    imgAsChar.setImage(CharacterIconUtils.createCharacterIcon(code, 64).getImage());
                    popup.hide();
                    loadAllMatchupData(selectedAsChar);
                    ev.consume();
                });
            }

            grid.add(icon, i % cols, i / cols);
        }

        javafx.geometry.Bounds bounds = imgAsChar.localToScreen(imgAsChar.getBoundsInLocal());
        popup.getContent().add(grid);
        popup.show(imgAsChar, bounds.getMinX(), bounds.getMaxY() + 4);
    }
    
    private void loadAllMatchupData(String mainChar) {
        try {
            List<MatchRecord> matches = DatabaseManager.getDetailedMatchesForPlayerChar(
                    mainPlayerID, mainChar, fromDate, toDate);

            Map<String, int[]> statsMap = new HashMap<>();
            Map<String, String> nameMap = new HashMap<>();
            Map<String, Long> steamIDMap = new HashMap<>();

            for (MatchRecord m : matches) {
                boolean mainIsP1 = m.isMainP1(mainPlayerID);
                long oppSteamID = mainIsP1 ? m.p2SteamID : m.p1SteamID;
                String oppName = mainIsP1 ? m.p2Name : m.p1Name;
                String oppChar = mainIsP1 ? m.p2Char : m.p1Char;
                String key = oppSteamID + "|" + oppChar;

                int[] stats = statsMap.computeIfAbsent(key, k -> new int[]{0, 0});
                stats[1]++;
                if (m.didMainWin(mainPlayerID)) {
                    stats[0]++;
                }
                nameMap.putIfAbsent(key, oppName);
                steamIDMap.putIfAbsent(key, oppSteamID);
            }
            

            List<MatchupCharData> rows = new ArrayList<>();
            
            int totalWins = 0;
            int totalMatches = 0;
            for (String key : statsMap.keySet()) {
                int[] stats = statsMap.get(key);
                String oppName = nameMap.get(key);
                String oppChar = key.substring(key.indexOf("|") + 1);
                long oppSteamID = steamIDMap.get(key);
                rows.add(new MatchupCharData(oppName, oppChar, stats[0], stats[1], oppSteamID));
            }
            allMatchupRows = rows;

            tbAllMachups.getSortOrder().setAll(colAllMatchupTotal);
            colAllMatchupTotal.setSortType(TableColumn.SortType.DESCENDING);
            applyGamesFilter();
            

        } catch (Exception e) {
            allMatchupRows = null;
            txtWinsSum.setText("0");
            txtTotalSum.setText("0");
            txtWinrateSum.setText("0%");
            pieWinLoss.setData(FXCollections.observableArrayList());
            tbAllMachups.getItems().clear();
            e.printStackTrace();
        }
    }
    
    private void applyGamesFilter() {
        if (allMatchupRows == null) return;
        
        int limit = spinnerGamesLimit.getValue();
        List<MatchupCharData> visible = new ArrayList<>();
        int totalWins = 0;
        int totalMatches = 0;
        for (MatchupCharData r : allMatchupRows) {
            if (r.getTotal() >= limit) {
                visible.add(r);
                totalWins += r.getWins();
                totalMatches += r.getTotal();
            }
        }

        tbAllMachups.getItems().setAll(visible);
        tbAllMachups.sort();

        int totalLosses = totalMatches - totalWins;
        if (totalMatches == 0) {
            pieWinLoss.setData(FXCollections.observableArrayList());
            txtWinsSum.setText("0");
            txtTotalSum.setText("0");
            txtWinrateSum.setText("0%");
        } else {
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Wins", totalWins),
                new PieChart.Data("Losses", totalLosses)
            );
            pieWinLoss.setData(pieChartData);

            String[] styleClasses = {"pie-slice-win", "pie-slice-loss"};
            String[] labels = {
                "Wins: " + totalWins + " (" + String.format("%.1f%%", (double) totalWins / totalMatches * 100) + ")",
                "Losses: " + totalLosses + " (" + String.format("%.1f%%", (double) totalLosses / totalMatches * 100) + ")"
            };
            for (int i = 0; i < pieWinLoss.getData().size(); i++) {
                PieChart.Data slice = pieWinLoss.getData().get(i);
                slice.getNode().getStyleClass().add(styleClasses[i]);
                Tooltip tooltip = new Tooltip(labels[i]);
                Tooltip.install(slice.getNode(), tooltip);
                tooltip.setShowDelay(Duration.ZERO);
                tooltip.setHideDelay(Duration.ZERO);
            }

            txtWinsSum.setText(String.valueOf(totalWins));
            txtTotalSum.setText(String.valueOf(totalMatches));
            txtWinrateSum.setText(String.format("%.1f%%", (double) totalWins / totalMatches * 100));
        }
    }

    @FXML
    public void initialize() {
        spinnerGamesLimit.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100000, 0, 1));
        spinnerGamesLimit.setEditable(true);
        spinnerGamesLimit.valueProperty().addListener((obs, oldVal, newVal) -> applyGamesFilter());
        colMatchupPlayer.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colMatchupWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colMatchupTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colMatchupWinrate.setCellValueFactory(new PropertyValueFactory<>("winrate"));
        colMatchupInclude.setCellValueFactory(new PropertyValueFactory<>("include"));
        colMatchupInclude.setCellFactory(CheckBoxTableCell.forTableColumn(colMatchupInclude));
        tblChars.setEditable(true);
        colMatchupInclude.setEditable(true);
        
        colMatchupWinrate.setComparator((a, b) -> Double.compare(
        	    Double.parseDouble(a.replace("%", "")),
        	    Double.parseDouble(b.replace("%", ""))));
        
        colAllMatchupOpponent.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colAllMatchupWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colAllMatchupTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colAllMatchupWinrate.setCellValueFactory(new PropertyValueFactory<>("winrate"));
        colAllMatchupWinrate.setComparator((a, b) -> Double.compare(
            Double.parseDouble(a.replace("%", "")),
            Double.parseDouble(b.replace("%", ""))));
        
        colAllMatchupChar.setCellFactory(col -> new TableCell<MatchupCharData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    MatchupCharData data = getTableRow().getItem();
                    ImageView icon = CharacterIconUtils.createCharacterIcon(data.getOppChar());
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    hbox.getChildren().add(icon);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });
        
        colDate.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().date));

        colMatchup.setCellFactory(col -> new TableCell<ReplayDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    ReplayDisplay replay = getTableRow().getItem();
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    ImageView mainCharIcon = CharacterIconUtils.createCharacterIcon(replay.mainChar);
                    ImageView oppCharIcon = CharacterIconUtils.createCharacterIcon(replay.oppChar);
                    Text vsText = new Text(" vs ");
                    vsText.getStyleClass().add("matchup-text");
                    hbox.getChildren().addAll(mainCharIcon, vsText, oppCharIcon);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });
        
        cbAll.setOnAction(e -> {
            boolean selected = cbAll.isSelected();
            for (MatchupOpponentData opp : tblChars.getItems()) {
                opp.includeProperty().set(selected);
            }
        });
        cbAll.setVisible(false);

        colOpponent.setCellFactory(col -> createTextCell(r -> r.opponentName, r -> "matchup-text"));
        colPlayer1.setCellFactory(col -> createTextCell(r -> r.p1Name, r -> r.mainWon ? "spec-matchup-text-bold" : "matchup-text"));
        colPlayer2.setCellFactory(col -> createTextCell(r -> r.p2Name, r -> !r.mainWon ? "spec-matchup-text-bold" : "matchup-text"));
        colVersion.setCellFactory(col -> createTextCell(r -> r.gameVersion, r -> "matchup-text"));
        colWinner.setCellFactory(col -> createTextCell(r -> r.winnerName, r -> "matchup-text-bold"));
        colFlag.setCellFactory(col -> createTextCell(r -> r.matchFlag, r -> "flag-text"));
        colOutdated.setCellFactory(col -> createTextCell(r -> r.outdated ? "Yes" : null, r -> "outdated-text"));
        
        colOpen.setCellFactory(col -> new TableCell<ReplayDisplay, Void>() {
            private final Button btnPlay = new Button("Play");
            private final Button btnFolder = new Button("Folder");
            private final HBox hbox = new HBox(4, btnPlay, btnFolder);
            {
                String btnStyle = "-fx-padding: 2 8 2 8; -fx-font-size: 11;";
                btnPlay.setStyle(btnStyle);
                btnFolder.setStyle(btnStyle);
                btnPlay.setOnAction(e -> {
                    ReplayDisplay replay = getTableRow().getItem();
                    if (replay != null && replay.filePath != null && !replay.filePath.isEmpty()) {
                        try {
                            Desktop.getDesktop().open(new File(replay.filePath));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                btnFolder.setOnAction(e -> {
                    ReplayDisplay replay = getTableRow().getItem();
                    if (replay != null && replay.filePath != null && !replay.filePath.isEmpty()) {
                        try {
                            File file = new File(replay.filePath);
                            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                                new ProcessBuilder("cmd.exe", "/c", "explorer /select,\"" + file.getAbsolutePath() + "\"").start();
                            } else {
                                Desktop.getDesktop().open(file.getParentFile());
                            }
                            System.out.println(replay.filePath);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ReplayDisplay replay = getTableRow().getItem();
                    setGraphic(replay.filePath != null && !replay.filePath.isEmpty() ? hbox : null);
                }
            }
        });
    }
}