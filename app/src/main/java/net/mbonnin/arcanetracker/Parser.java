package net.mbonnin.arcanetracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 10/27/16.
 */

public class Parser {
    private static final int STATE_IDLE = 0;
    private static final int STATE_BLOCK1 = 1;
    private static final int STATE_BLOCK2 = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_BLOCK0 = 4;

    static Parser sParser;

    private int state = STATE_IDLE;

    static class Player {
        String battleTag;
        String heroId;
        public String index;
    }

    static class InitialCard {
        public String cardId;
        public String playerIndex;
        public String zone;
    }

    HashMap<String, Player> indexToPlayer;
    HashMap<String, Player> battleTagToPlayer;

    /**
     * either "1" or "2"
     */
    String playerIndex;
    String opponentIndex;

    ArrayList<String> battleTags = new ArrayList<>();
    private String firstPlayerBattleTag;
    private String secondPlayerBattleTag;
    private ArrayList<InitialCard> initialCardList = new ArrayList<>();

    public static Parser get() {
        if (sParser == null) {
            sParser = new Parser();
        }

        return sParser;
    }

    public Parser() {
        new LogReader("Power", line -> parsePowerLine(line), false);
        /**
         * we need to read the whole loading screen if we start Arcane Tracker while in the 'tournament' play screen
         * or arena screen already (and not in main menu)
         */
        new LogReader("LoadingScreen", line -> parseLoadingScreen(line), true);
        /**
         * for Arena, we read the whole file again each time because the file is not that big and it allows us to
         * get the arena deck contents
         */
        new LogReader("Arena", line -> parseArena(line), true);
    }

    private void parseArena(String line) {
        Timber.d("Arena" + line);

        Pattern pattern = Pattern.compile(".* DraftManager.OnBegin - Got new draft deck with ID: (.*)");
        Matcher matcher = pattern.matcher(line);

        Pattern pattern2 = Pattern.compile(".* DraftManager.OnChosen\\(\\): hero=(.*) premium=NORMAL");
        Matcher matcher2 = pattern2.matcher(line);

        Pattern pattern3 = Pattern.compile(".* Client chooses: .* \\((.*)\\)");
        Matcher matcher3 = pattern3.matcher(line);

        Pattern pattern4 = Pattern.compile(".* DraftManager.OnChoicesAndContents - Draft deck contains card (.*)");
        Matcher matcher4 = pattern4.matcher(line);

        Deck deck = DeckList.getArenaDeck();

        if (matcher.matches()) {
            deck.clear();
            DeckList.saveArena();
        } else if (matcher2.matches()) {
            deck.classIndex = Card.heroIdToClassIndex(matcher2.group(1));
            MainViewCompanion.getPlayerCompanion().setDeck(deck);
        } else if (matcher3.matches()) {
            String cardId = matcher3.group(1);
            if (cardId.toLowerCase().startsWith("hero_")) {
                // This must be a hero ("Client chooses: Tyrande Whisperwind (HERO_09a)")
                Timber.e("skip hero " + cardId);
            } else {
                deck.addCard(cardId, 1);
                DeckList.saveArena();
            }
        } else if (matcher4.matches()) {
            String cardId = matcher4.group(1);
            Card card = CardDb.getCard(cardId);
            if (!deck.cards.containsKey(cardId)) {
                if (card.collectible) {
                    deck.addCard(cardId, 1);
                    DeckList.saveArena();
                } else {
                    Timber.e("not collectible2 " + cardId);
                }
            }
        }
    }

    private void parseLoadingScreen(String line) {
        Timber.d("LoadingScreen" + line);

        Pattern pattern = Pattern.compile(".* LoadingScreen.OnSceneLoaded\\(\\) - prevMode=(.*) currMode=(.*)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            String prevMode = matcher.group(1);
            String currMode = matcher.group(2);

            if (currMode.equals("GAMEPLAY")) {
                return;
            }

            int newMode = GameState.MODE_OTHER;
            if (currMode.equals("DRAFT")) {
                newMode = GameState.MODE_ARENA;
            } else if (currMode.equals("TOURNAMENT")) {
                newMode = GameState.MODE_PLAY;
            }

            GameState.onModeChanged(newMode);
        }
    }

    private String checkAndStripLine(String line) {
        if (!line.contains("PowerTaskList")) {
            return null;
        }

        int i = line.indexOf("-");
        if (i < 0) {
            Timber.e("bad line: " + line);
            return null;
        }

        /**
         * skip spaces
         */
        int start = i + 1;
        while (start < line.length() && line.charAt(start) == ' ') {
            start++;
        }

        if (start == line.length()) {
            return null;
        }

        line = line.substring(start);

        return line;
    }

    private void parsePowerLine(String line) {
        line = checkAndStripLine(line);
        if (line == null) {
            return;

        }
        Timber.d(line);

        switch (state) {
            case STATE_IDLE:
                if (line.startsWith("CREATE_GAME")) {
                    state = STATE_BLOCK0;
                    break;
                }
            case STATE_BLOCK0:
                if (line.startsWith("FULL_ENTITY - Updating ")) {
                    playerIndex = null;
                    opponentIndex = null;
                    battleTagToPlayer = null;

                    indexToPlayer = new HashMap<>();
                    indexToPlayer.put("1", new Player());
                    indexToPlayer.put("2", new Player());

                    firstPlayerBattleTag = null;
                    secondPlayerBattleTag = null;
                    battleTags.clear();
                    initialCardList.clear();
                    state = STATE_BLOCK1;
                    parsePowerLine(line);
                } else if (line.startsWith("tag=STATE value=RUNNING")) {
                    /**
                     * the game was just resumed... don't lose the state
                     */
                    if (initialCardList.size() > 0) {
                        Timber.e("resuming previous game");
                        state = STATE_PLAYING;
                    }
                }
                break;
            case STATE_BLOCK1: {
                Pattern p = Pattern.compile("TAG_CHANGE Entity=(.*) tag=PLAYSTATE value=PLAYING");
                Matcher m = p.matcher(line);

                if (line.startsWith("Block End")) {
                    if (battleTags.size() == 2
                            && indexToPlayer.get("1").heroId != null
                            && indexToPlayer.get("2").heroId != null) {
                        state = STATE_BLOCK2;
                    } else {
                        Timber.e("end of BLOCK1 too early");
                        state = STATE_IDLE;
                    }
                } else if (line.startsWith("FULL_ENTITY - Updating ")) {
                    HashMap<String, String> map = arrayParams(line);
                    if (map.containsKey("cardId") && map.containsKey("player")) {
                        Card card = CardDb.getCard(map.get("cardId"));
                        Player player = indexToPlayer.get(map.get("player"));
                        if (player != null && card.type.equals(Card.TYPE_HERO)) {
                            player.heroId = map.get("cardId");
                            Timber.w("Player " + map.get("player") + " has heroId " + player.heroId);
                        }
                    }
                } else if (m.matches()) {
                    battleTags.add(m.group(1));
                }
                break;
            }
            case STATE_BLOCK2: {
                Pattern p = Pattern.compile("TAG_CHANGE Entity=(.*) tag=FIRST_PLAYER value=1");
                Matcher m = p.matcher(line);

                if (line.startsWith("BLOCK_END")) {
                    if (playerIndex == null
                            || firstPlayerBattleTag == null) {
                        Timber.e("end of BLOCK2 too early");
                        state = STATE_IDLE;
                        return;

                    }
                    if (initialCardList.size() == 3) {
                        Timber.w("I am the first player");
                        indexToPlayer.get(playerIndex).battleTag = firstPlayerBattleTag;
                        indexToPlayer.get(opponentIndex).battleTag = secondPlayerBattleTag;
                    } else {
                        Timber.w("Opponent is the first player");
                        indexToPlayer.get(playerIndex).battleTag = secondPlayerBattleTag;
                        indexToPlayer.get(opponentIndex).battleTag = firstPlayerBattleTag;
                    }
                    indexToPlayer.get("1").index = "1";
                    indexToPlayer.get("2").index = "2";

                    battleTagToPlayer = new HashMap<>();
                    battleTagToPlayer.put(indexToPlayer.get("1").battleTag, indexToPlayer.get("1"));
                    battleTagToPlayer.put(indexToPlayer.get("2").battleTag, indexToPlayer.get("2"));


                    /**
                     * send the previous events now, we cannot do it before because we don't know what player is what index
                     */
                    Timber.w("sending initial Cards");
                    ArrayList<GameState.CardEvent> cardEventList = new ArrayList<>();
                    for (InitialCard ic : initialCardList) {
                        GameState.CardEvent event = new GameState.CardEvent();
                        event.cardId = ic.cardId;
                        event.zone = ic.zone;
                        event.isOpponent = ic.playerIndex.equals(opponentIndex);
                        event.isShown = true;
                        cardEventList.add(event);
                    }
                    initialCardList.clear();

                    GameState.onGameStart(indexToPlayer.get(playerIndex).heroId, indexToPlayer.get(opponentIndex).heroId, cardEventList);
                    state = STATE_PLAYING;
                } else if (line.startsWith("SHOW_ENTITY")) {
                    HashMap<String, String> map = allParams(line);
                    if (map.containsKey("player") && map.get("zone").equals("DECK") && map.containsKey("CardID")) {
                        if (playerIndex == null) {
                            playerIndex = map.get("player");
                            Timber.w("I am player " + playerIndex);
                            if (playerIndex.equals("1")) {
                                opponentIndex = "2";
                            } else {
                                opponentIndex = "1";
                            }
                        }
                        Timber.w("initial card " + map.get("CardID"));
                        InitialCard ic = new InitialCard();
                        ic.cardId = map.get("CardID");
                        ic.playerIndex = map.get("player");
                        ic.zone = map.get("zone");
                        initialCardList.add(ic);
                    }

                } else if (m.matches()) {
                    firstPlayerBattleTag = m.group(1);
                    Timber.w("firstPlayer is " + firstPlayerBattleTag);
                    if (firstPlayerBattleTag.equals(battleTags.get(0))) {
                        secondPlayerBattleTag = battleTags.get(1);
                    } else {
                        secondPlayerBattleTag = battleTags.get(0);
                    }
                }
                break;
            }
            case STATE_PLAYING:
                Pattern p = Pattern.compile("TAG_CHANGE Entity=(.*) tag=PLAYSTATE value=(.*)");
                Matcher m = p.matcher(line);

                HashMap<String, String> map = null;
                boolean isShown = false;
                String cardId = null;
                if (line.startsWith("SHOW_ENTITY")) {
                    map = allParams(line);
                    cardId = map.get("CardID");
                    isShown = true;
                } else if (line.startsWith("HIDE_ENTITY - Entity=")) {
                    map = allParams(line);
                    cardId = map.get("cardId");
                    isShown = false;
                } else if (m.matches()) {
                    if (m.group(1).equals(indexToPlayer.get(playerIndex).battleTag)) {
                        boolean victory = m.group(2).equals("WON");
                        Timber.w("I " + (victory ? "won" : "lost") + "!");
                        GameState.onGameEnd(victory);
                        state = STATE_IDLE;
                    }
                } else if (line.startsWith("CREATE_GAME")) {
                    state = STATE_IDLE;
                    parsePowerLine(line);
                }

                if (map != null && cardId != null) {
                    GameState.CardEvent event = new GameState.CardEvent();
                    event.cardId = cardId;
                    event.zone = map.get("zone");
                    event.isOpponent = map.get("player").equals(opponentIndex);
                    event.isShown = isShown;
                    GameState.onCard(event);
                }

        }
    }


    private static HashMap<String, String> allParams(String line) {
        HashMap<String, String> map = new HashMap<>();

        String result[] = splitLine(line);
        for (int i = 1; i <= 2; i++) {
            if (result[i] != null) {
                HashMap<String, String> map2 = decodeParams(result[i]);
                for (String key : map2.keySet()) {
                    if (map2.get(key) != null && !map2.get(key).equals("")) {
                        map.put(key, map2.get(key));
                    }
                }
            }
        }

        return map;
    }

    private static HashMap<String, String> decodeParams(String params) {
        int end = params.length();
        HashMap<String, String> map = new HashMap<>();

        while (true) {
            int start = end - 1;

            String value;
            while (start >= 0 && params.charAt(start) != '=') {
                start--;
            }
            if (start < 0) {
                return map;
            }
            value = params.substring(start + 1, end);
            end = start;
            if (end < 0) {
                return map;
            }
            start = end - 1;
            while (start >= 0 && params.charAt(start) != ' ') {
                start--;
            }
            String key;
            if (start == 0) {
                key = params.substring(start, end);
            } else {
                key = params.substring(start + 1, end);
            }
            map.put(key.trim(), value);
            if (start == 0) {
                break;
            } else {
                end = start;
            }
        }

        return map;
    }

    private static String[] splitLine(String line) {
        int i = 0;
        String result[] = new String[3];
        while (i < line.length() && line.charAt(i) != '[') {
            i++;
        }
        result[0] = line.substring(0, i);
        if (i == line.length()) {
            return result;
        }

        i++;
        int end = i;
        while (end < line.length() && line.charAt(end) != ']') {
            end++;
        }
        result[1] = line.substring(i, end);
        if (end >= line.length() - 1) {
            return result;
        }
        result[2] = line.substring(end + 1, line.length());

        return result;
    }

    private static HashMap<String, String> arrayParams(String line) {
        String split[] = splitLine(line);
        if (split[1] != null) {
            return decodeParams(split[1]);
        }

        return new HashMap<>();
    }

}
