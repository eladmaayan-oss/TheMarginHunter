package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class YahooBulkQuoteResponse {
    @SerializedName("quoteResponse")
    private QuoteResponse quoteResponse;

    public QuoteResponse getQuoteResponse() { return quoteResponse; }

    public static class QuoteResponse {
        @SerializedName("result")
        private List<Quote> result;

        public List<Quote> getResult() { return result; }
    }

    public static class Quote {
        @SerializedName("symbol")
        private String symbol;

        @SerializedName("longName") // נוסיף את השדה הזה
        private String companyName;

        @SerializedName("regularMarketPrice")
        private double regularMarketPrice;

        public String getSymbol() { return symbol; }
        public double getRegularMarketPrice() { return regularMarketPrice; }
        public String getCompanyName() { return companyName; }
    }
}