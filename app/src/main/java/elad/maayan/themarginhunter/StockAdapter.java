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

    @Override
    public void onBindViewHolder(@NonNull StockAdapter.StockViewHolder holder, int position) {
        Stock stock = stockList.get(position);

        holder.tvTicker.setText(stock.getTicker());
        holder.tvPrice.setText(String.format("$%.2f", stock.getCurrentPrice()));
        holder.tvIntrinsicValue.setText(String.format("$%.2f", stock.getIntrinsicValue()));
        holder.tvDividend.setText(String.format("Div Yield: %.1f%%", stock.getDividendYield()));
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClicked(stock);
            } else {
                Log.e("ADAPTER_ERROR", "Listener is null for delete click on ticker: " + stock.getTicker());
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClicked(stock);
            } else {
                Log.e("ADAPTER_ERROR", "Listener is null for edit click on ticker: " + stock.getTicker());
            }
        });

        // חישוב ועיצוב ה-Margin of Safety
        double mos = stock.getMarginOfSafety();
        holder.tvMoS.setText(String.format("Margin: %.1f%%", mos));

        if (stock.getEps() == 0 && stock.getIntrinsicValue() == 0) {
            holder.tvIntrinsicValue.setText("Loading...");
        } else {
            holder.tvIntrinsicValue.setText(String.format("$%.2f", stock.getIntrinsicValue()));
        }

        // לוגיקת צבעים לפי רמת ה"ערך"
        if (mos >= 30) {
            // "מציאה" עמוקה - ירוק כהה
            holder.tvMoS.setBackgroundColor(Color.parseColor("#2E7D32"));
        } else if (mos > 0) {
            // מתחת לערך - ירוק בהיר
            holder.tvMoS.setBackgroundColor(Color.parseColor("#81C784"));
        } else {
            // יקר מדי - אדום
            holder.tvMoS.setBackgroundColor(Color.parseColor("#E57373"));
        }
    }

    @Override
    public int getItemCount() {
        return stockList != null ? stockList.size() : 0;    }


    // ViewHolder שמחזיק את הרכיבים של ה-CardView
    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicker, tvPrice, tvIntrinsicValue, tvMoS, tvDividend;
        ImageButton btnDelete, btnEdit;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTicker = itemView.findViewById(R.id.tvTicker);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvIntrinsicValue = itemView.findViewById(R.id.tvIntrinsicValue);
            tvMoS = itemView.findViewById(R.id.tvMoS);
            tvDividend = itemView.findViewById(R.id.tvDividend);
            btnDelete = itemView.findViewById(R.id.btnDeleteStock);
            btnEdit = itemView.findViewById(R.id.btnEditStock);

        }
    }
}
