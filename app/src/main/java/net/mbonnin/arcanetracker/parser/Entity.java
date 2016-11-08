package net.mbonnin.arcanetracker.parser;

import java.util.HashMap;

import timber.log.Timber;

/**
 * Created by martin on 11/8/16.
 */

public class Entity {
    public HashMap<String, String> tags = new HashMap();
    public String EntityID;

    public void dump() {
        for (String key:tags.keySet()) {
            Timber.v("   " + key + "=" + tags.get(key));
        }
    }
}
