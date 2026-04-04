package elad.maayan.themarginhunter;

import java.util.Arrays;
import java.util.List;

public class StockUniverse {
    // רשימה מורחבת של חברות S&P 500 בולטות, מחולקות לסקטורים
    public static List<String> getTopStocks() {
        return Arrays.asList(
                // טכנולוגיה ותקשורת
                "AAPL", "MSFT", "GOOGL", "AMZN", "META", "NVDA", "TSLA",
                "NFLX", "ADBE", "CRM", "INTC", "AMD", "CSCO", "ORCL", "IBM", "QCOM", "TXN",

                // פיננסים
                "BRK-B", "V", "MA", "JPM", "BAC", "WFC", "GS", "MS", "AXP", "C", "BLK", "SPGI",

                // בריאות
                "JNJ", "UNH", "LLY", "MRK", "ABBV", "PFE", "TMO", "DHR", "ABT", "AMGN", "ISRG",

                // צריכה קמעונאית ובסיסית
                "WMT", "PG", "KO", "PEP", "COST", "MCD", "NKE", "SBUX", "TGT", "HD", "LOW", "PM",

                // תעשייה, אנרגיה וחומרים
                "XOM", "CVX", "CAT", "DE", "LMT", "BA", "HON", "GE", "MMM", "COP", "RTX", "UPS"
        );
    }
}