package elad.maayan.themarginhunter;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BargainsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BargainsFragment extends Fragment {

    private RecyclerView rvBargains;
    private ProgressBar progressBar;
    private View layoutEmpty;
    private List<Stock> bargainStocksList = new ArrayList<>();
    private StockAdapter adapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private View layoutProgress;
    private ProgressBar progressBarHorizontal;
    private android.widget.TextView tvProgressText;
    private float currentBasePE = 8.5f;
    private float currentGrowthMult = 2.0f;
    private android.content.SharedPreferences prefs;
    private int currentScanIndex = 0;
    private Handler scanHandler = new Handler(Looper.getMainLooper());
    private List<String> tickersToScan = new ArrayList<>();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public BargainsFragment() {
        // Required empty public constructor
    }

    private void startSmartScanner(List<Stock> list) {
        this.bargainStocksList = list;
        this.currentScanIndex = 0;
        processNextTicker();
    }

    private void processNextTicker() {
        if (getContext() == null || currentScanIndex >= tickersToScan.size()) {
            layoutProgress.setVisibility(View.GONE);
            Toast.makeText(getContext(), "סריקת השוק הסתיימה בהצלחה!", Toast.LENGTH_LONG).show();
            return;
        }

        String ticker = tickersToScan.get(currentScanIndex);

        // עדכון ה-UI
        updateProgressUI(currentScanIndex + 1, tickersToScan.size());

        // ביצוע הקריאה ליאהו עבור המניה הנוכחית בלבד
        fetchDataFromServer(ticker);

        currentScanIndex++;

        // כאן הקסם: במקום לרוץ למניה הבאה מיד, מחכים 1500 מילישניות (שנייה וחצי)
        scanHandler.postDelayed(this::processNextTicker, 1500);
    }

    private void scanMarketForBargains() {
        // טעינת רשימת הטיקרים מהיקום (ה-64 מניות)
        tickersToScan = StockUniverse.getTopStocks();
        int totalStocks = tickersToScan.size();

        // איפוס ממשק המשתמש (Progress Bar)
        layoutProgress.setVisibility(View.VISIBLE);
        progressBarHorizontal.setMax(totalStocks);
        progressBarHorizontal.setProgress(0);
        tvProgressText.setText("מכין סריקה...");

        currentScanIndex = 0;

        // התחלת התהליך הסדרתי
        processNextTicker();
    }
    // 3. פונקציית העזר שחייבת להיות כאן כדי לעדכן את התצוגה ולסגור אותה בסוף
    private void updateProgressUI(int current, int total) {
        progressBarHorizontal.setProgress(current);
        tvProgressText.setText("סורק מניות: " + current + " / " + total);

        // כשהגענו למניה האחרונה - מעלימים את הבר
        if (current == total) {
            layoutProgress.setVisibility(View.GONE);
            Toast.makeText(getContext(), "סריקת השוק הסתיימה", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BargainsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BargainsFragment newInstance(String param1, String param2) {
        BargainsFragment fragment = new BargainsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private void fetchDataFromServer(String ticker) {
        StockApiService apiService = RetrofitClient.getApiService();
        String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + ticker + "?modules=defaultKeyStatistics,financialData";

        apiService.getYahooSummary(url).enqueue(new Callback<YahooSummaryResponse>() {
            @Override
            public void onResponse(Call<YahooSummaryResponse> call, Response<YahooSummaryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // הלוגיקה הקיימת שלך לחישוב שווי פנימי ושמירה
                        processYahooResponse(ticker, response.body());
                    } catch (Exception e) {
                        Log.e("BARGAIN_HUNTER", "שגיאה בעיבוד נתונים עבור: " + ticker);
                    }
                }
            }

            @Override
            public void onFailure(Call<YahooSummaryResponse> call, Throwable t) {
                Log.e("BARGAIN_HUNTER", "נכשל במשיכת נתונים עבור: " + ticker);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bargains, container, false);
        rvBargains = view.findViewById(R.id.rvBargains);
        layoutProgress = view.findViewById(R.id.layoutProgress);
        progressBarHorizontal = view.findViewById(R.id.progressBarHorizontal);
        tvProgressText = view.findViewById(R.id.tvProgressText);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        adapter = new StockAdapter(bargainStocksList, new StockAdapterListener() {
            @Override
            public void onDeleteClicked(Stock stock) {
                db.collection("bargain_stocks").document(stock.getTicker()).delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "המניה נמחקה מרשימת המציאות", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onEditClicked(Stock stock) {
                Toast.makeText(getContext(), "עריכה לא זמינה במסך זה", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStockClicked(Stock stock) {

                AddStockFragment addStockFragment = new AddStockFragment();
                Bundle args = new Bundle();
                args.putString("ticker", stock.getTicker());
                addStockFragment.setArguments(args);
                addStockFragment.show(getChildFragmentManager(), "AddStockBottomSheet");

            }
        });

// הגדרת ה-RecyclerView
        rvBargains.setLayoutManager(new LinearLayoutManager(getContext()));

        // כאן נקרא לפונקציה שתמשוך נתונים מה-Firebase
        fetchBargainStocks();
        rvBargains.setAdapter(adapter);

// אם הרשימה ריקה, נציג את ה-Empty State
        if (bargainStocksList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvBargains.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvBargains.setVisibility(View.VISIBLE);
        }
        prefs = requireActivity().getSharedPreferences("GrahamPrefs", android.content.Context.MODE_PRIVATE);
        currentBasePE = prefs.getFloat("basePE", 8.5f);
        currentGrowthMult = prefs.getFloat("growthMult", 2.0f);
        android.widget.ImageButton btnFinetune = view.findViewById(R.id.btnFinetuneSettings);
        btnFinetune.setOnClickListener(v -> showFinetuneDialog());

// מציאת סרגל הכלים שהוספנו ל-XML
        com.google.android.material.appbar.MaterialToolbar toolbar = view.findViewById(R.id.topAppBar);

        // הגדרת מאזין ללחיצות על התפריט (הרדאר שלנו)
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_scan_market) {
                Toast.makeText(getContext(), "מפעיל סורק שוק...", Toast.LENGTH_SHORT).show();
                scanMarketForBargains(); // הפעלת פונקציית הסריקה!
                return true;
            }
            return false;
        });

        return view;
    }

    private void saveBargainToFirebase(Stock stock) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // אנחנו משתמשים בטיקר כ-ID כדי שלא יהיו כפילויות של אותה מניה
        db.collection("bargain_stocks").document(stock.getTicker())
                .set(stock)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Stock saved: " + stock.getTicker()))
                .addOnFailureListener(e -> Log.e("Firebase", "Error saving", e));
    }

    private void fetchBargainStocks() {

        // שימוש ב-addSnapshotListener במקום ב-get()
        db.collection("stocks")
                .whereGreaterThan("intrinsicValue", 0)
                .addSnapshotListener((value, error) -> {
                    if (isAdded()) {
                        if (error != null) {
                            Log.e("Firebase", "Listen failed.");
                            return;
                        }

                        if (value != null) {
                            bargainStocksList.clear();
                            for (QueryDocumentSnapshot document : value) {
                                Stock stock = document.toObject(Stock.class);
                                //   stock.setTicker(document.getId());

                                // סינון מניות ערך
                                if (stock.getCurrentPrice() < stock.getIntrinsicValue()) {
                                    bargainStocksList.add(stock);
                                    saveBargainToFirebase(stock);
                                }
                            }

                            Collections.sort(bargainStocksList, (s1, s2) ->
                                    Double.compare(s2.getMarginOfSafety(), s1.getMarginOfSafety())
                            );
                            // עדכון ה-UI (המתודה שלך)
                            updateUI();
                        }
                    }
                });
    }
    private void updateUI() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (bargainStocksList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvBargains.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvBargains.setVisibility(View.VISIBLE);
        }
    }

    private void processYahooResponse(String ticker, YahooSummaryResponse responseBody) {
        YahooSummaryResponse.Result result = responseBody.getQuoteSummary().getResult().get(0);

        double currentPrice = result.getFinancialData().getCurrentPrice().getRaw();
        double eps = result.getDefaultKeyStatistics().getTrailingEps().getRaw();

        double growthRate = 5.0; // ניתן לשכלל בעתיד
        double intrinsicValue = eps * (currentBasePE + currentGrowthMult * growthRate);
        double marginOfSafetyPrice = intrinsicValue * 0.90;

        if (currentPrice < marginOfSafetyPrice) {
            double marginOfSafetyPct = ((intrinsicValue - currentPrice) / intrinsicValue) * 100;

            Stock newBargain = new Stock();
            newBargain.setTicker(ticker);
            newBargain.setCurrentPrice(currentPrice);
            newBargain.setIntrinsicValue(intrinsicValue);
            newBargain.setMarginOfSafety(marginOfSafetyPct);
            newBargain.setLastCalculated(System.currentTimeMillis()); // הוספת זמן עדכון

            // שמירה ל-Firebase
            db.collection("stocks").document(ticker).set(newBargain);
        }
    }

    private void showFinetuneDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_finetune, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        com.google.android.material.slider.Slider sliderBasePE = dialogView.findViewById(R.id.sliderBasePE);
        com.google.android.material.slider.Slider sliderGrowthMult = dialogView.findViewById(R.id.sliderGrowthMult);
        android.widget.TextView tvBasePEValue = dialogView.findViewById(R.id.tvBasePEValue);
        android.widget.TextView tvGrowthMultValue = dialogView.findViewById(R.id.tvGrowthMultValue);
        android.widget.Button btnReset = dialogView.findViewById(R.id.btnReset);
        android.widget.Button btnSave = dialogView.findViewById(R.id.btnSaveFinetune);

        // עדכון ערכים התחלתיים מהזיכרון
        sliderBasePE.setValue(currentBasePE);
        sliderGrowthMult.setValue(currentGrowthMult);
        updateValueBox(tvBasePEValue, currentBasePE, 8.5f);
        updateValueBox(tvGrowthMultValue, currentGrowthMult, 2.0f);

        // מאזינים להזזת הסליידרים
        sliderBasePE.addOnChangeListener((slider, value, fromUser) -> {
            updateValueBox(tvBasePEValue, value, 8.5f);
        });

        sliderGrowthMult.addOnChangeListener((slider, value, fromUser) -> {
            updateValueBox(tvGrowthMultValue, value, 2.0f);
        });

        // כפתור איפוס לגראהם מקורי
        btnReset.setOnClickListener(v -> {
            sliderBasePE.setValue(8.5f);
            sliderGrowthMult.setValue(2.0f);
        });

        // כפתור שמירה
        btnSave.setOnClickListener(v -> {
            currentBasePE = sliderBasePE.getValue();
            currentGrowthMult = sliderGrowthMult.getValue();

            // שמירה ב-SharedPreferences כדי שיישאר גם מחר
            prefs.edit()
                    .putFloat("basePE", currentBasePE)
                    .putFloat("growthMult", currentGrowthMult)
                    .apply();

            dialog.dismiss();

            // מפעיל סריקה מחדש אוטומטית עם הפרמטרים החדשים
            Toast.makeText(getContext(), "הפרמטרים עודכנו! מתחיל סריקה...", Toast.LENGTH_SHORT).show();
            scanMarketForBargains();
        });

        dialog.show();
    }

    // פונקציית עזר שצובעת את הריבוע בירוק אם אנחנו על ערכי גראהם המקוריים
    private void updateValueBox(android.widget.TextView tv, float currentValue, float grahamOriginal) {
        tv.setText(String.format(java.util.Locale.US, "%.1f", currentValue));

        if (Math.abs(currentValue - grahamOriginal) < 0.01f) {
            // אם זה הערך המקורי - צבע טקסט ירוק
            tv.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
        } else {
            // אם הוזז - צבע שחור רגיל
            tv.setTextColor(android.graphics.Color.BLACK);
        }
    }
}
