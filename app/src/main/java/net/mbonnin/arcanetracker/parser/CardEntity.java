package net.mbonnin.arcanetracker.parser;

/**
 * Created by martin on 11/8/16.
 */

public class CardEntity extends Entity {
    public String CardID;

    @Override
    public String toString() {
        return "CardEntity [id=" + EntityID + "][CardID=" + CardID + "]";
    }
}
