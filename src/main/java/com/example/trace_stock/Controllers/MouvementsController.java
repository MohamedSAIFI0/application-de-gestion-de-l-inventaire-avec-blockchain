package com.example.trace_stock.Controllers;

import com.example.trace_stock.DAO.ConnexionBD;
import com.example.trace_stock.Entities.Mouvement;
import com.example.trace_stock.Service.GanacheBlockchainService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MouvementsController implements Initializable {

    @FXML private TableView<Mouvement> mouvementsTable;
    @FXML private TableColumn<Mouvement, Integer> colId;
    @FXML private TableColumn<Mouvement, String> colProduit;
    @FXML private TableColumn<Mouvement, String> colType;
    @FXML private TableColumn<Mouvement, Integer> colQuantite;
    @FXML private TableColumn<Mouvement, String> colDate;
    @FXML private TableColumn<Mouvement, String> colUtilisateur;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private TextField searchField;

    private final ObservableList<Mouvement> mouvementsList = FXCollections.observableArrayList();
    private GanacheBlockchainService blockchainService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colProduit.setCellValueFactory(cellData -> cellData.getValue().produitProperty());
        colType.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        colQuantite.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty().asObject());
        colDate.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        colUtilisateur.setCellValueFactory(cellData -> cellData.getValue().utilisateurNomProperty());

        chargerMouvements();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> filtrerMouvements(newValue));
        initializeBlockchain();
    }

    private int getCurrentUserId() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.example.trace_stock.Controllers.Login.class);
        int userId = prefs.getInt("utilisateurId", 0);
        System.out.println("ID utilisateur récupéré des préférences : " + userId);
        return userId;
    }

    private boolean utilisateurExiste(int utilisateurId) {
        if (utilisateurId <= 0) return false;

        String sql = "SELECT COUNT(*) FROM utilisateur WHERE id = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de l'utilisateur : " + e.getMessage());
        }
        return false;
    }

    private String getNomUtilisateur(int utilisateurId) {
        String sql = "SELECT nom FROM utilisateur WHERE id = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nom");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du nom utilisateur : " + e.getMessage());
        }
        return "Utilisateur inconnu";
    }

    private void initializeBlockchain() {
        try {
            blockchainService = new GanacheBlockchainService();
            if (blockchainService.isConnected()) {
                System.out.println("[BLOCKCHAIN-MOUVEMENTS] ✅ Service blockchain initialisé");
            } else {
                System.out.println("[BLOCKCHAIN-MOUVEMENTS] ⚠️ Ganache non accessible - Mode dégradé");
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN-MOUVEMENTS] ❌ Erreur initialisation: " + e.getMessage());
            blockchainService = null;
        }
    }

    private void enregistrerMouvementBlockchain(String action, String produit, String type, int quantite, String details) {
        if (blockchainService != null && blockchainService.isConnected()) {
            try {
                String typeBlockchain = convertirTypeBlockchain(type);
                String hash = blockchainService.addInventoryTransaction(
                        produit,
                        action + "_" + typeBlockchain,
                        quantite,
                        details + " | Date: " + LocalDate.now()
                );

                if (hash != null) {
                    System.out.println("[BLOCKCHAIN-MOUVEMENTS] ✅ " + action + " " + type + " enregistré: " + hash.substring(0, 10) + "...");
                } else {
                    System.out.println("[BLOCKCHAIN-MOUVEMENTS] ❌ Échec enregistrement " + action + " " + type);
                }
            } catch (Exception e) {
                System.out.println("[BLOCKCHAIN-MOUVEMENTS] ❌ Erreur " + action + ": " + e.getMessage());
            }
        } else {
            System.out.println("[BLOCKCHAIN-MOUVEMENTS] ⚠️ Service non disponible pour " + action);
        }
    }

    private String convertirTypeBlockchain(String type) {
        switch (type) {
            case "Entrée": return "ENTREE";
            case "Sortie": return "SORTIE";
            case "Transfert": return "TRANSFERT";
            default: return type.toUpperCase();
        }
    }

    private void chargerMouvements() {
        String sql = "SELECT m.id, p.nom as produit_nom, m.type, m.quantite, m.date, " +
                "m.utilisateur_id, u.nom as utilisateur_nom " +
                "FROM mouvement m " +
                "JOIN produit p ON m.produit_id = p.id " +
                "LEFT JOIN utilisateur u ON m.utilisateur_id = u.id " +
                "ORDER BY m.date DESC";

        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            mouvementsList.clear();

            while (rs.next()) {
                int utilisateurId = rs.getInt("utilisateur_id");
                String utilisateurNom = rs.getString("utilisateur_nom");

                // Si le nom utilisateur est null, on met un nom par défaut
                if (utilisateurNom == null || utilisateurNom.trim().isEmpty()) {
                    if (utilisateurId > 0) {
                        utilisateurNom = "Utilisateur ID: " + utilisateurId;
                    } else {
                        utilisateurNom = "Utilisateur inconnu";
                    }
                }

                Mouvement m = new Mouvement(
                        rs.getInt("id"),
                        rs.getString("produit_nom"),
                        rs.getString("type"),
                        rs.getInt("quantite"),
                        rs.getDate("date").toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        utilisateurId,
                        utilisateurNom
                );
                mouvementsList.add(m);
            }

            mouvementsTable.setItems(mouvementsList);

        } catch (SQLException e) {
            e.printStackTrace();
            afficherErreur("Erreur de chargement", "Impossible de charger les mouvements.", e.getMessage());
        }
    }

    @FXML
    private void ajouterMouvement() {
        try {
            // Vérification de l'utilisateur connecté
            int currentUserId = getCurrentUserId();
            if (currentUserId == 0) {
                afficherErreur("Utilisateur non connecté",
                        "Aucun utilisateur connecté trouvé.",
                        "Veuillez vous reconnecter à l'application.");
                return;
            }

            if (!utilisateurExiste(currentUserId)) {
                afficherErreur("Utilisateur invalide",
                        "L'utilisateur connecté n'existe plus dans la base de données.",
                        "Veuillez vous reconnecter.");
                return;
            }

            List<String> produits = getListeProduits();
            if (produits.isEmpty()) {
                afficherErreur("Aucun produit", "La base ne contient aucun produit.", "");
                return;
            }

            // Sélection du produit
            ChoiceDialog<String> produitDialog = new ChoiceDialog<>(produits.get(0), produits);
            produitDialog.setTitle("Ajouter un mouvement");
            produitDialog.setHeaderText("Sélectionnez un produit");
            produitDialog.setContentText("Produit :");

            try {
                produitDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
                produitDialog.getDialogPane().getStyleClass().add("text-input-dialog");
            } catch (Exception e) {
                System.out.println("Impossible de charger le CSS des dialogues");
            }

            String produit = produitDialog.showAndWait().orElse(null);
            if (produit == null) return;

            // Sélection du type
            ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Entrée", "Entrée", "Sortie", "Transfert");
            typeDialog.setTitle("Ajouter un mouvement");
            typeDialog.setHeaderText("Sélectionnez le type de mouvement");
            typeDialog.setContentText("Type :");

            try {
                typeDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
                typeDialog.getDialogPane().getStyleClass().add("text-input-dialog");
            } catch (Exception e) {
                System.out.println("Impossible de charger le CSS des dialogues");
            }

            String type = typeDialog.showAndWait().orElse(null);
            if (type == null) return;

            // Saisie de la quantité
            TextInputDialog quantiteDialog = new TextInputDialog();
            quantiteDialog.setTitle("Ajouter un mouvement");
            quantiteDialog.setHeaderText("Entrez la quantité (entier positif)");
            quantiteDialog.setContentText("Quantité :");

            try {
                quantiteDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
                quantiteDialog.getDialogPane().getStyleClass().add("text-input-dialog");
            } catch (Exception e) {
                System.out.println("Impossible de charger le CSS des dialogues");
            }

            String qteStr = quantiteDialog.showAndWait().orElse(null);
            if (qteStr == null || qteStr.isEmpty()) return;

            int quantite;
            try {
                quantite = Integer.parseInt(qteStr);
                if (quantite <= 0) {
                    afficherAvertissement("Quantité invalide", "La quantité doit être un entier strictement positif.");
                    return;
                }
            } catch (NumberFormatException e) {
                afficherAvertissement("Quantité invalide", "Veuillez entrer un nombre entier valide.");
                return;
            }

            LocalDate dateMouvement = LocalDate.now();
            int produitId = getProduitIdByName(produit);
            if (produitId == -1) {
                afficherErreur("Produit inconnu", "Le produit sélectionné n'existe pas.", "");
                return;
            }

            // Insertion en base de données
            try (Connection conn = ConnexionBD.getConnection()) {
                conn.setAutoCommit(false);

                try (PreparedStatement stmtInsert = conn.prepareStatement(
                        "INSERT INTO mouvement(produit_id, type, quantite, date, utilisateur_id) VALUES (?, ?, ?, ?, ?)")) {
                    stmtInsert.setInt(1, produitId);
                    stmtInsert.setString(2, type);
                    stmtInsert.setInt(3, quantite);
                    stmtInsert.setDate(4, Date.valueOf(dateMouvement));
                    stmtInsert.setInt(5, currentUserId);

                    System.out.println("Insertion mouvement - Utilisateur ID: " + currentUserId);
                    stmtInsert.executeUpdate();
                }

                // Mise à jour du stock
                if (type.equals("Entrée")) {
                    modifierQuantiteProduit(conn, produitId, quantite);
                } else if (type.equals("Sortie")) {
                    modifierQuantiteProduit(conn, produitId, -quantite);
                }

                conn.commit();

                // Enregistrement blockchain
                String nomUtilisateur = getNomUtilisateur(currentUserId);
                String details = "Produit: " + produit + " | Quantité: " + quantite + " | Utilisateur: " + nomUtilisateur;
                if (type.equals("Entrée")) {
                    details += " | Stock augmenté de " + quantite;
                } else if (type.equals("Sortie")) {
                    details += " | Stock diminué de " + quantite;
                } else {
                    details += " | Transfert (pas d'impact stock)";
                }

                enregistrerMouvementBlockchain("AJOUT_MOUVEMENT", produit, type, quantite, details);
            }

            afficherInformation("Succès", "Mouvement ajouté avec succès par " + getNomUtilisateur(currentUserId) + " !");
            chargerMouvements();

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur d'ajout", "Impossible d'ajouter le mouvement.", e.getMessage());
        }
    }

    @FXML
    private void modifierMouvement() {
        Mouvement mouvement = mouvementsTable.getSelectionModel().getSelectedItem();
        if (mouvement == null) {
            afficherAvertissement("Aucun mouvement sélectionné", "Veuillez sélectionner un mouvement à modifier.");
            return;
        }

        // Vérification de l'utilisateur connecté
        int currentUserId = getCurrentUserId();
        if (currentUserId == 0 || !utilisateurExiste(currentUserId)) {
            afficherErreur("Utilisateur non connecté", "Veuillez vous reconnecter.", "");
            return;
        }

        String ancienEtat = "Ancien: " + mouvement.getProduit() + " | " + mouvement.getType() +
                " | Qté: " + mouvement.getQuantite() + " | Date: " + mouvement.getDate();

        try {
            ChoiceDialog<String> typeDialog = new ChoiceDialog<>(mouvement.getType(), "Entrée", "Sortie", "Transfert");
            typeDialog.setTitle("Modifier le mouvement");
            typeDialog.setHeaderText("Modifier le type de mouvement");
            typeDialog.setContentText("Type :");

            try {
                typeDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
                typeDialog.getDialogPane().getStyleClass().add("text-input-dialog");
            } catch (Exception e) {
                System.out.println("Impossible de charger le CSS des dialogues");
            }

            String nouveauType = typeDialog.showAndWait().orElse(null);
            if (nouveauType == null) return;

            TextInputDialog quantiteDialog = new TextInputDialog(String.valueOf(mouvement.getQuantite()));
            quantiteDialog.setTitle("Modifier le mouvement");
            quantiteDialog.setHeaderText("Modifier la quantité (entier positif)");
            quantiteDialog.setContentText("Quantité :");

            try {
                quantiteDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
                quantiteDialog.getDialogPane().getStyleClass().add("text-input-dialog");
            } catch (Exception e) {
                System.out.println("Impossible de charger le CSS des dialogues");
            }

            String qteStr = quantiteDialog.showAndWait().orElse(null);
            if (qteStr == null) return;

            int nouvelleQuantite;
            try {
                nouvelleQuantite = Integer.parseInt(qteStr);
                if (nouvelleQuantite <= 0) {
                    afficherAvertissement("Quantité invalide", "La quantité doit être un entier strictement positif.");
                    return;
                }
            } catch (NumberFormatException e) {
                afficherAvertissement("Quantité invalide", "Veuillez entrer un nombre entier valide.");
                return;
            }

            int produitId = getProduitIdByName(mouvement.getProduit());
            if (produitId == -1) {
                afficherErreur("Produit inconnu", "Le produit sélectionné n'existe pas.", "");
                return;
            }

            try (Connection conn = ConnexionBD.getConnection()) {
                conn.setAutoCommit(false);

                // Annuler l'ancien mouvement
                if (mouvement.getType().equals("Entrée")) {
                    modifierQuantiteProduit(conn, produitId, -mouvement.getQuantite());
                } else if (mouvement.getType().equals("Sortie")) {
                    modifierQuantiteProduit(conn, produitId, mouvement.getQuantite());
                }

                // Mettre à jour le mouvement
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE mouvement SET type = ?, quantite = ? WHERE id = ?")) {
                    stmt.setString(1, nouveauType);
                    stmt.setInt(2, nouvelleQuantite);
                    stmt.setInt(3, mouvement.getId());
                    stmt.executeUpdate();
                }

                // Appliquer le nouveau mouvement
                if (nouveauType.equals("Entrée")) {
                    modifierQuantiteProduit(conn, produitId, nouvelleQuantite);
                } else if (nouveauType.equals("Sortie")) {
                    modifierQuantiteProduit(conn, produitId, -nouvelleQuantite);
                }

                conn.commit();

                String nouveauEtat = "Nouveau: " + mouvement.getProduit() + " | " + nouveauType +
                        " | Qté: " + nouvelleQuantite + " | Date: " + LocalDate.now();

                enregistrerMouvementBlockchain(
                        "MODIFICATION_MOUVEMENT",
                        "ID:" + mouvement.getId() + "-" + mouvement.getProduit(),
                        nouveauType,
                        nouvelleQuantite,
                        ancienEtat + " | " + nouveauEtat
                );
            }

            afficherInformation("Succès", "Mouvement modifié avec succès !");
            chargerMouvements();

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur de modification", "Impossible de modifier le mouvement.", e.getMessage());
        }
    }

    @FXML
    private void supprimerMouvement() {
        Mouvement mouvement = mouvementsTable.getSelectionModel().getSelectedItem();
        if (mouvement == null) {
            afficherAvertissement("Aucun mouvement sélectionné", "Veuillez sélectionner un mouvement à supprimer.");
            return;
        }

        // Vérification de l'utilisateur connecté
        int currentUserId = getCurrentUserId();
        if (currentUserId == 0 || !utilisateurExiste(currentUserId)) {
            afficherErreur("Utilisateur non connecté", "Veuillez vous reconnecter.", "");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer ce mouvement ?");

        try {
            confirm.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            confirm.getDialogPane().getStyleClass().add("confirmation");
        } catch (Exception e) {
            System.out.println("Impossible de charger le CSS des dialogues");
        }

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        String infoMouvement = "Supprimé: " + mouvement.getProduit() + " | " + mouvement.getType() +
                " | Qté: " + mouvement.getQuantite() + " | Date: " + mouvement.getDate() +
                " | ID: " + mouvement.getId();

        try (Connection conn = ConnexionBD.getConnection()) {
            conn.setAutoCommit(false);

            int produitId = getProduitIdByName(mouvement.getProduit());
            if (produitId == -1) {
                afficherErreur("Produit inconnu", "Le produit sélectionné n'existe pas.", "");
                return;
            }

            // Annuler l'impact du mouvement sur le stock
            if (mouvement.getType().equals("Entrée")) {
                modifierQuantiteProduit(conn, produitId, -mouvement.getQuantite());
            } else if (mouvement.getType().equals("Sortie")) {
                modifierQuantiteProduit(conn, produitId, mouvement.getQuantite());
            }

            // Supprimer le mouvement
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mouvement WHERE id = ?")) {
                stmt.setInt(1, mouvement.getId());
                stmt.executeUpdate();
            }

            conn.commit();

            enregistrerMouvementBlockchain(
                    "SUPPRESSION_MOUVEMENT",
                    "ID:" + mouvement.getId() + "-" + mouvement.getProduit(),
                    mouvement.getType(),
                    mouvement.getQuantite(),
                    infoMouvement
            );

            afficherInformation("Succès", "Mouvement supprimé avec succès !");
            chargerMouvements();

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur de suppression", "Impossible de supprimer le mouvement.", e.getMessage());
        }
    }

    private void modifierQuantiteProduit(Connection conn, int produitId, int delta) throws SQLException {
        String sql = "UPDATE produit SET quantite = quantite + ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, produitId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Produit non trouvé avec id=" + produitId);
            }
        }
    }

    private void filtrerMouvements(String recherche) {
        if (recherche == null || recherche.trim().isEmpty()) {
            mouvementsTable.setItems(mouvementsList);
            return;
        }
        String rechercheLower = recherche.toLowerCase().trim();
        ObservableList<Mouvement> filtres = FXCollections.observableArrayList();

        for (Mouvement m : mouvementsList) {
            if (m.getProduit().toLowerCase().contains(rechercheLower) ||
                    m.getType().toLowerCase().contains(rechercheLower) ||
                    String.valueOf(m.getQuantite()).contains(rechercheLower) ||
                    m.getDate().contains(rechercheLower) ||
                    m.getUtilisateurNom().toLowerCase().contains(rechercheLower)) {
                filtres.add(m);
            }
        }

        mouvementsTable.setItems(filtres);
    }

    private int getProduitIdByName(String nomProduit) throws SQLException {
        String sql = "SELECT id FROM produit WHERE nom = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomProduit);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }

    private List<String> getListeProduits() throws SQLException {
        List<String> produits = new ArrayList<>();
        String sql = "SELECT nom FROM produit";
        try (Connection conn = ConnexionBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                produits.add(rs.getString("nom"));
            }
        }
        return produits;
    }

    private void afficherInformation(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);

        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("information");
        } catch (Exception e) {
            System.out.println("Impossible de charger le CSS des dialogues");
        }

        alert.showAndWait();
    }

    private void afficherErreur(String titre, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(header);
        alert.setContentText(message);

        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("error");
        } catch (Exception e) {
            System.out.println("Impossible de charger le CSS des dialogues");
        }

        alert.showAndWait();
    }

    private void afficherAvertissement(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);

        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("warning");
        } catch (Exception e) {
            System.out.println("Impossible de charger le CSS des dialogues");
        }

        alert.showAndWait();
    }
}