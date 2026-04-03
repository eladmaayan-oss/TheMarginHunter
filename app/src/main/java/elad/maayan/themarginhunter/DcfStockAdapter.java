package elad.maayan.themarginhunter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DcfStockAdapter extends RecyclerView.Adapter<DcfStockAdapter.DcfViewHolder> {

    private List<Stock> stockList;

    public DcfStockAdapter(List<Stock> stockList) {
        this.stockList = stockList;
    }

    @NonNull
    @Override
    public DcfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dcf_stock, parent, false);
        return new DcfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DcfViewHolder holder, int position) {
        Stock stock = stockList.get(position);
        holder.tvTicker.setText(stock.getTicker());
        holder.tvCurrentPrice.setText(String.format("Price: $%.2f", stock.getCurrentPrice()));
        holder.tvIntrinsicValue.setText(String.format("Fair Value: $%.2f", stock.getIntrinsicValue()));
        holder.tvGrowth.setText(String.format("Expected Growth: %.1f%%", stock.getExpectedGrowth()));
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    static class DcfViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicker, tvCurrentPrice, tvIntrinsicValue, tvGrowth;

        public DcfViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicker = itemView.findViewById(R.id.tvDcfItemTicker);
            tvCurrentPrice = itemView.findViewById(R.id.tvDcfItemCurrentPrice);
            tvIntrinsicValue = itemView.findViewById(R.id.tvDcfItemIntrinsicValue);
            tvGrowth = itemView.findViewById(R.id.tvDcfItemGrowth);
        }
    }
}