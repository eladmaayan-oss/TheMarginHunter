package elad.maayan.themarginhunter;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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
            loadStocksFromFirebase(); // עכשיו מושך מיד מ-Firebase
        });
        swipeRefreshLayout.setRefreshing(true);
        loadStocksFromFirebase();

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

    private void filterList(String text) {
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

    private void loadStocksFromFirebase() {
        db.collection("stocks").get().addOnSuccessListener(queryDocumentSnapshots -> {
            swipeRefreshLayout.setRefreshing(false);

            // יצירת רשימה זמנית כדי לא לרוקן את המסך לפני שווידאנו שהכל תקין
            List<Stock> tempItems = new ArrayList<>();

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                try {
                    Stock stock = doc.toObject(Stock.class);
                    // הגנה: הוסף רק אם הטיקר קיים ולא ריק
                    if (stock != null && stock.getTicker() != null) {
                        tempItems.add(stock);
                    }
                } catch (Exception e) {
                    // אם מנייה אחת משובשת (כמו בעיית ה-long/string), פשוט נדלג עליה ולא נקריס הכל
                    android.util.Log.e("WATCHLIST", "Error parsing stock: " + doc.getId(), e);
                }
            }

            // מיון עם הגנה
            java.util.Collections.sort(tempItems, (s1, s2) -> {
                String t1 = s1.getTicker() != null ? s1.getTicker() : "";
                String t2 = s2.getTicker() != null ? s2.getTicker() : "";
                return t1.compareToIgnoreCase(t2);
            });

            // עדכון הרשימה הראשית והאדפטר
            watchlistStocks.clear();
            watchlistStocks.addAll(tempItems);

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

        }).addOnFailureListener(e -> {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getContext(), "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
        });
    }}