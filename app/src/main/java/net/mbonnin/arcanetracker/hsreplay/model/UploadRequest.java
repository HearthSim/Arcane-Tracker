package net.mbonnin.arcanetracker.hsreplay.model;

/**
 * Created by martin on 11/29/16.
 */

public class UploadRequest {
    public String match_start;
    public String friendly_player_id;
    public int build;
    public int game_type;
    public HSPlayer player1 = new HSPlayer();
    public HSPlayer player2 = new HSPlayer();
}
