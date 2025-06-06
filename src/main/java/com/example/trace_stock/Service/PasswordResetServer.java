package com.example.trace_stock.Service;

import com.example.trace_stock.DAO.ConnexionBD;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import static spark.Spark.*;

public class PasswordResetServer {

    public static void startServer() {
        port(8080);

        get("/reset-password", (request, response) -> {
            String email = request.queryParams("email");

            if (email == null || email.isEmpty()) {
                return "Email manquant !";
            }


            String newPassword = generatePassword(8);


            if (updatePasswordInDatabase(email, newPassword)) {
                return "<h1>Mot de passe réinitialisé</h1><p>Nouveau mot de passe : <b>" + newPassword + "</b></p>";
            } else {
                return "Erreur : utilisateur introuvable ou mise à jour échouée.";
            }
        });
    }

    private static String generatePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static boolean updatePasswordInDatabase(String email, String newPassword) {


        try (Connection conn = ConnexionBD.getConnection()) {
            String sql = "UPDATE utilisateur SET mot_de_passe = ? WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newPassword);
            stmt.setString(2, email);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
