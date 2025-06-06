module com.example.trace_stock {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires mysql.connector.j;
    requires jakarta.mail;
    requires spark.core;
    requires java.net.http;
    requires com.google.gson;
    requires okhttp3;
    requires java.sql;
    requires java.desktop;
    requires java.prefs;

    opens com.example.trace_stock to javafx.fxml;
    exports com.example.trace_stock;
    exports com.example.trace_stock.DAO;
    opens com.example.trace_stock.DAO to javafx.fxml;
    exports com.example.trace_stock.Entities;
    opens com.example.trace_stock.Entities to javafx.fxml;
    exports com.example.trace_stock.Controllers;
    opens com.example.trace_stock.Controllers to javafx.fxml;
    exports com.example.trace_stock.Service;
    opens com.example.trace_stock.Service to javafx.fxml;
}
