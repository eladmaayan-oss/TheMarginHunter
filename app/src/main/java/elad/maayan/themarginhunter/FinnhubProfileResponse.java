package elad.maayan.themarginhunter;

import com.google.gson.annotations.SerializedName;

public class FinnhubProfileResponse {
    @SerializedName("name")
    private String companyName;

    public String getCompanyName() {
        return companyName;
    }
}