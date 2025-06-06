package com.example.trace_stock.Service;

import com.example.trace_stock.Entities.TransactionInfo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GanacheBlockchainService {
    private static final String GANACHE_URL = "http://127.0.0.1:7545";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;
    private Gson gson;
    private String accountAddress;

    public GanacheBlockchainService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.accountAddress = getFirstAccount();

        if (accountAddress != null) {
            System.out.println("[BLOCKCHAIN] ✅ Compte utilisé: " + accountAddress);
            System.out.println("[BLOCKCHAIN] ✅ Solde: " + getAccountBalance(accountAddress) + " ETH");
        }
    }

    private String getFirstAccount() {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_accounts");
            request.put("params", new Object[]{});
            request.put("id", 1);

            String response = makeRequest(request);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            if (jsonResponse.has("result") && jsonResponse.get("result").isJsonArray()) {
                return jsonResponse.get("result").getAsJsonArray().get(0).getAsString();
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Erreur récupération compte: " + e.getMessage());
        }
        return null;
    }

    public String addInventoryTransaction(String productId, String type, int quantity, String location) {
        if (accountAddress == null) {
            System.out.println("[BLOCKCHAIN] ❌ Aucun compte disponible");
            return null;
        }

        try {

            String data = createTransactionData(productId, type, quantity, location);
            System.out.println("[BLOCKCHAIN] 📝 Données: " + data);


            String nonce = getNonce(accountAddress);
            System.out.println("[BLOCKCHAIN] 📊 Nonce: " + nonce);


            Map<String, Object> transactionParams = new HashMap<>();
            transactionParams.put("from", accountAddress);
            transactionParams.put("to", accountAddress); // Transaction vers soi-même
            transactionParams.put("data", data);
            transactionParams.put("gas", "0x15F90"); // 90000 en hexadécimal
            transactionParams.put("gasPrice", "0x4A817C800"); // 20 Gwei
            transactionParams.put("value", "0x0"); // 0 ETH
            transactionParams.put("nonce", nonce);

            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_sendTransaction");
            request.put("params", new Object[]{transactionParams});
            request.put("id", 1);

            System.out.println("[BLOCKCHAIN] 📤 Envoi transaction...");

            String response = makeRequest(request);
            System.out.println("[BLOCKCHAIN] 📥 Réponse: " + response);

            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            if (jsonResponse.has("result")) {
                String txHash = jsonResponse.get("result").getAsString();
                System.out.println("[BLOCKCHAIN] ✅ Transaction envoyée: " + txHash);
                return txHash;
            } else if (jsonResponse.has("error")) {
                JsonObject error = jsonResponse.get("error").getAsJsonObject();
                System.out.println("[BLOCKCHAIN] ❌ Erreur: " + error.get("message").getAsString());
                return null;
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Exception: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    private String getNonce(String account) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_getTransactionCount");
            request.put("params", new Object[]{account, "latest"});
            request.put("id", 1);

            String response = makeRequest(request);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            if (jsonResponse.has("result")) {
                return jsonResponse.get("result").getAsString();
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Erreur nonce: " + e.getMessage());
        }
        return "0x0";
    }

    private String getAccountBalance(String account) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_getBalance");
            request.put("params", new Object[]{account, "latest"});
            request.put("id", 1);

            String response = makeRequest(request);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            if (jsonResponse.has("result")) {
                String hexBalance = jsonResponse.get("result").getAsString();
                // Convertir de Wei en ETH
                long weiBalance = Long.parseLong(hexBalance.substring(2), 16);
                double ethBalance = weiBalance / Math.pow(10, 18);
                return String.format("%.2f", ethBalance);
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Erreur solde: " + e.getMessage());
        }
        return "0";
    }

    private String createTransactionData(String productId, String type, int quantity, String location) {

        String dataString = String.format("INVENTORY|%s|%s|%d|%s|%d",
                productId, type, quantity, location, System.currentTimeMillis());

        StringBuilder hex = new StringBuilder("0x");
        for (byte b : dataString.getBytes()) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }


    private String sendRequest(String jsonPayload) {
        try {
            RequestBody body = RequestBody.create(jsonPayload, JSON);
            Request request = new Request.Builder()
                    .url(GANACHE_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Code de réponse inattendu: " + response);
                }
                return response.body().string();
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Erreur sendRequest: " + e.getMessage());
            return null;
        }
    }


    private String extractJsonValue(String json, String key) {
        try {
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

            if (jsonObject.has("result") && jsonObject.get("result").isJsonObject()) {
                JsonObject result = jsonObject.get("result").getAsJsonObject();
                if (result.has(key)) {
                    return result.get(key).getAsString();
                }
            }


            String searchKey = "\"" + key + "\":\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex != -1) {
                startIndex += searchKey.length();
                int endIndex = json.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    return json.substring(startIndex, endIndex);
                }
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Erreur extraction " + key + ": " + e.getMessage());
        }
        return null;
    }


    public TransactionInfo getTransactionByHash(String hash) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_getTransactionByHash");
            request.put("params", new Object[]{hash});
            request.put("id", 1);

            String response = makeRequest(request);

            if (response != null) {
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

                if (jsonResponse.has("result") && !jsonResponse.get("result").isJsonNull()) {
                    JsonObject result = jsonResponse.get("result").getAsJsonObject();

                    TransactionInfo info = new TransactionInfo();


                    if (result.has("hash")) {
                        info.setHash(result.get("hash").getAsString());
                    }

                    if (result.has("from")) {
                        info.setFrom(result.get("from").getAsString());
                    }

                    if (result.has("to") && !result.get("to").isJsonNull()) {
                        info.setTo(result.get("to").getAsString());
                    }

                    if (result.has("blockNumber") && !result.get("blockNumber").isJsonNull()) {
                        String hexBlock = result.get("blockNumber").getAsString();
                        if (hexBlock.startsWith("0x")) {
                            info.setBlockNumber(String.valueOf(Integer.parseInt(hexBlock.substring(2), 16)));
                        }
                    }

                    if (result.has("input")) {
                        info.setData(result.get("input").getAsString());
                    }

                    return info;
                }
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Erreur getTransactionByHash: " + e.getMessage());
        }
        return null;
    }

    private String makeRequest(Map<String, Object> requestData) throws IOException {
        String json = gson.toJson(requestData);

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(GANACHE_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Code de réponse inattendu: " + response);
            }
            return response.body().string();
        }
    }

    public boolean isConnected() {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_blockNumber");
            request.put("params", new Object[]{});
            request.put("id", 1);

            String response = makeRequest(request);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            return jsonResponse.has("result");
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Erreur connexion: " + e.getMessage());
            return false;
        }
    }

    public String getCurrentBlockNumber() {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_blockNumber");
            request.put("params", new Object[]{});
            request.put("id", 1);

            String response = makeRequest(request);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            if (jsonResponse.has("result")) {
                String hexBlock = jsonResponse.get("result").getAsString();
                return String.valueOf(Integer.parseInt(hexBlock.substring(2), 16));
            }
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Erreur bloc: " + e.getMessage());
        }
        return "0";
    }


    public String getBlockByNumber(int blockNumber) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_getBlockByNumber");
            request.put("params", new Object[]{"0x" + Integer.toHexString(blockNumber), true});
            request.put("id", 1);

            return makeRequest(request);
        } catch (Exception e) {
            System.out.println("[BLOCKCHAIN] ❌ Erreur getBlockByNumber: " + e.getMessage());
            return null;
        }
    }


    public boolean recordProductAddition(String productName, String quantity) {
        String txHash = addInventoryTransaction(productName, "AJOUT_PRODUIT", Integer.parseInt(quantity), "STOCK");
        return txHash != null;
    }

    public boolean recordProductModification(String productName, String newQuantity) {
        String txHash = addInventoryTransaction(productName, "MODIFICATION_PRODUIT", Integer.parseInt(newQuantity), "STOCK");
        return txHash != null;
    }

    public boolean recordProductDeletion(String productName) {
        String txHash = addInventoryTransaction(productName, "SUPPRESSION_PRODUIT", 0, "STOCK");
        return txHash != null;
    }

    public boolean recordMovementAddition(String productName, String quantity) {
        String txHash = addInventoryTransaction(productName, "AJOUT_MOUVEMENT", Integer.parseInt(quantity), "MOUVEMENT");
        return txHash != null;
    }

    public boolean recordMovementModification(String productName, String quantity) {
        String txHash = addInventoryTransaction(productName, "MODIFICATION_MOUVEMENT", Integer.parseInt(quantity), "MOUVEMENT");
        return txHash != null;
    }

    public boolean recordMovementDeletion(String productName, String quantity) {
        String txHash = addInventoryTransaction(productName, "SUPPRESSION_MOUVEMENT", Integer.parseInt(quantity), "MOUVEMENT");
        return txHash != null;
    }
}