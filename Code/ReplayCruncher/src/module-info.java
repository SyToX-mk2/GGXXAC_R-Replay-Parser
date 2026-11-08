module ReplayCruncher {
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.fxml;
	requires java.sql;
	requires javafx.base;
	requires java.desktop;
	
	opens application to javafx.graphics, javafx.fxml, javafx.base;
	opens controllers to javafx.fxml;
}
