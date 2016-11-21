package net.mbonnin.arcanetracker;

import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.mbonnin.arcanetracker.adapter.Controller;
import net.mbonnin.arcanetracker.adapter.OpponentController;
import net.mbonnin.arcanetracker.adapter.PlayerController;
import net.mbonnin.arcanetracker.adapter.ItemAdapter;
import net.mbonnin.arcanetracker.parser.Player;

import io.paperdb.Paper;
import timber.log.Timber;

import static android.view.View.GONE;

/**
 * Created by martin on 10/14/16.
 */

public class DeckCompanion {
    private static final String KEY_LAST_USED_DECK_ID = "KEY_LAST_USED_DECK_ID";
    private Controller mController;

    View settings;
    TextView winLoss;
    public TextView deckName;

    private ItemAdapter mAdapter;
    private RecyclerView recyclerView;
    private ViewManager mViewManager;

    private ViewManager.Params mParams;
    private ViewManager.Params mRecyclerViewParams;
    private boolean isOpponent;
    private Deck mDeck;
    private ImageView background;
    private Player mPlayer;

    public DeckCompanion(View v, boolean isOpponent) {
        mViewManager = ViewManager.get();
        this.isOpponent = isOpponent;

        Timber.d("screen: " + mViewManager.getWidth() + "x" + mViewManager.getHeight());

        int w = (int) (0.33 * 0.5 * mViewManager.getWidth());
        int h = mViewManager.getHeight();

        settings = v.findViewById(R.id.edit);
        winLoss = (TextView) v.findViewById(R.id.winLoss);
        deckName = (TextView) v.findViewById(R.id.deckName);
        background = (ImageView) v.findViewById(R.id.background);
        recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);

        int x = Settings.get("x" + isOpponent, -1);
        if (x == -1) {
            x = 0;
        }
        mParams = new ViewManager.Params();
        mParams.x = x;
        mParams.y = 0;
        mParams.w = w;
        mParams.h = h;

        mRecyclerViewParams = new ViewManager.Params();
        mRecyclerViewParams.w = w;
        mRecyclerViewParams.h = mViewManager.getHeight() - h;
        mAdapter = new ItemAdapter();

        if (isOpponent) {
            settings.setVisibility(GONE);
            winLoss.setVisibility(GONE);


            mController = new OpponentController(mAdapter);
            setDeck(DeckList.getOpponentDeck(), null);
        } else {
            new SettingsButtonCompanion(settings);
            String lastUsedId = Paper.book().read(KEY_LAST_USED_DECK_ID);

            Deck deck = null;
            if (lastUsedId != null) {
                for (Deck deck2 : DeckList.get()) {
                    if (deck2.id.equals(lastUsedId)) {
                        deck = deck2;
                        break;
                    }
                }
                if (deck == null && lastUsedId.equals(DeckList.ARENA_DECK_ID)) {
                    deck = DeckList.getArenaDeck();
                }
            }

            if (deck == null) {
                deck = DeckList.createDeck(Card.CLASS_INDEX_WARRIOR);
                Paper.book().write(KEY_LAST_USED_DECK_ID, deck.id);
            }

            mController = new PlayerController(mAdapter);
            setDeck(deck, null);
        }

        recyclerView.setBackgroundColor(Color.BLACK);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        recyclerView.setAdapter(mAdapter);
    }

    public void setDeck(Deck deck) {
        setDeck(deck, mPlayer);
    }

    public void setDeck(Deck deck, Player player) {
        if (!isOpponent) {
            Paper.book().write(KEY_LAST_USED_DECK_ID, deck.id);
            winLoss.setText(deck.wins + " - " + deck.losses);
        }

        deck.checkClassIndex();

        mDeck = deck;
        mPlayer = player;
        background.setBackgroundDrawable(Utils.getDrawableForClassIndex(deck.classIndex));
        deckName.setText(deck.name);
        mAdapter.setDeck(deck);

        mController.setDeck(deck, player);
    }

    public Deck getDeck() {
        return mDeck;
    }
}

