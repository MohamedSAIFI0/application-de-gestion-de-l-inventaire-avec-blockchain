package com.example.trace_stock.Entities;

public class Alerte {
    private int id;
    private String nomProduit;
    private String message;
    private String dateAlerte;
    private boolean traitee;

    public Alerte(int id, String nomProduit, String message, String dateAlerte, boolean traitee) {
        this.id = id;
        this.nomProduit = nomProduit;
        this.message = message;
        this.dateAlerte = dateAlerte;
        this.traitee = traitee;
    }

    public int getId() { return id; }

    public String getNomProduit() { return nomProduit; }

    public String getMessage() { return message; }

    public String getDateAlerte() { return dateAlerte; }

    public boolean isTraitee() { return traitee; }
}
