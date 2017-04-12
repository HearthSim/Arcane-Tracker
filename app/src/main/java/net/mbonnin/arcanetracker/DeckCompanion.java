package net.mbonnin.arcanetracker;

import android.content.Context;
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

import io.paperdb.Book;
import io.paperdb.Paper;
import timber.log.Timber;

import static android.view.View.GONE;

/**
 * Created by martin on 10/14/16.
 */
public class DeckCompanion {
    private static final String KEY_LAST_USED_DECK_ID = "KEY_LAST_USED_DECK_ID";
    private Context context;
    private final CardDb cardDb;
    private final Book book;
    private Controller mController;

    private TextView winLoss;
    private TextView deckName;

    private ItemAdapter mAdapter;

    private boolean isOpponent;
    private Deck mDeck;
    private ImageView background;
    private Player mPlayer;

    public DeckCompanion(View v, boolean isOpponent, MainViewCompanion mainViewCompanion, DeckListManager deckListManager, ViewManager viewManager, Settings settings, CardDb cardDb, Book book) {
        this.isOpponent = isOpponent;
        this.context = v.getContext();
        this.cardDb = cardDb;
        this.book = book;

        Timber.d("screen: " + viewManager.getWidth() + "x" + viewManager.getHeight());

        int w = (int) (0.33 * 0.5 * viewManager.getWidth());
        int h = viewManager.getHeight();

        View settingsView = v.findViewById(R.id.edit);
        winLoss = (TextView) v.findViewById(R.id.winLoss);
        deckName = (TextView) v.findViewById(R.id.deckName);
        background = (ImageView) v.findViewById(R.id.background);
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);

        int x = settings.get("x" + isOpponent, -1);
        if (x == -1) {
            x = 0;
        }
        ViewManager.Params mParams = new ViewManager.Params();
        mParams.x = x;
        mParams.y = 0;
        mParams.w = w;
        mParams.h = h;

        ViewManager.Params mRecyclerViewParams = new ViewManager.Params();
        mRecyclerViewParams.w = w;
        mRecyclerViewParams.h = viewManager.getHeight() - h;
        mAdapter = new ItemAdapter(cardDb);

        if (isOpponent) {
            settingsView.setVisibility(GONE);
            winLoss.setVisibility(GONE);


            mController = new OpponentController(cardDb,context.getString(R.string.hand),mAdapter);
            setDeck(deckListManager.getOpponentDeck(), null);
        } else {
            new SettingsButtonCompanion(settingsView, mainViewCompanion, deckListManager, viewManager);
            String lastUsedId = book.read(KEY_LAST_USED_DECK_ID);

            Deck deck = null;
            if (lastUsedId != null) {
                for (Deck deck2 : deckListManager.get()) {
                    if (deck2.id.equals(lastUsedId)) {
                        deck = deck2;
                        break;
                    }
                }
                if (deck == null && lastUsedId.equals(DeckListManager.ARENA_DECK_ID)) {
                    deck = deckListManager.getArenaDeck();
                }
            }

            if (deck == null) {
                deck = deckListManager.createDeck(Card.CLASS_INDEX_WARRIOR);
                book.write(KEY_LAST_USED_DECK_ID, deck.id);
            }

            mController = new PlayerController(mAdapter, deckListManager, cardDb, settings);
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
            book.write(KEY_LAST_USED_DECK_ID, deck.id);
            winLoss.setText(deck.wins + " - " + deck.losses);
        }

        cardDb.checkClassIndex(deck);

        mDeck = deck;
        mPlayer = player;
        background.setBackgroundDrawable(Utils.getDrawableForClassIndex(context, deck.classIndex));
        deckName.setText(deck.name);
        mAdapter.setDeck(deck);

        mController.setDeck(deck, player);
    }

    public Deck getDeck() {
        return mDeck;
    }
}

