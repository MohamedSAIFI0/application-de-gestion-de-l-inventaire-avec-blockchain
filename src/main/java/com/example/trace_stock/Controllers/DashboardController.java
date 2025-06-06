package com.example.trace_stock.Controllers;

import com.example.trace_stock.DAO.ConnexionBD;
import com.example.trace_stock.Service.GanacheBlockchainService;
import com.example.trace_stock.HelloApplication;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class DashboardController implements Initializable {

    @FXML private BorderPane mainPane;
    @FXML private VBox sidebar;


    @FXML private Label totalProduitsLabel;
    @FXML private Label stockCritiqueLabel;
    @FXML private Label transactionsBlockchainLabel;
    @FXML private TableView<RecentTransaction> recentTransactionsTable;
    @FXML private TableColumn<RecentTransaction, String> colProduit;
    @FXML private TableColumn<RecentTransaction, String> colType;
    @FXML private TableColumn<RecentTransaction, String> colQuantite;
    @FXML private TableColumn<RecentTransaction, String> colDate;
    @FXML private TableColumn<RecentTransaction, String> colHashBlockchain;

    private GanacheBlockchainService blockchainService;
    private ObservableList<RecentTransaction> recentTransactionsList = FXCollections.observableArrayList();
    private Gson gson = new Gson();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("[DASHBOARD] 🚀 Initialisation du dashboard...");


        setupNavigation();


        setupRecentTransactionsTable();


        initializeBlockchainService();


        loadDashboardData();
    }

    private void setupNavigation() {
        // Récupérer le type d'utilisateur
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String typeUtilisateur = prefs.get("typeUtilisateur", "user");

        if (sidebar != null) {
            for (Node node : sidebar.getChildren()) {
                if (node instanceof Button button) {
                    if (button.getText().equals("Se déconnecter")) {
                        continue;
                    }

                    // Masquer le bouton Utilisateurs si pas admin
                    if (button.getText().equals("Utilisateurs") && !"admin".equalsIgnoreCase(typeUtilisateur)) {
                        button.setVisible(false);
                        button.setManaged(false);
                    }

                    button.setOnAction(e -> handleNavigation(button.getText()));
                }
            }
        }
    }

    private void setupRecentTransactionsTable() {
        if (recentTransactionsTable != null) {
            System.out.println("[DASHBOARD] 🔧 Configuration du tableau...");

            colProduit.setCellValueFactory(new PropertyValueFactory<>("produit"));
            colType.setCellValueFactory(new PropertyValueFactory<>("type"));
            colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
            colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            colHashBlockchain.setCellValueFactory(new PropertyValueFactory<>("hashBlockchain"));


            colHashBlockchain.setVisible(false);

            recentTransactionsTable.setItems(recentTransactionsList);

            System.out.println("[DASHBOARD] ✅ Tableau configuré");
        } else {
            System.out.println("[DASHBOARD] ❌ Tableau non trouvé dans le FXML");
        }
    }

    private void initializeBlockchainService() {
        try {
            blockchainService = new GanacheBlockchainService();
            if (blockchainService.isConnected()) {
                System.out.println("[DASHBOARD] ✅ Service blockchain connecté");
            } else {
                System.out.println("[DASHBOARD] ⚠️ Service blockchain non disponible");
            }
        } catch (Exception e) {
            System.out.println("[DASHBOARD] ❌ Erreur initialisation blockchain: " + e.getMessage());
            blockchainService = null;
        }
    }

    private void loadDashboardData() {
        // Utiliser un Task pour éviter de bloquer l'UI
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                loadStatistics();


                loadOnlyBlockchainTransactions();

                return null;
            }
        };

        new Thread(task).start();
    }

    private void loadStatistics() {
        Platform.runLater(() -> {
            try {

                int totalProduits = getTotalProduits();
                updateLabel(totalProduitsLabel, String.valueOf(totalProduits));


                int stockCritique = getStockCritique();
                updateLabel(stockCritiqueLabel, String.valueOf(stockCritique));


                int transactionsBlockchain = getTransactionsBlockchain();
                updateLabel(transactionsBlockchainLabel, String.valueOf(transactionsBlockchain));

            } catch (Exception e) {
                System.out.println("[DASHBOARD] ❌ Erreur chargement statistiques: " + e.getMessage());
                updateLabel(totalProduitsLabel, "Erreur");
                updateLabel(stockCritiqueLabel, "Erreur");
                updateLabel(transactionsBlockchainLabel, "Erreur");
            }
        });
    }

    private void updateLabel(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private int getTotalProduits() {
        try (Connection connection = ConnexionBD.getConnection()) {
            String query = "SELECT COUNT(*) FROM produit";
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("[DASHBOARD] ❌ Erreur total produits: " + e.getMessage());
        }
        return 0;
    }

    private int getStockCritique() {
        try (Connection connection = ConnexionBD.getConnection()) {
            String query = "SELECT COUNT(*) FROM produit WHERE quantite < 10";
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("[DASHBOARD] ❌ Erreur stock critique: " + e.getMessage());
        }
        return 0;
    }

    private int getTransactionsBlockchain() {
        try {
            if (blockchainService != null && blockchainService.isConnected()) {
                String currentBlock = blockchainService.getCurrentBlockNumber();
                return Integer.parseInt(currentBlock);
            }
        } catch (Exception e) {
            System.out.println("[DASHBOARD] ❌ Erreur transactions blockchain: " + e.getMessage());
        }
        return 0;
    }


    private void loadOnlyBlockchainTransactions() {
        Platform.runLater(() -> {
            System.out.println("[DASHBOARD] 🔄 Chargement des transactions blockchain uniquement...");

            recentTransactionsList.clear();

            if (recentTransactionsTable != null) {
                recentTransactionsTable.refresh();
            }

            try {

                loadBlockchainTransactions();


                recentTransactionsList.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));

                if (recentTransactionsList.isEmpty()) {
                    System.out.println("[DASHBOARD] ⚠️ Aucune transaction blockchain trouvée");
                    recentTransactionsList.add(new RecentTransaction(
                            "Aucune transaction", "Info", "0",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                            "N/A"
                    ));
                } else {
                    System.out.println("[DASHBOARD] ✅ " + recentTransactionsList.size() + " transactions blockchain chargées");
                }

                if (recentTransactionsTable != null) {
                    recentTransactionsTable.refresh();
                }

            } catch (Exception e) {
                System.out.println("[DASHBOARD] ❌ Erreur chargement transactions: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void loadBlockchainTransactions() {
        if (blockchainService == null || !blockchainService.isConnected()) {
            System.out.println("[DASHBOARD] ⚠️ Service blockchain non disponible");
            return;
        }

        try {
            System.out.println("[DASHBOARD] 🔍 Chargement transactions blockchain...");


            String currentBlockStr = blockchainService.getCurrentBlockNumber();
            int currentBlock = Integer.parseInt(currentBlockStr);

            System.out.println("[DASHBOARD] 📊 Bloc actuel: " + currentBlock);

            int count = 0;

            for (int i = 1; i <= currentBlock; i++) {
                String blockData = blockchainService.getBlockByNumber(i);

                if (blockData != null) {
                    List<RecentTransaction> blockTransactions = parseBlockTransactions(blockData);
                    recentTransactionsList.addAll(blockTransactions);
                    count += blockTransactions.size();
                }
            }

            System.out.println("[DASHBOARD] 📊 Total transactions blockchain: " + count);

        } catch (Exception e) {
            System.out.println("[DASHBOARD] ❌ Erreur blockchain: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private List<RecentTransaction> parseBlockTransactions(String blockData) {
        List<RecentTransaction> transactions = new ArrayList<>();

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
                                RecentTransaction transaction = decodeInventoryTransactionFixed(tx, input);
                                if (transaction != null) {
                                    transactions.add(transaction);
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("[DASHBOARD] ❌ Erreur parsing bloc: " + e.getMessage());
        }

        return transactions;
    }


    private RecentTransaction decodeInventoryTransactionFixed(JsonObject tx, String hexData) {
        try {

            String data = hexToStringUTF8(hexData);

            System.out.println("[DASHBOARD] 📝 Données décodées: " + data);

            if (data.startsWith("INVENTORY|")) {
                String[] parts = data.split("\\|");

                if (parts.length >= 4) {
                    String produit = parts[1];
                    String type = convertBlockchainType(parts[2]);
                    String quantite = parts[3];


                    String date = "N/A";
                    try {
                        if (parts.length >= 6) {
                            String timestampStr = parts[parts.length - 1].trim();
                            timestampStr = timestampStr.replaceAll("[^0-9]", "");

                            if (!timestampStr.isEmpty()) {
                                long timestamp = Long.parseLong(timestampStr);
                                LocalDateTime dateTime = LocalDateTime.ofInstant(
                                        Instant.ofEpochMilli(timestamp),
                                        ZoneId.systemDefault()
                                );
                                date = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("[DASHBOARD] ⚠️ Erreur timestamp, utilisation date actuelle: " + e.getMessage());
                        date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    }

                    System.out.println("[DASHBOARD] ✅ Transaction blockchain: " + produit + " - " + type + " - " + quantite + " - " + date);

                    return new RecentTransaction(produit, type, quantite, date, "N/A");
                }
            }

        } catch (Exception e) {
            System.out.println("[DASHBOARD] ❌ Erreur décodage: " + e.getMessage());
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
            System.out.println("[DASHBOARD] ❌ Erreur conversion hex: " + e.getMessage());
            return "";
        }
    }

    private String convertBlockchainType(String blockchainType) {
        return switch (blockchainType) {
            case "AJOUT_PRODUIT" -> "Ajout de produit";
            case "MODIFICATION_PRODUIT" -> "Modification";
            case "SUPPRESSION_PRODUIT" -> "Suppression";
            case "AJOUT_MOUVEMENT", "AJOUT_MOUVEMENT_ENTREE" -> "Entrée";
            case "AJOUT_MOUVEMENT_SORTIE" -> "Sortie";
            case "MODIFICATION_MOUVEMENT" -> "Modif. mouvement";
            case "SUPPRESSION_MOUVEMENT" -> "Suppr. mouvement";
            default -> blockchainType;
        };
    }

    private void handleNavigation(String buttonText) {

        Button clickedButton = null;
        if (sidebar != null) {
            for (Node node : sidebar.getChildren()) {
                if (node instanceof Button button && button.getText().equals(buttonText)) {
                    clickedButton = button;
                    break;
                }
            }
        }


        if (clickedButton != null) {
            setActiveButton(clickedButton);
        }

        String fxml = switch (buttonText) {
            case "Dashboard" -> "/com/example/trace_stock/Ficherfxml/dashboard.fxml";
            case "Produits" -> "/com/example/trace_stock/Ficherfxml/produitsView.fxml";
            case "Mouvements" -> "/com/example/trace_stock/Ficherfxml/mouvementsView.fxml";
            case "Blockchain" -> "/com/example/trace_stock/Ficherfxml/blockchainView.fxml";
            case "Alertes" -> "/com/example/trace_stock/Ficherfxml/alertesView.fxml";
            case "Utilisateurs" -> "/com/example/trace_stock/Ficherfxml/utilisateursView.fxml";
            case "Rapports" -> "/com/example/trace_stock/Ficherfxml/rapportsView.fxml";
            default -> null;
        };

        if (fxml != null) {
            try {
                Node view = FXMLLoader.load(getClass().getResource(fxml));
                mainPane.setCenter(view);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void setActiveButton(Button activeButton) {

        if (sidebar != null) {
            for (Node node : sidebar.getChildren()) {
                if (node instanceof Button button) {

                    if (button.getText().equals("Se déconnecter")) {
                        continue;
                    }


                    button.getStyleClass().removeAll("active");


                    if (button == activeButton) {
                        if (!button.getStyleClass().contains("active")) {
                            button.getStyleClass().add("active");
                        }
                    }
                }
            }
        }
    }

    @FXML
    public void loadDashboard(ActionEvent actionEvent) {

        Button dashboardButton = null;
        if (sidebar != null) {
            for (Node node : sidebar.getChildren()) {
                if (node instanceof Button button && button.getText().equals("Dashboard")) {
                    dashboardButton = button;
                    break;
                }
            }
        }

        if (dashboardButton != null) {
            setActiveButton(dashboardButton);
        }

        try {
            Node dashboardView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/org/example/tracestockproject/views/Ficherfxml/dashboard.fxml")));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void seDeconnecter(ActionEvent event) {
        try {

            Preferences prefs = Preferences.userNodeForPackage(getClass());
            prefs.putBoolean("isLoggedIn", false);


            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Ficherfxml/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load());


            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Connexion");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la déconnexion.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class RecentTransaction {
        private String produit;
        private String type;
        private String quantite;
        private String date;
        private String hashBlockchain;

        public RecentTransaction(String produit, String type, String quantite, String date, String hashBlockchain) {
            this.produit = produit;
            this.type = type;
            this.quantite = quantite;
            this.date = date;
            this.hashBlockchain = hashBlockchain;
        }


        public String getProduit() { return produit; }
        public String getType() { return type; }
        public String getQuantite() { return quantite; }
        public String getDate() { return date; }
        public String getHashBlockchain() { return hashBlockchain; }


        public void setProduit(String produit) { this.produit = produit; }
        public void setType(String type) { this.type = type; }
        public void setQuantite(String quantite) { this.quantite = quantite; }
        public void setDate(String date) { this.date = date; }
        public void setHashBlockchain(String hashBlockchain) { this.hashBlockchain = hashBlockchain; }
    }
}