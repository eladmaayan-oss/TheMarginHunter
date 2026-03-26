package elad.maayan.themarginhunter;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://www.alphavantage.co/";
    private static Retrofit retrofit = null;

    public static StockApiService getApiService() {
        if (retrofit == null) {

            // הוספת לוגר כדי שתראה בדיוק מה נשלח ומה חוזר
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // כאן השינוי: מחברים גם את יאהו וגם את הלוגר ל-OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new YahooAuthInterceptor()) // הסוכן החשאי ליאהו
                    .addInterceptor(loggingInterceptor)         // הלוגר ששכחת לחבר
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client) // עכשיו הקליינט מכיל את שניהם
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(StockApiService.class);
    }
}