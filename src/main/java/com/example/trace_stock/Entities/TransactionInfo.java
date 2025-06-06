package com.example.trace_stock.Entities;

public class TransactionInfo {
    private String hash;
    private String from;
    private String to;
    private String blockNumber;
    private String data;
    private String decodedData;
    private long timestamp;


    public TransactionInfo() {
        this.timestamp = System.currentTimeMillis();
    }


    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }


    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getBlockNumber() { return blockNumber; }
    public void setBlockNumber(String blockNumber) { this.blockNumber = blockNumber; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getDecodedData() { return decodedData; }
    public void setDecodedData(String decodedData) { this.decodedData = decodedData; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "TransactionInfo{" +
                "hash='" + hash + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", blockNumber='" + blockNumber + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}