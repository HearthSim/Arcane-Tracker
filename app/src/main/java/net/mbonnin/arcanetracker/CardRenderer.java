package net.mbonnin.arcanetracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.DynamicLayout;
import android.text.Html;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class CardRenderer {
    /**
     * text size to stroke width ratio for black outlined texts
     */
    private static final float STROKE_RATIO = 0.1f;
    private static CardRenderer sRenderer;
    HashMap<String, Bitmap> assets;
    Typeface belwe;
    Typeface franklin;

    /**
     * should match
     *          -> 5 |4(point,points)
     *          -> (5) |4(point,points)
     */
    private final Pattern PLURAL_PATTERN = Pattern.compile("\\(?(\\d+)\\)?([^\\|]*)\\|4\\((.+?),(.+?)\\)");

    /**
     * this is the 1:1 size of the original frame-XXX assets. To save on space, I scale everything by x2
     */
    public static final int TOTAL_WIDTH = 764;
    public static final int TOTAL_HEIGHT = 1100;

    boolean ready;
    TextView view;
    private int parallelRenders;

    private void initAsync() {
        String [] list;

        Context context = ArcaneTrackerApplication.getContext();

        assets = new HashMap<>();

        try {
            String p = "renderer";
            list = context.getAssets().list(p);
            if (list.length > 0) {
                // This is a folder
                for (String file : list) {
                    int i = file.lastIndexOf('.');
                    if (i >= 0) {
                        String name = file.substring(0, i);
                        Bitmap b = Utils.getAssetBitmap(p + "/" + file);
                        assets.put(name, b);
                    }
                }
            } else {
            }
        } catch (IOException e) {
        }

        synchronized (this) {
            ready = true;
            this.notifyAll();
        }
    }

    public CardRenderer() {
        belwe = Typefaces.belwe();
        franklin = Typefaces.franklin();

        new Thread(() -> initAsync()).start();
    }

    private void drawAssetBitmap(Canvas canvas, Bitmap b, int dx, int dy) {
        drawAssetBitmap(canvas, b, dx, dy, null);
    }
    private void drawAssetBitmap(Canvas canvas, Bitmap b, int dx, int dy, Paint paint) {
        RectF rect = new RectF(dx, dy, dx +  2 * b.getWidth(), dy + 2 * b.getHeight());
        canvas.drawBitmap(b, null, rect, paint);

    }
    private void drawAssetIfExists(Canvas canvas, String name, int dx, int dy) {
        drawAssetIfExists(canvas, name, dx, dy, null);
    }
    private void drawAssetIfExists(Canvas canvas, String name, int dx, int dy, Paint paint) {
        Bitmap b = assets.get(name);
        if (b != null) {
            drawAssetBitmap(canvas, b, dx, dy, paint);
        }
    }

    public void renderCard(String id, OutputStream outputStream) {
        synchronized (this) {
            while (!ready) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        Timber.d("start render " + id + " (" + parallelRenders + " parallel)");
        parallelRenders++;
        Bitmap bitmap = Bitmap.createBitmap(TOTAL_WIDTH/2, TOTAL_HEIGHT/2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(0.5f, 0.5f);
        Card card = CardDb.getCard(id);
        Bitmap b;
        int dx, dy;
        String s;

        if (card.type == null) {
            return;
        }

        drawCardArt(canvas, card);

        drawAssetIfExists(canvas, "frame-" + card.type.toLowerCase() + "-" + card.playerClass.toLowerCase(), 0, 0);

        if (!TextUtils.isEmpty(card.multiClassGroup)) {
            drawAssetIfExists(canvas, "multi-" + card.multiClassGroup.toLowerCase(), 17, 88);
        }

        drawAssetIfExists(canvas, "cost-mana", 24, 82);

        if (!TextUtils.isEmpty(card.rarity)) {
            dy = 607;
            if (Card.TYPE_MINION.equals(card.type)) {
                dx = 326;
            } else {
                dx = 311;
            }

            s = "rarity-" + card.type.toLowerCase() + "-" + card.rarity.toLowerCase();
            drawAssetIfExists(canvas, s, dx, dy);
        }

        if (Card.RARITY_LEGENDARY.equals(card.rarity) && Card.TYPE_MINION.equals(card.type)) {
            drawAssetIfExists(canvas, "elite", 196, 0);
        }

        if (!TextUtils.isEmpty(card.set)) {
            dx = 270;
            dy = 735;

            Paint paint = new Paint();
            paint.setAlpha(60);

            if (Card.TYPE_MINION.equals(card.type) && !TextUtils.isEmpty(card.race)) {
                dx -= 12; // Shift up
            } else if (Card.TYPE_SPELL.equals(card.type)) {
                dx = 264;
                dy = 726;
            } else if (Card.TYPE_WEAPON.equals(card.type)) {
                dx = 264;
            }

            drawAssetIfExists(canvas, "set-" + card.set.toLowerCase(), dx, dy, paint);
        }

        drawName(canvas, card);

        if (!TextUtils.isEmpty(card.race)) {
            drawRace(canvas, card);
        }

        drawStats(canvas, card);

        drawBodyText(canvas, card);

        bitmap.compress(Bitmap.CompressFormat.WEBP, 90,  outputStream);

        if (false && Utils.isAppDebuggable()) {
            File debugFile = new File("/sdcard/" + card.id + ".png");
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(debugFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        bitmap.recycle();
        parallelRenders--;
    }

    private void drawBodyText(Canvas canvas, Card card) {
        if (TextUtils.isEmpty(card.text)) {
            return;
        }

        /**
         * Trying to reverse engineer the body text:
         *
         * -> '$' is for damage as in 'Deal $5 damage'
         * -> '#' is for PV as in 'Restore #10 Health'
         * -> |4 (singular,plural) is for plurals as in 'Inflige $4 |4(point,points) de dégâts'
         * -> [x]: not sure what this is
         * -> some text has line breaks inside, some other doesnt, right now we ignore it
         */
        String text = card.text.replace("$", "").replace("#", "");
        if (text.startsWith("[x]")) {
            text = text.substring(3);
        }
        Matcher m = PLURAL_PATTERN.matcher(text);
        StringBuilder builder = new StringBuilder();
        int startPos = 0;
        while (m.find()) {
            builder.append(text.substring(startPos, m.start()));
            builder.append(m.group(1));
            builder.append(m.group(2));
            try {
                int n = Integer.parseInt(m.group(1));
                if (n > 1) {
                    builder.append(m.group(4));
                } else {
                    builder.append(m.group(3));
                }
            } catch (Exception e) {
                Timber.e(e);
            }
            startPos = m.end();
        }
        builder.append(text.substring(startPos));
        text = builder.toString();

        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Card.TYPE_WEAPON.equals(card.type) ? Color.WHITE: Color.BLACK);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(franklin);
        textPaint.setTextSize(50);

        DynamicLayout layout = new DynamicLayout(Html.fromHtml(text), textPaint, 442, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, true);

        canvas.save();
        canvas.translate(TOTAL_WIDTH/2 - layout.getWidth()/2, 860 - layout.getHeight()/2);
        layout.draw(canvas);
        canvas.restore();

    }

    private void drawRace(Canvas canvas, Card card) {
        Bitmap b = assets.get("race-banner");

        if (b == null) {
            return;
        }

        String s = null;
        Context context = ArcaneTrackerApplication.getContext();

        switch(card.race) {
            case Card.RACE_MECHANICAL:
                s = context.getString(R.string.race_mechanical);
                break;
            case Card.RACE_DEMON:
                s = context.getString(R.string.race_demon);
                break;
            case Card.RACE_BEAST:
                s = context.getString(R.string.race_beast);
                break;
            case Card.RACE_DRAGON:
                s = context.getString(R.string.race_dragon);
                break;
            case Card.RACE_MURLOC:
                s = context.getString(R.string.race_murloc);
                break;
            case Card.RACE_PIRATE:
                s = context.getString(R.string.race_pirate);
                break;
            case Card.RACE_TOTEM:
                s = context.getString(R.string.race_totem);
                break;
        }

        if (s == null) {
            return;
        }

        float textSize = 45;

        drawAssetBitmap(canvas, b, 125, 937);

        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setTypeface(belwe);
        paint.setTextSize(textSize);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(textSize*STROKE_RATIO);

        int y = 1015;
        canvas.drawText(s, TOTAL_WIDTH/2, y, paint);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawText(s, TOTAL_WIDTH/2, y, paint);
    }

    private void drawStats(Canvas canvas, Card card) {
        int offset = 50;


        drawNumber(canvas, 116, 170 + offset, card.cost, 170);

        switch (card.type) {
            case Card.TYPE_MINION:
                drawAssetIfExists(canvas, "attack", 0, 862);
                drawAssetIfExists(canvas, "health", 575, 876);
                break;
            case Card.TYPE_WEAPON:
                drawAssetIfExists(canvas, "attack-weapon", 32, 906);
                drawAssetIfExists(canvas, "health-weapon", 584, 890);
                break;
        }

        if (Card.TYPE_MINION.equals(card.type)) {
            drawNumber(canvas, 128, 994 + offset, card.attack, 150);
            drawNumber(canvas, 668, 994 + offset, card.health, 150);
        } else if (Card.TYPE_WEAPON.equals(card.type)) {
            drawNumber(canvas, 128, 994 + offset, card.attack, 150);
            drawNumber(canvas, 668, 994 + offset, card.durability, 150);
        }


    }

    public static void drawNumber(Canvas canvas, int dx, int dy, int number, int size) {
        drawNumber(canvas, dx, dy, Integer.toString(number), size);

    }

    public static void drawNumber(Canvas canvas, int dx, int dy, String number, int size) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setTypeface(Typefaces.belwe());
        paint.setTextSize(size);

        canvas.drawText(number, dx, dy, paint);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(size * 0.03f);
        paint.setStyle(Paint.Style.STROKE);

        canvas.drawText(number, dx, dy, paint);
    }

    private void drawName(Canvas canvas, Card card) {
        Path path = new Path();
        String s = "name-banner-" + card.type.toLowerCase();
        Bitmap b = assets.get(s);
        int x, y;

        if (b == null) {
            return;
        }

        float textSize = 60;
        final float voffset = -20f;

        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setTypeface(belwe);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            paint.setLetterSpacing(-0.05f);
        }
        paint.setTextSize(textSize);

        float size = paint.measureText(card.name);
        if (size > 570) {
            textSize = 50;
            paint.setTextSize(textSize);
        }

        /**
         * the path data is generated by drawing a path in inkscape and looking at the svg path
         * the path is aligned to the bottom of the banner, hence the negative voffset to center the text
         */
        switch (card.type) {
            case Card.TYPE_MINION:
                x = 94;
                y = 546;

                path.moveTo(20.854373f, 124.74981f);
                path.cubicTo(113.21275f, 144.704f, 203.93683f, 101.6693f, 307.99815f,91.580244f);
                path.cubicTo(411.44719f, 81.55055f, 487.45236f, 71.558015f, 578.30781f, 115.12471f);

                break;
            case Card.TYPE_SPELL:
                x = 66;
                y = 530;
                path.moveTo(55.440299f,131.50746f);
                path.rCubicTo(165.651711f,-48.547726f, 319.389151f,-69.712531f, 530.298511f,0);
                break;
            case Card.TYPE_WEAPON:
                x = 56;
                y = 551;

                path.moveTo(57.873134f, 89.514925f);
                path.lineTo(597.20110f, 89.514925f);

                break;
            default:
                return;
        }

        canvas.save();
        canvas.translate(x, y);
        drawAssetBitmap(canvas, b, 0, 0);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(textSize*STROKE_RATIO);
        canvas.drawTextOnPath(card.name, path, 0, voffset, paint);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawTextOnPath(card.name, path, 0, voffset, paint);

        canvas.restore();
    }

    private void drawCardArt(Canvas canvas, Card card) {
        int dx, dy, dWidth, dHeight;
        Path clipPath = new Path();

        switch (card.type) {
            case Card.TYPE_MINION: {
                dx = 100;
                dy = 75;
                dWidth = 590;
                dHeight = 590;
                RectF rect = new RectF(180, dy, 180 + 430, dy + dHeight);
                clipPath.addOval(rect, Path.Direction.CCW);
                break;
            }
            case Card.TYPE_SPELL:
                dx = 125;
                dy = 117;
                dWidth = 529;
                dHeight = 529;
                clipPath.addRect(dx, 165, dx + dWidth, 165 + 434, Path.Direction.CCW);
                break;
            case Card.TYPE_WEAPON: {
                dx = 150;
                dy = 135;
                dWidth = 476;
                dHeight = 476;
                RectF rect = new RectF(dx, dy, dx + dWidth, dy + 468);
                clipPath.addOval(rect, Path.Direction.CCW);
                break;
            }
            default:
                return;
        }

        canvas.save();
        canvas.clipPath(clipPath);
        Bitmap t = Utils.getAssetBitmap("cards/" + card.id + ".webp", "renderer/default.webp");
        RectF dstRect = new RectF(dx, dy, dx + dWidth, dy + dHeight);
        if (t != null) {
            Rect srcRect = new Rect(0, 0, t.getWidth(), t.getHeight());
            canvas.drawBitmap(t, srcRect, dstRect, null);
            t.recycle();
        } else {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.DKGRAY);
            canvas.drawRect(dstRect, paint);
        }
        canvas.restore();
    }

    public static CardRenderer get() {
        if (sRenderer == null) {
            sRenderer = new CardRenderer();
        }
        return sRenderer;
    }
}
