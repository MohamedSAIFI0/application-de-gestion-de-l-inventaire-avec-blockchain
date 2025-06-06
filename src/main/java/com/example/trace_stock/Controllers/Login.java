package com.example.trace_stock.Controllers;

import com.example.trace_stock.DAO.ConnexionBD;
import com.example.trace_stock.Service.EmailSender;
import com.example.trace_stock.HelloApplication;
import com.example.trace_stock.Service.PasswordResetServer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.prefs.Preferences;

public class Login {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Hyperlink forgotPasswordLink;

    @FXML
    private CheckBox rememberMe;

    @FXML
    public void initialize() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());

        String savedEmail = prefs.get("email", "");
        String savedPassword = prefs.get("password", "");
        boolean remember = prefs.getBoolean("rememberMe", false);

        if (remember) {
            emailField.setText(savedEmail);
            passwordField.setText(savedPassword);
            rememberMe.setSelected(true);
        }

        Platform.runLater(() -> {
            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
            if (isLoggedIn) {
                showMainInterface();
            }
        });

        forgotPasswordLink.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Réinitialisation du mot de passe");
            dialog.setHeaderText("Entrez votre adresse e-mail :");
            dialog.setContentText("Email :");

            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
            dialog.getDialogPane().getStyleClass().add("text-input-dialog");

            dialog.showAndWait().ifPresent(email -> {
                new Thread(() -> PasswordResetServer.startServer()).start();
                String resetLink = "http://localhost:8080/reset-password?email=" + email;
                EmailSender.sendEmail(email, resetLink);

                showAlert("Lien envoyé !", "Vérifiez votre boîte mail.");
            });
        });
    }

    public void SignIn(ActionEvent actionEvent) throws SQLException {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer un email et un mot de passe valides.");
            return;
        }

        Connection connexion = ConnexionBD.getConnection();
        if (connexion != null) {
            String requete = "SELECT * FROM utilisateur WHERE email = ? AND mot_de_passe = ?";
            PreparedStatement statement = connexion.prepareStatement(requete);
            statement.setString(1, email);
            statement.setString(2, password);
            ResultSet resultat = statement.executeQuery();

            if (resultat.next()) {
                // Récupération des données utilisateur
                int utilisateurId = resultat.getInt("id");  // AJOUT : Récupération de l'ID
                String nom = resultat.getString("nom");
                String typeUtilisateur = resultat.getString("role");

                Preferences prefs = Preferences.userNodeForPackage(getClass());
                if (rememberMe.isSelected()) {
                    prefs.put("email", email);
                    prefs.put("password", password);
                    prefs.putBoolean("rememberMe", true);
                } else {
                    prefs.remove("email");
                    prefs.remove("password");
                    prefs.putBoolean("rememberMe", false);
                }

                // Stockage des informations de session
                prefs.putBoolean("isLoggedIn", true);
                prefs.putInt("utilisateurId", utilisateurId);  // AJOUT : Stockage de l'ID utilisateur
                prefs.put("nomUtilisateur", nom);
                prefs.put("typeUtilisateur", typeUtilisateur);

                showMainInterface();
            } else {
                showAlert("Erreur", "Email ou mot de passe incorrect.");
            }
        }
    }

    private void showMainInterface() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/com/example/trace_stock/Ficherfxml/main.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);

            scene.getStylesheets().add("org/kordamp/bootstrapfx/bootstrapfx.css");
            scene.getStylesheets().add(Objects.requireNonNull(HelloApplication.class.getResource("/com/example/trace_stock/CSS/style.css")).toExternalForm());

            Platform.runLater(() -> {
                Stage stage = (Stage) emailField.getScene().getWindow();
                stage.setTitle("Accueil");
                stage.setScene(scene);
                stage.show();
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur de chargement de l'interface.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/trace_stock/CSS/dialogs.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("information");

        alert.showAndWait();
    }
}