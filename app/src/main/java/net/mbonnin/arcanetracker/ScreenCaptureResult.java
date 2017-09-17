package net.mbonnin.arcanetracker;

class ScreenCaptureResult {
    private static volatile int rank;


    public static void setRank(int rank) {
        ScreenCaptureResult.rank = rank;
    }

    public static void getRank(int rank) {
        ScreenCaptureResult.rank = rank;
    }
}
