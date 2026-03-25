package elad.maayan.themarginhunter;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
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

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.util.ArrayList;
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
    private String API_KEY;
    private String divYieldStr;
    private String currentTicker = "";
    private List<Double> currentChartPrices; // שומרים את המחירים לטובת Firebase
    private List<Long> currentChartTimestamps;

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
        API_KEY = getString(R.string.finnhub_api_key);

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
        RadioGroup chartRangeGroup = view.findViewById(R.id.chart_range_group);
        chartRangeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (currentTicker.isEmpty()) return; // לא עושים כלום אם עדיין לא חיפשו מניה

            if (checkedId == R.id.btn_1m) {
                fetchYahooChartDynamic(currentTicker, "1mo", "1d", "dd/MM");
            } else if (checkedId == R.id.btn_6m) {
                fetchYahooChartDynamic(currentTicker, "6mo", "1d", "MM/yy");
            } else if (checkedId == R.id.btn_1y) {
                fetchYahooChartDynamic(currentTicker, "1y", "1d", "MM/yy");
            } else if (checkedId == R.id.btn_3y) {
                // ב-3 שנים אנחנו קופצים שבוע קדימה בכל נר, אחרת הגרף יהיה צפוף מדי ויקרוס
                fetchYahooChartDynamic(currentTicker, "3y", "1wk", "MM/yy");
            }
        });
    }

    private void setupListeners() {
        etGrowth.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateIntrinsicValueRealTime(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnFetch.setOnClickListener(v -> performSmartFetch());
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void fetchYahooChartDynamic(String ticker, String range, String interval, String dateFormat) {
        if (ticker == null || ticker.isEmpty()) return;
        StockApiService api = RetrofitClient.getApiService();
        String yahooUrl = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker + "?range=" + range + "&interval=" + interval;

        api.getYahooChart(yahooUrl).enqueue(new Callback<YahooChartResponse>() {
            @Override
            public void onResponse(Call<YahooChartResponse> call, Response<YahooChartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        List<Double> closes = response.body().getChart().getResult().get(0).getIndicators().getQuote().get(0).getClose();
                        List<Long> timestamps = response.body().getChart().getResult().get(0).getTimestamp();

                        List<Double> cleanPrices = new ArrayList<>();
                        List<Long> cleanTimestamps = new ArrayList<>();

                        for (int i = 0; i < closes.size(); i++) {
                            Double price = closes.get(i);
                            if (price != null && timestamps != null && timestamps.get(i) != null) {
                                cleanPrices.add(price);
                                cleanTimestamps.add(timestamps.get(i));
                            }
                        }

                        currentChartPrices = cleanPrices;
                        currentChartTimestamps = cleanTimestamps;

                        if(getActivity() != null) {
                            getActivity().runOnUiThread(() -> drawChart(cleanPrices, dateFormat));
                        }

                    } catch (Exception e) {
                        Log.e("YAHOO_ERROR", "שגיאה בפענוח הגרף של יאהו", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<YahooChartResponse> call, Throwable t) {
                if(getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "שגיאה במשיכת גרף: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
    private void handleArguments(View view) {
        if (getArguments() != null) {
            String tickerArg = getArguments().getString("ticker");
            if (tickerArg != null) {
                etTicker.setText(tickerArg);
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

        db.collection("stocks")
                .document(ticker)
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Stock stock = doc.toObject(Stock.class);
                        if (stock != null) {
                            fillFieldsFromFirebase(stock);
                            // שליפת הגרף מ-Firebase
                            List<Double> rawChart = (List<Double>) doc.get("chartPrices");
                            if (rawChart != null) {
                                currentChartPrices = new ArrayList<>();
                                for(Double d : rawChart) {
                                    currentChartPrices.add(d);
                                }
                                drawChart(currentChartPrices, "dd/MM");
                                Toast.makeText(getContext(), "נטען מהמאגר המקומי", Toast.LENGTH_SHORT).show();
                            } else {
                                fetchStockData(ticker); // אם אין גרף, נמשוך מהרשת
                            }
                        }
                    } else {
                        fetchStockData(ticker);
                    }
                }).addOnFailureListener(e -> fetchStockData(ticker));
    }

    private void fetchStockData(String ticker) {
        StockApiService api = RetrofitClient.getApiService();

        // 1. קריאת שם החברה
        api.getCompanyProfile(ticker, API_KEY).enqueue(new Callback<FinnhubProfileResponse>() {
            @Override
            public void onResponse(Call<FinnhubProfileResponse> call, Response<FinnhubProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCompanyName() != null) {
                    String companyName = response.body().getCompanyName();
                    if(getActivity() != null) {
                        getActivity().runOnUiThread(() -> etCompanyName.setText(companyName));
                    }
                }
            }
            @Override
            public void onFailure(Call<FinnhubProfileResponse> call, Throwable t) {}
        });

        // 2. קריאת המחיר הנוכחי
        api.getStockQuote(ticker, API_KEY).enqueue(new Callback<FinnhubQuoteResponse>() {
            @Override
            public void onResponse(Call<FinnhubQuoteResponse> call, Response<FinnhubQuoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    double currentPrice = response.body().getCurrentPrice();
                    if(getActivity() != null) {
                        getActivity().runOnUiThread(() -> etPrice.setText(String.valueOf(currentPrice)));
                    }
                }
            }
            @Override
            public void onFailure(Call<FinnhubQuoteResponse> call, Throwable t) {
                Log.e("API_ERROR", "Failed to fetch quote");
            }
        });

        // 3. קריאת הנתונים הפיננסיים (EPS ו-Growth)
        api.getStockMetrics(ticker, "all", API_KEY).enqueue(new Callback<FinnhubMetricResponse>() {
            @Override
            public void onResponse(Call<FinnhubMetricResponse> call, Response<FinnhubMetricResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getMetric() != null) {
                    double eps = response.body().getMetric().getEps();
                    double growth = response.body().getMetric().getGrowth();
                    if(getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            etEPS.setText(String.valueOf(eps));
                            etGrowth.setText(String.valueOf(growth));
                            updateIntrinsicValueRealTime();
                        });
                    }
                }
            }
            @Override
            public void onFailure(Call<FinnhubMetricResponse> call, Throwable t) {}
        });

        currentTicker = ticker; // שמירת המניה הנוכחית
        fetchYahooChartDynamic(ticker, "1mo", "1d", "dd/MM"); // קריאה ראשונית של חודש
            }

    private void drawChart(List<Double> closePrices, String dateFormat) {
        lineChart.setVisibility(View.VISIBLE);
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < closePrices.size(); i++) {
            // Entry מקבל Float, אז אנחנו ממירים את ה-Double ל-Float כאן:
            entries.add(new Entry(i, closePrices.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Price History");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // מציג למטה
        xAxis.setDrawLabels(true); // מפעיל את הכיתוב
        xAxis.setDrawGridLines(false); // מעלים את קווי הרשת המכוערים
        xAxis.setTextColor(Color.GRAY); // צבע אפור ואלגנטי
        xAxis.setGranularity(1f); // קפיצות שלמות
        xAxis.setLabelCount(5, false); // מציג רק 5 תאריכים כדי שלא יהיה צפוף מדי

        // ממיר את האינדקסים בגרף לתאריכים אמיתיים (יום/חודש)
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat(dateFormat, Locale.getDefault());            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (currentChartTimestamps != null && index >= 0 && index < currentChartTimestamps.size()) {
                    long millis = currentChartTimestamps.get(index) * 1000L;
                    return mFormat.format(new Date(millis));
                }
                return "";
            }
        });
        lineChart.getLegend().setEnabled(false);

        lineChart.animateX(1000);
        lineChart.invalidate();
    }
    private void updateIntrinsicValueRealTime() {
        try {
            String epsS = etEPS.getText().toString();
            String priS = etPrice.getText().toString();
            String groS = etGrowth.getText().toString();
            if (!epsS.isEmpty() && !priS.isEmpty()) {
                double eps = Double.parseDouble(epsS), pri = Double.parseDouble(priS), gro = groS.isEmpty() ? 0 : Double.parseDouble(groS);
                double yield = (divYieldStr != null && !divYieldStr.equals("None")) ?
                        Double.parseDouble(divYieldStr) : 0;
                double baseValue = eps * (8.5 + 2 * gro);
                double dividendValue = (pri * yield) * 5;
                double finalIv = baseValue + dividendValue;

                tvIntrinsicValueResult.setText(String.format("Intrinsic Value: $%.2f", finalIv));
                tvIntrinsicValueResult.setTextColor(finalIv > pri ? Color.parseColor("#2E7D32") : Color.RED);
            }
        } catch (Exception ignored) {}
    }

    private void validateAndSave() {
        String ticker = etTicker.getText().toString().trim().toUpperCase();
        String epsStr = etEPS.getText().toString();
        String priceStr = etPrice.getText().toString();

        if (ticker.isEmpty() || epsStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(getContext(), "נא למלא את כל הנתונים לפני השמירה", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            double eps = Double.parseDouble(epsStr);
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
            data.put("lastUpdated", System.currentTimeMillis());

            // שומרים את רשימת המחירים עבור הגרף
            if (currentChartPrices != null) {
                data.put("chartPrices", currentChartPrices);
            }
            if (tilGrowth.getHelperText() != null) {
                data.put("growthHint", tilGrowth.getHelperText().toString());
            }
            if (divYieldStr != null) {
                data.put("dividendYield", divYieldStr);
            }

            db.collection("stocks").document(ticker).set(data, SetOptions.merge()).addOnSuccessListener(aVoid -> {
                if (isAdded())
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).popBackStack();
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), "שגיאה בפורמט המספרים", Toast.LENGTH_SHORT).show();
        }
    }

    private void fillFieldsFromFirebase(Stock stock) {
        etCompanyName.setText(stock.getName());
        etPrice.setText(String.valueOf(stock.getPrice()));
        etEPS.setText(String.valueOf(stock.getEps()));
        etGrowth.setText(String.valueOf(stock.getGrowthRate()));

        if (stock.getGrowthHint() != null) {
            tilGrowth.setHelperText(stock.getGrowthHint());
        }

        this.divYieldStr = String.valueOf(stock.getDividendYield());
        updateIntrinsicValueRealTime();
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
        android.app.Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }
}