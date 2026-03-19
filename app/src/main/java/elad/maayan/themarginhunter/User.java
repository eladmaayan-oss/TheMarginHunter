package elad.maayan.themarginhunter;

public class User {
    private String uid;
    private String displayName;
    private Number preferredMarginOfSafety;
    private String[] watchList;

    public User() {
    }

    public User(String uid, String displayName, Number preferredMarginOfSafety, String[] watchList) {
        this.uid = uid;
        this.displayName = displayName;
        this.preferredMarginOfSafety = preferredMarginOfSafety;
        this.watchList = watchList;
    }
}
