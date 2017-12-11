package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.Game;
import net.mbonnin.arcanetracker.parser.GameLogic;
import net.mbonnin.arcanetracker.parser.PowerParser;
import net.mbonnin.hsmodel.CardId;
import net.mbonnin.hsmodel.CardJson;
import net.mbonnin.hsmodel.PlayerClass;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import timber.log.Timber;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TestParser {

    static class SimpleListener implements GameLogic.Listener {
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
        CardJson.INSTANCE.init("enUS", new ArrayList<>());
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
        GameLogic.get().removeListener(listener);
    }

    private Game runParser(String resource) throws IOException {
        SimpleListener listener = new SimpleListener();

        runParser(resource, listener);
        return listener.game;
    }

    @Test
    public void testCreatedBy() throws Exception {
        Game game = runParser("/created_by.log");

        Assert.assertEquals(game.findEntitySafe("73").extra.createdBy, CardId.SERVANT_OF_KALIMOS);
        Assert.assertEquals(game.findEntitySafe("78").extra.createdBy, CardId.SERVANT_OF_KALIMOS);
        Assert.assertEquals(game.findEntitySafe("75").extra.createdBy, CardId.FROZEN_CLONE);
        Assert.assertEquals(game.findEntitySafe("76").extra.createdBy, CardId.FROZEN_CLONE);
        Assert.assertEquals(game.findEntitySafe("80").extra.createdBy, CardId.MIRROR_ENTITY);
    }

    @Test
    public void testDoubleSecret() throws Exception {

        Game game = runParser("/double_secret.log");

        Assert.assertFalse(game.victory);

    }

    @Test
    public void testSecrets() throws Exception {

        runParser("/secrets.log", new SimpleListener() {
            @Override
            public void somethingChanged() {
                if ("19".equals(game.gameEntity.tags.get(Entity.KEY_TURN))) {
                    Entity secretEntity = game.findEntityUnsafe("84");
                    Assert.assertFalse(secretEntity.extra.competitiveSpiritTriggerConditionHappened);
                    Assert.assertTrue(secretEntity.extra.otherPlayerHeroPowered);
                    Assert.assertTrue(secretEntity.extra.otherPlayerPlayedMinion);
                    Assert.assertTrue(secretEntity.extra.selfHeroDamaged);
                    Assert.assertTrue(secretEntity.extra.selfHeroAttacked);
                }
            }
        });
    }

    @Test
    public void testMirrorEntity() throws Exception {
        runParser("/mirror_entity.log", new SimpleListener() {
            @Override
            public void somethingChanged() {
                Entity kabalCrystalRunner = game.findEntitySafe("84");
                if (Entity.ZONE_PLAY.equals(kabalCrystalRunner.tags.get(Entity.KEY_ZONE))) {
                    Entity iceBarrier = game.findEntitySafe("41");
                    Assert.assertTrue(iceBarrier.extra.otherPlayerPlayedMinion);
                }
            }
        });
    }

    @Test
    public void testFrozenClone() throws Exception {
        runParser("/mirror_entity.log", new SimpleListener() {
            @Override
            public void somethingChanged() {
                Entity kabalCrystalRunner = game.findEntitySafe("84");
                if (Entity.ZONE_PLAY.equals(kabalCrystalRunner.tags.get(Entity.KEY_ZONE))) {
                    Entity iceBarrier = game.findEntitySafe("41");
                    Assert.assertTrue(iceBarrier.extra.otherPlayerPlayedMinion);
                }
            }
        });
    }

    @Test
    public void testExploreUngoro() throws Exception {
        runParser("/exploreUngoro.log", new SimpleListener() {
            @Override
            public void somethingChanged() {
                Entity e = game.findEntityUnsafe("99");
                if (e != null) {
                    Assert.assertTrue(e.CardID.equals(CardId.CHOOSE_YOUR_PATH));
                }
            }
        });
    }

    @Test
    public void testInterrupted() throws Exception {
        class InterruptedListener extends SimpleListener{
            public int gameOverCount;

            @Override
            public void gameOver() {
                gameOverCount++;
            }
        }

        InterruptedListener listener = new InterruptedListener();
        runParser("/interrupted.log", listener);

        Assert.assertTrue(listener.gameOverCount == 1);
    }

    @Test
    public void testEndedInAttack() throws Exception {
        class InterruptedListener extends SimpleListener{
            public int gameOverCount;

            @Override
            public void gameOver() {
                gameOverCount++;
            }
        }

        InterruptedListener listener = new InterruptedListener();
        runParser("/endedInAttack.log", listener);

        Assert.assertTrue(listener.gameOverCount == 1);
    }

    @Test
    public void testSpectator() throws Exception {
        class InterruptedListener extends SimpleListener{
            public int gameOverCount;

            @Override
            public void gameOver() {
                gameOverCount++;
            }
        }

        InterruptedListener listener = new InterruptedListener();
        runParser("/continuation.log", listener);

        /*
         * the first game is a continuation game and should not be detected
         */
        Assert.assertTrue(listener.gameOverCount == 1);
    }

    @Test
    public void testEffigy() throws Exception {
        runParser("/effigy.log", new SimpleListener() {
            @Override
            public void somethingChanged() {
                if ("11".equals(game.gameEntity.tags.get(Entity.KEY_TURN))) {
                    Entity secretEntity = game.findEntityUnsafe("18");
                    Assert.assertFalse(secretEntity.extra.selfPlayerMinionDied);
                }
            }
        });
    }

    @Test
    public void testNemsy() throws Exception {
        SimpleListener listener = new SimpleListener() {
            @Override
            public void gameStarted(Game game) {
                super.gameStarted(game);
                Assert.assertTrue(game.player.playerClass().equals(PlayerClass.WARLOCK));

            }
        };

        runParser("/nemsy.log", listener);
    }

}