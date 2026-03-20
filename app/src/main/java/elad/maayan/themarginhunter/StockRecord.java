package elad.maayan.themarginhunter;

public class StockRecord {
    public String ticker;
    public String companyName;
    public double price;
    public double eps;
    public double marginOfSafety;
    public long timestamp;

    // חובה להוסיף קונסטרקטור ריק עבור Firestore
    public StockRecord() {}

    public StockRecord(String ticker, String companyName, double price, double eps, double mos) {
        this.ticker = ticker;
        this.companyName = companyName;
        this.price = price;
        this.eps = eps;
        this.marginOfSafety = mos;
        this.timestamp = System.currentTimeMillis();
    }
}
