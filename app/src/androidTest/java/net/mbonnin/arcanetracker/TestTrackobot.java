package net.mbonnin.arcanetracker;

import android.support.test.InstrumentationRegistry;

import junit.framework.Assert;

import net.mbonnin.arcanetracker.trackobot.Trackobot;
import net.mbonnin.arcanetracker.trackobot.User;
import net.mbonnin.arcanetracker.trackobot.model.Result;
import net.mbonnin.arcanetracker.trackobot.model.ResultData;

import org.junit.Test;

import java.util.Date;

import io.paperdb.Paper;


public class TestTrackobot {
    @Test
    public void testRank() {
        Paper.init(InstrumentationRegistry.getTargetContext());
        ResultData resultData = new ResultData();

        resultData.result = new Result();
        resultData.result.coin = true;
        resultData.result.win = true;
        resultData.result.mode = "practice";

        resultData.result.hero = Trackobot.Companion.getHero(0);
        resultData.result.opponent = Trackobot.Companion.getHero(0);
        resultData.result.added = Utils.INSTANCE.getISO8601DATEFORMAT().format(new Date());

        Trackobot.Companion.get().link(new User("bitter-void-terror-7444", "f762d37712"));
        Lce<ResultData> lce = Trackobot.Companion.get().sendResultSingle(resultData)
                .toBlocking().value();

        Assert.assertTrue(lce.getError() == null);
    }
}
