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
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client) // מחבר את הלוגר
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(StockApiService.class);
    }
}