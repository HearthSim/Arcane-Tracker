package net.mbonnin.arcanetracker.parser;

import java.util.ArrayList;

/**
 * Created by martin on 11/29/16.
 */

public class BroadcastLineConsumer implements LogReader.LineConsumer {
    ArrayList<LogReader.LineConsumer> consumerList = new ArrayList<>();

    public void add(LogReader.LineConsumer consumer) {
        consumerList.add(consumer);
    }

    @Override
    public void onLine(String rawLine, int seconds, String line) {
        for (LogReader.LineConsumer consumer:consumerList) {
            consumer.onLine(rawLine, seconds, line);
        }
    }
}
