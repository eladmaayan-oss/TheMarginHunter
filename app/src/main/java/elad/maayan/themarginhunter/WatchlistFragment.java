package elad.maayan.themarginhunter;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchlistFragment extends Fragment {

    private RecyclerView recyclerView;
    private WatchlistAdapter adapter;
    private List<Stock> watchlistStocks = new ArrayList<>();
    private FloatingActionButton fabAddStock;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchlist, container, false);
        fabAddStock = view.findViewById(R.id.fabAddStock);

        fabAddStock.setOnClickListener(v -> {
            // יצירת מופע חדש של ה-Bottom Sheet
            AddStockFragment addStockSheet = new AddStockFragment();

            addStockSheet.show(getParentFragmentManager(), "AddStockBottomSheet");
        });
        recyclerView = view.findViewById(R.id.rvWatchlist);
        RecyclerView rvWatchlist = view.findViewById(R.id.rvWatchlist);
        FloatingActionButton fabAddStock = view.findViewById(R.id.fabAddStock);


// הוספת האנימציה בגלילה
        rvWatchlist.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // אם גוללים למטה (dy > 0) והכפתור מוצג - נסתיר אותו
                if (dy > 0 && fabAddStock.isShown()) {
                    fabAddStock.hide();
                }
                // אם גוללים למעלה (dy < 0) והכפתור מוסתר - נציג אותו
                else if (dy < 0 && !fabAddStock.isShown()) {
                    fabAddStock.show();
                }
            }
        });

        setupRecyclerView();
        EditText etSearchStock = view.findViewById(R.id.etSearchStock);

        etSearchStock.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // מופעלת בכל פעם שהטקסט משתנה (אפילו הוספת אות אחת)
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
        loadStocksFromYahoo();

        return view;
    }

    private void filterList(String text) {
        List<Stock> filteredList = new ArrayList<>();

        // מעבר על הרשימה המקורית המלאה שלנו
        for (Stock stock : watchlistStocks) {
            // אם הטקסט שהוקלד מוכל בתוך הטיקר (ללא התחשבות באותיות גדולות/קטנות)
            if (stock.getTicker().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(stock);
            }
        }

        // שליחת הרשימה המסוננת לאדפטר שיירענן את המסך
        if (adapter != null) {
            adapter.updateList(filteredList);
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // כאן אנחנו מעבירים את המאזין שמפעיל את הפונקציה שלנו בעת לחיצה על מניה
        adapter = new WatchlistAdapter(watchlistStocks, new WatchlistAdapter.OnStockClickListener() {
            @Override
            public void onStockClick(String ticker) {
                openAddStockBottomSheet(ticker);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    // הפונקציה שפותחת את ה-Bottom Sheet
    private void openAddStockBottomSheet(String ticker) {
        AddStockFragment addStockFragment = new AddStockFragment();

        // אריזת הטיקר והעברתו לפרגמנט
        Bundle args = new Bundle();
        args.putString("ticker", ticker);
        addStockFragment.setArguments(args);

        // הצגת ה-BottomSheet על המסך
        addStockFragment.show(getChildFragmentManager(), "AddStockBottomSheet");
    }

    private void loadStocksFromYahoo() {
        List<String> tickers = StockUniverse.getTopStocks();

        // הופך את הרשימה למחרוזת: "AAPL,MSFT,GOOGL"
        String joinedTickers = TextUtils.join(",", tickers);
        String url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=" + joinedTickers;

        StockApiService apiService = RetrofitClient.getApiService();
        apiService.getYahooBulkQuotes(url).enqueue(new Callback<YahooBulkQuoteResponse>() {
            @Override
            public void onResponse(Call<YahooBulkQuoteResponse> call, Response<YahooBulkQuoteResponse> response) {
                watchlistStocks.clear();

                // ניצור מילון שיחבר בין הטיקר למחיר שלו לשליפה מהירה
                Map<String, Double> priceMap = new HashMap<>();
                if (response.isSuccessful() && response.body() != null && response.body().getQuoteResponse() != null) {
                    List<YahooBulkQuoteResponse.Quote> quotes = response.body().getQuoteResponse().getResult();
                    if (quotes != null) {
                        for (YahooBulkQuoteResponse.Quote quote : quotes) {
                            priceMap.put(quote.getSymbol(), quote.getRegularMarketPrice());
                        }
                    }
                }

                // בניית רשימת המניות הסופית
                for (String ticker : tickers) {
                    Stock stock = new Stock();
                    stock.setTicker(ticker);
                    stock.setPrice(priceMap.containsKey(ticker) ? priceMap.get(ticker) : 0.0);
                    watchlistStocks.add(stock);
                }
                java.util.Collections.sort(watchlistStocks, (s1, s2) -> s1.getTicker().compareToIgnoreCase(s2.getTicker()));

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<YahooBulkQuoteResponse> call, Throwable t) {
                if(getActivity() != null) {
                    Toast.makeText(getContext(), "שגיאה בטעינת נתונים מיאהו", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}