package com.example.trace_stock.Entities;

import javafx.beans.property.*;

public class Utilisateur {
    private final IntegerProperty id;
    private final StringProperty nom;
    private final StringProperty email;
    private final StringProperty role;

    public Utilisateur(int id, String nom, String email, String role) {
        this.id = new SimpleIntegerProperty(id);
        this.nom = new SimpleStringProperty(nom);
        this.email = new SimpleStringProperty(email);
        this.role = new SimpleStringProperty(role);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getNom() { return nom.get(); }
    public StringProperty nomProperty() { return nom; }

    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }

    public String getRole() { return role.get(); }
    public StringProperty roleProperty() { return role; }
}
