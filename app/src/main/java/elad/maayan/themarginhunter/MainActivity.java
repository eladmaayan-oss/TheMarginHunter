package elad.maayan.themarginhunter;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
// בדיקה אם הגענו מהתראה
        if (getIntent() != null && getIntent().hasExtra("open_ticker")) {
            String ticker = getIntent().getStringExtra("open_ticker");
            Toast.makeText(this, "בודק את המניה: " + ticker, Toast.LENGTH_LONG).show();

            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();

                // יצירת Bundle עם הנתונים למסך הבא
                Bundle bundle = new Bundle();
                bundle.putString("ticker", ticker);

                // ניווט למסך הוספה/עריכה (או מסך פרטים אם קיים)
                // השתמש ב-ID של ה-Action שיש לך ב-nav_graph.xml
                navController.navigate(R.id.action_global_addStockFragment, bundle);

                Log.d("NAV", "Navigated to details for: " + ticker);
            }
            // כאן תוכל להוסיף לוגיקה של ניווט (Navigation) למסך פירוט המניה אם תרצה
        }
        setupBackgroundWork();
        // טיפול ב-Padding של המערכת (סטטוס בר וכדומה)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // הגדרת הניווט
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            // חיבור ה-BottomNav ל-Navigation Components
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
        PeriodicWorkRequest priceCheckRequest =
                new PeriodicWorkRequest.Builder(PriceCheckWorker.class, 12, TimeUnit.HOURS)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED) // רק כשיש אינטרנט
                                .build())
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "StockPriceCheck",
                ExistingPeriodicWorkPolicy.KEEP, // אל תתחיל מחדש אם כבר יש תור
                priceCheckRequest
        );
    }
    private void setupBackgroundWork() {
        // הגדרת אילוצים: רק כשיש אינטרנט והסוללה לא במצב חיסכון קיצוני
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // יצירת בקשת עבודה מחזורית (כל 12 שעות)
        PeriodicWorkRequest priceCheckRequest =
                new PeriodicWorkRequest.Builder(PriceCheckWorker.class, 12, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .setInitialDelay(1, TimeUnit.HOURS) // התחלה ראשונה רק עוד שעה (לא ישר בפתיחה)
                        .build();

        // רישום העבודה במערכת (KEEP אומר שאם כבר קיימת עבודה כזו, אל תדרוס אותה)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "StockPriceCheckWork",
                ExistingPeriodicWorkPolicy.KEEP,
                priceCheckRequest
        );

        Log.d("WORK_MANAGER", "Background work scheduled successfully.");
    }
}