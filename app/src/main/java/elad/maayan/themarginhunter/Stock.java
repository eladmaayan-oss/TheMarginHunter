package elad.maayan.themarginhunter;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.List;

public class Stock {

    private String sector = "כללי"; // מגזר ברירת המחדל

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    @PropertyName("fcf")
    private double fcf;

    @PropertyName("sharesOutstanding")
    private double sharesOutstanding;

    @PropertyName("fcf")
    public double getFcf() { return fcf; }

    @PropertyName("fcf")
    public void setFcf(double fcf) { this.fcf = fcf; }

    @PropertyName("sharesOutstanding")
    public double getSharesOutstanding() { return sharesOutstanding; }

    @PropertyName("sharesOutstanding")
    public void setSharesOutstanding(double sharesOutstanding) { this.sharesOutstanding = sharesOutstanding; }

    @PropertyName("ticker")
    private String ticker;
    @PropertyName("growthHint")
    private String growthHint;

    @PropertyName("companyName")
    private String companyName;
    
    @PropertyName("currentPrice")
    private double currentPrice;
    
    @PropertyName("intrinsicValue")
    private double intrinsicValue; 
    
    @PropertyName("peRatio")
    private double peRatio;
    
    @PropertyName("pbRatio")
    private double pbRatio;
    
    @PropertyName("lastUpdated")
    private long lastUpdated;
    
    @PropertyName("dividendYield")
    private double dividendYield;
    
    @PropertyName("dividendPerShare")
    private double dividendPerShare;
    
    @PropertyName("eps")
    private double eps;
    private double expectedGrowth;
    
    @PropertyName("growthRate")
    private double growthRate;
    @PropertyName("marginOfSafety")
    private double marginOfSafety;

    @PropertyName("marginOfSafety")
    public double getMarginOfSafety() { // שיניתי שם כדי לא להתנגש עם הפונקציה הקיימת שלך
        return marginOfSafety;
    }

    @PropertyName("marginOfSafety")
    public void setMarginOfSafety(double marginOfSafety) {
        this.marginOfSafety = marginOfSafety;
    }
    public Stock() {
        // Required for Firestore
    }

    public void setExpectedGrowth(double expectedGrowth) {
        this.expectedGrowth = expectedGrowth;
    }
    public double getExpectedGrowth() {
        return expectedGrowth;
    }
    public void setPrice(double price) {
        this.currentPrice = price;
    }


    @PropertyName("ticker")
    public String getTicker() {
        return ticker;
    }

    @PropertyName("ticker")
    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    @PropertyName("growthHint")
    public String getGrowthHint() { return growthHint; }

    @PropertyName("growthHint")
    public void setGrowthHint(String growthHint) { this.growthHint = growthHint; }

    @PropertyName("companyName")
    public String getCompanyName() {
        return companyName;
    }

    @PropertyName("companyName")
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    @PropertyName("currentPrice")
    public double getCurrentPrice() {
        return currentPrice;
    }

    @PropertyName("currentPrice")
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    @PropertyName("intrinsicValue")
    public double getIntrinsicValue() {
        return intrinsicValue;
    }

    @PropertyName("intrinsicValue")
    public void setIntrinsicValue(double intrinsicValue) {
        this.intrinsicValue = intrinsicValue;
    }

    @PropertyName("peRatio")
    public double getPeRatio() {
        return peRatio;
    }

    @PropertyName("peRatio")
    public void setPeRatio(double peRatio) {
        this.peRatio = peRatio;
    }

    @PropertyName("pbRatio")
    public double getPbRatio() {
        return pbRatio;
    }

    @PropertyName("pbRatio")
    public void setPbRatio(double pbRatio) {
        this.pbRatio = pbRatio;
    }

    @PropertyName("lastUpdated")
    public long getLastUpdated() {
        return lastUpdated;
    }

    @PropertyName("lastUpdated")
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @PropertyName("dividendYield")
    public double getDividendYield() {
        return dividendYield;
    }

    @PropertyName("dividendYield")
    public void setDividendYield(double dividendYield) {
        this.dividendYield = dividendYield;
    }

    @PropertyName("dividendPerShare")
    public double getDividendPerShare() {
        return dividendPerShare;
    }

    @PropertyName("dividendPerShare")
    public void setDividendPerShare(double dividendPerShare) {
        this.dividendPerShare = dividendPerShare;
    }

    @PropertyName("eps")
    public double getEps() {
        return eps;
    }

    @PropertyName("eps")
    public void setEps(double eps) {
        this.eps = eps;
    }

    @PropertyName("growthRate")
    public double getGrowthRate() {
        return growthRate;
    }

    @PropertyName("growthRate")
    public void setGrowthRate(double growthRate) {
        this.growthRate = growthRate;
    }

    // שדות חסרים שגרמו לשגיאה:
    @PropertyName("chartPrices")
    private List<Double> chartPrices;

    @PropertyName("lastCalculated")
    private long lastCalculated;

    // Getters & Setters לשדות החדשים (חשוב מאוד!)
    @PropertyName("chartPrices")
    public List<Double> getChartPrices() { return chartPrices; }

    @PropertyName("chartPrices")
    public void setChartPrices(List<Double> chartPrices) { this.chartPrices = chartPrices; }

    @PropertyName("lastCalculated")
    public long getLastCalculated() { return lastCalculated; }

    @PropertyName("lastCalculated")
    public void setLastCalculated(long lastCalculated) { this.lastCalculated = lastCalculated; }
    @Exclude
    public double calculateMarginOfSafetyLogic() {
        if (intrinsicValue <= 0 || currentPrice <= 0) return 0;
        return ((intrinsicValue - currentPrice) / intrinsicValue) * 100;
    }
}
