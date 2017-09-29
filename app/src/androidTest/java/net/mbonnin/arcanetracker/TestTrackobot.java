package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.trackobot.Service;
import net.mbonnin.arcanetracker.trackobot.Trackobot;
import net.mbonnin.arcanetracker.trackobot.model.Result;
import net.mbonnin.arcanetracker.trackobot.model.ResultData;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;


public class TestTrackobot {
    private static Service mService;

    @BeforeClass
    public static void beforeClass() {
        mService = Trackobot.createService("bitter-void-terror-7444", "f762d37712");
    }

    @Test
    public void testRank() {
        ResultData resultData = new ResultData();

        resultData.result = new Result();
        resultData.result.coin = true;
        resultData.result.win = true;
        resultData.result.mode = "practice";

        resultData.result.hero = Trackobot.getHero(0);
        resultData.result.opponent = Trackobot.getHero(0);
        resultData.result.added = Utils.ISO8601DATEFORMAT.format(new Date());

        mService.postResults(resultData)
                .toBlocking().value();
    }
}
