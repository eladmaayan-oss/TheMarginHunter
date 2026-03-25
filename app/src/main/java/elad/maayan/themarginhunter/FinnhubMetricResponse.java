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

        public double getGrowth() {
            return growth;
        }

        public double getEps() {
            return eps;
        }
    }
}