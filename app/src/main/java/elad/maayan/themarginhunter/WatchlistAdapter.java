package elad.maayan.themarginhunter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.ViewHolder> {

    private List<Stock> stocks;
    private OnStockClickListener listener;

    // 1. הגדרת ממשק להאזנה ללחיצות
    public interface OnStockClickListener {
        void onStockClick(String ticker);
    }

    // 2. עדכון הבנאי כך שיקבל גם את המאזין
    public WatchlistAdapter(List<Stock> stocks, OnStockClickListener listener) {
        this.stocks = stocks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_watchlist, parent, false);
        return new ViewHolder(view);
    }

    public void updateList(List<Stock> filteredList) {
        this.stocks = filteredList;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stocks.get(position);
        holder.tvTicker.setText(stock.getTicker());

        if (stock.getPrice() == 0.0) {
            holder.tvPrice.setText("טוען...");
        } else {
            holder.tvPrice.setText(String.format(Locale.US, "$%.2f", stock.getPrice()));
        }

        // 3. הוספת מאזין ללחיצה על כל השורה
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStockClick(stock.getTicker());
            }
        });
    }

    @Override
    public int getItemCount() {
        return stocks != null ? stocks.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicker;
        TextView tvPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicker = itemView.findViewById(R.id.tvWatchlistTicker);
            tvPrice = itemView.findViewById(R.id.tvWatchlistPrice);
        }
    }
}