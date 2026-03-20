package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;

public class CompanyOverviewResponse {
    @SerializedName("Symbol") private String symbol;
    @SerializedName("EPS") private String eps;
    @SerializedName("Name") private String name;

    public String getEps() { return eps; }
    public String getName() { return name; }
}
