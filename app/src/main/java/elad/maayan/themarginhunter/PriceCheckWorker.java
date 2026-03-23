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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class PriceCheckWorker extends Worker {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public PriceCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("WORKER", "Background price check started...");

        // כאן נמשוך את כל המניות מה-Firestore ונבדוק מחירים מה-API
        // אם מניה חוצה את ה-30% MOS, נקרא לפונקציה של ההתראה
        checkStocksAndNotify();

        return Result.success();
    }

    private void checkStocksAndNotify() {
        db.collection("stocks").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Stock stock = doc.toObject(Stock.class);
                // כאן תבוא הלוגיקה של משיכת המחיר החדש מה-API (כמו ב-Refresh)
                // ואם ה-MOS החדש > 30, נשלח התראה
                if (stock.getMarginOfSafety() >= 30) {
                    sendNotification(stock.getTicker(), stock.getMarginOfSafety());
                }
            }
        });
    }

    private void sendNotification(String ticker, double mos) {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "stock_alerts";

        // 1. יצירת ה-Intent שיפתח את ה-MainActivity
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // שליחת ה-Ticker כנתון נוסף (Extra) כדי שנדע איזו מניה להציג
        intent.putExtra("open_ticker", ticker);

        // 2. עטיפת ה-Intent ב-PendingIntent
        // שימוש ב-FLAG_IMMUTABLE הוא חובה באנדרואיד מודרני (12+)
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                ticker.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // 3. הגדרת ה-Channel (עבור אנדרואיד 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Stock Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("התראות על מרווח ביטחון גבוה");
            manager.createNotificationChannel(channel);
        }

// 4. בניית ההתראה
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.trending_up)// אייקון של גרף עולה
                .setContentTitle("הזדמנות קנייה: " + ticker)
                .setContentText("מרווח הביטחון הגיע ל-" + String.format("%.1f%%", mos))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // ההתראה תיעלם אחרי הלחיצה
                .setContentIntent(pendingIntent); // חיבור ה-PendingIntent

        manager.notify(ticker.hashCode(), builder.build());
    }
}