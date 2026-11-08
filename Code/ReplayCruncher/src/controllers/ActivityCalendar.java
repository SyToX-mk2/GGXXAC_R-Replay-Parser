package controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import application.DatabaseManager;
import application.MatchRecord;
import application.ReplayDisplay;
import application.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import utils.WindowUtils;

public class ActivityCalendar {

    @FXML
    private Label lblYear;
    @FXML
    private GridPane gridCalendar;
    @FXML
    private Pane monthLabelsBox;
    @FXML
    private VBox dayLabelsBox;

    private long mainPlayerID;
    private int currentYear;

    private static final double CELL_SIZE = 24;
    private static final double MIN_RADIUS = 3;
    private static final double MAX_RADIUS = 12;

    private static final String[] MONTH_NAMES = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    private static final String[] DAY_NAMES = {
        "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    };
    
    private Color getWinColor() {
        return ThemeManager.isDark() ? Color.valueOf("#3bc429") : Color.valueOf("#15fc05");
    }
    private Color getLossColor() {
        return ThemeManager.isDark() ? Color.valueOf("#c42929") : Color.valueOf("#fc0505");
    }

    private Tooltip instantTooltip(String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.ZERO);
        t.setHideDelay(Duration.ZERO);
        return t;
    }

    public void setMainPlayer(long steamID) {
        this.mainPlayerID = steamID;
        this.currentYear = LocalDate.now().getYear();
        buildDayLabels();
        populateCalendar();
    }

    private void buildDayLabels() {
        dayLabelsBox.getChildren().clear();
        for (String name : DAY_NAMES) {
            Label lbl = new Label(name);
            lbl.setPrefHeight(CELL_SIZE + 2);
            lbl.setStyle("-fx-font-size: 9px; -fx-text-alignment: right;");
            dayLabelsBox.getChildren().add(lbl);
        }
    }

    @FXML
    private void handlePrevYear() {
        currentYear--;
        populateCalendar();
    }

    @FXML
    private void handleNextYear() {
        currentYear++;
        populateCalendar();
    }

    private void populateCalendar() {    	
        lblYear.setText(String.valueOf(currentYear));
        gridCalendar.getChildren().clear();
        monthLabelsBox.getChildren().clear();

        Map<Integer, int[]> activity;
        try {
            activity = DatabaseManager.getYearlyActivity(mainPlayerID, currentYear);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        LocalDate jan1 = LocalDate.of(currentYear, 1, 1);
        int offset = jan1.getDayOfWeek().getValue() - 1;
        int daysInYear = jan1.lengthOfYear();
        int totalColumns = (offset + daysInYear + 6) / 7;

        int maxMatches = activity.values().stream()
            .mapToInt(a -> a[0])
            .max()
            .orElse(1);
        if (maxMatches == 0) maxMatches = 1;

        for (int row = 0; row < offset; row++) {
            gridCalendar.add(createEmptyCell(), 0, row);
        }

        for (int dayOfYear = 1; dayOfYear <= daysInYear; dayOfYear++) {
            int row = (offset + dayOfYear - 1) % 7;
            int col = (offset + dayOfYear - 1) / 7;
            LocalDate date = LocalDate.ofYearDay(currentYear, dayOfYear);

            StackPane cell = new StackPane();
            cell.setPrefSize(CELL_SIZE, CELL_SIZE);
            cell.setMinSize(CELL_SIZE, CELL_SIZE);
            cell.getStyleClass().add("activity-calendar-cell");
            //cell.setStyle("-fx-background-color: list-cell:odd; -fx-background-radius: 2;");
            
            int[] stats = activity.get(dayOfYear);
            if (stats != null && stats[0] > 0) {
                int total = stats[0];
                int wins = stats[1];
                double winRatio = (double) wins / total;
                double radius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * ((double) total / maxMatches);
                Color color = interpolateColor(getLossColor(), getWinColor(), winRatio);
                cell.getChildren().add(new Circle(radius, color));

                double winrate = wins * 100.0 / total;
                Tooltip.install(cell, instantTooltip(String.format("%tF\n%d matches\n%d wins (%.0f%%)", date, total, wins, winrate)));

                final LocalDate cellDate = date;
                cell.setOnMouseClicked(e -> openReplaysForDate(cellDate));
                cell.getStyleClass().add("activity-calendar-cell-active");
                cell.setOnMouseEntered(e -> cell.setViewOrder(-1));
                cell.setOnMouseExited(e -> cell.setViewOrder(0));
            } else {
                Tooltip.install(cell, instantTooltip(String.format("%tF\nNo matches", date)));
            }

            gridCalendar.add(cell, col, row);
        }

        int lastRow = (offset + daysInYear - 1) % 7;
        for (int row = lastRow + 1; row < 7; row++) {
            int col = (offset + daysInYear - 1) / 7;
            gridCalendar.add(createEmptyCell(), col, row);
        }

        monthLabelsBox.getChildren().clear();
        double colWidth = CELL_SIZE + 2;
        for (int m = 1; m <= 12; m++) {
            int startDayOfYear = LocalDate.of(currentYear, m, 1).getDayOfYear();
            int startCol = (offset + startDayOfYear - 1) / 7;
            Label monthLabel = new Label(MONTH_NAMES[m - 1]);
            monthLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -my-text-primary;");
            monthLabel.setLayoutX(40 + startCol * colWidth);
            monthLabelsBox.getChildren().add(monthLabel);
        }
    }

    private void openReplaysForDate(LocalDate date) {
        try {
            List<MatchRecord> matches = DatabaseManager.getDetailedMatchesForDate(mainPlayerID, date);
            if (matches.isEmpty()) return;

            List<ReplayDisplay> replays = new ArrayList<>();
            for (MatchRecord m : matches) {
                replays.add(m.toReplayDisplay(mainPlayerID, true));
            }

            WindowUtils.openModal("ReplayList.fxml", "Replays on " + date, true,
                c -> {
                    ReplayList ctrl = (ReplayList) c;
                    ctrl.setSpectatedMode(false);
                    ctrl.setShowOpponent(true);
                    ctrl.setReplays(replays);
                }, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private StackPane createEmptyCell() {
        StackPane cell = new StackPane();
        cell.setPrefSize(CELL_SIZE, CELL_SIZE);
        cell.setMinSize(CELL_SIZE, CELL_SIZE);
        return cell;
    }

    
    private Color interpolateColor(Color from, Color to, double ratio) {
        return new Color(
            clamp(from.getRed() + (to.getRed() - from.getRed()) * ratio),
            clamp(from.getGreen() + (to.getGreen() - from.getGreen()) * ratio),
            clamp(from.getBlue() + (to.getBlue() - from.getBlue()) * ratio),
            1.0);
    }

    private double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}