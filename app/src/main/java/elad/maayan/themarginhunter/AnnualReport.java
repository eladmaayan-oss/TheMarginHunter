package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;

public class AnnualReport {
    @SerializedName("fiscalDateEnding")
    private String fiscalDateEnding;

    @SerializedName("operatingCashflow")
    private String operatingCashflow;

    // לפעמים CapEx מגיע כמספר שלילי בדוחות, נצטרך לטפל בזה בחישוב
    @SerializedName("capitalExpenditures")
    private String capitalExpenditures;

    public String getFiscalDateEnding() { return fiscalDateEnding; }
    public String getOperatingCashflow() { return operatingCashflow; }
    public String getCapitalExpenditures() { return capitalExpenditures; }
}
