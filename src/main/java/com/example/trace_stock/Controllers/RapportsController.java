package com.example.trace_stock.Controllers;

import com.example.trace_stock.DAO.ConnexionBD;
import com.example.trace_stock.Service.GanacheBlockchainService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RapportsController implements Initializable {

    @FXML private Button btnGenerer;
    @FXML private TextArea rapportArea;

    private GanacheBlockchainService blockchainService;
    private Gson gson = new Gson();


    public static class TransactionBlockchain {
        public String produit;
        public String type;
        public int quantite;
        public LocalDateTime date;
        public String description;

        public TransactionBlockchain(String produit, String type, int quantite, LocalDateTime date, String description) {
            this.produit = produit;
            this.type = type;
            this.quantite = quantite;
            this.date = date;
            this.description = description;
        }
    }

    public static class MouvementStock {
        public String produit;
        public String type;
        public int quantite;
        public LocalDateTime date;

        public MouvementStock(String produit, String type, int quantite, LocalDateTime date) {
            this.produit = produit;
            this.type = type;
            this.quantite = quantite;
            this.date = date;
        }
    }

    public static class StatistiquesGenerales {
        public int totalProduits;
        public int stockCritique;
        public int totalMouvements;
        public int totalTransactionsBlockchain;
        public int totalEntrees;
        public int totalSorties;
        public LocalDateTime derniereActivite;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("[RAPPORTS] Initialisation du module rapports...");


        initializeBlockchainService();


        btnGenerer.setOnAction(e -> genererRapport());


        rapportArea.setText(" Cliquez sur 'Générer un rapport' pour créer un rapport complet de l'inventaire et des transactions blockchain.");
    }

    private void initializeBlockchainService() {
        try {
            blockchainService = new GanacheBlockchainService();
            if (blockchainService.isConnected()) {
                System.out.println("[RAPPORTS] Service blockchain connecté");
            } else {
                System.out.println("[RAPPORTS] Service blockchain non disponible");
            }
        } catch (Exception e) {
            System.out.println("[RAPPORTS] Erreur initialisation blockchain: " + e.getMessage());
            blockchainService = null;
        }
    }

    private void genererRapport() {
        System.out.println("[RAPPORTS] Génération du rapport en cours...");


        btnGenerer.setDisable(true);
        btnGenerer.setText("Génération en cours...");


        rapportArea.setText(" Génération du rapport en cours...\n\n" +
                " Analyse des données blockchain...\n" +
                " Collecte des informations produits...\n" +
                " Calcul des statistiques...");


        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return genererRapportComplet();
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    rapportArea.setText(getValue());
                    btnGenerer.setDisable(false);
                    btnGenerer.setText("Générer un rapport");
                    System.out.println("[RAPPORTS]  Rapport généré avec succès");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    rapportArea.setText(" Erreur lors de la génération du rapport.\n\n" +
                            "Détails: " + getException().getMessage());
                    btnGenerer.setDisable(false);
                    btnGenerer.setText("Générer un rapport");
                    System.out.println("[RAPPORTS] Erreur génération rapport: " + getException().getMessage());
                });
            }
        };

        new Thread(task).start();
    }

    private String genererRapportComplet() {
        StringBuilder rapport = new StringBuilder();

        try {

            rapport.append(genererEnteteRapport());


            StatistiquesGenerales stats = collecterStatistiques();
            rapport.append(genererSectionStatistiques(stats));


            List<TransactionBlockchain> transactionsBlockchain = collecterTransactionsBlockchain();
            rapport.append(genererSectionBlockchain(transactionsBlockchain));


            List<MouvementStock> mouvements = collecterMouvementsStock();
            rapport.append(genererSectionMouvements(mouvements));


            rapport.append(genererSectionProduits());


            rapport.append(genererSectionRecommandations(stats, transactionsBlockchain, mouvements));


            rapport.append(genererPiedRapport());

        } catch (Exception e) {
            rapport.append(" Erreur lors de la génération du rapport: ").append(e.getMessage());
            e.printStackTrace();
        }

        return rapport.toString();
    }

    private String genererEnteteRapport() {
        LocalDateTime maintenant = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

        return """
               ═══════════════════════════════════════════════════════════════
                RAPPORT COMPLET DE L'INVENTAIRE ET BLOCKCHAIN
               ═══════════════════════════════════════════════════════════════
               
                Date de génération: %s
                Système: Inventaire+ avec Blockchain
               """.formatted(maintenant.format(formatter));
    }

    private StatistiquesGenerales collecterStatistiques() {
        StatistiquesGenerales stats = new StatistiquesGenerales();

        try (Connection connection = ConnexionBD.getConnection()) {

            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM produit");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.totalProduits = rs.getInt(1);
            }


            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM produit WHERE quantite < 10");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.stockCritique = rs.getInt(1);
            }


            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM mouvement");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.totalMouvements = rs.getInt(1);
            }


            try (PreparedStatement ps = connection.prepareStatement("SELECT type, SUM(quantite) FROM mouvement GROUP BY type");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString(1);
                    int quantite = rs.getInt(2);
                    if ("Entrée".equals(type)) stats.totalEntrees = quantite;
                    else if ("Sortie".equals(type)) stats.totalSorties = quantite;
                }
            }


            try (PreparedStatement ps = connection.prepareStatement("SELECT MAX(date) FROM mouvement");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getTimestamp(1) != null) {
                    stats.derniereActivite = rs.getTimestamp(1).toLocalDateTime();
                }
            }

        } catch (SQLException e) {
            System.out.println("[RAPPORTS] Erreur collecte statistiques: " + e.getMessage());
        }


        if (blockchainService != null && blockchainService.isConnected()) {
            try {
                String currentBlock = blockchainService.getCurrentBlockNumber();
                stats.totalTransactionsBlockchain = Integer.parseInt(currentBlock);
            } catch (Exception e) {
                System.out.println("[RAPPORTS] Erreur stats blockchain: " + e.getMessage());
            }
        }

        return stats;
    }

    private String genererSectionStatistiques(StatistiquesGenerales stats) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String derniereActiviteStr = stats.derniereActivite != null ?
                stats.derniereActivite.format(formatter) : "Aucune activité";

        return """
               ┌─────────────────────────────────────────────────────────────┐
               │  STATISTIQUES GÉNÉRALES                                   │
               └─────────────────────────────────────────────────────────────┘
               
                  Produits en inventaire: %d
                  Produits en stock critique (<10): %d
                  Total des mouvements de stock: %d
                  Blocs blockchain générés: %d
               
                  Entrées totales: %d unités
                  Sorties totales: %d unités
                  Solde net: %d unités
               
                  Dernière activité: %s
               
               """.formatted(
                stats.totalProduits,
                stats.stockCritique,
                stats.totalMouvements,
                stats.totalTransactionsBlockchain,
                stats.totalEntrees,
                stats.totalSorties,
                stats.totalEntrees - stats.totalSorties,
                derniereActiviteStr
        );
    }

    private List<TransactionBlockchain> collecterTransactionsBlockchain() {
        List<TransactionBlockchain> transactions = new ArrayList<>();

        if (blockchainService == null || !blockchainService.isConnected()) {
            return transactions;
        }

        try {
            String currentBlockStr = blockchainService.getCurrentBlockNumber();
            int currentBlock = Integer.parseInt(currentBlockStr);

            for (int i = 1; i <= currentBlock; i++) {
                String blockData = blockchainService.getBlockByNumber(i);
                if (blockData != null) {
                    transactions.addAll(parseBlockchainTransactions(blockData));
                }
            }

        } catch (Exception e) {
            System.out.println("[RAPPORTS] Erreur collecte blockchain: " + e.getMessage());
        }

        return transactions;
    }

    private List<TransactionBlockchain> parseBlockchainTransactions(String blockData) {
        List<TransactionBlockchain> transactions = new ArrayList<>();

        try {
            JsonObject blockJson = gson.fromJson(blockData, JsonObject.class);

            if (blockJson.has("result") && !blockJson.get("result").isJsonNull()) {
                JsonObject result = blockJson.get("result").getAsJsonObject();

                if (result.has("transactions") && result.get("transactions").isJsonArray()) {
                    JsonArray txArray = result.get("transactions").getAsJsonArray();

                    for (int i = 0; i < txArray.size(); i++) {
                        JsonObject tx = txArray.get(i).getAsJsonObject();

                        if (tx.has("input") && !tx.get("input").isJsonNull()) {
                            String input = tx.get("input").getAsString();

                            if (input.startsWith("0x") && input.length() > 2) {
                                TransactionBlockchain transaction = decodeBlockchainTransaction(input);
                                if (transaction != null) {
                                    transactions.add(transaction);
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("[RAPPORTS] Erreur parsing blockchain: " + e.getMessage());
        }

        return transactions;
    }

    private TransactionBlockchain decodeBlockchainTransaction(String hexData) {
        try {
            String data = hexToStringUTF8(hexData);

            if (data.startsWith("INVENTORY|")) {
                String[] parts = data.split("\\|");

                if (parts.length >= 4) {
                    String produit = parts[1];
                    String type = parts[2];
                    int quantite = Integer.parseInt(parts[3]);
                    String description = parts.length > 4 ? parts[4] : "";

                    LocalDateTime date = LocalDateTime.now();
                    if (parts.length >= 6) {
                        try {
                            String timestampStr = parts[parts.length - 1].replaceAll("[^0-9]", "");
                            if (!timestampStr.isEmpty()) {
                                long timestamp = Long.parseLong(timestampStr);
                                date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    return new TransactionBlockchain(produit, type, quantite, date, description);
                }
            }

        } catch (Exception e) {
            System.out.println("[RAPPORTS] Erreur décodage transaction: " + e.getMessage());
        }

        return null;
    }

    private String hexToStringUTF8(String hex) {
        try {
            if (hex.startsWith("0x")) {
                hex = hex.substring(2);
            }

            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < hex.length(); i += 2) {
                String hexByte = hex.substring(i, i + 2);
                bytes[i / 2] = (byte) Integer.parseInt(hexByte, 16);
            }

            return new String(bytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            return "";
        }
    }

    private String genererSectionBlockchain(List<TransactionBlockchain> transactions) {
        StringBuilder section = new StringBuilder();

        section.append("""
                      ┌─────────────────────────────────────────────────────────────┐
                      │      ANALYSE DES TRANSACTIONS BLOCKCHAIN                      │
                      └─────────────────────────────────────────────────────────────┘
                      
                      """);

        if (transactions.isEmpty()) {
            section.append("ℹ  Aucune transaction blockchain trouvée.\n\n");
            return section.toString();
        }


        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Integer> typeQuantite = new HashMap<>();

        for (TransactionBlockchain tx : transactions) {
            typeCount.merge(tx.type, 1, Integer::sum);
            typeQuantite.merge(tx.type, tx.quantite, Integer::sum);
        }

        section.append("📊 Résumé des activités blockchain:\n");
        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            String type = convertirTypeBlockchain(entry.getKey());
            int count = entry.getValue();
            int quantite = typeQuantite.get(entry.getKey());
            section.append(String.format("   • %s: %d opérations (%d unités)\n", type, count, quantite));
        }

        section.append("\n📝 Détail des transactions récentes:\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");


        transactions.stream()
                .sorted((t1, t2) -> t2.date.compareTo(t1.date))
                .limit(10)
                .forEach(tx -> {
                    String type = convertirTypeBlockchain(tx.type);
                    section.append(String.format("   %s | %s | %s (%d unités)\n",
                            tx.date.format(formatter), type, tx.produit, tx.quantite));
                });

        section.append("\n");
        return section.toString();
    }

    private List<MouvementStock> collecterMouvementsStock() {
        List<MouvementStock> mouvements = new ArrayList<>();

        try (Connection connection = ConnexionBD.getConnection()) {
            String query = """
                SELECT p.nom, m.type, m.quantite, m.date 
                FROM mouvement m 
                JOIN produit p ON m.produit_id = p.id 
                ORDER BY m.date DESC
                """;

            try (PreparedStatement ps = connection.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String produit = rs.getString("nom");
                    String type = rs.getString("type");
                    int quantite = rs.getInt("quantite");
                    LocalDateTime date = rs.getTimestamp("date").toLocalDateTime();

                    mouvements.add(new MouvementStock(produit, type, quantite, date));
                }
            }

        } catch (SQLException e) {
            System.out.println("[RAPPORTS]  Erreur collecte mouvements: " + e.getMessage());
        }

        return mouvements;
    }

    private String genererSectionMouvements(List<MouvementStock> mouvements) {
        StringBuilder section = new StringBuilder();

        section.append("""
                      ┌─────────────────────────────────────────────────────────────┐
                      │  ANALYSE DES MOUVEMENTS DE STOCK                          │
                      └─────────────────────────────────────────────────────────────┘
                      
                      """);

        if (mouvements.isEmpty()) {
            section.append("  Aucun mouvement de stock enregistré.\n\n");
            return section.toString();
        }


        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Integer> typeQuantite = new HashMap<>();

        for (MouvementStock mv : mouvements) {
            typeCount.merge(mv.type, 1, Integer::sum);
            typeQuantite.merge(mv.type, mv.quantite, Integer::sum);
        }

        section.append(" Résumé des mouvements:\n");
        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            int quantite = typeQuantite.get(type);
            section.append(String.format("   • %s: %d mouvements (%d unités)\n", type, count, quantite));
        }

        section.append("\n Mouvements récents:\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");

        mouvements.stream()
                .limit(10)
                .forEach(mv -> {
                    section.append(String.format("   %s | %s | %s (%d unités)\n",
                            mv.date.format(formatter), mv.type, mv.produit, mv.quantite));
                });

        section.append("\n");
        return section.toString();
    }

    private String genererSectionProduits() {
        StringBuilder section = new StringBuilder();

        section.append("""
                      ┌─────────────────────────────────────────────────────────────┐
                      │   ANALYSE DES PRODUITS                                    │
                      └─────────────────────────────────────────────────────────────┘
                      
                      """);

        try (Connection connection = ConnexionBD.getConnection()) {

            section.append("  Produits en stock critique (< 10 unités):\n");
            String queryCritique = "SELECT nom, quantite FROM produit WHERE quantite < 10 ORDER BY quantite ASC";
            try (PreparedStatement ps = connection.prepareStatement(queryCritique);
                 ResultSet rs = ps.executeQuery()) {

                boolean hasCritique = false;
                while (rs.next()) {
                    hasCritique = true;
                    section.append(String.format("   • %s: %d unités\n", rs.getString("nom"), rs.getInt("quantite")));
                }

                if (!hasCritique) {
                    section.append("    Aucun produit en stock critique\n");
                }
            }


            section.append("\n Top 5 des produits les mieux stockés:\n");
            String queryTop = "SELECT nom, quantite FROM produit ORDER BY quantite DESC LIMIT 5";
            try (PreparedStatement ps = connection.prepareStatement(queryTop);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    section.append(String.format("   • %s: %d unités\n", rs.getString("nom"), rs.getInt("quantite")));
                }
            }

        } catch (SQLException e) {
            section.append(" Erreur lors de l'analyse des produits\n");
        }

        section.append("\n");
        return section.toString();
    }

    private String genererSectionRecommandations(StatistiquesGenerales stats,
                                                 List<TransactionBlockchain> blockchain,
                                                 List<MouvementStock> mouvements) {
        StringBuilder section = new StringBuilder();

        section.append("""
                      ┌─────────────────────────────────────────────────────────────┐
                      │  RECOMMANDATIONS ET CONCLUSIONS                           │
                      └─────────────────────────────────────────────────────────────┘
                      
                      """);


        if (stats.stockCritique > 0) {
            section.append(String.format(" ATTENTION: %d produit(s) en stock critique nécessitent un réapprovisionnement urgent.\n", stats.stockCritique));
        } else {
            section.append(" Excellent: Aucun produit en stock critique.\n");
        }


        if (stats.totalMouvements > 0) {
            double ratioSortie = (double) stats.totalSorties / stats.totalEntrees * 100;
            section.append(String.format(" Ratio sorties/entrées: %.1f%% ", ratioSortie));
            if (ratioSortie > 80) {
                section.append("(Activité de sortie élevée)\n");
            } else if (ratioSortie < 50) {
                section.append("(Accumulation de stock)\n");
            } else {
                section.append("(Équilibre correct)\n");
            }
        }


        if (!blockchain.isEmpty()) {
            section.append(String.format("  Traçabilité blockchain: %d transactions enregistrées garantissent l'intégrité des données.\n", blockchain.size()));
        }


        section.append("\n Recommandations:\n");

        if (stats.stockCritique > 0) {
            section.append("   • Planifier un réapprovisionnement pour les produits en stock critique\n");
        }

        if (stats.totalMouvements == 0) {
            section.append("   • Commencer à enregistrer les mouvements de stock pour un meilleur suivi\n");
        }

        if (blockchain.isEmpty()) {
            section.append("   • Activer l'enregistrement blockchain pour une traçabilité complète\n");
        } else {
            section.append("   • Continuer l'utilisation de la blockchain pour maintenir l'intégrité des données\n");
        }

        section.append("   • Effectuer des audits réguliers pour maintenir la précision de l'inventaire\n");
        section.append("   • Mettre en place des alertes automatiques pour les stocks critiques\n");

        section.append("\n");
        return section.toString();
    }

    private String genererPiedRapport() {
        return """
               ═══════════════════════════════════════════════════════════════
                   FIN DU RAPPORT
               ═══════════════════════════════════════════════════════════════
               
                  Ce rapport a été généré automatiquement par le système Inventaire+
                  Les données blockchain garantissent l'intégrité et la traçabilité
          
               
               """;
    }

    private String convertirTypeBlockchain(String type) {
        return switch (type) {
            case "AJOUT_PRODUIT" -> "Ajout de produit";
            case "MODIFICATION_PRODUIT" -> "Modification de produit";
            case "SUPPRESSION_PRODUIT" -> "Suppression de produit";
            case "AJOUT_MOUVEMENT_ENTREE" -> "Entrée de stock";
            case "AJOUT_MOUVEMENT_SORTIE" -> "Sortie de stock";
            default -> type;
        };
    }
}