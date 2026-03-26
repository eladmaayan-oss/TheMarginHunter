package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class YahooSummaryResponse {
    @SerializedName("quoteSummary")
    private QuoteSummary quoteSummary;

    public QuoteSummary getQuoteSummary() { return quoteSummary; }

    public static class QuoteSummary {
        @SerializedName("result")
        private List<Result> result;
        public List<Result> getResult() { return result; }
    }

    public static class Result {
        @SerializedName("defaultKeyStatistics")
        private DefaultKeyStatistics defaultKeyStatistics;

        @SerializedName("financialData")
        private FinancialData financialData;

        @SerializedName("price")
        private Price price;

        // *** התוספת החדשה שלנו לתחזיות צמיחה ***
        @SerializedName("earningsTrend")
        private EarningsTrend earningsTrend;

        public DefaultKeyStatistics getDefaultKeyStatistics() { return defaultKeyStatistics; }
        public FinancialData getFinancialData() { return financialData; }
        public Price getPrice() { return price; }
        public EarningsTrend getEarningsTrend() { return earningsTrend; }
    }

    public static class DefaultKeyStatistics {
        @SerializedName("sharesOutstanding")
        private RawValue sharesOutstanding;

        // *** התוספת החדשה שלנו לחישוב הסיכון ***
        @SerializedName("beta")
        private RawValue beta;

        public RawValue getSharesOutstanding() { return sharesOutstanding; }
        public RawValue getBeta() { return beta; }
    }

    // ... (FinancialData ו-Price נשארים אותו דבר) ...
    public static class FinancialData {
        @SerializedName("freeCashflow")
        private RawValue freeCashflow;
        public RawValue getFreeCashflow() { return freeCashflow; }
    }

    public static class Price {
        @SerializedName("regularMarketPrice")
        private RawValue regularMarketPrice;
        public RawValue getRegularMarketPrice() { return regularMarketPrice; }
    }

    // *** מחלקות חדשות למשיכת תחזית האנליסטים ל-5 שנים ***
    public static class EarningsTrend {
        @SerializedName("trend")
        private List<Trend> trend;
        public List<Trend> getTrend() { return trend; }
    }

    public static class Trend {
        @SerializedName("period")
        private String period; // יאהו מחזירים כאן "+5y"

        @SerializedName("growth")
        private RawValue growth; // הצמיחה הצפויה

        public String getPeriod() { return period; }
        public RawValue getGrowth() { return growth; }
    }

    public static class RawValue {
        @SerializedName("raw")
        private double raw;
        public double getRaw() { return raw; }
    }
}