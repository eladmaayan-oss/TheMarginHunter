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

        @SerializedName("earningsTrend")
        private EarningsTrend earningsTrend;

        public DefaultKeyStatistics getDefaultKeyStatistics() { return defaultKeyStatistics; }
        public FinancialData getFinancialData() { return financialData; }
        public EarningsTrend getEarningsTrend() { return earningsTrend; }
    }

    public static class DefaultKeyStatistics {
        @SerializedName("sharesOutstanding")
        private RawValue sharesOutstanding;

        @SerializedName("trailingEps")
        private RawValue trailingEps;

        @SerializedName("beta")
        private RawValue beta;

        public RawValue getSharesOutstanding() { return sharesOutstanding; }
        public RawValue getTrailingEps() { return trailingEps; }
        public RawValue getBeta() { return beta; }
    }

    public static class FinancialData {
        @SerializedName("currentPrice")
        private RawValue currentPrice;
        @SerializedName("freeCashflow")
        private RawValue freeCashflow;

        public RawValue getFreeCashflow() {
            return freeCashflow;
        }
        public RawValue getCurrentPrice() { return currentPrice; }
    }


    public static class EarningsTrend {
        @SerializedName("trend")
        private List<Trend> trend;

        public List<Trend> getTrend() { return trend; }
    }

    public static class Trend {
        @SerializedName("period")
        private String period;

        @SerializedName("growth")
        private RawValue growth;

        public String getPeriod() { return period; }
        public RawValue getGrowth() { return growth; }
    }

    public static class RawValue {
        @SerializedName("raw")
        private double raw;

        public double getRaw() { return raw; }
    }
}