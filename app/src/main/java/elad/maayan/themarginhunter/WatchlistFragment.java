package elad.maayan.themarginhunter;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchlist, container, false);
        adapter = new StockAdapter(stockList, this);
        view.findViewById(R.id.fabAddStock).setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_global_addStockFragment);
        });

        // בתוך WatchlistFragment.java או BargainsFragment.java

        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setSubmitButtonEnabled(false); // מבטל את הצורך בכפתור "שלח"
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // לא צריך ללחוץ Enter
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // כאן הקסם קורה!
                // בכל פעם שהמשתמש מקליד או מוחק אות, האדפטר מעדכן את הרשימה מיד.
                if (adapter != null) {
                    adapter.filter(newText);
                }
                return true; // מחזירים true כדי לציין שטיפלנו באירוע
            }
        });

        rvWatchlist = view.findViewById(R.id.rvWatchlist);
        FloatingActionButton fabAddStock = view.findViewById(R.id.fabAddStock);

        rvWatchlist.setLayoutManager(new LinearLayoutManager(getContext()));
        rvWatchlist.setAdapter(adapter);

        fetchAllStocks(); // קריאה לנתונים

        // Inflate the layout for this fragment
        return view;
    }

    private void fetchAllStocks() {
        db.collection("stocks")
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