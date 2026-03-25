package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;

public class FinnhubQuoteResponse {
    @SerializedName("c") // "c" = Current price בפינהאב
    private double currentPrice;

    public double getCurrentPrice() {
        return currentPrice;
    }
}
