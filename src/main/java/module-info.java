module com.example.pdfreader {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.pdfbox;
    requires java.desktop;
    requires javafx.swing;
    requires tabula;
    requires itextpdf;
    requires tess4j;
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires java.management;
    requires org.controlsfx.controls;
    requires org.apache.commons.collections4;
    requires org.slf4j;
    requires javafx.media;
    requires jlayer;


    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.naming;
    requires java.sql;
    requires com.fasterxml.jackson.core;

    opens com.example.pdfreader to javafx.fxml,org.hibernate.orm.core;
    exports com.example.pdfreader;
    exports com.example.pdfreader.enums to com.fasterxml.jackson.databind;
    exports com.example.pdfreader.Helpers;
    exports com.example.pdfreader.Controllers;
    exports com.example.pdfreader.Interfaces;
    exports com.example.pdfreader.MyCustomEvents;
    exports com.example.pdfreader.Controllers.States;
    exports com.example.pdfreader.Entities;
    exports com.example.pdfreader.Entities.Attributes;
    exports com.example.pdfreader.DAOs;
    exports com.example.pdfreader.DTOs;
    exports com.example.pdfreader.MyCustomEvents.DBError;
    opens com.example.pdfreader.Controllers to javafx.fxml;
    opens com.example.pdfreader.DAOs to org.hibernate.orm.core;
    opens com.example.pdfreader.Entities to javafx.fxml,org.hibernate.orm.core;
    opens com.example.pdfreader.Entities.Attributes to org.hibernate.orm.core;
    opens com.example.pdfreader.Helpers to javafx.fxml, org.hibernate.orm.core;
}