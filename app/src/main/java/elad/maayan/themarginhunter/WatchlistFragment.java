package elad.maayan.themarginhunter;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchlistFragment extends Fragment {

    private RecyclerView recyclerView;
    private WatchlistAdapter adapter;
    private List<Stock> watchlistStocks = new ArrayList<>();
    private FloatingActionButton fabAddStock;
    private String currentSelectedSector = "הכל";

    // --- משתנים חדשים לרענון וזמן ---
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvLastUpdated;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "WatchlistPrefs";
    private static final String KEY_LAST_UPDATED = "last_updated";
    private FirebaseFirestore db;
    private com.google.firebase.firestore.ListenerRegistration watchlistListener;
    int currentScanIndex;
    private boolean isScanning = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchlist, container, false);

        db = FirebaseFirestore.getInstance();
        // אתחול משתנים חדשים
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated);
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // טעינת זמן עדכון קודם מהזיכרון
        String lastUpdated = prefs.getString(KEY_LAST_UPDATED, "טרם עודכן");
        tvLastUpdated.setText("עודכן לאחרונה: " + lastUpdated);

        fabAddStock = view.findViewById(R.id.fabAddStock);
        fabAddStock.setOnClickListener(v -> {
            AddStockFragment addStockSheet = new AddStockFragment();
            addStockSheet.show(getParentFragmentManager(), "AddStockBottomSheet");
        });

        recyclerView = view.findViewById(R.id.rvWatchlist);
        setupRecyclerView();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshAllStocksFromYahoo();
        });

        // הוספת האנימציה בגלילה (הסתרת הכפתור)
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && fabAddStock.isShown()) {
                    fabAddStock.hide();
                } else if (dy < 0 && !fabAddStock.isShown()) {
                    fabAddStock.show();
                }
            }
        });

        // מאזין לתיבת החיפוש
        EditText etSearchStock = view.findViewById(R.id.etSearchStock);
        etSearchStock.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // מומלץ להתחיל את ההאזנה כאן
        startListeningToFirebase();
    }

    private void refreshAllStocksFromYahoo() {
        if (isScanning) return; // אם כבר רץ, אל תעשה כלום
        isScanning = true;

        List<String> allTickers = StockUniverse.getTopStocks(); // המקור שלך
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        currentScanIndex = 0; // תוסיף משתנה גלובלי int currentScanIndex
        processTickerForWatchlist(allTickers);
        isScanning = false;
    }

    private void processTickerForWatchlist(List<String> tickers) {
        if (!isAdded() || currentScanIndex >= tickers.size()) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String ticker = tickers.get(currentScanIndex);

        // קריאה ליאהו
        StockApiService apiService = RetrofitClient.getApiService();
        String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + ticker + "?modules=defaultKeyStatistics,financialData";

        apiService.getYahooSummary(url).enqueue(new Callback<YahooSummaryResponse>() {
            @Override
            public void onResponse(Call<YahooSummaryResponse> call, Response<YahooSummaryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveStockToGeneralCollection(ticker, response.body());
                }

                // עוברים למניה הבאה בהשהיה קלה כדי לא להיחסם ע"י יאהו
                currentScanIndex++;
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        processTickerForWatchlist(tickers), 1000);
            }

            @Override
            public void onFailure(Call<YahooSummaryResponse> call, Throwable t) {
                currentScanIndex++;
                processTickerForWatchlist(tickers);
            }
        });
    }

    private void saveStockToGeneralCollection(String ticker, YahooSummaryResponse response) {
        try {
            if (response.getQuoteSummary() == null || response.getQuoteSummary().getResult().isEmpty()) return;

            YahooSummaryResponse.Result result = response.getQuoteSummary().getResult().get(0);
            Stock stock = new Stock();
            stock.setTicker(ticker);
            stock.setLastUpdated(System.currentTimeMillis());

            // מחיר נוכחי - קריטי
            if (result.getFinancialData() != null && result.getFinancialData().getCurrentPrice() != null) {
                stock.setCurrentPrice(result.getFinancialData().getCurrentPrice().getRaw());
            }

            // EPS - קריטי ל-Bargain
            if (result.getDefaultKeyStatistics() != null && result.getDefaultKeyStatistics().getTrailingEps() != null) {
                stock.setEps(result.getDefaultKeyStatistics().getTrailingEps().getRaw());
            }

            // FCF ו-Shares - קריטי ל-DCF (המרנו ל-double כפי שביקשת)
            if (result.getFinancialData() != null && result.getFinancialData().getFreeCashflow() != null) {
                stock.setFcf(result.getFinancialData().getFreeCashflow().getRaw());
            }

            if (result.getDefaultKeyStatistics() != null && result.getDefaultKeyStatistics().getSharesOutstanding() != null) {
                stock.setSharesOutstanding(result.getDefaultKeyStatistics().getSharesOutstanding().getRaw());
            }

            // צמיחה חזויה
            double growthRate = 5.0;
            if (result.getEarningsTrend() != null && !result.getEarningsTrend().getTrend().isEmpty()) {
                YahooSummaryResponse.Trend trend = result.getEarningsTrend().getTrend().get(0);
                if (trend.getGrowth() != null) {
                    growthRate = trend.getGrowth().getRaw() * 100;
                }
            }
            stock.setGrowthRate(Math.max(0, Math.min(growthRate, 20.0)));

            // שמירה ל-Firebase
            db.collection("stocks").document(ticker)
                    .set(stock, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("WATCHLIST", "Synced: " + ticker));

        } catch (Exception e) {
            Log.e("WATCHLIST", "Error parsing data for " + ticker, e);
        }
    }

    private void filterList(String text) {
        if (TextUtils.isEmpty(text)) {
            // אם החיפוש ריק, הצג את הרשימה המלאה המקורית
            if (adapter != null) {
                adapter.updateList(new ArrayList<>(watchlistStocks));
            }
            return;
        }

        List<Stock> filteredList = new ArrayList<>();
        for (Stock stock : watchlistStocks) {
            if (stock.getTicker().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(stock);
            }
        }
        if (adapter != null) {
            adapter.updateList(filteredList);
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WatchlistAdapter(watchlistStocks, new WatchlistAdapter.OnStockClickListener() {
            @Override
            public void onStockClick(String ticker) {
                openAddStockBottomSheet(ticker);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void openAddStockBottomSheet(String ticker) {
        AddStockFragment addStockFragment = new AddStockFragment();
        Bundle args = new Bundle();
        args.putString("ticker", ticker);
        addStockFragment.setArguments(args);
        addStockFragment.show(getChildFragmentManager(), "AddStockBottomSheet");
    }

    private void startListeningToFirebase() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        // הסרת מאזין קודם בצורה בטוחה
        if (watchlistListener != null) {
            watchlistListener.remove();
            watchlistListener = null;
        }

        watchlistListener = db.collection("stocks")
                .addSnapshotListener((value, error) -> {
                    // בדיקת הגנה קריטית: האם הפרגמנט עדיין "חי"?
                    if (!isAdded() || getContext() == null) return;

                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                    if (error != null) {
                        Log.e("WATCHLIST", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        List<Stock> tempItems = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                // המרה ידנית בטוחה יותר כדי למנוע קריסות מנתונים לא תקינים
                                Map<String, Object> data = doc.getData();
                                Stock stock = new Stock();
                                stock.setTicker(String.valueOf(data.get("ticker")));

                                Object priceObj = data.get("currentPrice");
                                if (priceObj instanceof Number) {
                                    stock.setCurrentPrice(((Number) priceObj).doubleValue());
                                }

                                if (stock.getTicker() != null && !stock.getTicker().equals("null")) {
                                    tempItems.add(stock);
                                }
                            } catch (Exception e) {
                                Log.e("WATCHLIST", "Error parsing stock: " + doc.getId(), e);
                            }
                        }

                        // מיון
                        java.util.Collections.sort(tempItems, (s1, s2) ->
                                s1.getTicker().compareToIgnoreCase(s2.getTicker()));

                        // עדכון הרשימה
                        watchlistStocks.clear();
                        watchlistStocks.addAll(tempItems);

                        if (adapter != null) {
                            // עדיף להשתמש ב-updateList אם קיים אצלך באדפטר, או ב-notifyDataSetChanged
                            adapter.updateList(new ArrayList<>(watchlistStocks));
                        }

                        updateLastUpdatedTime();
                    }
                });
    }

    // פונקציית עזר לעדכון זמן
    private void updateLastUpdatedTime() {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvLastUpdated.setText("עודכן לאחרונה: " + currentTime);
        prefs.edit().putString(KEY_LAST_UPDATED, currentTime).apply();
    }

    // חשוב מאוד: שחרור המאזין כשהפרגמנט עוצר
    @Override
    public void onStop() {
        super.onStop();
        if (watchlistListener != null) {
            watchlistListener.remove();
            watchlistListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (watchlistListener != null) {
            watchlistListener.remove();
            watchlistListener = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}