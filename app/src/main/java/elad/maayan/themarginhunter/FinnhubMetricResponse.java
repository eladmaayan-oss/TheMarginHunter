package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;

public class FinnhubMetricResponse {
    @SerializedName("metric")
    private MetricData metric;

    public MetricData getMetric() {
        return metric;
    }

    public static class MetricData {
        // שים לב: בדוק ב-Postman שלך איך בדיוק קוראים לשדה ה-EPS שם. לרוב זה נראה ככה:
        @SerializedName("epsTTM")
        private double eps;

        @SerializedName("epsGrowth5Y")
        private double growth;

        @SerializedName("dividendYieldIndicatedAnnual")
        private Double dividendYield;

        public Double getDividendYield() {
            return dividendYield != null ? dividendYield : 0.0;
        }
        public double getGrowth() {
            return growth;
        }


        public double getEps() {
            return eps;
        }
    }
}