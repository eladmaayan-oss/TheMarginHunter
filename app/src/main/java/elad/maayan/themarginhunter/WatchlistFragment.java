package elad.maayan.themarginhunter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WatchlistFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WatchlistFragment extends Fragment implements StockAdapterListener {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<Stock> stockList = new ArrayList<>();
    private StockAdapter adapter;
    private RecyclerView rvWatchlist;
    private ProgressBar pbRefresh;
    private ImageButton btnRefreshAll;
    private boolean isRefreshing = false; // משתנה מחלקה למניעת כפילויות
    private SwipeRefreshLayout swipeRefreshLayout;
    private int apiRequestsLeft;
    private static final int DAILY_LIMIT = 25;



    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public WatchlistFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment WatchlistFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WatchlistFragment newInstance(String param1, String param2) {
        WatchlistFragment fragment = new WatchlistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchlist, container, false);

        // אתחול רשימה ואדפטר
        rvWatchlist = view.findViewById(R.id.rvWatchlist);
        adapter = new StockAdapter(stockList, this);
        rvWatchlist.setLayoutManager(new LinearLayoutManager(getContext()));
        rvWatchlist.setAdapter(adapter);
        pbRefresh = view.findViewById(R.id.pbRefresh);
        btnRefreshAll = view.findViewById(R.id.btnRefreshAll);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        checkApiLimit();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshAllStocksPrices();
        });

        btnRefreshAll.setOnClickListener(v -> {
            Log.d("REFRESH", "Refresh button clicked");
            refreshAllStocks();
        });
        // פתיחת הדיאלוג - הדרך הנכונה
        view.findViewById(R.id.fabAddStock).setOnClickListener(v -> {
            AddStockFragment addStockDialog = new AddStockFragment();
            addStockDialog.show(getChildFragmentManager(), "AddStockTag");
        });

        // חיפוש
        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) adapter.filter(newText);
                return true;
            }
        });
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // אנחנו לא צריכים גרירה (Drag & Drop) כרגע
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // 1. שולפים את המניה שהוחלקה
                Stock deletedStock = adapter.getStockAt(position);
                String ticker = deletedStock.getTicker(); // או איך שקראת ל-Getter של הטיקר

                // 2. מסירים אותה קודם כל מהתצוגה (כדי שהאנימציה תהיה חלקה ומידית)
                adapter.removeItem(position);

                // 3. מוחקים מ-Firebase
                db.collection("stocks").document(ticker).delete()
                        .addOnSuccessListener(aVoid -> {
                            // 4. מציגים הודעה עם אפשרות ביטול (Undo)
                            Snackbar snackbar = Snackbar.make(getView(), ticker + " removed from Watchlist", Snackbar.LENGTH_LONG);
                            snackbar.setAction("UNDO", v -> {
                                // אם המשתמש התחרט - מחזירים ל-Firebase ולתצוגה
                                db.collection("stocks").document(ticker).set(deletedStock);
                                adapter.restoreItem(deletedStock, position);
                            });
                            snackbar.setActionTextColor(android.graphics.Color.YELLOW);
                            snackbar.show();
                        })
                        .addOnFailureListener(e -> {
                            // אם המחיקה נכשלה, נחזיר את המניה לתצוגה ונראה שגיאה
                            adapter.restoreItem(deletedStock, position);
                            Toast.makeText(getContext(), "Failed to delete stock", Toast.LENGTH_SHORT).show();
                        });
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(rvWatchlist);
        fetchAllStocks(); // מאזין לשינויים ב-Firestore
        return view;
    }
    private void fetchAllStocks() {
        db.collection("stocks")
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                //   .orderBy("marginOfSafety", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("WATCHLIST", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        List<Stock> tempSearchList = new ArrayList<>(); // רשימה זמנית חדשה
                        for (QueryDocumentSnapshot document : value) {
                            try {
                                Stock stock = document.toObject(Stock.class);
                                if (stock != null) {
                                    tempSearchList.add(stock);
                                }
                            } catch (Exception e) {
                                Log.e("FIRESTORE", "Error parsing stock: " + document.getId(), e);
                            }
                        }

                        Log.d("WATCHLIST", "Successfully parsed " + tempSearchList.size() + " stocks");

                        // מעדכנים את האדפטר ברשימה החדשה
                        adapter.updateData(tempSearchList);
                    }
                });
    }
    private void refreshAllStocks() {
        if (isRefreshing) {
            Log.d("REFRESH", "Already refreshing, ignoring click.");
            return;
        }


        List<Stock> currentStocks = adapter.getStocks();
        if (currentStocks == null || currentStocks.isEmpty()) return;
        Log.d("REFRESH","starting refresh");

        isRefreshing = true; // נועל את הכפתור
        // הצגת ה-Progress Bar
        pbRefresh.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "מעדכן מחירים (2 שניות למניה)...", Toast.LENGTH_SHORT).show();

        for (int i = 0; i < currentStocks.size(); i++) {
            final int index = i; // עבור ה-Handler
            Stock stock = currentStocks.get(index);

            int finalI = i;
            new android.os.Handler().postDelayed(() -> {
                updateSingleStockPrice(stock);
                Log.d("REGRESH","refreshing stock"+finalI);
                // אם הגענו למניה האחרונה ברשימה - נסתיר את ה-Progress
                if (index == currentStocks.size() - 1) {
                    isRefreshing = false; // משחרר את הנעילה
                    pbRefresh.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "העדכון הסתיים!", Toast.LENGTH_SHORT).show();
                }
            }, i * 2000L); // השהיה מצטברת
        }
    }
    private void checkApiLimit() {
        SharedPreferences apiPrefs = requireContext().getSharedPreferences("API_PREFS", Context.MODE_PRIVATE);
        long lastReset = apiPrefs.getLong("last_reset_date", 0);
        apiRequestsLeft = apiPrefs.getInt("requests_left", DAILY_LIMIT);

        if (System.currentTimeMillis() - lastReset > 86400000) {
            apiRequestsLeft = DAILY_LIMIT;
            apiPrefs.edit()
                    .putLong("last_reset_date", System.currentTimeMillis())
                    .putInt("requests_left", apiRequestsLeft)
                    .apply();
        }
    }
    private void refreshAllStocksPrices() {
        // נניח שהרשימה שלך נשמרת במשתנה שנקרא stockList
        // (תחליף את זה בשם של הרשימה האמיתית שיש לך באדפטר או בפרגמנט)
        int numberOfStocks = stockList.size();

        if (numberOfStocks == 0) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (apiRequestsLeft < numberOfStocks) {
            Toast.makeText(getContext(), "אין מספיק קריאות API לרענן את כל הרשימה (" + apiRequestsLeft + " נותרו)", Toast.LENGTH_LONG).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // אם הגענו לכאן - יש מספיק קריאות! נעבור מניה מניה ונעדכן מחיר
        StockApiService api = RetrofitClient.getApiService();
        for (Stock stock : stockList) {
            api.getStockQuote("GLOBAL_QUOTE", stock.getTicker(), "BH00QGEFNFNZ1IDN").enqueue(new Callback<AlphaVantageResponse>() {
                @Override
                public void onResponse(Call<AlphaVantageResponse> call, Response<AlphaVantageResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getQuote() != null) {
                        try {
                            // מורידים קריאה אחת על כל מניה
                            decreaseApiCount(1);

                            double newPrice = Double.parseDouble(response.body().getQuote().getPrice());

                            // מחשבים את שולי הביטחון (MOS) מחדש!
                            double iv = stock.getIntrinsicValue();
                            double newMos = ((iv - newPrice) / iv) * 100;

                            // מעדכנים ב-Firebase (אם יש לך Listener פעיל - הרשימה תתעדכן לבד!)
                            FirebaseFirestore.getInstance().collection("stocks")
                                    .document(stock.getTicker())
                                    .update("currentPrice", newPrice, "marginOfSafety", newMos);

                        } catch (Exception ignored) {}
                    }
                }
                @Override
                public void onFailure(Call<AlphaVantageResponse> call, Throwable t) {}
            });
        }

        // עוצרים את האנימציה המסתובבת (הנתונים יתעדכנו ברקע מ-Firebase)
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(getContext(), "מרענן מחירים...", Toast.LENGTH_SHORT).show();
    }
    private void decreaseApiCount(int amount) {
        apiRequestsLeft -= amount;
        if (apiRequestsLeft < 0) apiRequestsLeft = 0;

        SharedPreferences prefs = requireContext().getSharedPreferences("API_PREFS", Context.MODE_PRIVATE);
        prefs.edit().putInt("requests_left", apiRequestsLeft).apply();
    }

    private void updateSingleStockPrice(Stock stock) {
        StockApiService api = RetrofitClient.getApiService();
        String API_KEY = "BH00QGEFNFNZ1IDN";

        api.getStockQuote("GLOBAL_QUOTE", stock.getTicker(), API_KEY).enqueue(new Callback<AlphaVantageResponse>() {
            @Override
            public void onResponse(Call<AlphaVantageResponse> call, Response<AlphaVantageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AlphaVantageResponse.StockQuote quote = response.body().getQuote();
                    if (quote != null && quote.getPrice() != null) {
                        double newPrice = Double.parseDouble(quote.getPrice());

                        // חישוב מחדש של ה-Margin of Safety עם המחיר החדש
                        double newMos = ((stock.getIntrinsicValue() - newPrice) / stock.getIntrinsicValue()) * 100;

                        // עדכון ב-Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("currentPrice", newPrice);
                        updates.put("marginOfSafety", newMos);
                        updates.put("lastUpdated", System.currentTimeMillis());

                        db.collection("stocks").document(stock.getTicker())
                                .update(updates)
                                .addOnSuccessListener(aVoid -> Log.d("REFRESH", "Updated: " + stock.getTicker()));
                    }
                }
            }

            @Override
            public void onFailure(Call<AlphaVantageResponse> call, Throwable t) {
                Log.e("REFRESH", "Failed updating " + stock.getTicker());
            }
        });
    }

    @Override
    public void onDeleteClicked(Stock stock) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Stock?")
                .setMessage("Are you sure you want to remove " + stock.getTicker() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // 2. מחק מ-Firestore
                    db.collection("stocks").document(stock.getTicker())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Deleted!", Toast.LENGTH_SHORT).show();
                                // אין צורך לעדכן ידנית את הרשימה, ה snapshotListener יעשה את זה
                            });
                })
                .setNegativeButton("No", null)
                .show();


    }

    @Override
    public void onEditClicked(Stock stock) {
        Bundle bundle = new Bundle();
        bundle.putString("ticker", stock.getTicker());
        Navigation.findNavController(getView()).navigate(R.id.action_global_addStockFragment, bundle);    }
}