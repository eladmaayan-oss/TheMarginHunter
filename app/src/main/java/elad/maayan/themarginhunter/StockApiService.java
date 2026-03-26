package elad.maayan.themarginhunter;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface StockApiService {
    @GET("query")
    Call<AlphaVantageResponse> getStockQuote(
            @Query("function") String function, // ערך: "GLOBAL_QUOTE"
            @Query("symbol") String ticker,     // למשל: "MSFT"
            @Query("apikey") String apiKey      // המפתח שלך
    );
    @GET("query")
    Call<CompanyOverviewResponse> getCompanyOverview(
            @Query("function") String function, // ערך: "OVERVIEW"
            @Query("symbol") String ticker,
            @Query("apikey") String apiKey
    );
    @GET("query")
    Call<AlphaVantageResponse> getDailySeries(
            @Query("function") String function, // כאן תשלח "TIME_SERIES_DAILY"
            @Query("symbol") String symbol,
            @Query("apikey") String apiKey
    );
    @GET("query")
    Call<CashFlowResponse> getCashFlow(
            @Query("function") String function, // כאן נעביר "CASH_FLOW"
            @Query("symbol") String symbol,     // הטיקר, למשל "AAPL"
            @Query("apikey") String apiKey      // המפתח שלך
    );
    @GET("quote")
    Call<FinnhubQuoteResponse> getStockQuote(
            @Query("symbol") String symbol,
            @Query("token") String apiKey
    );

    @GET("stock/metric")
    Call<FinnhubMetricResponse> getStockMetrics(
            @Query("symbol") String symbol,
            @Query("metric") String metricType, // אנחנו נעביר כאן "all"
            @Query("token") String apiKey
    );
    @GET("stock/profile2")
    Call<FinnhubProfileResponse> getCompanyProfile(
            @Query("symbol") String symbol,
            @Query("token") String apiKey
    );
    @GET
    Call<YahooSummaryResponse> getYahooSummary(@Url String url);
    @GET
    Call<YahooChartResponse> getYahooChart(@Url String url);
}