package com.example.trace_stock.Controllers;

import com.example.trace_stock.DAO.ConnexionBD;
import com.example.trace_stock.Entities.Alerte;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AlertesController {

    @FXML
    private TableView<Alerte> alertesTable;

    @FXML
    private TableColumn<Alerte, Integer> colId;

    @FXML
    private TableColumn<Alerte, String> colProduit;

    @FXML
    private TableColumn<Alerte, String> colMessage;

    @FXML
    private TableColumn<Alerte, String> colDate;

    @FXML
    private TableColumn<Alerte, Boolean> colTraitee;

    private ObservableList<Alerte> alertesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProduit.setCellValueFactory(new PropertyValueFactory<>("nomProduit"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateAlerte"));
        colTraitee.setCellValueFactory(new PropertyValueFactory<>("traitee"));
        verifierEtAjouterAlertes();
        mettreAJourAlertesTraitees();
        chargerAlertes();
    }

    private void chargerAlertes() {
        Connection conn = ConnexionBD.getConnection();
        if (conn == null) return;

        String requete = "SELECT a.id, p.nom AS nom_produit, a.message, a.date_alerte, a.traitee " +
                "FROM alertes a " +
                "JOIN produit p ON a.id_produit = p.id";

        try (PreparedStatement stmt = conn.prepareStatement(requete);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Alerte alerte = new Alerte(
                        rs.getInt("id"),
                        rs.getString("nom_produit"),
                        rs.getString("message"),
                        rs.getString("date_alerte"),
                        rs.getBoolean("traitee")
                );

                alertesList.add(alerte);
            }

            alertesTable.setItems(alertesList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void verifierEtAjouterAlertes() {
        Connection conn = ConnexionBD.getConnection();
        if (conn == null) return;

        String requeteProduits = "SELECT id, nom, quantite FROM produit";

        try (PreparedStatement stmt = conn.prepareStatement(requeteProduits);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int idProduit = rs.getInt("id");
                String nom = rs.getString("nom");
                int quantite = rs.getInt("quantite");

                // Si quantité ≤ seuil, créer une alerte
                if (quantite <= 10) {

                    String verifier = "SELECT COUNT(*) FROM alertes WHERE id_produit = ? AND traitee = 0";
                    try (PreparedStatement checkStmt = conn.prepareStatement(verifier)) {
                        checkStmt.setInt(1, idProduit);
                        ResultSet rsCheck = checkStmt.executeQuery();
                        rsCheck.next();
                        int count = rsCheck.getInt(1);

                        if (count == 0) {
                            String message = "La quantité du produit \"" + nom + "\" est presque épuisée (" + quantite + " <= " + 10 + ")";
                            String insert = "INSERT INTO alertes (id_produit, message, traitee) VALUES (?, ?, 0)";
                            try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                                insertStmt.setInt(1, idProduit);
                                insertStmt.setString(2, message);
                                insertStmt.executeUpdate();
                                System.out.println("Alerte ajoutée pour le produit : " + nom);
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void mettreAJourAlertesTraitees() {
        Connection conn = ConnexionBD.getConnection();
        if (conn == null) return;

        String requete = "UPDATE alertes " +
                "SET traitee = 1 " +
                "WHERE traitee = 0 AND id_produit IN (" +
                "    SELECT id FROM produit WHERE quantite > 10" +
                ")";

        try (PreparedStatement stmt = conn.prepareStatement(requete)) {
            int lignesModifiees = stmt.executeUpdate();
            if (lignesModifiees > 0) {
                System.out.println(lignesModifiees + " alertes ont été marquées comme traitées (produit > 10).");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
