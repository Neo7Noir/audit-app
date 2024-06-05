module com.example.auditapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.auditapp to javafx.fxml;
    exports com.example.auditapp;
}