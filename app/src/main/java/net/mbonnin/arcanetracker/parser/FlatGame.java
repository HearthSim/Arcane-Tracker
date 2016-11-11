package net.mbonnin.arcanetracker.parser;

import java.util.ArrayList;
import java.util.HashMap;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

/**
 * a FlatGame is a mere representation of entities, it has almost no intelligence inside
 *
 * it just stores all the entities
 */
public class FlatGame {

    public HashMap<String, Entity> entityMap = new HashMap<>();
    public ArrayList<String> battleTags = new ArrayList<>();

}
