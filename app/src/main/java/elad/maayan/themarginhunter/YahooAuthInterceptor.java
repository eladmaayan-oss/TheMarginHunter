package elad.maayan.themarginhunter;

import java.io.IOException;
import java.util.List;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class YahooAuthInterceptor implements Interceptor {
    private static String cookie = null;
    private static String crumb = null;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // 1. מוודאים שיש לנו עוגייה וקראמב, ואם אין - מושכים אותם
        synchronized (this) {
            if (cookie == null || crumb == null) {
                fetchAuthTokens(chain);
            }
        }

        // 2. שותלים את הקראמב בכתובת ה-URL
        HttpUrl.Builder urlBuilder = originalRequest.url().newBuilder();
        if (crumb != null) {
            urlBuilder.addQueryParameter("crumb", crumb);
        }

        // 3. שותלים את העוגייה והדפדפן המזויף בכותרות הבקשה
        Request.Builder requestBuilder = originalRequest.newBuilder()
                .url(urlBuilder.build())
                .header("User-Agent", USER_AGENT);

        if (cookie != null) {
            requestBuilder.header("Cookie", cookie);
        }

        return chain.proceed(requestBuilder.build());
    }

    private void fetchAuthTokens(Chain chain) throws IOException {
        // משיכת עוגייה מיאהו
        Request cookieReq = new Request.Builder()
                .url("https://fc.yahoo.com")
                .header("User-Agent", USER_AGENT)
                .build();

        try (Response cookieRes = chain.proceed(cookieReq)) {
            List<String> cookies = cookieRes.headers("Set-Cookie");
            if (!cookies.isEmpty()) {
                cookie = cookies.get(0).split(";")[0]; // שומרים רק את מזהה העוגייה
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // משיכת הקראמב בעזרת העוגייה
        if (cookie != null) {
            Request crumbReq = new Request.Builder()
                    .url("https://query1.finance.yahoo.com/v1/test/getcrumb")
                    .header("Cookie", cookie)
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response crumbRes = chain.proceed(crumbReq)) {
                if (crumbRes.isSuccessful() && crumbRes.body() != null) {
                    crumb = crumbRes.body().string();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}