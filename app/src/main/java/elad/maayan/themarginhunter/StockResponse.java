package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;

public class StockResponse {
    @SerializedName("symbol")
    private String symbol;

    @SerializedName("companyName") // בפרופיל זה נקרא companyName ולא name
    private String companyName;

    @SerializedName("price")
    private double price;

    @SerializedName("lastDiv") // בונוס: בפרופיל יש גם דיבידנד
    private double lastDiv;

    @SerializedName("eps")
    private double eps;

    // Getters
    public String getName() { return companyName; } // נשאיר את השם getName כדי לא לשבור את הפרגמנט
    public double getPrice() { return price; }
    public double getEps() { return eps; }
}