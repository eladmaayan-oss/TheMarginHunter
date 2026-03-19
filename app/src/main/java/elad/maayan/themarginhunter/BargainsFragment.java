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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BargainsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BargainsFragment extends Fragment implements StockAdapterListener {

    private RecyclerView rvBargains;
    private ProgressBar progressBar;
    private View layoutEmpty;
    private List<Stock> bargainStocksList = new ArrayList<>();
    private StockAdapter adapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();


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
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        adapter = new StockAdapter(bargainStocksList, this);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddStock);

        fabAdd.setOnClickListener(v -> {
            // 1. הוסף את השורה הזו כדי לבדוק ב-Logcat
            Log.d("NAV_CHECK", "FAB was clicked!");

            try {
                // 2. פקודת הניווט
                Navigation.findNavController(v).navigate(R.id.action_nav_bargains_to_addStockFragment);
            } catch (Exception e) {
                // 3. אם יש שגיאה בניווט, נראה אותה כאן
                Log.e("NAV_CHECK", "Navigation failed: " + e.getMessage());
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

        return view;
    }

    private void fetchBargainStocks() {
        progressBar.setVisibility(View.VISIBLE);

        // שימוש ב-addSnapshotListener במקום ב-get()
        db.collection("stocks")
                .whereGreaterThan("intrinsicValue", 0)
                .addSnapshotListener((value, error) -> {
                    if (isAdded()) {
                        progressBar.setVisibility(View.GONE);

                        if (error != null) {
                            Log.e("Firebase", "Listen failed.", error);
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
                                }
                            }
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
        Navigation.findNavController(getView()).navigate(R.id.action_global_addStockFragment, bundle);
    }
}
