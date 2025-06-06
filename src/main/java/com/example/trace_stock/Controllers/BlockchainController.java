package com.example.trace_stock.Controllers;

import com.example.trace_stock.Entities.BlockchainTransaction;
import com.example.trace_stock.Service.GanacheBlockchainService;
import com.example.trace_stock.Entities.TransactionInfo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;

public class BlockchainController implements Initializable {

    @FXML private TableView<BlockchainTransaction> blockchainTable;
    @FXML private TableColumn<BlockchainTransaction, String> colHash;
    @FXML private TableColumn<BlockchainTransaction, String> colProduit;
    @FXML private TableColumn<BlockchainTransaction, String> colQuantite;
    @FXML private TableColumn<BlockchainTransaction, String> colDate;

    private GanacheBlockchainService blockchainService;
    private ObservableList<BlockchainTransaction> transactionsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        colHash.setCellValueFactory(cellData -> cellData.getValue().hashProperty());
        colProduit.setCellValueFactory(cellData -> cellData.getValue().produitProperty());
        colQuantite.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty());
        colDate.setCellValueFactory(cellData -> cellData.getValue().dateProperty());


        colHash.setCellFactory(column -> new TableCell<BlockchainTransaction, String>() {
            @Override
            protected void updateItem(String hash, boolean empty) {
                super.updateItem(hash, empty);
                if (empty || hash == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    if (hash.length() > 25) {
                        setText(hash.substring(0, 15) + "..." + hash.substring(hash.length() - 5));
                    } else {
                        setText(hash);
                    }
                    setTooltip(new Tooltip(hash));
                }
            }
        });


        blockchainTable.setRowFactory(tv -> {
            TableRow<BlockchainTransaction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    afficherDetailsTransaction(row.getItem());
                }
            });
            return row;
        });

        initializeBlockchain();
        chargerTransactions();
    }

    private void initializeBlockchain() {
        try {
            blockchainService = new GanacheBlockchainService();
            if (blockchainService.isConnected()) {
                System.out.println("[BLOCKCHAIN-VIEW] ✅ Service blockchain connecté");
            } else {
                System.out.println("[BLOCKCHAIN-VIEW] ⚠️ Ganache non accessible");
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN-VIEW] ❌ Erreur initialisation: " + e.getMessage());
            blockchainService = null;
        }
    }

    private void chargerTransactions() {

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    transactionsList.clear();

                    // Ajouter un message de chargement
                    ajouterTransaction("🔄 CHARGEMENT", "Récupération en cours...", "0",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    blockchainTable.setItems(transactionsList);
                });

                if (blockchainService != null && blockchainService.isConnected()) {
                    try {

                        recupererToutesLesTransactions();

                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            transactionsList.clear();
                            ajouterTransaction("❌ ERREUR", "Erreur chargement", "0",
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                            blockchainTable.setItems(transactionsList);
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        transactionsList.clear();
                        ajouterTransaction("⚠️ OFFLINE", "Ganache déconnecté", "0",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                        blockchainTable.setItems(transactionsList);
                    });
                }
                return null;
            }
        };

        new Thread(task).start();
    }


    private void recupererToutesLesTransactions() {
        try {
            String blockNumber = blockchainService.getCurrentBlockNumber();
            int currentBlock = Integer.parseInt(blockNumber);

            System.out.println("[BLOCKCHAIN-VIEW] 📊 Analyse de " + currentBlock + " blocs...");

            List<BlockchainTransaction> transactionsTrouvees = new ArrayList<>();


            for (int blockNum = 1; blockNum <= currentBlock; blockNum++) {
                try {

                    String blockInfo = blockchainService.getBlockByNumber(blockNum);
                    if (blockInfo != null) {

                        List<String> hashTransactions = extraireHashDuBloc(blockInfo);

                        for (String hash : hashTransactions) {
                            if (hash != null && !hash.isEmpty() && !hash.equals("null")) {
                                TransactionInfo info = blockchainService.getTransactionByHash(hash);
                                if (info != null && info.getData() != null && !info.getData().equals("0x")) {

                                    String[] donneesDecodees = decoderTransaction(info.getData());
                                    String nomProduit = donneesDecodees[0];
                                    String quantite = donneesDecodees[1];
                                    String dateTransaction = obtenirDateTransaction(info);


                                    if (!nomProduit.equals("Produit inconnu") || !quantite.equals("0")) {
                                        transactionsTrouvees.add(new BlockchainTransaction(hash, nomProduit, quantite, dateTransaction));
                                        System.out.println("[BLOCKCHAIN-VIEW] ✅ Transaction trouvée: " + nomProduit + " (" + quantite + ")");
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[BLOCKCHAIN-VIEW] ⚠️ Erreur bloc " + blockNum + ": " + e.getMessage());
                }
            }


            Platform.runLater(() -> {
                transactionsList.clear();

                if (transactionsTrouvees.isEmpty()) {
                    ajouterTransaction("ℹ️ INFO", "Aucune transaction trouvée", "0",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    System.out.println("[BLOCKCHAIN-VIEW] ℹ️ Aucune transaction avec données trouvée");
                } else {
                    transactionsList.addAll(transactionsTrouvees);
                    System.out.println("[BLOCKCHAIN-VIEW] ✅ " + transactionsTrouvees.size() + " transactions chargées");
                }

                blockchainTable.setItems(transactionsList);
            });

        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN-VIEW] ❌ Erreur récupération globale: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private List<String> extraireHashDuBloc(String blockInfo) {
        List<String> hashList = new ArrayList<>();

        try {


            if (blockInfo.contains("\"transactions\"")) {
                String transactionsSection = blockInfo.substring(blockInfo.indexOf("\"transactions\""));


                String[] parts = transactionsSection.split("\"");
                for (String part : parts) {
                    if (part.startsWith("0x") && part.length() == 66) { // Hash Ethereum standard
                        hashList.add(part);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN-VIEW] ❌ Erreur extraction hash: " + e.getMessage());
        }

        return hashList;
    }


    private String[] decoderTransaction(String hexData) {
        String[] result = {"Produit inconnu", "0"};

        try {
            if (hexData != null && hexData.startsWith("0x") && hexData.length() > 10) {
                String hex = hexData.substring(2);
                StringBuilder texte = new StringBuilder();

                // Convertir hex en texte
                for (int i = 0; i < hex.length(); i += 2) {
                    if (i + 1 < hex.length()) {
                        String hexByte = hex.substring(i, i + 2);
                        try {
                            int decimal = Integer.parseInt(hexByte, 16);
                            if (decimal >= 32 && decimal <= 126) {
                                texte.append((char) decimal);
                            } else if (decimal == 124) { // |
                                texte.append("|");
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }

                String decodedText = texte.toString();
                System.out.println("[BLOCKCHAIN-VIEW] 📝 Données décodées: " + decodedText);


                String[] parts = decodedText.split("\\|");
                if (parts.length >= 4) {
                    result[0] = parts[1].trim(); // Nom du produit
                    result[1] = parts[3].trim(); // Quantité
                } else {

                    if (decodedText.toLowerCase().contains("test")) result[0] = "test";
                    else if (decodedText.toLowerCase().contains("produit")) result[0] = "Produit";
                    else if (decodedText.toLowerCase().contains("inventory")) result[0] = "Inventory Item";


                    StringBuilder numbers = new StringBuilder();
                    for (char c : decodedText.toCharArray()) {
                        if (Character.isDigit(c)) {
                            numbers.append(c);
                        }
                    }
                    if (numbers.length() > 0 && numbers.length() < 6) {
                        result[1] = numbers.toString();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN-VIEW] ❌ Erreur décodage: " + e.getMessage());
        }

        return result;
    }

    private String obtenirDateTransaction(TransactionInfo info) {
        try {
            if (info.getBlockNumber() != null) {
                int blockNum = Integer.parseInt(info.getBlockNumber());
                LocalDateTime dateEstimee = LocalDateTime.now().minusMinutes(blockNum * 2);
                return dateEstimee.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN-VIEW] ❌ Erreur date: " + e.getMessage());
        }

        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    @FXML
    private void actualiserTransactions() {
        System.out.println("[BLOCKCHAIN-VIEW] 🔄 Actualisation manuelle...");
        chargerTransactions();
    }

    private void ajouterTransaction(String hash, String produit, String quantite, String date) {
        transactionsList.add(new BlockchainTransaction(hash, produit, quantite, date));
    }

    private void afficherDetailsTransaction(BlockchainTransaction transaction) {
        if (transaction.getHash().startsWith("ℹ️") || transaction.getHash().startsWith("❌") ||
                transaction.getHash().startsWith("⚠️") || transaction.getHash().startsWith("🔄")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText("Statut Blockchain");
            alert.setContentText(transaction.getProduit());
            alert.showAndWait();
            return;
        }


        if (blockchainService != null && blockchainService.isConnected()) {
            TransactionInfo info = blockchainService.getTransactionByHash(transaction.getHash());

            StringBuilder details = new StringBuilder();
            details.append("🔗 TRANSACTION BLOCKCHAIN\n");
            details.append("========================\n\n");
            details.append("Hash: ").append(transaction.getHash()).append("\n");
            details.append("Produit: ").append(transaction.getProduit()).append("\n");
            details.append("Quantité: ").append(transaction.getQuantite()).append("\n");
            details.append("Date: ").append(transaction.getDate()).append("\n\n");

            if (info != null) {
                details.append("📊 DÉTAILS GANACHE:\n");
                details.append("From: ").append(info.getFrom()).append("\n");
                details.append("To: ").append(info.getTo()).append("\n");
                details.append("Bloc: ").append(info.getBlockNumber()).append("\n\n");

                details.append("📝 DONNÉES BRUTES:\n");
                details.append(info.getData()).append("\n\n");

                details.append("📝 DONNÉES DÉCODÉES:\n");
                String[] decoded = decoderTransaction(info.getData());
                details.append("Produit: ").append(decoded[0]).append("\n");
                details.append("Quantité: ").append(decoded[1]).append("\n");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Détails Transaction");
            alert.setHeaderText("Blockchain - " + transaction.getProduit());

            TextArea textArea = new TextArea(details.toString());
            textArea.setEditable(false);
            textArea.setPrefRowCount(25);
            textArea.setPrefColumnCount(80);

            alert.getDialogPane().setContent(textArea);
            alert.getDialogPane().setPrefWidth(800);
            alert.showAndWait();
        }
    }
}