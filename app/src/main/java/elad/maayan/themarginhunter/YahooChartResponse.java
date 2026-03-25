package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class YahooChartResponse {
    @SerializedName("chart")
    private Chart chart;

    public Chart getChart() { return chart; }

    public static class Chart {
        @SerializedName("result")
        private List<Result> result;
        public List<Result> getResult() { return result; }
    }

    public static class Result {
        @SerializedName("timestamp")
        private List<Long> timestamp;
        public List<Long> getTimestamp() { return timestamp; }

        @SerializedName("indicators")
        private Indicators indicators;
        public Indicators getIndicators() { return indicators; }
    }

    public static class Indicators {
        @SerializedName("quote")
        private List<Quote> quote;
        public List<Quote> getQuote() { return quote; }
    }

    public static class Quote {
        @SerializedName("close")
        private List<Double> close;
        public List<Double> getClose() { return close; }
    }
}