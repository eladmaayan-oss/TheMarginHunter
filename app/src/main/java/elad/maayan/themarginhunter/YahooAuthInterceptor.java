package elad.maayan.themarginhunter;

import java.io.IOException;
import java.util.List;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient; // נוסף

public class YahooAuthInterceptor implements Interceptor {
    private static String cookie = null;
    private static String crumb = null;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String url = originalRequest.url().toString();

        // הגנה 1: אם זו לא בקשה ליאהו, אל תיגע בכלום (אלפא ונטאג' ופינהאב יעבדו רגיל)
        if (!url.contains("yahoo.com")) {
            return chain.proceed(originalRequest);
        }

        // הגנה 2: מניעת לולאה אינסופית בזמן משיכת טוקנים
        if (url.contains("fc.yahoo.com") || url.contains("getcrumb")) {
            return chain.proceed(originalRequest);
        }

        synchronized (this) {
            if (cookie == null || crumb == null) {
                fetchAuthTokens(); // שימוש בשיטה חיצונית ללא Chain
            }
        }

        HttpUrl.Builder urlBuilder = originalRequest.url().newBuilder();
        if (crumb != null) {
            urlBuilder.addQueryParameter("crumb", crumb);
        }

        Request.Builder requestBuilder = originalRequest.newBuilder()
                .url(urlBuilder.build())
                .header("User-Agent", USER_AGENT);

        if (cookie != null) {
            requestBuilder.header("Cookie", cookie);
        }

        return chain.proceed(requestBuilder.build());
    }

    private void fetchAuthTokens() {
        OkHttpClient internalClient = new OkHttpClient();

        try {
            // משיכת עוגייה
            Request cookieReq = new Request.Builder()
                    .url("https://fc.yahoo.com")
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response res = internalClient.newCall(cookieReq).execute()) {
                List<String> cookies = res.headers("Set-Cookie");
                if (!cookies.isEmpty()) {
                    cookie = cookies.get(0).split(";")[0];
                }
            }

            // משיכת קראמב
            if (cookie != null) {
                Request crumbReq = new Request.Builder()
                        .url("https://query1.finance.yahoo.com/v1/test/getcrumb")
                        .header("Cookie", cookie)
                        .header("User-Agent", USER_AGENT)
                        .build();
                try (Response res = internalClient.newCall(crumbReq).execute()) {
                    if (res.isSuccessful() && res.body() != null) {
                        crumb = res.body().string();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}