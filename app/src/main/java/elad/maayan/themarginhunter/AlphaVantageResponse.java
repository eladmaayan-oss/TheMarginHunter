package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;

public class AlphaVantageResponse {

    @SerializedName("Global Quote")
    private StockQuote quote;

    public StockQuote getQuote() {
        return quote;
    }

    public static class StockQuote {
        @SerializedName("01. symbol")
        private String symbol;

        @SerializedName("05. price")
        private String price;

        // הנה ה-Getters שהיו חסרים לך:
        public String getSymbol() {
            return symbol;
        }

        public String getPrice() {
            return price;
        }
    }
}