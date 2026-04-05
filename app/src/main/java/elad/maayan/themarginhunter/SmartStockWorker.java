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

                    if (response.isSuccessful() && response.body() != null) {
                        YahooSummaryResponse.Result result = response.body().getQuoteSummary().getResult().get(0);

                        double currentPrice = result.getFinancialData().getCurrentPrice().getRaw();
                        double intrinsicValue = stock.getIntrinsicValue(); // מתחילים עם הערך הקיים

                        Map<String, Object> updates = new HashMap<>();
                        if (stock.getCompanyName() == null || stock.getCompanyName().isEmpty()) {
                            updates.put("companyName", ticker);
                        }
                        updates.put("currentPrice", currentPrice);
                        updates.put("lastUpdated", System.currentTimeMillis());

                        // 3. אם זה העדכון השבועי המלא - נחשב גם ערך פנימי מחדש
                        if (isFullRefresh) {
                            double eps = result.getDefaultKeyStatistics().getTrailingEps().getRaw();
                            // שימוש בתחזית הצמיחה הספציפית של המניה (ואם חסר, ברירת מחדל של 5%)
                            double expectedGrowth = stock.getExpectedGrowth() > 0 ? stock.getExpectedGrowth() : 5.0;

                            intrinsicValue = eps * (8.5 + 2 * expectedGrowth);

                            updates.put("eps", eps);
                            updates.put("intrinsicValue", intrinsicValue);
                        }

                        // 4. חישוב מרווח ביטחון (MOS) עדכני מול המחיר החדש
                        double mos = 0;
                        if (intrinsicValue > 0) {
                            mos = ((intrinsicValue - currentPrice) / intrinsicValue) * 100;
                            updates.put("marginOfSafety", mos);
                        }

                        // 5. שמירה ב-Firebase (באופן סינכרוני)
                        Tasks.await(db.collection("stocks").document(ticker).update(updates));

                        // 6. בדיקת מציאות - אם המניה במבצע, נקפיץ התראה!
                        if (mos >= 30) {
                            sendNotification(ticker, mos);
                        }
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