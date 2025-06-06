package com.example.trace_stock.Controllers;

import com.example.trace_stock.DAO.ConnexionBD;
import com.example.trace_stock.Entities.Utilisateur;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class UtilisateursController implements Initializable {

    @FXML private TableView<Utilisateur> utilisateursTable;
    @FXML private TableColumn<Utilisateur, Integer> colId;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colRole;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private TextField searchField;

    ObservableList<Utilisateur> utilisateursList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colNom.setCellValueFactory(cellData -> cellData.getValue().nomProperty());
        colEmail.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        colRole.setCellValueFactory(cellData -> cellData.getValue().roleProperty());


        chargerUtilisateurs();
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filtrerUtilisateurs(newValue));
    }

    private void chargerUtilisateurs() {
        String sql = "SELECT id, nom, email, role FROM utilisateur";

        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            utilisateursList.clear();

            while (rs.next()) {
                Utilisateur u = new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("role")
                );
                utilisateursList.add(u);
            }

            utilisateursTable.setItems(utilisateursList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void Ajouter_Utilisateur() {

        TextInputDialog nomDialog = new TextInputDialog();
        nomDialog.setTitle("Ajouter un utilisateur");
        nomDialog.setHeaderText("Entrer le nom de l'utilisateur");
        nomDialog.setContentText("Nom :");


        nomDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
        nomDialog.getDialogPane().getStyleClass().add("text-input-dialog");

        String nom = nomDialog.showAndWait().orElse(null);
        if (nom == null || nom.isEmpty()) return;


        TextInputDialog emailDialog = new TextInputDialog();
        emailDialog.setTitle("Ajouter un utilisateur");
        emailDialog.setHeaderText("Entrer l'email de l'utilisateur");
        emailDialog.setContentText("Email :");


        emailDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
        emailDialog.getDialogPane().getStyleClass().add("text-input-dialog");

        String email = emailDialog.showAndWait().orElse(null);
        if (email == null || email.isEmpty()) return;


        TextInputDialog mdpDialog = new TextInputDialog();
        mdpDialog.setTitle("Ajouter un utilisateur");
        mdpDialog.setHeaderText("Entrer le mot de passe");
        mdpDialog.setContentText("Mot de passe :");


        mdpDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
        mdpDialog.getDialogPane().getStyleClass().add("text-input-dialog");

        String motDePasse = mdpDialog.showAndWait().orElse(null);
        if (motDePasse == null || motDePasse.isEmpty()) return;


        ChoiceDialog<String> roleDialog = new ChoiceDialog<>("user", "user", "admin");
        roleDialog.setTitle("Ajouter un utilisateur");
        roleDialog.setHeaderText("Sélectionner le rôle");
        roleDialog.setContentText("Rôle :");


        roleDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
        roleDialog.getDialogPane().getStyleClass().add("text-input-dialog");

        String role = roleDialog.showAndWait().orElse(null);
        if (role == null || role.isEmpty()) return;


        try {
            Connection conn = ConnexionBD.getConnection();
            String sql = "INSERT INTO utilisateur(nom, email, mot_de_passe, role) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nom);
            stmt.setString(2, email);
            stmt.setString(3, motDePasse);
            stmt.setString(4, role);
            stmt.executeUpdate();

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succès");
            success.setHeaderText(null);
            success.setContentText("Utilisateur ajouté avec succès !");


            success.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            success.getDialogPane().getStyleClass().add("information");

            success.showAndWait();

            chargerUtilisateurs();
        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Erreur lors de l'ajout");
            error.setContentText(e.getMessage());


            error.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            error.getDialogPane().getStyleClass().add("error");

            error.showAndWait();
        }
    }

    @FXML
    private void Modifier_Utilisateur() {
        Utilisateur utilisateur = utilisateursTable.getSelectionModel().getSelectedItem();
        if (utilisateur == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aucun utilisateur sélectionné");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez sélectionner un utilisateur à modifier.");

            // ✅ Appliquer le style moderne
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("warning");

            alert.showAndWait();
            return;
        }

        // Demande nouveau nom
        TextInputDialog nomDialog = new TextInputDialog(utilisateur.getNom());
        nomDialog.setTitle("Modifier l'utilisateur");
        nomDialog.setHeaderText("Modifier le nom de l'utilisateur");
        nomDialog.setContentText("Nom :");

        // ✅ Appliquer le style moderne
        nomDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
        nomDialog.getDialogPane().getStyleClass().add("text-input-dialog");

        String nom = nomDialog.showAndWait().orElse(null);
        if (nom == null || nom.isEmpty()) return;

        // Demande nouvel email
        TextInputDialog emailDialog = new TextInputDialog(utilisateur.getEmail());
        emailDialog.setTitle("Modifier l'utilisateur");
        emailDialog.setHeaderText("Modifier l'email de l'utilisateur");
        emailDialog.setContentText("Email :");

        // ✅ Appliquer le style moderne
        emailDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
        emailDialog.getDialogPane().getStyleClass().add("text-input-dialog");

        String email = emailDialog.showAndWait().orElse(null);
        if (email == null || email.isEmpty()) return;

        // Demande nouveau rôle
        ChoiceDialog<String> roleDialog = new ChoiceDialog<>(utilisateur.getRole(), "user", "admin");
        roleDialog.setTitle("Modifier l'utilisateur");
        roleDialog.setHeaderText("Modifier le rôle");
        roleDialog.setContentText("Rôle :");

        // ✅ Appliquer le style moderne
        roleDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
        roleDialog.getDialogPane().getStyleClass().add("text-input-dialog");

        String role = roleDialog.showAndWait().orElse(null);
        if (role == null || role.isEmpty()) return;

        // Mise à jour dans la base et blockchain
        try {
            Connection conn = ConnexionBD.getConnection();
            String sql = "UPDATE utilisateur SET nom = ?, email = ?, role = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nom);
            stmt.setString(2, email);
            stmt.setString(3, role);
            stmt.setInt(4, utilisateur.getId());
            stmt.executeUpdate();

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succès");
            success.setHeaderText(null);
            success.setContentText("Utilisateur modifié avec succès !");

            // ✅ Appliquer le style moderne
            success.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            success.getDialogPane().getStyleClass().add("information");

            success.showAndWait();
            chargerUtilisateurs();

        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Erreur lors de la modification");
            error.setContentText(e.getMessage());

            // ✅ Appliquer le style moderne
            error.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            error.getDialogPane().getStyleClass().add("error");

            error.showAndWait();
        }
    }

    @FXML
    private void Supprimer_Utilisateur() {
        Utilisateur utilisateur = utilisateursTable.getSelectionModel().getSelectedItem();
        if (utilisateur == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aucun utilisateur sélectionné");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez sélectionner un utilisateur à supprimer.");

            // ✅ Appliquer le style moderne
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("warning");

            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer cet utilisateur ?");

        // ✅ Appliquer le style moderne
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
        confirm.getDialogPane().getStyleClass().add("confirmation");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            Connection conn = ConnexionBD.getConnection();
            String sql = "DELETE FROM utilisateur WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, utilisateur.getId());
            stmt.executeUpdate();

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succès");
            success.setHeaderText(null);
            success.setContentText("Utilisateur supprimé avec succès !");

            // ✅ Appliquer le style moderne
            success.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            success.getDialogPane().getStyleClass().add("information");

            success.showAndWait();

            chargerUtilisateurs();

        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Erreur lors de la suppression");
            error.setContentText(e.getMessage());


            error.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            error.getDialogPane().getStyleClass().add("error");

            error.showAndWait();
        }
    }

    private void filtrerUtilisateurs(String recherche) {
        if (recherche == null || recherche.isEmpty()) {
            utilisateursTable.setItems(utilisateursList); // remettre toute la liste
            return;
        }
        ObservableList<Utilisateur> utilisateursFiltres = FXCollections.observableArrayList();

        for (Utilisateur u : utilisateursList) {
            if (u.getNom().toLowerCase().contains(recherche.toLowerCase()) ||
                    u.getEmail().toLowerCase().contains(recherche.toLowerCase()) ||
                    u.getRole().toLowerCase().contains(recherche.toLowerCase())) {
                utilisateursFiltres.add(u);
            }
        }

        utilisateursTable.setItems(utilisateursFiltres);
    }
}
