package com.example.trace_stock.Entities;

import javafx.beans.property.*;

public class Mouvement {

    private IntegerProperty id;
    private StringProperty produit;
    private StringProperty type;
    private IntegerProperty quantite;
    private StringProperty date;
    private IntegerProperty utilisateurId;
    private StringProperty utilisateurNom;

    public Mouvement(int id, String produit, String type, int quantite, String date, int utilisateurId, String utilisateurNom) {
        this.id = new SimpleIntegerProperty(id);
        this.produit = new SimpleStringProperty(produit);
        this.type = new SimpleStringProperty(type);
        this.quantite = new SimpleIntegerProperty(quantite);
        this.date = new SimpleStringProperty(date);
        this.utilisateurId = new SimpleIntegerProperty(utilisateurId);
        this.utilisateurNom = new SimpleStringProperty(utilisateurNom != null ? utilisateurNom : "N/A");
    }

    // Constructeur existant pour compatibilité
    public Mouvement(int id, String produit, String type, int quantite, String date) {
        this(id, produit, type, quantite, date, 0, "N/A");
    }

    // Getters et setters existants
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getProduit() { return produit.get(); }
    public void setProduit(String produit) { this.produit.set(produit); }
    public StringProperty produitProperty() { return produit; }

    public String getType() { return type.get(); }
    public void setType(String type) { this.type.set(type); }
    public StringProperty typeProperty() { return type; }

    public int getQuantite() { return quantite.get(); }
    public void setQuantite(int quantite) { this.quantite.set(quantite); }
    public IntegerProperty quantiteProperty() { return quantite; }

    public String getDate() { return date.get(); }
    public void setDate(String date) { this.date.set(date); }
    public StringProperty dateProperty() { return date; }

    // Nouveaux getters et setters pour utilisateur
    public int getUtilisateurId() { return utilisateurId.get(); }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId.set(utilisateurId); }
    public IntegerProperty utilisateurIdProperty() { return utilisateurId; }

    public String getUtilisateurNom() { return utilisateurNom.get(); }
    public void setUtilisateurNom(String utilisateurNom) { this.utilisateurNom.set(utilisateurNom != null ? utilisateurNom : "N/A"); }
    public StringProperty utilisateurNomProperty() { return utilisateurNom; }
}