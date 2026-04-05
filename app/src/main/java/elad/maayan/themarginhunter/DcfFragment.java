package elad.maayan.themarginhunter;

import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DcfFragment extends Fragment {
    private static final String API_KEY = "7IGGZ2PPMOUAVS98";


    private TextInputEditText etTicker, etGrowth, etDiscount, etTerminalGrowth;
    private MaterialButton btnCalculate;
    private View layoutProgress;
    private ProgressBar progressBarHorizontal;
    private TextView tvProgressText;
    private ProgressBar progressBar;
    private MaterialCardView cardResult;
    private TextView tvIntrinsicValue, tvCurrentPrice, tvConclusion;
    private RecyclerView rvDcfStocks;
    private DcfStockAdapter dcfAdapter;
    private List<Stock> dcfBargainList;
    private Button btnScanDcf;
    private float currentGrowthRate;
    private float currentDiscountRate;
    private float currentTerminalRate;
    private static final String PREF_NAME = "DcfPreferences";
    private static final String KEY_GROWTH = "growth_rate";
    private static final String KEY_DISCOUNT = "discount_rate";
    private static final String KEY_TERMINAL = "terminal_rate";

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dcf, container, false);
        db = FirebaseFirestore.getInstance();
        initViews(view);
        ImageButton btnDcfSettings = view.findViewById(R.id.btnDcfSettings);
        btnDcfSettings.setOnClickListener(v -> {
            // כאן תוכל לפתוח את חלון ההגדרות שיצרנו, או חלון הגדרות ייעודי ל-DCF
            showFinetuneDialog();
        });
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentGrowthRate = prefs.getFloat(KEY_GROWTH, 10.0f);
        currentDiscountRate = prefs.getFloat(KEY_DISCOUNT, 10.0f);
        currentTerminalRate = prefs.getFloat(KEY_TERMINAL, 2.5f);
        return view;
    }

    private void showFinetuneDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_finetune_dcf, null);

        // מציאת הסליידרים
        com.google.android.material.slider.Slider sliderGrowth = dialogView.findViewById(R.id.sliderDcfGrowth);
        com.google.android.material.slider.Slider sliderDiscount = dialogView.findViewById(R.id.sliderDcfDiscount);
        com.google.android.material.slider.Slider sliderTerminal = dialogView.findViewById(R.id.sliderDcfTerminal);

        // מציאת חלוניות הטקסט
        TextView tvGrowthValue = dialogView.findViewById(R.id.tvGrowthValue);
        TextView tvDiscountValue = dialogView.findViewById(R.id.tvDiscountValue);
        TextView tvTerminalValue = dialogView.findViewById(R.id.tvTerminalValue);

        // הגדרת מאזינים לסליידרים שיעדכנו את הטקסט בזמן אמת
        sliderGrowth.addOnChangeListener((slider, value, fromUser) -> tvGrowthValue.setText(value + "%"));
        sliderDiscount.addOnChangeListener((slider, value, fromUser) -> tvDiscountValue.setText(value + "%"));
        sliderTerminal.addOnChangeListener((slider, value, fromUser) -> tvTerminalValue.setText(value + "%"));

        // טעינת הערכים ההתחלתיים לסליידרים (הטקסט יתעדכן אוטומטית בגלל המאזינים)
        sliderGrowth.setValue(currentGrowthRate);
        sliderDiscount.setValue(currentDiscountRate);
        sliderTerminal.setValue(currentTerminalRate);

        // יצירת הדיאלוג ושמירתו כמשתנה
        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("הגדרות סורק DCF")
                .setView(dialogView)
                .setPositiveButton("שמור", (d, which) -> {
                    currentGrowthRate = sliderGrowth.getValue();
                    currentDiscountRate = sliderDiscount.getValue();
                    currentTerminalRate = sliderTerminal.getValue();

                    SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putFloat(KEY_GROWTH, currentGrowthRate);
                    editor.putFloat(KEY_DISCOUNT, currentDiscountRate);
                    editor.putFloat(KEY_TERMINAL, currentTerminalRate);
                    editor.apply();

                    Toast.makeText(getContext(), "הגדרות נשמרו בהצלחה", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", (d, which) -> d.dismiss())
                // אנחנו מגדירים את הלחצן כ-null כדי לדרוס אותו מיד אחרי ההצגה
                .setNeutralButton("איפוס למקור", null)
                .create();

        // חובה להציג את הדיאלוג לפני שדורסים את לחצן ה-Neutral
        dialog.show();

        // דריסת כפתור האיפוס כך שישנה את הסליידרים אך *לא* יסגור את החלון
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            sliderGrowth.setValue(10.0f);
            sliderDiscount.setValue(10.0f);
            sliderTerminal.setValue(2.5f);
            // חלוניות הטקסט מתעדכנות אוטומטית בזכות ה-addOnChangeListener למעלה
        });
    }

    private void initViews(View view) {
        etTicker = view.findViewById(R.id.etDcfTicker);
        etGrowth = view.findViewById(R.id.etDcfGrowth);
        etDiscount = view.findViewById(R.id.etDcfDiscountRate);
        etTerminalGrowth = view.findViewById(R.id.etDcfTerminalGrowth);
        btnCalculate = view.findViewById(R.id.btnCalculateDcf);
        layoutProgress = view.findViewById(R.id.layoutProgress);
        progressBarHorizontal = view.findViewById(R.id.progressBarHorizontal);
        tvProgressText = view.findViewById(R.id.tvProgressText);
        cardResult = view.findViewById(R.id.cardDcfResult);
        tvIntrinsicValue = view.findViewById(R.id.tvDcfIntrinsicValue);
        tvCurrentPrice = view.findViewById(R.id.tvDcfCurrentPrice);
        tvConclusion = view.findViewById(R.id.tvDcfConclusion);
        progressBar = view.findViewById(R.id.progressBarDcf);
        rvDcfStocks = view.findViewById(R.id.rvDcfStocks);
        rvDcfStocks.setLayoutManager(new LinearLayoutManager(getContext()));
        dcfBargainList = new ArrayList<>();
        dcfAdapter = new DcfStockAdapter(dcfBargainList, new DcfStockAdapter.OnStockClickListener() {
            @Override
            public void onStockClick(Stock stock) {
                AddStockFragment addStockFragment = new AddStockFragment();
                Bundle args = new Bundle();
                args.putString("ticker", stock.getTicker());
                addStockFragment.setArguments(args);

                addStockFragment.show(getChildFragmentManager(), "AddStockBottomSheet");
            }
        });

        rvDcfStocks.setAdapter(dcfAdapter);
        btnScanDcf = view.findViewById(R.id.btnScanDcfMarket);
        btnScanDcf.setOnClickListener(v -> scanDcfMarket());

        btnCalculate.setOnClickListener(v -> startDcfProcess());
        initApiCounter();
        com.google.android.material.textfield.TextInputLayout layoutGrowth = view.findViewById(R.id.layoutDcfGrowth);
        com.google.android.material.textfield.TextInputLayout layoutDiscount = view.findViewById(R.id.layoutDcfDiscount);
        com.google.android.material.textfield.TextInputLayout layoutTerminal = view.findViewById(R.id.layoutDcfTerminal);
        layoutGrowth.setEndIconOnClickListener(v -> showTipDialog(
                "Expected Growth Rate",
                "הערך בכמה אחוזים החברה תגדיל את התזרים שלה ב-5 השנים הקרובות.\n\n" +
                        "• חברות בוגרות ויציבות: 3%-7%\n" +
                        "• חברות צמיחה: 10%-15%\n\n" +
                        "💡 טיפ: אל תהיה אופטימי מדי, צמיחה עקבית של מעל 20% לאורך זמן היא נדירה מאוד."
        ));

        layoutDiscount.setEndIconOnClickListener(v -> showTipDialog(
                "Discount Rate (WACC)",
                "התשואה השנתית שאתה דורש על ההשקעה, שמשקללת את הסיכון.\n\n" +
                        "• חברות בטוחות ענקיות (כמו Apple): 8%-9%\n" +
                        "• חברות ממוצעות: 10%\n" +
                        "• מניות מסוכנות או קטנות: 12% ומעלה"
        ));
        layoutTerminal.setEndIconOnClickListener(v -> showTipDialog(
                "Terminal Growth Rate",
                "קצב הצמיחה של החברה לנצח (החל מהשנה ה-6 ואילך).\n\n" +
                        "הכלל הכי חשוב: חברה לא יכולה לצמוח לנצח יותר מהר מהכלכלה העולמית, אחרת היא תבלע את העולם.\n\n" +
                        "💡 טיפ: הסטנדרט המקובל בוורן באפט ובוול סטריט הוא תמיד בין 2% ל-3% (באזור קצב האינפלציה)."
        ));
        Button btnAutoFill = view.findViewById(R.id.btnAutoFill);
        btnAutoFill.setOnClickListener(v -> {
            String ticker = etTicker.getText().toString().trim().toUpperCase();
            autoFillDcfParams(ticker);
        });
        fetchDcfBargains();
    }

    private void fetchDcfBargains() {
        db.collection("dcf_stocks")
                .addSnapshotListener((value, error) -> {
                    if (isAdded()) {
                        if (error != null) {
                            Log.e("DCF", "Listen failed.");
                            return;
                        }
                        if (value != null) {
                            dcfBargainList.clear();
                            for (QueryDocumentSnapshot document : value) {
                                Stock stock = document.toObject(Stock.class);
                                dcfBargainList.add(stock);
                            }
                            dcfAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void updateProgressUI(int current, int total) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            progressBarHorizontal.setProgress(current);
            tvProgressText.setText("סורק מניות: " + current + " / " + total);

            if (current == total) {
                layoutProgress.setVisibility(View.GONE);
                Toast.makeText(getContext(), "סריקת DCF הסתיימה", Toast.LENGTH_SHORT).show();
            }
        });
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
                    checkQuotaAndFetch(ticker, growthRate, discountRate, terminalGrowth);
                }
            } else {
                Log.d("DCF", "Data missing in Firebase. Fetching from API...");
                checkQuotaAndFetch(ticker, growthRate, discountRate, terminalGrowth);
            }
        }).addOnFailureListener(e -> {
            checkQuotaAndFetch(ticker, growthRate, discountRate, terminalGrowth);
        });
    }

    private void autoFillDcfParams(String ticker) {
        if (ticker.isEmpty()) {
            showError("אנא הזן סימול מניה קודם.");
            return;
        }

        showError("מחשב נתוני סיכון ותחזיות אנליסטים..."); // הודעה קטנה למשתמש

        // חייבים לבקש מיאהו במפורש את המודל של earningsTrend בנוסף לסטטיסטיקות
        String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + ticker +
                "?modules=defaultKeyStatistics,earningsTrend";

        StockApiService api = RetrofitClient.getApiService();
        api.getYahooSummary(url).enqueue(new Callback<YahooSummaryResponse>() {
            @Override
            public void onResponse(Call<YahooSummaryResponse> call, Response<YahooSummaryResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getQuoteSummary() != null &&
                        response.body().getQuoteSummary().getResult() != null) {

                    YahooSummaryResponse.Result result = response.body().getQuoteSummary().getResult().get(0);

                    // 1. חישוב Discount Rate (WACC) בעזרת Beta
                    double discountRate = 10.0; // ברירת מחדל
                    if (result.getDefaultKeyStatistics() != null && result.getDefaultKeyStatistics().getBeta() != null) {
                        double beta = result.getDefaultKeyStatistics().getBeta().getRaw();
                        if (beta < 0.8) discountRate = 8.0;       // חברה מאוד בטוחה ויציבה
                        else if (beta > 1.3) discountRate = 12.0; // חברה מסוכנת/תנודתית
                        else discountRate = 10.0;                 // חברה ממוצעת
                    }

                    // 2. שליפת תחזית צמיחה ל-5 שנים (Expected Growth)
                    double expectedGrowth = 10.0; // ברירת מחדל
                    if (result.getEarningsTrend() != null && result.getEarningsTrend().getTrend() != null) {
                        for (YahooSummaryResponse.Trend trend : result.getEarningsTrend().getTrend()) {
                            if ("+5y".equals(trend.getPeriod()) && trend.getGrowth() != null) {
                                // יאהו מחזיר את זה כשבר עשרוני (למשל 0.15 עבור 15%), אז נכפיל ב-100
                                expectedGrowth = trend.getGrowth().getRaw() * 100;
                                break;
                            }
                        }
                    }

                    // 3. הצגת הנתונים בשדות הטקסט של ה-UI (אל תשכח לשנות ל-IDs המדויקים של ה-EditText שלך!)
                    etGrowth.setText(String.format("%.1f", expectedGrowth));
                    etDiscount.setText(String.format("%.1f", discountRate));
                    etTerminalGrowth.setText("2.5"); // נתון מאקרו קבוע ובטוח

                    showError("הנתונים מולאו בהצלחה!"); // אפשר גם להחליף את זה ב-Toast של הצלחה
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        showError("שגיאת יאהו (" + response.code() + "): " + response.message());
                        android.util.Log.e("DCF_MAGIC", "HTTP " + response.code() + " | Body: " + errorBody);
                    } catch (Exception e) {
                        showError("שגיאה לא ידועה מקוד השרת: " + response.code());
                    }
                }
            }


                @Override
                public void onFailure (Call < YahooSummaryResponse > call, Throwable t){
                    showError("קריסה בחיבור ליאהו: " + t.getMessage());
                    android.util.Log.e("DCF_MAGIC", "Network failure", t);
                }

        });
    }

    private void scanDcfMarket() {
        // 1. מוודאים ששדה הטיקר ריק
        String tickerInput = etTicker.getText().toString().trim();
        if (!TextUtils.isEmpty(tickerInput)) {
            showError("כדי לסרוק את השוק, אנא נקה את שדה הטיקר (Ticker).");
            return;
        }

        // 2. משיכת הנתונים מהמחשבון
        String growthStr = etGrowth.getText().toString().trim();
        String discountStr = etDiscount.getText().toString().trim();
        String terminalStr = etTerminalGrowth.getText().toString().trim();

        final Double customGrowth = TextUtils.isEmpty(growthStr) ? null : Double.parseDouble(growthStr) / 100.0;
        final Double customDiscount = TextUtils.isEmpty(discountStr) ? null : Double.parseDouble(discountStr) / 100.0;
        final Double customTerminal = TextUtils.isEmpty(terminalStr) ? null : Double.parseDouble(terminalStr) / 100.0;

        layoutProgress.setVisibility(View.VISIBLE);
        tvProgressText.setText("מכין רשימה ממניות גראהם...");

        // 3. מחיקת תוצאות DCF קודמות
        db.collection("dcf_stocks").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                doc.getReference().delete();
            }

            // 4. *** כאן השינוי! שולפים את המניות מה-Bargain Bin ***
            // שנה את "bargain_stocks" אם האוסף שלך נקרא אחרת
            db.collection("bargain_stocks").get().addOnSuccessListener(bargainSnapshots -> {
                List<String> tickers = new ArrayList<>();
                for (QueryDocumentSnapshot doc : bargainSnapshots) {
                    String ticker = doc.getString("ticker");
                    if (ticker != null) {
                        tickers.add(ticker);
                    }
                }

                if (tickers.isEmpty()) {
                    showError("אין מניות ב-Bargain Bin לסרוק!");
                    layoutProgress.setVisibility(View.GONE);
                    return;
                }

                int totalStocks = tickers.size();
                progressBarHorizontal.setMax(totalStocks);
                progressBarHorizontal.setProgress(0);
                final int[] completed = {0};

                StockApiService apiService = RetrofitClient.getApiService();

                // 5. מריצים את ה-DCF *רק* על המניות ששלפנו מקודם
                for (String ticker : tickers) {
                    String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + ticker +
                            "?modules=defaultKeyStatistics,financialData,earningsTrend";

                    apiService.getYahooSummary(url).enqueue(new Callback<YahooSummaryResponse>() {
                        @Override
                        public void onResponse(Call<YahooSummaryResponse> call, Response<YahooSummaryResponse> response) {
                            completed[0]++;
                            updateProgressUI(completed[0], totalStocks);

                            if (response.isSuccessful() && response.body() != null) {
                                try {
                                    YahooSummaryResponse.Result result = response.body().getQuoteSummary().getResult().get(0);

                                    double currentPrice = result.getFinancialData().getCurrentPrice().getRaw();
                                    double shares = result.getDefaultKeyStatistics().getSharesOutstanding().getRaw();
                                    double fcf = result.getFinancialData().getFreeCashflow().getRaw();

                                    double discountRate;
                                    if (customDiscount != null) {
                                        discountRate = customDiscount;
                                    } else {
                                        double beta = (result.getDefaultKeyStatistics().getBeta() != null) ?
                                                result.getDefaultKeyStatistics().getBeta().getRaw() : 1.0;
                                        discountRate = (beta < 0.8) ? 0.08 : (beta > 1.3) ? 0.12 : 0.10;
                                    }

                                    double expectedGrowth;
                                    if (customGrowth != null) {
                                        expectedGrowth = customGrowth;
                                    } else {
                                        expectedGrowth = 0.05;
                                        if (result.getEarningsTrend() != null) {
                                            for (YahooSummaryResponse.Trend trend : result.getEarningsTrend().getTrend()) {
                                                if ("+5y".equals(trend.getPeriod()) && trend.getGrowth() != null) {
                                                    expectedGrowth = trend.getGrowth().getRaw();
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    double terminalGrowth = (customTerminal != null) ? customTerminal : 0.025;

                                    double intrinsicValue = runDcfCalculation(fcf, shares, expectedGrowth, discountRate, terminalGrowth);

                                    if (currentPrice < intrinsicValue) {
                                        saveDcfBargainToFirebase(ticker, currentPrice, intrinsicValue, expectedGrowth);
                                    }

                                } catch (Exception e) {
                                    Log.e("DCF_SCAN", "Error parsing " + ticker + ": " + e.getMessage());
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<YahooSummaryResponse> call, Throwable t) {
                            completed[0]++;
                            updateProgressUI(completed[0], totalStocks);
                        }
                    });
                }
            }).addOnFailureListener(e -> {
                showError("שגיאה בשליפת מניות מה-Bargain Bin: " + e.getMessage());
                layoutProgress.setVisibility(View.GONE);
            });
        });
    }
    // פונקציית עזר לחישוב המתמטי
    private double runDcfCalculation(double fcf, double shares, double growth, double discount, double terminal) {
        double projectedFcf = fcf;
        double pvSum = 0;
        for (int i = 1; i <= 5; i++) {
            projectedFcf *= (1 + growth);
            pvSum += projectedFcf / Math.pow((1 + discount), i);
        }
        double terminalValue = (projectedFcf * (1 + terminal)) / (discount - terminal);
        double pvTerminal = terminalValue / Math.pow((1 + discount), 5);
        return (pvSum + pvTerminal) / shares;
    }

    // שמירה לאוסף נפרד ב-Firebase בשם "dcf_stocks"
    private void saveDcfBargainToFirebase(String ticker, double price, double intrinsic, double growth) {
        Map<String, Object> data = new HashMap<>();
        data.put("ticker", ticker);
        data.put("currentPrice", price);
        data.put("intrinsicValue", intrinsic);
        data.put("expectedGrowth", growth * 100);
        data.put("timestamp", System.currentTimeMillis());

        db.collection("dcf_stocks").document(ticker).set(data);
    }

    private void checkQuotaAndFetch(String ticker, double growthRate, double discountRate, double terminalGrowth) {
        fetchDataFromApi(ticker, growthRate, discountRate, terminalGrowth);
    }

    // שרשור קריאות API: קודם Cash Flow, ואז Overview (למניות), ואז Quote (למחיר)
    private void fetchDataFromApi(String ticker, double growthRate, double discountRate, double terminalGrowth) {
        StockApiService api = RetrofitClient.getApiService();

        api.getCashFlow("CASH_FLOW", ticker, API_KEY).enqueue(new Callback<CashFlowResponse>() {
            @Override
            public void onResponse(Call<CashFlowResponse> call, Response<CashFlowResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getAnnualReports() != null && !response.body().getAnnualReports().isEmpty()) {
                    AnnualReport report = response.body().getAnnualReports().get(0);

                    double opCash = parseAlphaDouble(report.getOperatingCashflow());
                    double capEx = parseAlphaDouble(report.getCapitalExpenditures());
                    double fcf = opCash - Math.abs(capEx);

                    if (fcf == 0.0) {
                        showError("לא נמצאו נתוני תזרים מזומנים תקינים (FCF) עבור המניה הזו באלפא.");
                        return;
                    }

                    // *** כאן הקסם: ממתינים 1.2 שניות לפני הקריאה הבאה! ***
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        fetchOverviewForDcf(api, ticker, fcf, growthRate, discountRate, terminalGrowth);
                    }, 1200);

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
                        double shares = parseAlphaDouble(response.body().getSharesOutstanding());

                        if (shares == 0.0) {
                            showError("לא נמצאו נתוני מניות (Shares Outstanding) לחברה זו.");
                            return;
                        }

                        // *** ממתינים עוד 1.2 שניות לפני ששואבים את המחיר! ***
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            fetchQuoteForDcf(api, ticker, fcf, shares, growthRate, discountRate, terminalGrowth);
                        }, 1200);

                    } catch (Exception e) {
                        showError("Failed to parse Shares Outstanding");
                    }
                } else {
                    showError("API Error: Overview data not found");
                }
            }
            @Override
            public void onFailure(Call<CompanyOverviewResponse> call, Throwable t) { showError("Network Error"); }
        });
    }

    private void showTipDialog(String title, String message) {
        if (getContext() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("הבנתי", null)
                .show();
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

        db.collection("stocks").document(ticker).set(updates, SetOptions.merge());
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
        if (getActivity() == null) return;
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

    private void initApiCounter() {
        if (getActivity() == null) return;

        SharedPreferences apiPrefs = requireContext().getSharedPreferences("API_PREFS", Context.MODE_PRIVATE);
        long lastReset = apiPrefs.getLong("last_reset_date", 0);

        // אם עברו 24 שעות (86,400,000 מילישניות), נאפס חזרה ל-25
//        if (System.currentTimeMillis() - lastReset > 86400000) {
        if (System.currentTimeMillis() - lastReset > 0) {

            apiPrefs.edit()
                    .putLong("last_reset_date", System.currentTimeMillis())
                    .putInt("requests_left", 100)
                    .apply();
        }
        updateCalculateButtonUI();
    }

    private void updateCalculateButtonUI() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            btnCalculate.setText("Calculate");

                btnCalculate.setTextColor(android.graphics.Color.WHITE);

        });
    }

    private void showError(String msg) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            btnCalculate.setEnabled(true);
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
        });
    }
    private double parseAlphaDouble(String value) {
        if (value == null || value.equalsIgnoreCase("none") || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}