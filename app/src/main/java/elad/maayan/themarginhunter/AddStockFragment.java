package elad.maayan.themarginhunter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Callback;
import retrofit2.Call;
import retrofit2.Response;

public class AddStockFragment extends Fragment {

    private TextInputEditText etTicker, etEPS, etGrowth,etPrice, etCompanyName;
    private TextInputLayout tilTicker, tilEPS, tilGrowth;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Button btnSave;
    private final String API_KEY = "3PY2WSBJ2P1ZUR0K";
    private StockApiService apiService = RetrofitClient.getApiService();

    // ב-Alpha Vantage אנחנו שולחים function כפרמטר
    Call<AlphaVantageResponse> call = apiService.getStockQuote(
            "GLOBAL_QUOTE",
            "MSFT",
            "3PY2WSBJ2P1ZUR0K"
    );


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_stock, container, false);

        etTicker = view.findViewById(R.id.etTicker);
        etEPS = view.findViewById(R.id.etEPS);
        etGrowth = view.findViewById(R.id.etGrowth);
        etPrice = view.findViewById(R.id.etPrice);
        etCompanyName = view.findViewById(R.id.etCompanyName);

        tilTicker = view.findViewById(R.id.tilTicker);
        tilEPS = view.findViewById(R.id.tilEPS);
        tilGrowth = view.findViewById(R.id.tilGrowth);

        btnSave = view.findViewById(R.id.btnSaveStock);

        call.enqueue(new Callback<AlphaVantageResponse>() {
            @Override
            public void onResponse(Call<AlphaVantageResponse> call, Response<AlphaVantageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AlphaVantageResponse.StockQuote quote = response.body().getQuote();
                    if (quote != null) {
                        Log.d("API_SUCCESS", "Price: " + quote.getPrice());
                    } else {
                        Log.e("API_ERROR", "Note: Check if you hit the 5 calls per minute limit!");
                    }
                }
            }

            @Override
            public void onFailure(Call<AlphaVantageResponse> call, Throwable t) {
                Log.e("API_FAILURE", t.getMessage());
            }
        });

        etTicker.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { // המשתמש סיים להקליד ועבר לשדה הבא
                String ticker = etTicker.getText().toString().toUpperCase().trim();
                Log.d("API_CHECK", "User left Ticker field. Ticker: " + ticker); // בדיקה בלוג
                if (!ticker.isEmpty()) {
                    fetchStockData(ticker);
                }
            }
        });
        btnSave.setOnClickListener(v -> validateAndSave());
        String editTicker = getArguments() != null ? getArguments().getString("ticker") : null;
        if (editTicker != null) {
            // אנחנו במצב עריכה!
            etTicker.setText(editTicker);
            etTicker.setEnabled(false); // אסור לשנות טיקר של מניה קיימת (זה המפתח ב-DB)
            btnSave.setText("Update Stock"); // שינוי טקסט הכפתור בשביל ה-UX

            // משיכת הנתונים הנוכחיים מהענן כדי למלא את השדות
            db.collection("stocks").document(editTicker).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Stock stock = documentSnapshot.toObject(Stock.class);
                            if (stock != null) {
                                etEPS.setText(String.valueOf(stock.getEps()));
                                etGrowth.setText(String.valueOf(stock.getGrowthRate()));
                                // אפשר למלא כאן עוד שדות אם הוספת
                            }
                        }
                    });
        }
        return view;
    }
    private void fetchStockData(String ticker) {
        StockApiService api = RetrofitClient.getApiService();

        // 1. נמשוך קודם את ה-EPS מה-OVERVIEW
        api.getCompanyOverview("OVERVIEW", ticker, API_KEY).enqueue(new Callback<CompanyOverviewResponse>() {
            @Override
            public void onResponse(Call<CompanyOverviewResponse> call, Response<CompanyOverviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String epsStr = response.body().getEps();
                    String companyName = response.body().getName();

                    // 2. עכשיו נמשוך את המחיר מה-GLOBAL_QUOTE
                    api.getStockQuote("GLOBAL_QUOTE", ticker, API_KEY).enqueue(new Callback<AlphaVantageResponse>() {
                        @Override
                        public void onResponse(Call<AlphaVantageResponse> call, Response<AlphaVantageResponse> priceResponse) {
                            if (priceResponse.isSuccessful() && priceResponse.body() != null) {
                                AlphaVantageResponse.StockQuote quote = priceResponse.body().getQuote();

                                double price = Double.parseDouble(quote.getPrice());
                                double eps = Double.parseDouble(epsStr);
                                double mos = calculateMarginOfSafety(price, eps);

                                getActivity().runOnUiThread(() -> {
                                    etCompanyName.setText(companyName);
                                    etPrice.setText(String.format("%.2f", price));
                                    etEPS.setText(String.valueOf(eps));

                                    // הצגת מרווח הביטחון (למשל ב-TextView חדש או בלוג)
                                    Log.d("ANALYSIS", "Margin of Safety: " + String.format("%.2f%%", mos));
                                    // אם המרווח חיובי - המניה "זולה", אם שלילי - היא "יקרה"
                                });
                            }
                        }
                        @Override public void onFailure(Call<AlphaVantageResponse> call, Throwable t) {}
                    });
                }
            }
            @Override public void onFailure(Call<CompanyOverviewResponse> call, Throwable t) {}
        });
    }

    private double calculateMarginOfSafety(double currentPrice, double eps) {
        // הערכה שמרנית: מכפיל רווח הוגן של 15 (אפשר לשנות לפי ראות עיניך)
        double intrinsicValue = eps * 15;

        if (intrinsicValue <= 0) return 0;

        // חישוב המרווח: כמה המחיר הנוכחי נמוך מהערך הפנימי
        return ((intrinsicValue - currentPrice) / intrinsicValue) * 100;
    }


    private void validateAndSave() {
        String ticker = etTicker.getText().toString().trim().toUpperCase();
        String epsStr = etEPS.getText().toString().trim();
        String growthStr = etGrowth.getText().toString().trim();

        boolean isValid = true;

// --- וולידציה ל-Ticker ---
        if (ticker.isEmpty()) {
            tilTicker.setError("חובה להזין סימול מניה");
            isValid = false;
        } else if (ticker.length() > 5) {
            tilTicker.setError("סימול מניה לא יכול לעלות על 5 תווים");
            isValid = false;
        } else {
            tilTicker.setError(null); // ניקוי שגיאה אם תקין
        }
// --- וולידציה ל-EPS ---
        if (epsStr.isEmpty()) {
            tilEPS.setError("חובה להזין רווח למניה");
            isValid = false;
        } else {
            try {
                double eps = Double.parseDouble(epsStr);
                tilEPS.setError(null);
            } catch (NumberFormatException e) {
                tilEPS.setError("נא להזין מספר תקין");
                isValid = false;
            }
        }
        // --- וולידציה ל-Growth Rate ---
        if (growthStr.isEmpty()) {
            tilGrowth.setError("חובה להזין קצב צמיחה");
            isValid = false;
        } else {
            try {
                double growth = Double.parseDouble(growthStr);
                if (growth < 0 || growth > 100) {
                    tilGrowth.setError("קצב צמיחה חייב להיות בין 0 ל-100");
                    isValid = false;
                } else {
                    tilGrowth.setError(null);
                }
            } catch (NumberFormatException e) {
                tilGrowth.setError("נא להזין מספר תקין");
                isValid = false;
            }
        }
        if (isValid) {
            saveToFirestore(ticker, Double.parseDouble(epsStr), Double.parseDouble(growthStr));
        }
    }

    private void saveToFirestore(String ticker, double eps, double growth) {
        // יצירת אובייקט זמני למשלוח (הענן יחשב את השאר)
        Map<String, Object> stockData = new HashMap<>();
        stockData.put("eps", eps);
        stockData.put("growthRate", growth);
        stockData.put("lastUpdated", System.currentTimeMillis());

        db.collection("stocks").document(ticker)
                .set(stockData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), ticker + " added to Hunt!", Toast.LENGTH_SHORT).show();
                    // חזרה למסך ה-Watchlist
                    Navigation.findNavController(getView()).navigateUp();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error saving", Toast.LENGTH_SHORT).show());
    }


}