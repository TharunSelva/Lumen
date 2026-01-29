module com.energyapp {
    requires javafx.controls;
    requires javafx.fxml;
    opens com.energyapp to javafx.fxml;
    opens com.energyapp.controller to javafx.fxml;
    exports com.energyapp;
}