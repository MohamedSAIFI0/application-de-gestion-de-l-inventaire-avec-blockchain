package com.example.trace_stock.Entities;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BlockchainTransaction {
    private final StringProperty hash;
    private final StringProperty produit;
    private final StringProperty quantite;
    private final StringProperty date;

    public BlockchainTransaction(String hash, String produit, String quantite, String date) {
        this.hash = new SimpleStringProperty(hash);
        this.produit = new SimpleStringProperty(produit);
        this.quantite = new SimpleStringProperty(quantite);
        this.date = new SimpleStringProperty(date);
    }


    public String getHash() { return hash.get(); }
    public String getProduit() { return produit.get(); }
    public String getQuantite() { return quantite.get(); }
    public String getDate() { return date.get(); }

    public StringProperty hashProperty() { return hash; }
    public StringProperty produitProperty() { return produit; }
    public StringProperty quantiteProperty() { return quantite; }
    public StringProperty dateProperty() { return date; }


    public void setHash(String hash) { this.hash.set(hash); }
    public void setProduit(String produit) { this.produit.set(produit); }
    public void setQuantite(String quantite) { this.quantite.set(quantite); }
    public void setDate(String date) { this.date.set(date); }
}