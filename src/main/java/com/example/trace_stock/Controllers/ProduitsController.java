package com.example.trace_stock.Controllers;

import com.example.trace_stock.DAO.ConnexionBD;
import com.example.trace_stock.Entities.Produit;
import com.example.trace_stock.Service.GanacheBlockchainService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.awt.event.ActionEvent;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class ProduitsController implements Initializable {

    @FXML
    private TableView<Produit> produitsTable;
    @FXML
    private TableColumn<Produit, Integer> colId;
    @FXML
    private TableColumn<Produit, String> colNom;
    @FXML
    private TableColumn<Produit, Integer> colQuantite;
    @FXML
    private TableColumn<Produit, String> colCategorie;
    @FXML
    private TableColumn<Produit, String> colDepot;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private TextField searchField;

    ObservableList<Produit> produitsList = FXCollections.observableArrayList();
    private GanacheBlockchainService blockchainService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colNom.setCellValueFactory(cellData -> cellData.getValue().nomProperty());
        colQuantite.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty().asObject());
        colCategorie.setCellValueFactory(cellData -> cellData.getValue().categorieProperty());
        colDepot.setCellValueFactory(cellData -> cellData.getValue().depotProperty());

        chargerProduits();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> filtrerProduits(newValue));
        btnAjouter.setOnAction(event -> Ajouter_Produit());
        btnModifier.setOnAction(event -> Modifier_Produit());
        btnSupprimer.setOnAction(event -> Supprimer_Produit());

        initializeBlockchain();
    }

    private void initializeBlockchain() {
        try {
            blockchainService = new GanacheBlockchainService();
            if (blockchainService.isConnected()) {
                System.out.println("[BLOCKCHAIN] ✅ Service blockchain initialisé et connecté");
            } else {
                System.out.println("[BLOCKCHAIN] ⚠️ Ganache non accessible - Mode dégradé");
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Erreur initialisation: " + e.getMessage());
            blockchainService = null;
        }
    }

    private void enregistrerActionBlockchain(String action, String produitInfo, int quantite, String details) {
        if (blockchainService != null && blockchainService.isConnected()) {
            try {
                String hash = blockchainService.addInventoryTransaction(produitInfo, action, quantite, details);
                if (hash != null) {
                    System.out.println("[BLOCKCHAIN] ✅ " + action + " enregistré: " + hash.substring(0, 10) + "...");
                } else {
                    System.out.println("[BLOCKCHAIN] ❌ Échec enregistrement " + action);
                }
            } catch (Exception e) {
                System.out.println("[BLOCKCHAIN] ❌ Erreur " + action + ": " + e.getMessage());
            }
        } else {
            System.out.println("[BLOCKCHAIN] ⚠️ Service non disponible pour " + action);
        }
    }

    private void chargerProduits() {
        String sql = "SELECT id, nom, quantite, categorie, depot FROM produit";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            produitsList.clear();
            while (rs.next()) {
                Produit p = new Produit(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("quantite"),
                        rs.getString("categorie"),
                        rs.getString("depot")
                );
                produitsList.add(p);
            }
            produitsTable.setItems(produitsList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement des produits", e.getMessage());
        }
    }

    @FXML
    private void Ajouter_Produit() {
        String nom = showTextInputDialog("Ajouter un produit", "Entrer le nom du produit", "Nom :");
        if (nom == null || nom.isEmpty()) return;

        String quantiteStr = showTextInputDialog("Ajouter un produit", "Entrer la quantité", "Quantité :");
        if (quantiteStr == null || quantiteStr.isEmpty()) return;

        int quantite;
        try {
            quantite = Integer.parseInt(quantiteStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Quantité invalide", "Veuillez entrer un nombre entier valide.");
            return;
        }

        String categorie = showTextInputDialog("Ajouter un produit", "Entrer la catégorie", "Catégorie :");
        if (categorie == null || categorie.isEmpty()) return;

        String depot = showTextInputDialog("Ajouter un produit", "Entrer le dépôt", "Dépôt :");
        if (depot == null || depot.isEmpty()) return;

        try (Connection conn = ConnexionBD.getConnection()) {
            String sql = "INSERT INTO produit(nom, quantite, categorie, depot) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nom);
            stmt.setInt(2, quantite);
            stmt.setString(3, categorie);
            stmt.setString(4, depot);
            stmt.executeUpdate();

            enregistrerActionBlockchain("AJOUT_PRODUIT", nom, quantite, "Catégorie: " + categorie + " | Dépôt: " + depot);
            showAlert(Alert.AlertType.INFORMATION, "Succès", null, "Produit ajouté avec succès !");
            chargerProduits();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout", e.getMessage());
        }
    }

    @FXML
    private void Modifier_Produit() {
        Produit produit = produitsTable.getSelectionModel().getSelectedItem();
        if (produit == null) {
            showAlert(Alert.AlertType.WARNING, "Aucun produit sélectionné", null, "Veuillez sélectionner un produit à modifier.");
            return;
        }

        String ancienEtat = "Ancien: " + produit.getNom() + " (Qté: " + produit.getQuantite() +
                ", Cat: " + produit.getCategorie() + ", Dépôt: " + produit.getDepot() + ")";

        String nom = showTextInputDialog("Modifier le produit", "Modifier le nom", "Nom :", produit.getNom());
        if (nom == null || nom.isEmpty()) return;

        String quantiteStr = showTextInputDialog("Modifier le produit", "Modifier la quantité", "Quantité :", String.valueOf(produit.getQuantite()));
        if (quantiteStr == null || quantiteStr.isEmpty()) return;

        int quantite;
        try {
            quantite = Integer.parseInt(quantiteStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Quantité invalide", "Veuillez entrer un nombre entier valide.");
            return;
        }

        String categorie = showTextInputDialog("Modifier le produit", "Modifier la catégorie", "Catégorie :", produit.getCategorie());
        if (categorie == null || categorie.isEmpty()) return;

        String depot = showTextInputDialog("Modifier le produit", "Modifier le dépôt", "Dépôt :", produit.getDepot());
        if (depot == null || depot.isEmpty()) return;

        try (Connection conn = ConnexionBD.getConnection()) {
            String sql = "UPDATE produit SET nom = ?, quantite = ?, categorie = ?, depot = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nom);
            stmt.setInt(2, quantite);
            stmt.setString(3, categorie);
            stmt.setString(4, depot);
            stmt.setInt(5, produit.getId());
            stmt.executeUpdate();

            String nouveauEtat = "Nouveau: " + nom + " (Qté: " + quantite +
                    ", Cat: " + categorie + ", Dépôt: " + depot + ")";

            enregistrerActionBlockchain("MODIFICATION_PRODUIT", "ID:" + produit.getId() + "-" + nom, quantite, ancienEtat + " | " + nouveauEtat);
            showAlert(Alert.AlertType.INFORMATION, "Succès", null, "Produit modifié avec succès !");
            chargerProduits();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification", e.getMessage());
        }
    }

    @FXML
    private void Supprimer_Produit() {
        Produit produit = produitsTable.getSelectionModel().getSelectedItem();
        if (produit == null) {
            showAlert(Alert.AlertType.WARNING, "Aucun produit sélectionné", null, "Veuillez sélectionner un produit à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer ce produit ?");

        URL css = getClass().getResource("/com/example/trace_stock/CSS/dialogs.css");
        if (css != null) confirm.getDialogPane().getStylesheets().add(css.toExternalForm());
        confirm.getDialogPane().getStyleClass().add("confirmation");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try (Connection conn = ConnexionBD.getConnection()) {
            String sql = "DELETE FROM produit WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, produit.getId());
            stmt.executeUpdate();

            String infoProduit = "Supprimé: " + produit.getNom() + " (ID: " + produit.getId() +
                    ", Qté: " + produit.getQuantite() + ", Cat: " + produit.getCategorie() +
                    ", Dépôt: " + produit.getDepot() + ")";

            enregistrerActionBlockchain("SUPPRESSION_PRODUIT", "ID:" + produit.getId() + "-" + produit.getNom(), produit.getQuantite(), infoProduit);
            showAlert(Alert.AlertType.INFORMATION, "Succès", null, "Produit supprimé avec succès !");
            chargerProduits();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression", e.getMessage());
        }
    }

    private void filtrerProduits(String recherche) {
        if (recherche == null || recherche.isEmpty()) {
            produitsTable.setItems(produitsList);
            return;
        }
        ObservableList<Produit> filtres = FXCollections.observableArrayList();
        for (Produit p : produitsList) {
            if (p.getNom().toLowerCase().contains(recherche.toLowerCase()) ||
                    p.getCategorie().toLowerCase().contains(recherche.toLowerCase()) ||
                    String.valueOf(p.getQuantite()).contains(recherche)) {
                filtres.add(p);
            }
        }
        produitsTable.setItems(filtres);
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        URL css = getClass().getResource("/com/example/trace_stock/CSS/dialogs.css");
        if (css != null) alert.getDialogPane().getStylesheets().add(css.toExternalForm());

        switch (type) {
            case INFORMATION -> alert.getDialogPane().getStyleClass().add("information");
            case CONFIRMATION -> alert.getDialogPane().getStyleClass().add("confirmation");
            case WARNING -> alert.getDialogPane().getStyleClass().add("warning");
            case ERROR -> alert.getDialogPane().getStyleClass().add("error");
        }

        alert.showAndWait();
    }

    private String showTextInputDialog(String title, String header, String content) {
        return showTextInputDialog(title, header, content, "");
    }

    private String showTextInputDialog(String title, String header, String content, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);

        URL css = getClass().getResource("/com/example/trace_stock/CSS/dialogs.css");
        if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());

        dialog.getDialogPane().getStyleClass().add("text-input-dialog");
        return dialog.showAndWait().orElse(null);
    }
}
