package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.Game;
import net.mbonnin.arcanetracker.parser.GameLogic;
import net.mbonnin.arcanetracker.parser.Play;
import net.mbonnin.arcanetracker.parser.PowerParser;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TestParser {

    private static class TestListener implements GameLogic.Listener {

        public Game game;

        @Override
        public void gameStarted(Game game) {
            this.game = game;
        }

        @Override
        public void gameOver() {

        }

        @Override
        public void somethingChanged() {

        }
    }

    @BeforeClass
    public static void beforeClass() {
        Timber.plant(new TestTree());
    }

    private void runParser(String resource, GameLogic.Listener listener) throws IOException {
        GameLogic.get().addListener(listener);
        PowerParser powerParser = new PowerParser(tag -> GameLogic.get().handleRootTag(tag), null);
        InputStream inputStream = getClass().getResourceAsStream(resource);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        powerParser.onPreviousDataRead();
        while ((line = br.readLine()) != null) {
            powerParser.onLine(line);
        }
    }

    private Game runParser(String resource) throws IOException {
        TestListener listener = new TestListener();

        runParser(resource, listener);
        return listener.game;
    }

    @Test
    public void test0() throws Exception {

        Game game = runParser("/fishstick.log");

        Assert.assertFalse(game.victory);

        /*for (Play play: listener.game.plays) {
            Timber.d("list.add(new Play(" + play.turn + ", " + play.isOpponent + ", \"" + play.cardId + "\"));");
        }*/

        List<Play> list = new ArrayList<>();
        list.add(new Play(4, true, "AT_116"));
        list.add(new Play(5, false, "CS2_181"));
        list.add(new Play(7, false, "CS2_004"));
        list.add(new Play(7, false, "CS1h_001_H1"));
        list.add(new Play(8, true, "AT_017"));
        list.add(new Play(9, false, "CS1h_001_H1"));
        list.add(new Play(9, false, "AT_055"));
        list.add(new Play(10, true, "EX1_284"));
        list.add(new Play(11, false, "LOE_111"));
        list.add(new Play(11, false, "AT_055"));
        list.add(new Play(12, true, "EX1_091"));
        list.add(new Play(13, false, "EX1_591"));
        list.add(new Play(13, false, "CS1h_001_H1"));
        list.add(new Play(14, true, "CS2_234"));
        list.add(new Play(14, true, "BRM_034"));
        list.add(new Play(15, false, "BRM_017"));
        list.add(new Play(15, false, "OG_234"));
        list.add(new Play(17, false, "EX1_298"));
        list.add(new Play(18, true, "LOE_104"));
        list.add(new Play(18, true, "AT_116"));
        list.add(new Play(18, true, "GAME_005"));
        list.add(new Play(18, true, "CS1h_001"));
        list.add(new Play(19, false, "KAR_204"));
        list.add(new Play(19, false, "CS1h_001_H1"));
        list.add(new Play(20, true, "LOE_111"));
        list.add(new Play(20, true, "CS1_112"));
        list.add(new Play(21, false, "KAR_204"));
        list.add(new Play(21, false, "CS1h_001_H1"));
        list.add(new Play(22, true, "EX1_298"));
        list.add(new Play(22, true, "CS1h_001"));
        list.add(new Play(23, false, "EX1_622"));
        list.add(new Play(23, false, "CS2_181"));
        list.add(new Play(23, false, "CS1h_001_H1"));
        list.add(new Play(24, true, "AT_017"));
        list.add(new Play(24, true, "EX1_284"));
        list.add(new Play(25, false, "OG_153"));
        list.add(new Play(25, false, "CS1h_001_H1"));
        list.add(new Play(26, true, "EX1_622"));
        list.add(new Play(26, true, "LOE_111"));
        list.add(new Play(26, true, "CS1h_001"));
        list.add(new Play(27, false, "EX1_591"));
        list.add(new Play(27, false, "CS1h_001_H1"));
        list.add(new Play(28, true, "KAR_033"));
        list.add(new Play(28, true, "CS1h_001"));
        list.add(new Play(29, false, "OG_234"));
        list.add(new Play(30, true, "CS2_004"));
        list.add(new Play(30, true, "CS2_004"));
        list.add(new Play(30, true, "BRM_033"));
        list.add(new Play(30, true, "CS1h_001"));
        list.add(new Play(31, false, "CS2_234"));
        list.add(new Play(31, false, "CS1h_001_H1"));
        list.add(new Play(32, true, "OG_317"));
        list.add(new Play(33, false, "KAR_035"));
        list.add(new Play(33, false, "EX1_622"));
        list.add(new Play(33, false, "CS1h_001_H1"));
        list.add(new Play(34, true, "CS1h_001"));
        list.add(new Play(35, false, "EX1_339"));
        list.add(new Play(35, false, "CS1h_001_H1"));
        list.add(new Play(36, true, "EX1_572"));
        list.add(new Play(37, false, "OG_153"));
        list.add(new Play(37, false, "CS1h_001_H1"));
        list.add(new Play(38, true, "EX1_622"));
        list.add(new Play(38, true, "CS1h_001"));
        list.add(new Play(38, true, "DREAM_01"));
        list.add(new Play(39, false, "BRM_004"));
        list.add(new Play(39, false, "CS2_004"));
        list.add(new Play(39, false, "LOE_104"));
        list.add(new Play(39, false, "CS1h_001_H1"));
        list.add(new Play(40, true, "BRM_033"));
        list.add(new Play(40, true, "CS1h_001"));
        list.add(new Play(41, false, "LOE_111"));
        list.add(new Play(41, false, "LOE_111"));
        list.add(new Play(42, true, "AT_123"));
        list.add(new Play(42, true, "CS1h_001"));
        list.add(new Play(43, false, "CS1h_001_H1"));
        list.add(new Play(44, true, "CS1h_001"));
        list.add(new Play(45, false, "BRM_017"));
        list.add(new Play(45, false, "CS1h_001_H1"));
        list.add(new Play(46, true, "LOE_111"));
        list.add(new Play(46, true, "CS1h_001"));
        list.add(new Play(47, false, "KAR_035"));
        list.add(new Play(47, false, "CS1h_001_H1"));
        list.add(new Play(48, true, "BRM_034"));
        list.add(new Play(48, true, "CS1h_001"));
        list.add(new Play(49, false, "EX1_621"));
        list.add(new Play(49, false, "CS1h_001_H1"));
        list.add(new Play(50, true, "CS2_234"));
        list.add(new Play(50, true, "CS1h_001"));
        list.add(new Play(51, false, "CS1h_001_H1"));
        list.add(new Play(52, true, "CS1h_001"));
        list.add(new Play(53, false, "CS1h_001_H1"));
        list.add(new Play(54, true, "CS1h_001"));
        list.add(new Play(55, false, "LOE_111"));
        list.add(new Play(55, false, "LOE_111"));
        list.add(new Play(56, true, "CS1h_001"));
        list.add(new Play(56, true, "BRM_004"));
        list.add(new Play(57, false, "EX1_572"));
        list.add(new Play(58, true, "LOE_104"));
        list.add(new Play(58, true, "CS1h_001"));
        list.add(new Play(59, false, "CS2_234"));
        list.add(new Play(59, false, "CS1h_001_H1"));
        list.add(new Play(60, true, "EX1_572"));
        list.add(new Play(61, false, "LOE_104"));
        list.add(new Play(61, false, "CS1h_001_H1"));
        list.add(new Play(62, true, "EX1_016"));
        list.add(new Play(62, true, "CS1h_001"));
        list.add(new Play(63, false, "EX1_572"));
        list.add(new Play(63, false, "DREAM_04"));
        list.add(new Play(64, true, "EX1_016"));
        list.add(new Play(64, true, "DREAM_02"));
        list.add(new Play(64, true, "CS1h_001"));
        list.add(new Play(65, false, "DREAM_01"));
        list.add(new Play(65, false, "CS1h_001_H1"));
        list.add(new Play(66, true, "DREAM_02"));
        list.add(new Play(66, true, "DREAM_02"));

        Assert.assertEquals(list.size(), game.plays.size());
        for (int i = 0; i < list.size(); i++) {
            Assert.assertEquals(list.get(i).cardId, game.plays.get(i).cardId);
            Assert.assertEquals(list.get(i).isOpponent, game.plays.get(i).isOpponent);
            Assert.assertEquals(list.get(i).turn, game.plays.get(i).turn);
        }
    }

    @Test
    public void testCreatedBy() throws Exception {
        Game game = runParser("/gRievoUS.log");

        Assert.assertEquals(game.findEntitySafe("73").extra.createdBy, Card.SERVANT_OF_KALYMOS);
        Assert.assertEquals(game.findEntitySafe("78").extra.createdBy, Card.SERVANT_OF_KALYMOS);
        Assert.assertEquals(game.findEntitySafe("75").extra.createdBy, Card.FROZEN_CLONE);
        Assert.assertEquals(game.findEntitySafe("76").extra.createdBy, Card.FROZEN_CLONE);
        Assert.assertEquals(game.findEntitySafe("80").extra.createdBy, Card.MIRROR_ENTITY);
    }

    @Test
    public void testDoubleSecret() throws Exception {

        Game game = runParser("/k0l0banov.log");

        Assert.assertFalse(game.victory);

    }

    static class SecretsListener implements GameLogic.Listener {

        public Game mGame;
        private int lastTurn;

        @Override
        public void gameStarted(Game game) {
            mGame = game;
        }

        @Override
        public void gameOver() {

        }

        @Override
        public void somethingChanged() {
            int turn = 0;

            try {
                turn = Integer.parseInt(mGame.gameEntity.tags.get(Entity.KEY_TURN));
            } catch(Exception e) {
                return;
            }

            if (turn != lastTurn && turn == 19) {
                Entity secretEntity = mGame.findEntityUnsafe("84");
                Assert.assertFalse(secretEntity.extra.competitiveSpiritTriggerConditionHappened);
                Assert.assertTrue(secretEntity.extra.otherPlayerHeroPowered);
                Assert.assertTrue(secretEntity.extra.otherPlayerPlayedMinion);
                Assert.assertTrue(secretEntity.extra.selfHeroDamaged);
                Assert.assertTrue(secretEntity.extra.selfHeroAttacked);
            }

        }
    }
    @Test
    public void testSecrets() throws Exception {

        runParser("/Laugeolesen.log", new SecretsListener());
    }
}