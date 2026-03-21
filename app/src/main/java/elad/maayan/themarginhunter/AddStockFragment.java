package elad.maayan.themarginhunter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
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

public class AddStockFragment extends BottomSheetDialogFragment {

    private TextInputEditText etTicker, etEPS, etGrowth, etPrice, etCompanyName;
    private TextInputLayout tilTicker, tilEPS, tilGrowth;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Button btnSave;
    private final String API_KEY = "3PY2WSBJ2P1ZUR0K";
    String divYieldStr;
    double divYield = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_stock, container, false);

        // אתחול ה-Views
        etTicker = view.findViewById(R.id.etTicker);
        etEPS = view.findViewById(R.id.etEPS);
        etGrowth = view.findViewById(R.id.etGrowth);
        etPrice = view.findViewById(R.id.etPrice);
        etCompanyName = view.findViewById(R.id.etCompanyName);
        btnSave = view.findViewById(R.id.btnSaveStock);
        tilTicker = view.findViewById(R.id.tilTicker);
        tilEPS = view.findViewById(R.id.tilEPS);
        tilGrowth = view.findViewById(R.id.tilGrowth);

        // מאזין ליציאה משדה הטיקר כדי למשוך נתונים
        etTicker.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String ticker = etTicker.getText().toString().toUpperCase().trim();
                if (!ticker.isEmpty()) fetchStockData(ticker);
            }
        });

        btnSave.setOnClickListener(v -> validateAndSave());

        // בדיקה אם אנחנו במצב עריכה
        if (getArguments() != null && getArguments().getString("ticker") != null) {
            setupEditMode(getArguments().getString("ticker"));
        }

        return view;
    }

    // --- הפונקציות שהיו חסרות לך ---

    private void fetchStockData(String ticker) {
        StockApiService api = RetrofitClient.getApiService();

        // 1. קריאה ראשונה: OVERVIEW
        api.getCompanyOverview("OVERVIEW", ticker, API_KEY).enqueue(new Callback<CompanyOverviewResponse>() {
            @Override
            public void onResponse(Call<CompanyOverviewResponse> call, Response<CompanyOverviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // אם קיבלנו את הודעת ה-Limit (הגוף לא מכיל EPS)
                    divYieldStr = response.body().getDividendYield();
                    if (response.body().getEps() == null) {
                        Log.e("API_LIMIT", "Hit API limit on Overview");
                        return;
                    }

                    String epsStr = response.body().getEps();
                    String companyName = response.body().getName();

                    // מחכים שנייה אחת לפני הקריאה הבאה כדי לא להיחסם
                    new android.os.Handler().postDelayed(() -> {

                        // 2. קריאה שנייה: GLOBAL_QUOTE
                        api.getStockQuote("GLOBAL_QUOTE", ticker, API_KEY).enqueue(new Callback<AlphaVantageResponse>() {
                            @Override
                            public void onResponse(Call<AlphaVantageResponse> call, Response<AlphaVantageResponse> priceResponse) {
                                if (priceResponse.isSuccessful() && priceResponse.body() != null) {
                                    AlphaVantageResponse.StockQuote quote = priceResponse.body().getQuote();

                                    if (quote != null && quote.getPrice() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            etCompanyName.setText(companyName);
                                            etPrice.setText(quote.getPrice());
                                            etEPS.setText(epsStr);
                                            String growthHint = response.body().getQuarterlyGrowth();
                                            if (growthHint != null && !growthHint.equals("0")) {
                                                // הופכים את הנתון לאחוז (למשל 0.15 הופך ל-15%)
                                                double hintVal = Double.parseDouble(growthHint) * 100;
                                                etGrowth.setHint("רמז (YOY): " + String.format("%.1f%%", hintVal));
                                            }
                                            Log.d("API_SUCCESS", "Fields updated!");
                                        });
                                    } else {
                                        Log.e("API_LIMIT", "Hit API limit on Quote");
                                    }
                                }
                            }
                            @Override public void onFailure(Call<AlphaVantageResponse> call, Throwable t) {}
                        });
                    }, 1500); // השהיה של 1.5 שניות
                }
            }
            @Override public void onFailure(Call<CompanyOverviewResponse> call, Throwable t) {}
        });

    }
    private void setupEditMode(String ticker) {
        etTicker.setText(ticker);
        etTicker.setEnabled(false); // אי אפשר לשנות טיקר קיים
        btnSave.setText("Update Stock");

        db.collection("stocks").document(ticker).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Stock stock = documentSnapshot.toObject(Stock.class);
                        if (stock != null) {
                            etEPS.setText(String.valueOf(stock.getEps()));
                            etGrowth.setText(String.valueOf(stock.getGrowthRate()));
                            etCompanyName.setText(stock.getName());
                            etPrice.setText(String.valueOf(stock.getPrice()));
                        }
                    }
                });
    }

    private double calculateMarginOfSafety(double currentPrice, double eps, double growthRate) {
        // נוסחת גרהאם: (8.5 + 2 * growthRate) * EPS
        double intrinsicValue = eps * (8.5 + 2 * growthRate);

        if (intrinsicValue <= 0) return 0;
        return ((intrinsicValue - currentPrice) / intrinsicValue) * 100;
    }

    private void validateAndSave() {
        String ticker = etTicker.getText().toString().trim().toUpperCase();
        String epsStr = etEPS.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String growthStr = etGrowth.getText().toString().trim();

        if (ticker.isEmpty() || epsStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (divYieldStr != null && !divYieldStr.isEmpty() && !divYieldStr.equalsIgnoreCase("None")) {
                divYield = Double.parseDouble(divYieldStr);
            } else {
                divYield = 0.0; // אם זה None או ריק, זה פשוט אפס
            }
        } catch (NumberFormatException e) {
            divYield = 0.0;
            Log.e("CONVERSION_ERROR", "Could not parse dividend");
        }

        double price = Double.parseDouble(priceStr);
        double eps = Double.parseDouble(epsStr);
        double growth = growthStr.isEmpty() ? 0 : Double.parseDouble(growthStr);
        double mos = calculateMarginOfSafety(price, eps, growth);

        saveToFirestore(ticker, etCompanyName.getText().toString(), price, eps, growth, mos);
    }

    private void saveToFirestore(String ticker, String companyName, double currentPrice, double eps, double growth, double mos) {
        Map<String, Object> stockData = new HashMap<>();
        stockData.put("ticker", ticker);
        stockData.put("companyName", companyName);
        stockData.put("currentPrice", currentPrice);
        stockData.put("eps", eps);
        stockData.put("growthRate", growth);
        stockData.put("marginOfSafety", mos);
        stockData.put("dividendYield", divYield);
        stockData.put("lastUpdated", System.currentTimeMillis());
// חישוב הערך הפנימי (Intrinsic Value) כי ה-Stock.java מצפה לו
        double intrinsicValue = eps * (8.5 + 2 * growth);
        stockData.put("intrinsicValue", intrinsicValue);

        db.collection("stocks").document(ticker)
                .set(stockData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Success!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                                .popBackStack();                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error saving", Toast.LENGTH_SHORT).show());
    }
}