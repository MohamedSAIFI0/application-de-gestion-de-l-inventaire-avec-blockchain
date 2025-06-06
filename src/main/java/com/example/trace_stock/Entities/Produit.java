package com.example.trace_stock.Entities;

import javafx.beans.property.*;

public class Produit {

    private IntegerProperty id;
    private StringProperty nom;
    private IntegerProperty quantite;
    private StringProperty categorie;
    private StringProperty depot;


    public Produit(int id, String nom, int quantite, String categorie, String depot) {
        this.id = new SimpleIntegerProperty(id);
        this.nom = new SimpleStringProperty(nom);
        this.quantite = new SimpleIntegerProperty(quantite);
        this.categorie = new SimpleStringProperty(categorie);
        this.depot = new SimpleStringProperty(depot);
    }

    public Produit(String nom, int quantite, String categorie, String depot) {
        this.id = new SimpleIntegerProperty(0);
        this.nom = new SimpleStringProperty(nom);
        this.quantite = new SimpleIntegerProperty(quantite);
        this.categorie = new SimpleStringProperty(categorie);
        this.depot = new SimpleStringProperty(depot);
    }



    public String getDepot() {
        return depot.get();
    }

    public void setDepot(String depot) {
        this.depot.set(depot);
    }

    public StringProperty depotProperty() {
        return depot;
    }


    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getNom() {
        return nom.get();
    }

    public void setNom(String nom) {
        this.nom.set(nom);
    }

    public StringProperty nomProperty() {
        return nom;
    }

    public int getQuantite() {
        return quantite.get();
    }

    public void setQuantite(int quantite) {
        this.quantite.set(quantite);
    }

    public IntegerProperty quantiteProperty() {
        return quantite;
    }

    public String getCategorie() {
        return categorie.get();
    }

    public void setCategorie(String categorie) {
        this.categorie.set(categorie);
    }

    public StringProperty categorieProperty() {
        return categorie;
    }
}
