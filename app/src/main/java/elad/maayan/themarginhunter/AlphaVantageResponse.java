package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class AlphaVantageResponse {

    @SerializedName("Global Quote")
    private StockQuote quote;

    @SerializedName("Time Series (Daily)")
    private Map<String, DailyData> timeSeries;

    public Map<String, DailyData> getTimeSeries() {
        return timeSeries;
    }


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

    public static class DailyData {
        @SerializedName("4. close")
        private String close;

        public String getClose() {
            return close;
        }
        public void setClose(String close) { this.close = close; }
    }
}