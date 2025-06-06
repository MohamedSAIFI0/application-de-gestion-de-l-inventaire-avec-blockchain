package com.example.trace_stock.DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnexionBD {
    public static Connection getConnection() {
        try {
            String url = "jdbc:mysql://localhost:3306/gestion_stock";
            String utilisateur = "root";
            String motDePasse = "";


            Connection connexion = DriverManager.getConnection(url, utilisateur, motDePasse);

            return connexion;
        } catch (SQLException e) {
            System.out.println("Erreur de connexion à MySQL !");
            e.printStackTrace();
            return null;
        }
    }

}
