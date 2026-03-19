package elad.maayan.themarginhunter;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AddStockFragment extends Fragment {

    private TextInputEditText etTicker, etEPS, etGrowth;
    private TextInputLayout tilTicker, tilEPS, tilGrowth;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Button btnSave;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_stock, container, false);

        etTicker = view.findViewById(R.id.etTicker);
        etEPS = view.findViewById(R.id.etEPS);
        etGrowth = view.findViewById(R.id.etGrowth);

        tilTicker = view.findViewById(R.id.tilTicker);
        tilEPS = view.findViewById(R.id.tilEPS);
        tilGrowth = view.findViewById(R.id.tilGrowth);

        btnSave = view.findViewById(R.id.btnSaveStock);
        btnSave.setOnClickListener(v -> validateAndSave());
        String editTicker = getArguments() != null ? getArguments().getString("ticker") : null;
        if (editTicker != null) {
            // אנחנו במצב עריכה!
            etTicker.setText(editTicker);
            etTicker.setEnabled(false); // אסור לשנות טיקר של מניה קיימת (זה המפתח ב-DB)
            btnSave.setText("Update Stock"); // שינוי טקסט הכפתור בשביל ה-UX

            // משיכת הנתונים הנוכחיים מהענן כדי למלא את השדות
            db.collection("stocks").document(editTicker).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Stock stock = documentSnapshot.toObject(Stock.class);
                            if (stock != null) {
                                etEPS.setText(String.valueOf(stock.getEps()));
                                etGrowth.setText(String.valueOf(stock.getGrowthRate()));
                                // אפשר למלא כאן עוד שדות אם הוספת
                            }
                        }
                    });
        }
        return view;
    }

    private void validateAndSave() {
        String ticker = etTicker.getText().toString().trim().toUpperCase();
        String epsStr = etEPS.getText().toString().trim();
        String growthStr = etGrowth.getText().toString().trim();

        boolean isValid = true;

// --- וולידציה ל-Ticker ---
        if (ticker.isEmpty()) {
            tilTicker.setError("חובה להזין סימול מניה");
            isValid = false;
        } else if (ticker.length() > 5) {
            tilTicker.setError("סימול מניה לא יכול לעלות על 5 תווים");
            isValid = false;
        } else {
            tilTicker.setError(null); // ניקוי שגיאה אם תקין
        }
// --- וולידציה ל-EPS ---
        if (epsStr.isEmpty()) {
            tilEPS.setError("חובה להזין רווח למניה");
            isValid = false;
        } else {
            try {
                double eps = Double.parseDouble(epsStr);
                tilEPS.setError(null);
            } catch (NumberFormatException e) {
                tilEPS.setError("נא להזין מספר תקין");
                isValid = false;
            }
        }
        // --- וולידציה ל-Growth Rate ---
        if (growthStr.isEmpty()) {
            tilGrowth.setError("חובה להזין קצב צמיחה");
            isValid = false;
        } else {
            try {
                double growth = Double.parseDouble(growthStr);
                if (growth < 0 || growth > 100) {
                    tilGrowth.setError("קצב צמיחה חייב להיות בין 0 ל-100");
                    isValid = false;
                } else {
                    tilGrowth.setError(null);
                }
            } catch (NumberFormatException e) {
                tilGrowth.setError("נא להזין מספר תקין");
                isValid = false;
            }
        }
        if (isValid) {
            saveToFirestore(ticker, Double.parseDouble(epsStr), Double.parseDouble(growthStr));
        }
    }

    private void saveToFirestore(String ticker, double eps, double growth) {
        // יצירת אובייקט זמני למשלוח (הענן יחשב את השאר)
        Map<String, Object> stockData = new HashMap<>();
        stockData.put("eps", eps);
        stockData.put("growthRate", growth);
        stockData.put("lastUpdated", System.currentTimeMillis());

        db.collection("stocks").document(ticker)
                .set(stockData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), ticker + " added to Hunt!", Toast.LENGTH_SHORT).show();
                    // חזרה למסך ה-Watchlist
                    Navigation.findNavController(getView()).navigateUp();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error saving", Toast.LENGTH_SHORT).show());
    }


}