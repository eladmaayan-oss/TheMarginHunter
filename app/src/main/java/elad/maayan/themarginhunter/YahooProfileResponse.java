package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class YahooProfileResponse {
    @SerializedName("quoteSummary")
    private QuoteSummary quoteSummary;

    public QuoteSummary getQuoteSummary() { return quoteSummary; }

    public static class QuoteSummary {
        @SerializedName("result")
        private List<Result> result;

        public List<Result> getResult() { return result; }
    }

    public static class Result {
        @SerializedName("summaryProfile")
        private SummaryProfile summaryProfile;

        public SummaryProfile getSummaryProfile() { return summaryProfile; }
    }

    public static class SummaryProfile {
        @SerializedName("sector")
        private String sector;

        public String getSector() { return sector; }
    }
}