package elad.maayan.themarginhunter;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    private List<Stock> stockList;
    private StockAdapterListener listener;
    private List<Stock> fullStockList = new ArrayList<>();

    @NonNull
    @Override
    public StockAdapter.StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stock_item, parent, false);
        return new StockViewHolder(view);
    }
    public StockAdapter(List<Stock> stockList, StockAdapterListener listener) {
        this.stockList = stockList;
        this.listener = listener;
        this.fullStockList = new ArrayList<>(stockList);
    }
    // מתודה לעדכון הרשימה המסוננת
    public void filter(String text) {
        // אם אין טקסט חיפוש, פשוט תציג את כל הגיבוי
        if (text == null || text.isEmpty()) {
            stockList.clear();
            stockList.addAll(fullStockList);
            notifyDataSetChanged();
            return;
        }

        // אם אנחנו כאן, יש טקסט חיפוש
        String query = text.toLowerCase().trim();
        stockList.clear();

        for (Stock stock : fullStockList) {
            String ticker = stock.getTicker() != null ? stock.getTicker().toLowerCase() : "";
            String name = stock.getCompanyName() != null ? stock.getCompanyName().toLowerCase() : "";

            if (ticker.contains(query) || name.contains(query)) {
                stockList.add(stock);
            }
        }
        notifyDataSetChanged();
    }


    public void updateData(List<Stock> newList) {
        this.fullStockList = new ArrayList<>(newList);
        this.stockList.clear();
        this.stockList.addAll(newList);
        notifyDataSetChanged();
    }

    public Stock getStockAt(int position) {
        return stockList.get(position); // stockList זה שם הרשימה שלך באדפטר
    }

    public void removeItem(int position) {
        stockList.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Stock item, int position) {
        stockList.add(position, item);
        notifyItemInserted(position);
    }

    @Override
    public void onBindViewHolder(@NonNull StockAdapter.StockViewHolder holder, int position) {
        Stock stock = stockList.get(position);

        // 1. נתונים בסיסיים
        holder.tvTicker.setText(stock.getTicker());
        holder.tvPrice.setText(String.format("מחיר שוק: $%.2f", stock.getCurrentPrice()));

        // 2. טיפול בערך פנימי (בדיקה אם הנתון קיים)
        double iv = stock.getIntrinsicValue();
        if (iv <= 0) {
            holder.tvIntrinsicValue.setText("ערך פנימי: בחישוב...");
        } else {
            holder.tvIntrinsicValue.setText(String.format("ערך פנימי: $%.2f", iv));
        }

        double div = 0.0;
        String yieldStr = stock.getDividendYield();

// בודקים שהמחרוזת לא null, לא ריקה, ולא המילה "None"
        if (yieldStr != null && !yieldStr.trim().isEmpty() && !yieldStr.equalsIgnoreCase("None")) {
            try {
                div = Double.parseDouble(yieldStr);
            } catch (NumberFormatException e) {
                // אם מאיזושהי סיבה זה לא מספר, נבלע את השגיאה והערך יישאר 0.0
            }
        }
        // 4. לוגיקת צבעים ואמוג'ים לפי ה-MOS (מרווח ביטחון)
        double mos = stock.getMarginOfSafety();
        holder.tvMOS.setText(String.format("מרווח ביטחון: %.1f%%", mos));

        if (mos >= 30) {
            // מציאה עמוקה - ירוק כהה + אמוג'י
            holder.tvMOS.setText("🔥 " + holder.tvMOS.getText());
            holder.tvMOS.setBackgroundColor(Color.parseColor("#2E7D32"));
            holder.tvMOS.setTextColor(Color.WHITE);
        } else if (mos > 0) {
            // מתחת לערך - ירוק בהיר
            holder.tvMOS.setText("✅ " + holder.tvMOS.getText());
            holder.tvMOS.setBackgroundColor(Color.parseColor("#81C784"));
            holder.tvMOS.setTextColor(Color.BLACK);
        } else {
            // יקר מדי - אדום
            holder.tvMOS.setText("⚠️ " + holder.tvMOS.getText());
            holder.tvMOS.setBackgroundColor(Color.parseColor("#E57373"));
            holder.tvMOS.setTextColor(Color.WHITE);
        }

        // 5. כפתורי עריכה ומחיקה
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(stock);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(stock);
        });
    }
    public List<Stock> getStocks() {
        return stockList;
    }
    @Override
    public int getItemCount() {
        return stockList != null ? stockList.size() : 0;    }


    // ViewHolder שמחזיק את הרכיבים של ה-CardView
    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicker, tvPrice, tvIntrinsicValue, tvMOS, tvDividend;
        ImageButton btnDelete, btnEdit;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTicker = itemView.findViewById(R.id.tvTicker);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvIntrinsicValue = itemView.findViewById(R.id.tvIntrinsicValue);
            tvMOS = itemView.findViewById(R.id.tvMoS);
            tvDividend = itemView.findViewById(R.id.tvDividend);
            btnDelete = itemView.findViewById(R.id.btnDeleteStock);
            btnEdit = itemView.findViewById(R.id.btnEditStock);

        }
    }
}
