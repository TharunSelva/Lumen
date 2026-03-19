module com.energyapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;
    requires org.xerial.sqlitejdbc;
    requires com.google.gson;
    requires freetts;
    opens com.energyapp to javafx.fxml;
    opens com.energyapp.controller to javafx.fxml;
    opens com.energyapp.db to com.google.gson;
    exports com.energyapp;
}