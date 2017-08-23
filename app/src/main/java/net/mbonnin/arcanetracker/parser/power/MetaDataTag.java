package net.mbonnin.arcanetracker.parser.power;

import java.util.ArrayList;

/**
 * Created by martin on 8/22/17.
 */

public class MetaDataTag extends Tag {
    public static final String META_DAMAGE="DAMAGE";
    public static final String META_TARGET="TARGET";

    public String Meta;
    public String Data;
    public ArrayList<String> Info = new ArrayList<>();
}
