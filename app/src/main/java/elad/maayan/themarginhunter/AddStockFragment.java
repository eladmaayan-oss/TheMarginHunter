package elad.maayan.themarginhunter;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddStockFragment extends BottomSheetDialogFragment {

    private TextInputEditText etTicker, etEPS, etGrowth, etPrice, etCompanyName;
    private TextInputLayout tilTicker, tilEPS, tilGrowth;
    private Button btnSave, btnFetch;
    private LineChart lineChart;
    private TextView tvIntrinsicValueResult;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
//    private final String API_KEY = "3PY2WSBJ2P1ZUR0K";
private final String API_KEY = "BH00QGEFNFNZ1IDN";
    private String divYieldStr;
    private Map<String, AlphaVantageResponse.DailyData> currentChartData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_stock, container, false);
        initViews(view);
        setupListeners();
        handleArguments(view);
        return view;
    }

    private void initViews(View view) {
        etTicker = view.findViewById(R.id.etTicker);
        etEPS = view.findViewById(R.id.etEPS);
        etGrowth = view.findViewById(R.id.etGrowth);
        etPrice = view.findViewById(R.id.etPrice);
        etCompanyName = view.findViewById(R.id.etCompanyName);
        tilTicker = view.findViewById(R.id.tilTicker);
        tilEPS = view.findViewById(R.id.tilEPS);
        tilGrowth = view.findViewById(R.id.tilGrowth);
        btnSave = view.findViewById(R.id.btnSaveStock);
        btnFetch = view.findViewById(R.id.btnFetch);
        lineChart = view.findViewById(R.id.lineChart);
        tvIntrinsicValueResult = view.findViewById(R.id.tvIntrinsicValueResult);
    }

    private void setupListeners() {
        // חישוב בזמן אמת
        etGrowth.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateIntrinsicValueRealTime(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // כפתור Fetch משולב (Firebase -> API)
        btnFetch.setOnClickListener(v -> performSmartFetch());

        // כפתור שמירה
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void handleArguments(View view) {
        if (getArguments() != null) {
            String tickerArg = getArguments().getString("ticker");
            if (tickerArg != null) {
                etTicker.setText(tickerArg);
                // אם אנחנו במצב "עדכון", נטען מ-Firebase, אחרת נלחץ אוטומטית על Fetch
                if (btnSave.getText().toString().equals("Update Stock")) {
                    setupEditMode(tickerArg);
                } else {
                    view.post(() -> btnFetch.performClick());
                }
            }
        }
    }

    private void performSmartFetch() {
        String ticker = etTicker.getText().toString().trim().toUpperCase();
        if (ticker.isEmpty()) {
            etTicker.setError("נא להזין טיקר");
            return;
        }

        db.collection("stocks").document(ticker).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Stock stock = doc.toObject(Stock.class);
                if (stock != null) {
                    fillFieldsFromFirebase(stock);
                    Map<String, Object> rawChart = (Map<String, Object>) doc.get("chartData");
                    if (rawChart != null) {
                        currentChartData = convertToDailyData(rawChart);
                        setupChart(lineChart, currentChartData);
                        lineChart.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), "נטען מהמאגר המקומי", Toast.LENGTH_SHORT).show();
                    } else {
                        fetchChartData(ticker, lineChart);
                    }
                }
            } else {
                // אם לא קיים ב-Firebase: משוך קודם נתונים, הגרף ימשך אוטומטית אחריהם
                fetchStockData(ticker);
            }
        }).addOnFailureListener(e -> fetchStockData(ticker));
    }


    private void fetchStockData(String ticker) {
        StockApiService api = RetrofitClient.getApiService();

        api.getCompanyOverview("OVERVIEW", ticker, API_KEY).enqueue(new Callback<CompanyOverviewResponse>() {
            @Override
            public void onResponse(Call<CompanyOverviewResponse> call, Response<CompanyOverviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CompanyOverviewResponse overview = response.body();

                    if (overview.getEps() != null) {
                        // עדכון UI ראשוני (שם ו-EPS)
                        getActivity().runOnUiThread(() -> {
                            etCompanyName.setText(overview.getName());
                            etEPS.setText(overview.getEps());

                            // עדכון Growth hint
                            if (overview.getQuarterlyGrowth() != null) {
                                try {
                                    double g = Double.parseDouble(overview.getQuarterlyGrowth()) * 100;
                                    tilGrowth.setHelperText("Consensus: " + String.format("%.1f%%", g));
                                } catch (Exception e) { Log.e("API", "Growth parse error"); }
                            }
                        });

                        // מחכים 2 שניות ל-Quote
                        new android.os.Handler().postDelayed(() -> {
                            api.getStockQuote("GLOBAL_QUOTE", ticker, API_KEY).enqueue(new Callback<AlphaVantageResponse>() {
                                @Override
                                public void onResponse(Call<AlphaVantageResponse> call, Response<AlphaVantageResponse> pr) {
                                    if (pr.isSuccessful() && pr.body() != null && pr.body().getQuote() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            etPrice.setText(pr.body().getQuote().getPrice());
                                            updateIntrinsicValueRealTime();
                                        });

                                        // מחכים *עוד* 2 שניות לגרף - כדי לא לעצבן את ה-API
                                        new android.os.Handler().postDelayed(() -> {
                                            fetchChartData(ticker, lineChart);
                                        }, 2000);
                                    }
                                }
                                @Override public void onFailure(Call<AlphaVantageResponse> call, Throwable t) {}
                            });
                        }, 2000);
                    } else {
                        Log.e("API", "EPS is null - Limit reached");
                    }
                }
            }
            @Override public void onFailure(Call<CompanyOverviewResponse> call, Throwable t) {}
        });
    }
    private void fetchChartData(String ticker, LineChart chart) {
        RetrofitClient.getApiService().getDailySeries("TIME_SERIES_DAILY", ticker, API_KEY).enqueue(new Callback<AlphaVantageResponse>() {
            @Override
            public void onResponse(Call<AlphaVantageResponse> call, Response<AlphaVantageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentChartData = response.body().getTimeSeries();
                    if (currentChartData != null) {
                        // הוסף את השורה הזו:
                        getActivity().runOnUiThread(() -> {
                            chart.setVisibility(View.VISIBLE);
                            setupChart(chart, currentChartData);
                        });
                    }                }
            }
            @Override public void onFailure(Call<AlphaVantageResponse> call, Throwable t) {}
        });
    }


    private void setupChart(LineChart chart, Map<String, AlphaVantageResponse.DailyData> history) {
        if (history == null || history.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>(history.keySet());
        Collections.sort(dates);

        int index = 0;
        for (String date : dates) {
            String closePrice = history.get(date).getClose();
            if (closePrice != null) {
                entries.add(new Entry(index++, Float.parseFloat(closePrice)));
            }
        }
        LineDataSet ds = new LineDataSet(entries, "Price History");
        ds.setColor(Color.parseColor("#2E7D32")); // ירוק כהה שיתאים לאפליקציה
        ds.setLineWidth(2f);
        ds.setDrawCircles(false);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER); // גרף חלק יותר

        LineData lineData = new LineData(ds);
        chart.setData(lineData);

        // הגדרות עיצוב הכרחיות
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setDrawLabels(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        chart.animateX(800);
        chart.invalidate(); // פקודה קריטית לציור מחדש    }
    }

    private void updateIntrinsicValueRealTime() {
        try {
            String epsS = etEPS.getText().toString();
            String priS = etPrice.getText().toString();
            String groS = etGrowth.getText().toString();
            if (!epsS.isEmpty() && !priS.isEmpty()) {
                double eps = Double.parseDouble(epsS), pri = Double.parseDouble(priS), gro = groS.isEmpty() ? 0 : Double.parseDouble(groS);
                double iv = eps * (8.5 + 2 * gro);
                tvIntrinsicValueResult.setText(String.format("Intrinsic Value: $%.2f", iv));
                tvIntrinsicValueResult.setTextColor(iv > pri ? Color.parseColor("#2E7D32") : Color.RED);
            }
        } catch (Exception ignored) {}
    }

    private void validateAndSave() {
        String ticker = etTicker.getText().toString().trim().toUpperCase();
        if (ticker.isEmpty() || etEPS.getText().toString().isEmpty()) return;

        double price = Double.parseDouble(etPrice.getText().toString());
        double eps = Double.parseDouble(etEPS.getText().toString());
        double growth = etGrowth.getText().toString().isEmpty() ? 0 : Double.parseDouble(etGrowth.getText().toString());
        double iv = eps * (8.5 + 2 * growth);
        double mos = ((iv - price) / iv) * 100;

        Map<String, Object> data = new HashMap<>();
        data.put("ticker", ticker);
        data.put("companyName", etCompanyName.getText().toString());
        data.put("currentPrice", price);
        data.put("eps", eps);
        data.put("growthRate", growth);
        data.put("marginOfSafety", mos);
        data.put("intrinsicValue", iv);
        data.put("chartData", currentChartData);
        data.put("lastUpdated", System.currentTimeMillis());

        db.collection("stocks").document(ticker).set(data, SetOptions.merge()).addOnSuccessListener(aVoid -> {
            if (isAdded()) Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).popBackStack();
        });
    }

    private void fillFieldsFromFirebase(Stock stock) {
        etCompanyName.setText(stock.getName());
        etPrice.setText(String.valueOf(stock.getPrice()));
        etEPS.setText(String.valueOf(stock.getEps()));
        etGrowth.setText(String.valueOf(stock.getGrowthRate()));
        updateIntrinsicValueRealTime();
    }

    private Map<String, AlphaVantageResponse.DailyData> convertToDailyData(Map<String, Object> raw) {
        Map<String, AlphaVantageResponse.DailyData> history = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            Map<String, Object> vals = (Map<String, Object>) entry.getValue();
            AlphaVantageResponse.DailyData daily = new AlphaVantageResponse.DailyData();
            Object close = vals.get("close") != null ? vals.get("close") : vals.get("4. close");
            daily.setClose(String.valueOf(close));
            history.put(entry.getKey(), daily);
        }
        return history;
    }

    private void setupEditMode(String ticker) {
        etTicker.setEnabled(false);
        btnSave.setText("Update Stock");
        db.collection("stocks").document(ticker).get().addOnSuccessListener(doc -> {
            if (doc.exists()) fillFieldsFromFirebase(doc.toObject(Stock.class));
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        View v = getView();
        if (v != null) BottomSheetBehavior.from((View) v.getParent()).setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}