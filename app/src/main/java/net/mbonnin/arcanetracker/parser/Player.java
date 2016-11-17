package net.mbonnin.arcanetracker.parser;

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardDb;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by martin on 11/7/16.
 */
public class Player {
    public Entity entity;
    public String battleTag;
    public boolean isOpponent;
    public boolean hasCoin;

    public Entity hero;
    public Entity heroPower;

    public int classIndex() {
        Card card = CardDb.getCard(hero.CardID);
        return Card.playerClassToClassIndex(card.playerClass);
    }

    public EntityList zone(String zoneId) {
        EntityList zone = zoneMap.get(zoneId);
        if (zone == null) {
            zone = new EntityList();
            zoneMap.put(zoneId, zone);
        }

        return zone;
    }

    public void notifyListeners() {
        Iterator<WeakReference<Listener>> it = listeners.iterator();

        while (it.hasNext()) {
            WeakReference<Listener> ref = it.next();
            Listener listener = ref.get();
            if (listener == null) {
                it.remove();
            } else {
                listener.onPlayerStateChanged();
            }
        }
    }

    public void registerListener(Listener listener) {
        listeners.add(new WeakReference<Listener>(listener));
    }

    public void unregisterListener(Listener listener) {
        Iterator<WeakReference<Listener>> it = listeners.iterator();

        while (it.hasNext()) {
            if (it.next() == listener) {
                it.remove();
            }
        }
    }
    public interface Listener {
        void onPlayerStateChanged();
    }

    private HashMap<String, EntityList> zoneMap = new HashMap<>();

    public EntityList entities = new EntityList();

    public void reset() {
        entities.clear();
    }

    public ArrayList<WeakReference<Listener>> listeners = new ArrayList<>();

}
