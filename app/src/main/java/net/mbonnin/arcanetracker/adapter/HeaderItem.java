package net.mbonnin.arcanetracker.adapter;

/**
 * Created by martin on 11/8/16.
 */

public class HeaderItem {
    boolean expanded;
    String title;

    Runnable onClicked;

    public HeaderItem(String title) {
        this.title = title;
    }
}
