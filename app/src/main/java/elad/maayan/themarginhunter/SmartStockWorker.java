package elad.maayan.themarginhunter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;

public class SmartStockWorker extends Worker {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public SmartStockWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // קוראים את הנתון שהעברנו כשזימנו את ה-Worker כדי לדעת איזה סוג עדכון זה
        boolean isFullRefresh = getInputData().getBoolean("is_full_refresh", false);
        Log.d("SmartWorker", "התעוררתי! האם זה עדכון שבועי מלא? " + isFullRefresh);

        try {
            StockApiService apiService = RetrofitClient.getApiService();

            // 1. משיכת כל המניות מ-Firebase (באופן סינכרוני שמותאם ל-Worker!)
            QuerySnapshot querySnapshot = Tasks.await(db.collection("stocks").get());

            for (QueryDocumentSnapshot doc : querySnapshot) {
                Stock stock = doc.toObject(Stock.class);
                String ticker = stock.getTicker();

                if (ticker == null || ticker.isEmpty()) continue;

                try {
                    // 2. קריאה ליאהו פיננסים
                    String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + ticker + "?modules=defaultKeyStatistics,financialData";
                    Response<YahooSummaryResponse> response = apiService.getYahooSummary(url).execute();

                    if (response.isSuccessful() && response.body() != null &&
                            response.body().getQuoteSummary() != null &&
                            !response.body().getQuoteSummary().getResult().isEmpty()) {

                        YahooSummaryResponse.Result result = response.body().getQuoteSummary().getResult().get(0);

                        // --- בדיקת נתוני מחיר (FinancialData) ---
                        if (result.getFinancialData() != null && result.getFinancialData().getCurrentPrice() != null) {
                            stock.setCurrentPrice(result.getFinancialData().getCurrentPrice().getRaw());

                            // עדכון FCF אם קיים
                            if (result.getFinancialData().getFreeCashflow() != null) {
                                stock.setFcf(result.getFinancialData().getFreeCashflow().getRaw());
                            }
                        }

                        // --- בדיקת נתונים סטטיסטיים (DefaultKeyStatistics) ---
                        if (result.getDefaultKeyStatistics() != null) {
                            // עדכון EPS
                            if (result.getDefaultKeyStatistics().getTrailingEps() != null) {
                                stock.setEps(result.getDefaultKeyStatistics().getTrailingEps().getRaw());
                            }
                            // עדכון כמות מניות
                            if (result.getDefaultKeyStatistics().getSharesOutstanding() != null) {
                                stock.setSharesOutstanding(result.getDefaultKeyStatistics().getSharesOutstanding().getRaw());
                            }
                        }

                        // --- בדיקת צמיחה (Growth) ---
                        double growthRate = 5.0; // ערך ברירת מחדל
                        if (result.getEarningsTrend() != null &&
                                result.getEarningsTrend().getTrend() != null &&
                                !result.getEarningsTrend().getTrend().isEmpty()) {

                            YahooSummaryResponse.Trend trend = result.getEarningsTrend().getTrend().get(0);
                            if (trend.getGrowth() != null) {
                                growthRate = trend.getGrowth().getRaw() * 100;
                            }
                        }
                        stock.setGrowthRate(Math.max(0, Math.min(growthRate, 20.0)));

                        stock.setLastUpdated(System.currentTimeMillis());

                        // --- חישובי שווי פנימי ו-Margin of Safety ---
                        // רק אם יש לנו נתונים בסיסיים (מחיר ו-EPS)
                        if (stock.getCurrentPrice() > 0 && stock.getEps() != 0) {

                            if (isFullRefresh) {
                                // נוסחת גראהם: EPS * (8.5 + 2 * g)
                                double intrinsic = stock.getEps() * (8.5 + 2 * stock.getGrowthRate());
                                stock.setIntrinsicValue(intrinsic);
                            }

                            if (stock.getIntrinsicValue() > 0) {
                                double mos = ((stock.getIntrinsicValue() - stock.getCurrentPrice()) / stock.getIntrinsicValue()) * 100;
                                stock.setMarginOfSafety(mos);

                                if (mos >= 30) {
                                    sendNotification(ticker, mos);
                                }
                            }
                        }

                        // 7. שמירה ב-Firebase
                        Tasks.await(db.collection("stocks").document(ticker).set(stock, com.google.firebase.firestore.SetOptions.merge()));
                    }
                    // נשימה קלה בין קריאות כדי לא להיחסם על ידי Yahoo
                    Thread.sleep(1000);

                } catch (Exception e) {
                    Log.e("SmartWorker", "שגיאה בעדכון המניה: " + ticker, e);
                }
            }
            return Result.success();

        } catch (Exception e) {
            Log.e("SmartWorker", "שגיאה כללית במשיכת הנתונים מ-Firebase", e);
            return Result.retry(); // אומר ל-WorkManager לנסות שוב מאוחר יותר כי הייתה תקלת רשת
        }
    }

    private void sendNotification(String ticker, double mos) {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "stock_alerts";

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("open_ticker", ticker);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                ticker.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Stock Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("התראות על מרווח ביטחון גבוה");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // שים לב: החלפתי לאייקון מובנה של אנדרואיד כדי שלא יקרוס לך אם R.drawable.trending_up לא קיים
                .setContentTitle("הזדמנות קנייה: " + ticker)
                .setContentText("מרווח הביטחון הגיע ל-" + String.format("%.1f%%", mos))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify(ticker.hashCode(), builder.build());
    }
}