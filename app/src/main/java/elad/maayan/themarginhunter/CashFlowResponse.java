package elad.maayan.themarginhunter;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CashFlowResponse {
    @SerializedName("symbol")
    private String symbol;

    @SerializedName("annualReports")
    private List<AnnualReport> annualReports;

    public String getSymbol() { return symbol; }
    public List<AnnualReport> getAnnualReports() { return annualReports; }
}
