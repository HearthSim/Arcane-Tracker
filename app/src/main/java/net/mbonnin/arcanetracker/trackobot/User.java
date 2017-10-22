package net.mbonnin.arcanetracker.trackobot;

public class User {
    public final String password;
    public final String username;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
