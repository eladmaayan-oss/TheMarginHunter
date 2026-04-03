package elad.maayan.themarginhunter;

import java.util.Arrays;
import java.util.List;

public class StockUniverse {
    // רשימה התחלתית של מניות מובילות ובטוחות לסינון
    public static List<String> getTopStocks() {
        return Arrays.asList(
                "AAPL", "MSFT", "GOOGL", "AMZN", "META",
                "BRK-B", "V", "JNJ", "WMT", "PG",
                "MA", "UNH", "HD", "BAC", "XOM",
                "KO", "PEP", "COST", "MCD", "DIS"
        );
    }
}