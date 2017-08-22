package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.GameLogic;
import net.mbonnin.arcanetracker.parser.PowerParser;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import timber.log.Timber;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TestParser {
    @Test
    public void test0() throws Exception {
        Timber.plant(new TestTree());
        PowerParser powerParser = new PowerParser(tag -> GameLogic.get().handleRootTag(tag), null);
        InputStream inputStream = getClass().getResourceAsStream("/power0.log");
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        powerParser.onPreviousDataRead();
        while ((line = br.readLine()) != null) {
            powerParser.onLine(line);
        }
    }
}