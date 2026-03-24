package elad.maayan.themarginhunter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DcfFragment extends Fragment {
    private static final String API_KEY = "BH00QGEFNFNZ1IDN";
    private TextInputEditText etTicker, etGrowth, etDiscount, etTerminalGrowth;
    private MaterialButton btnCalculate;
    private ProgressBar progressBar;
    private MaterialCardView cardResult;
    private TextView tvIntrinsicValue, tvCurrentPrice, tvConclusion;
    private static final int DAILY_LIMIT = 25;
    private int apiRequestsLeft;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dcf, container, false);
        db = FirebaseFirestore.getInstance();
        initViews(view);
        return view;
    }
    private void initViews(View view) {
        etTicker = view.findViewById(R.id.etDcfTicker);
        etGrowth = view.findViewById(R.id.etDcfGrowth);
        etDiscount = view.findViewById(R.id.etDcfDiscountRate);
        etTerminalGrowth = view.findViewById(R.id.etDcfTerminalGrowth);
        btnCalculate = view.findViewById(R.id.btnCalculateDcf);
        progressBar = view.findViewById(R.id.progressBarDcf);
        cardResult = view.findViewById(R.id.cardDcfResult);
        tvIntrinsicValue = view.findViewById(R.id.tvDcfIntrinsicValue);
        tvCurrentPrice = view.findViewById(R.id.tvDcfCurrentPrice);
        tvConclusion = view.findViewById(R.id.tvDcfConclusion);

        btnCalculate.setOnClickListener(v -> startDcfProcess());
        initApiCounter();
    }
    private void startDcfProcess() {
        String ticker = etTicker.getText().toString().trim().toUpperCase();
        String growthStr = etGrowth.getText().toString().trim();
        String discountStr = etDiscount.getText().toString().trim();
        String terminalStr = etTerminalGrowth.getText().toString().trim();

        if (TextUtils.isEmpty(ticker) || TextUtils.isEmpty(growthStr) || TextUtils.isEmpty(discountStr) || TextUtils.isEmpty(terminalStr)) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double growthRate = Double.parseDouble(growthStr) / 100.0;
        double discountRate = Double.parseDouble(discountStr) / 100.0;
        double terminalGrowth = Double.parseDouble(terminalStr) / 100.0;

        progressBar.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);
        btnCalculate.setEnabled(false);

        db.collection("stocks").document(ticker).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("fcf") && documentSnapshot.contains("sharesOutstanding")) {
                Log.d("DCF", "Data found in Firebase! No API calls needed.");
                try {
                    double fcf = Double.parseDouble(documentSnapshot.getString("fcf"));
                    double shares = Double.parseDouble(documentSnapshot.getString("sharesOutstanding"));
                    double currentPrice = documentSnapshot.contains("price") ? Double.parseDouble(documentSnapshot.getString("price")) : 0.0;

                    calculateAndDisplayDcf(fcf, shares, currentPrice, growthRate, discountRate, terminalGrowth);
                } catch (Exception e) {
                    // אם הייתה שגיאת המרה מה-DB, נמשוך מחדש מה-API
                    fetchDataFromApi(ticker, growthRate, discountRate, terminalGrowth);
                }
            } else {
                if (apiRequestsLeft < 3) {
                    Toast.makeText(getContext(), "חרגת ממכסת ה-API להיום!", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    btnCalculate.setEnabled(true);
                    return; // עוצרים הכל ולא פונים ל-API
                }
                Log.d("DCF", "Data missing in Firebase. Fetching from API...");
                fetchDataFromApi(ticker, growthRate, discountRate, terminalGrowth);
            }
        }).addOnFailureListener(e -> {
            fetchDataFromApi(ticker, growthRate, discountRate, terminalGrowth);

        });
    }
    // שרשור קריאות API: קודם Cash Flow, ואז Overview (למניות), ואז Quote (למחיר)
    private void fetchDataFromApi(String ticker, double growthRate, double discountRate, double terminalGrowth) {
        decreaseApiCount(3);
        StockApiService api = RetrofitClient.getApiService();

        // 1. קריאה ל-CASH_FLOW
        api.getCashFlow("CASH_FLOW", ticker, API_KEY).enqueue(new Callback<CashFlowResponse>() {
            @Override
            public void onResponse(Call<CashFlowResponse> call, Response<CashFlowResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getAnnualReports() != null && !response.body().getAnnualReports().isEmpty()) {
                    AnnualReport report = response.body().getAnnualReports().get(0);
                    try {
                        double opCash = Double.parseDouble(report.getOperatingCashflow());
                        double capEx = Double.parseDouble(report.getCapitalExpenditures());
                        double fcf = opCash - Math.abs(capEx);

                        // 2. קריאה ל-OVERVIEW כדי להביא Shares Outstanding
                        fetchOverviewForDcf(api, ticker, fcf, growthRate, discountRate, terminalGrowth);
                    } catch (NumberFormatException e) {
                        showError("Failed to parse Cash Flow data");
                    }
                } else {
                    showError("API Error: Cash Flow data not found");
                }
            }
            @Override
            public void onFailure(Call<CashFlowResponse> call, Throwable t) { showError("Network Error"); }
        });
    }
    private void fetchOverviewForDcf(StockApiService api, String ticker, double fcf, double growthRate, double discountRate, double terminalGrowth) {
        api.getCompanyOverview("OVERVIEW", ticker, API_KEY).enqueue(new Callback<CompanyOverviewResponse>() {
            @Override
            public void onResponse(Call<CompanyOverviewResponse> call, Response<CompanyOverviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        double shares = Double.parseDouble(response.body().getSharesOutstanding());
                        // 3. קריאה למחיר הנוכחי
                        fetchQuoteForDcf(api, ticker, fcf, shares, growthRate, discountRate, terminalGrowth);
                    } catch (Exception e) {
                        showError("Failed to parse Shares Outstanding");
                    }
                }
            }
            @Override
            public void onFailure(Call<CompanyOverviewResponse> call, Throwable t) { showError("Network Error"); }
        });
    }

    private void initApiCounter() {
        if (getActivity() == null) return;

        SharedPreferences apiPrefs = requireContext().getSharedPreferences("API_PREFS", Context.MODE_PRIVATE);
        long lastReset = apiPrefs.getLong("last_reset_date", 0);
        apiRequestsLeft = apiPrefs.getInt("requests_left", DAILY_LIMIT);

        // אם עברו 24 שעות (86,400,000 מילישניות), נאפס חזרה ל-25
        if (System.currentTimeMillis() - lastReset > 86400000) {
            apiRequestsLeft = DAILY_LIMIT;
            apiPrefs.edit()
                    .putLong("last_reset_date", System.currentTimeMillis())
                    .putInt("requests_left", apiRequestsLeft)
                    .apply();
        }
        updateCalculateButtonUI();
    }

    private void updateCalculateButtonUI() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            btnCalculate.setText("Calculate (" + apiRequestsLeft + " calls left)");

            // צביעת הטקסט באדום אם אין מספיק קריאות לחישוב מלא (צריך 3 קריאות)
            if (apiRequestsLeft < 3) {
                btnCalculate.setTextColor(android.graphics.Color.RED);
            } else {
                btnCalculate.setTextColor(android.graphics.Color.WHITE); // או הצבע הדיפולטיבי של הכפתור שלך
            }
        });
    }
    private void decreaseApiCount(int amount) {
        if (getActivity() == null) return;
        apiRequestsLeft -= amount;
        if (apiRequestsLeft < 0) apiRequestsLeft = 0; // לא נרד מתחת לאפס

        SharedPreferences prefs = requireContext().getSharedPreferences("API_PREFS", Context.MODE_PRIVATE);
        prefs.edit().putInt("requests_left", apiRequestsLeft).apply();

        updateCalculateButtonUI();
    }

    private void fetchQuoteForDcf(StockApiService api, String ticker, double fcf, double shares, double growthRate, double discountRate, double terminalGrowth) {
        api.getStockQuote("GLOBAL_QUOTE", ticker, API_KEY).enqueue(new Callback<AlphaVantageResponse>() {
            @Override
            public void onResponse(Call<AlphaVantageResponse> call, Response<AlphaVantageResponse> response) {
                double currentPrice = 0.0;
                if (response.isSuccessful() && response.body() != null && response.body().getQuote() != null) {
                    try {
                        currentPrice = Double.parseDouble(response.body().getQuote().getPrice());
                    } catch (Exception ignored) {}
                }

                // שומרים ב-Firebase לפעם הבאה כדי לחסוך קריאות!
                saveToFirebase(ticker, fcf, shares, currentPrice);

                // מחשבים סוף סוף
                calculateAndDisplayDcf(fcf, shares, currentPrice, growthRate, discountRate, terminalGrowth);
            }
            @Override
            public void onFailure(Call<AlphaVantageResponse> call, Throwable t) {
                calculateAndDisplayDcf(fcf, shares, 0.0, growthRate, discountRate, terminalGrowth);
            }
        });
    }
    private void saveToFirebase(String ticker, double fcf, double shares, double price) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcf", String.valueOf(fcf));
        updates.put("sharesOutstanding", String.valueOf(shares));
        if (price > 0) updates.put("price", String.valueOf(price));

        db.collection("stocks").document(ticker).set(updates, com.google.firebase.firestore.SetOptions.merge());
    }
    // נוסחת ה-DCF
    private void calculateAndDisplayDcf(double fcf, double shares, double currentPrice, double growthRate, double discountRate, double terminalGrowth) {
        double projectedFcf = fcf;
        double presentValueSum = 0;

        // חישוב תזרים מהוון ל-5 שנים
        for (int i = 1; i <= 5; i++) {
            projectedFcf = projectedFcf * (1 + growthRate);
            presentValueSum += projectedFcf / Math.pow((1 + discountRate), i);
        }

        // חישוב ערך טרמינלי
        double terminalValue = (projectedFcf * (1 + terminalGrowth)) / (discountRate - terminalGrowth);
        double presentTerminalValue = terminalValue / Math.pow((1 + discountRate), 5);

        // שווי הוגן כולל
        double totalIntrinsicValue = presentValueSum + presentTerminalValue;
        // שווי למניה בודדת
        double valuePerShare = totalIntrinsicValue / shares;

        // עדכון UI
        getActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            btnCalculate.setEnabled(true);
            cardResult.setVisibility(View.VISIBLE);

            tvIntrinsicValue.setText(String.format("$%.2f", valuePerShare));
            tvCurrentPrice.setText(String.format("Current Price: $%.2f", currentPrice));

            if (currentPrice > 0) {
                if (valuePerShare > currentPrice) {
                    double margin = ((valuePerShare - currentPrice) / valuePerShare) * 100;
                    tvConclusion.setText(String.format("Undervalued by %.1f%%", margin));
                    tvConclusion.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // ירוק
                    cardResult.setStrokeColor(android.graphics.Color.parseColor("#4CAF50"));
                } else {
                    double premium = ((currentPrice - valuePerShare) / currentPrice) * 100;
                    tvConclusion.setText(String.format("Overvalued by %.1f%%", premium));
                    tvConclusion.setTextColor(android.graphics.Color.parseColor("#F44336")); // אדום
                    cardResult.setStrokeColor(android.graphics.Color.parseColor("#F44336"));
                }
            } else {
                tvConclusion.setText("Price data unavailable");
                tvConclusion.setTextColor(android.graphics.Color.GRAY);
            }
        });
    }
    private void showError(String msg) {
        getActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            btnCalculate.setEnabled(true);
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
        });
    }
}

