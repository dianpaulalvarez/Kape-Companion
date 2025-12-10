public class PaymentProof {
    private String id;
    private String userId;
    private String userName;
    private double amount;
    private long timestamp;
    private String proofImageUrl;
    private String status; // pending, approved, rejected

    public PaymentProof() {
    }

    public PaymentProof(String userId, String userName, double amount, long timestamp, String proofImageUrl, String status) {
        this.userId = userId;
        this.userName = userName;
        this.amount = amount;
        this.timestamp = timestamp;
        this.proofImageUrl = proofImageUrl;
        this.status = status;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getProofImageUrl() {
        return proofImageUrl;
    }

    public void setProofImageUrl(String proofImageUrl) {
        this.proofImageUrl = proofImageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}