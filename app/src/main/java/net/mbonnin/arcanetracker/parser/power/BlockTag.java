package net.mbonnin.arcanetracker.parser.power;

import java.util.ArrayList;
import java.util.List;

public class BlockTag extends Tag {
    public static final String TYPE_PLAY = "PLAY";
    public static final String TYPE_POWER = "POWER";
    public static final String TYPE_TRIGGER = "TRIGGER";
    public static final String TYPE_ATTACK = "ATTACK";
    public String BlockType;
    public String Entity;
    public String EffectCardId;
    public String EffectIndex;
    public String Target;
    public List<Tag> children = new ArrayList<>();
}
