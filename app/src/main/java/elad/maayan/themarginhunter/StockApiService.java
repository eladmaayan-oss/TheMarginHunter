package elad.maayan.themarginhunter;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

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
}