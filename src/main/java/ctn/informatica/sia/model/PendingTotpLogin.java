package ctn.informatica.sia.model;

public class PendingTotpLogin {

    private final int userId;
    private final String username;
    private final int level;
    private final boolean rememberMe;

    public PendingTotpLogin(int userId, String username, int level, boolean rememberMe) {
        this.userId = userId;
        this.username = username;
        this.level = level;
        this.rememberMe = rememberMe;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getLevel() {
        return level;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }
}
