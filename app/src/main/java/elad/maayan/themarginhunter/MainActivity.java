package elad.maayan.themarginhunter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
//        seedDatabase();

        // 1. בקשת הרשאת התראות (חובה לאנדרואיד 13 ומעלה)
        checkNotificationPermission();

        // 2. טיפול ב-Padding של המערכת
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 3. הגדרת הניווט (Bottom Navigation)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);

            // 4. בדיקה אם הגענו מהתראה (רק אחרי שהניווט מוכן)
            if (getIntent() != null && getIntent().hasExtra("open_ticker")) {
                String ticker = getIntent().getStringExtra("open_ticker");
                Toast.makeText(this, "בודק את המניה: " + ticker, Toast.LENGTH_LONG).show();

                Bundle bundle = new Bundle();
                bundle.putString("ticker", ticker); // שים לב לאותיות קטנות כדי שיתאים למה שתיקנו קודם!
                navController.navigate(R.id.action_global_addStockFragment, bundle);

                Log.d("NAV", "Navigated to details for: " + ticker);
            }
        }

        // 5. הפעלת משימות הרקע
//        setupBackgroundWork();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void setupBackgroundWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        // עבודה 1: בדיקת מחירים מהירה כל 12 שעות
        Data fastCheckData = new Data.Builder()
                .putBoolean("is_full_refresh", false)
                .build();

        PeriodicWorkRequest priceCheckRequest =
                new PeriodicWorkRequest.Builder(SmartStockWorker.class, 12, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .setInputData(fastCheckData)
                        .setInitialDelay(1, TimeUnit.HOURS) // אל תרוץ מיד בפתיחת האפליקציה
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "StockPriceCheckWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                priceCheckRequest
        );

        // עבודה 2: עדכון נתונים פיננסיים מלא (EPS וכו') פעם ב-7 ימים
        Data fullRefreshData = new Data.Builder()
                .putBoolean("is_full_refresh", true)
                .build();

        PeriodicWorkRequest weeklyRefreshRequest =
                new PeriodicWorkRequest.Builder(SmartStockWorker.class, 7, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .setInputData(fullRefreshData)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "WeeklyStockUpdate",
                ExistingPeriodicWorkPolicy.KEEP,
                weeklyRefreshRequest
        );

        Log.d("WORK_MANAGER", "Smart Background tasks scheduled successfully.");
    }

//    private void seedDatabase() {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        // בדיקה אם האוסף ריק
//        db.collection("stocks").limit(1).get().addOnSuccessListener(snapshot -> {
//            if (snapshot.isEmpty()) {
//                // רשימת המניות הראשונית שלך
//                String[] initialStocks = {"AAPL", "GOOGL", "MSFT", "TSLA", "NVDA"};
//                for (String ticker : initialStocks) {
//                    Stock s = new Stock();
//                    s.setTicker(ticker);
//                    s.setCompanyName("Loading...");
//                    db.collection("stocks").document(ticker).set(s);
//                }
//                Toast.makeText(this, "מאתחל נתונים ראשוניים...", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

}
