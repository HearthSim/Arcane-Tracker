package net.mbonnin.arcanetracker

import android.graphics.*
import android.os.Build
import android.text.*
import net.mbonnin.arcanetracker.helper.TypefaceHelper
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.enum.Race
import net.mbonnin.hsmodel.enum.Rarity
import net.mbonnin.hsmodel.enum.Type
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.regex.Pattern

class CardRenderer {

    internal var belwe: Typeface? = null
    internal var franklin: Typeface? = null

    /**
     * should match
     * -> 5 |4(point,points)
     * -> (5) |4(point,points)
     */
    private val PLURAL_PATTERN = Pattern.compile("\\(?(\\d+)\\)?([^\\|]*)\\|4\\((.+?),(.+?)\\)")

    private var parallelRenders: Int = 0

    init {
        belwe = TypefaceHelper.belwe()
        franklin = TypefaceHelper.franklin()
    }

    enum class Result {
        SUCCESS,
        ERROR,
    }

    @Synchronized
    private fun getAsset(name: String): Bitmap? {
        val lruCache = HDTApplication.get().imageCache
        var bitmap: Bitmap? = lruCache!!.get(name)
        if (bitmap == null) {
            bitmap = Utils.getAssetBitmap(String.format("renderer/%s.webp", name))
            if (bitmap != null) {
                lruCache.set(name, bitmap)
            }
        }
        return bitmap
    }

    private fun drawAssetBitmap(canvas: Canvas, b: Bitmap, dx: Int, dy: Int, paint: Paint? = null) {
        val rect = RectF(dx.toFloat(), dy.toFloat(), (dx + 2 * b.width).toFloat(), (dy + 2 * b.height).toFloat())
        canvas.drawBitmap(b, null, rect, paint)

    }

    private fun drawAssetIfExists(canvas: Canvas, name: String, dx: Int, dy: Int, paint: Paint? = null) {
        val b = getAsset(name)
        if (b != null) {
            drawAssetBitmap(canvas, b, dx, dy, paint)
        }
    }

    fun renderCard(id: String, outputStream: OutputStream): Result {
        Timber.d("start render $id ($parallelRenders parallel)")
        parallelRenders++
        val bitmap = Bitmap.createBitmap(TOTAL_WIDTH / 2, TOTAL_HEIGHT / 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(0.5f, 0.5f)
        val card = CardUtil.getCard(id)
        var dx: Int
        var dy: Int
        val s: String

        val result = drawCardArt(canvas, card)

        var type: String
        if (Type.HERO == card.type) {
            // special case until I add support for heroes cards
            type = Type.MINION
        } else {
            type = card.type
        }
        type = type.toLowerCase()

        drawAssetIfExists(canvas, "frame-" + type + "-" + card.playerClass.toLowerCase(), 0, 0)

        if (!TextUtils.isEmpty(card.multiClassGroup)) {
            drawAssetIfExists(canvas, "multi-" + card.multiClassGroup!!.toLowerCase(), 17, 88)
        }

        drawAssetIfExists(canvas, "cost-mana", 24, 82)

        if (!TextUtils.isEmpty(card.rarity)) {
            dy = 607
            if (Type.MINION == card.type) {
                dx = 326
            } else {
                dx = 311
            }

            s = "rarity-" + card.type.toLowerCase() + "-" + card.rarity!!.toLowerCase()
            drawAssetIfExists(canvas, s, dx, dy)
        }

        if (Rarity.LEGENDARY == card.rarity && Type.MINION == card.type) {
            drawAssetIfExists(canvas, "elite", 196, 0)
        }

        if (!TextUtils.isEmpty(card.set)) {
            dx = 270
            dy = 735

            val paint = Paint()
            paint.alpha = 60

            if (Type.MINION == card.type && !TextUtils.isEmpty(card.race)) {
                dx -= 12 // Shift up
            } else if (Type.SPELL == card.type) {
                dx = 264
                dy = 726
            } else if (Type.WEAPON == card.type) {
                dx = 264
            }

            drawAssetIfExists(canvas, "set-" + card.set.toLowerCase(), dx, dy, paint)
        }

        drawName(canvas, card)

        if (!TextUtils.isEmpty(card.race)) {
            drawRace(canvas, card)
        }

        drawStats(canvas, card)

        drawBodyText(canvas, card)

        bitmap.compress(Bitmap.CompressFormat.WEBP, 90, outputStream)

        if (false && Utils.isAppDebuggable) {
            val debugFile = File("/sdcard/" + card.id + ".png")
            try {
                val fileOutputStream = FileOutputStream(debugFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream)
                fileOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        bitmap.recycle()
        parallelRenders--

        return result
    }

    private fun drawBodyText(canvas: Canvas, card: Card) {
        if (TextUtils.isEmpty(card.text)) {
            return
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
        var text = card.text!!.replace("$", "").replace("#", "")
        if (text.startsWith("[x]")) {
            text = text.substring(3)
        }
        val m = PLURAL_PATTERN.matcher(text)
        val builder = StringBuilder()
        var startPos = 0
        while (m.find()) {
            builder.append(text.substring(startPos, m.start()))
            builder.append(m.group(1))
            builder.append(m.group(2))
            try {
                val n = Integer.parseInt(m.group(1))
                if (n > 1) {
                    builder.append(m.group(4))
                } else {
                    builder.append(m.group(3))
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            startPos = m.end()
        }
        builder.append(text.substring(startPos))
        text = builder.toString()

        val textPaint = TextPaint()
        textPaint.color = if (Type.WEAPON == card.type) Color.WHITE else Color.BLACK
        textPaint.style = Paint.Style.FILL
        textPaint.isAntiAlias = true
        textPaint.typeface = franklin
        textPaint.textSize = 50f

        val layout = DynamicLayout(Html.fromHtml(text), textPaint, 442, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, true)

        canvas.save()
        canvas.translate((TOTAL_WIDTH / 2 - layout.width / 2).toFloat(), (860 - layout.height / 2).toFloat())
        layout.draw(canvas)
        canvas.restore()

    }

    private fun drawRace(canvas: Canvas, card: Card) {
        val b = getAsset("race-banner") ?: return

        var s: String? = null
        val context = HDTApplication.context

        when (card.race) {
            Race.MECHANICAL -> s = context.getString(R.string.race_mechanical)
            Race.DEMON -> s = context.getString(R.string.race_demon)
            Race.BEAST -> s = context.getString(R.string.race_beast)
            Race.DRAGON -> s = context.getString(R.string.race_dragon)
            Race.MURLOC -> s = context.getString(R.string.race_murloc)
            Race.PIRATE -> s = context.getString(R.string.race_pirate)
            Race.TOTEM -> s = context.getString(R.string.race_totem)
            Race.ELEMENTAL -> s = context.getString(R.string.race_elemental)
        }

        if (s == null) {
            return
        }

        val textSize = 45f

        drawAssetBitmap(canvas, b, 125, 937)

        val paint = Paint()
        paint.textAlign = Paint.Align.CENTER
        paint.isAntiAlias = true
        paint.typeface = belwe
        paint.textSize = textSize

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = textSize * STROKE_RATIO

        val y = 1015
        canvas.drawText(s, (TOTAL_WIDTH / 2).toFloat(), y.toFloat(), paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL

        canvas.drawText(s, (TOTAL_WIDTH / 2).toFloat(), y.toFloat(), paint)
    }

    private fun drawStats(canvas: Canvas, card: Card) {
        val offset = 50


        drawNumber(canvas, 116, 170 + offset, card.cost!!, 170)

        when (card.type) {
            Type.MINION -> {
                drawAssetIfExists(canvas, "attack", 0, 862)
                drawAssetIfExists(canvas, "health", 575, 876)
            }
            Type.WEAPON -> {
                drawAssetIfExists(canvas, "attack-weapon", 32, 906)
                drawAssetIfExists(canvas, "health-weapon", 584, 890)
            }
        }

        if (Type.MINION == card.type) {
            drawNumber(canvas, 128, 994 + offset, card.attack!!, 150)
            drawNumber(canvas, 668, 994 + offset, card.health!!, 150)
        } else if (Type.WEAPON == card.type) {
            drawNumber(canvas, 128, 994 + offset, card.attack!!, 150)
            drawNumber(canvas, 668, 994 + offset, card.durability!!, 150)
        }


    }

    private fun drawName(canvas: Canvas, card: Card) {
        val path = Path()
        val s = "name-banner-" + card.type.toLowerCase()
        val b = getAsset(s)
        val x: Int
        val y: Int

        if (b == null) {
            return
        }

        var textSize = 60f
        val voffset = -20f

        val paint = Paint()
        paint.textAlign = Paint.Align.CENTER
        paint.isAntiAlias = true
        paint.typeface = belwe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            paint.letterSpacing = -0.05f
        }
        paint.textSize = textSize

        val size = paint.measureText(card.name)
        if (size > 570) {
            textSize = 50f
            paint.textSize = textSize
        }

        /**
         * the path data is generated by drawing a path in inkscape and looking at the svg path
         * the path is aligned to the bottom of the banner, hence the negative voffset to center the text
         */
        when (card.type) {
            Type.MINION -> {
                x = 94
                y = 546

                path.moveTo(20.854373f, 124.74981f)
                path.cubicTo(113.21275f, 144.704f, 203.93683f, 101.6693f, 307.99815f, 91.580244f)
                path.cubicTo(411.44719f, 81.55055f, 487.45236f, 71.558015f, 578.30781f, 115.12471f)
            }
            Type.SPELL -> {
                x = 66
                y = 530
                path.moveTo(55.440299f, 131.50746f)
                path.rCubicTo(165.651711f, -48.547726f, 319.389151f, -69.712531f, 530.298511f, 0f)
            }
            Type.WEAPON -> {
                x = 56
                y = 551

                path.moveTo(57.873134f, 89.514925f)
                path.lineTo(597.20110f, 89.514925f)
            }
            else -> return
        }

        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        drawAssetBitmap(canvas, b, 0, 0)

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = textSize * STROKE_RATIO
        canvas.drawTextOnPath(card.name, path, 0f, voffset, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawTextOnPath(card.name, path, 0f, voffset, paint)

        canvas.restore()
    }

    private fun drawCardArt(canvas: Canvas, card: Card): Result {
        val dx: Int
        val dy: Int
        val dWidth: Int
        val dHeight: Int
        val clipPath = Path()

        when (card.type) {
            Type.MINION -> {
                dx = 100
                dy = 75
                dWidth = 590
                dHeight = 590
                val rect = RectF(180f, dy.toFloat(), (180 + 430).toFloat(), (dy + dHeight).toFloat())
                clipPath.addOval(rect, Path.Direction.CCW)
            }
            Type.SPELL -> {
                dx = 125
                dy = 117
                dWidth = 529
                dHeight = 529
                clipPath.addRect(dx.toFloat(), 165f, (dx + dWidth).toFloat(), (165 + 434).toFloat(), Path.Direction.CCW)
            }
            Type.WEAPON -> {
                dx = 150
                dy = 135
                dWidth = 476
                dHeight = 476
                val rect = RectF(dx.toFloat(), dy.toFloat(), (dx + dWidth).toFloat(), (dy + 468).toFloat())
                clipPath.addOval(rect, Path.Direction.CCW)
            }
            else -> {
                dx = 100
                dy = 75
                dWidth = 590
                dHeight = 590
                val rect = RectF(180f, dy.toFloat(), (180 + 430).toFloat(), (dy + dHeight).toFloat())
                clipPath.addOval(rect, Path.Direction.CCW)
            }
        }

        canvas.save()
        canvas.clipPath(clipPath)
        val dstRect = RectF(dx.toFloat(), dy.toFloat(), (dx + dWidth).toFloat(), (dy + dHeight).toFloat())
        val t = Utils.getCardArtBlocking(card.id)

        val result: Result
        if (t != null) {
            val srcRect = Rect(0, 0, t.width, t.height)
            canvas.drawBitmap(t, srcRect, dstRect, null)
            t.recycle()
            result = Result.SUCCESS
        } else {
            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.color = Color.DKGRAY
            canvas.drawRect(dstRect, paint)
            result = Result.ERROR
        }
        canvas.restore()

        return result
    }

    companion object {
        /**
         * text size to stroke width ratio for black outlined texts
         */
        private val STROKE_RATIO = 0.1f
        private var sRenderer: CardRenderer? = null

        /**
         * this is the 1:1 size of the original frame-XXX assets. To save on space, I scale everything by x2
         */
        val TOTAL_WIDTH = 764
        val TOTAL_HEIGHT = 1100

        fun drawNumber(canvas: Canvas, dx: Int, dy: Int, number: Int, size: Int) {
            drawNumber(canvas, dx, dy, Integer.toString(number), size)

        }

        fun drawNumber(canvas: Canvas, dx: Int, dy: Int, number: String, size: Int) {
            val paint = Paint()
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.isAntiAlias = true
            paint.typeface = TypefaceHelper.belwe()
            paint.textSize = size.toFloat()

            canvas.drawText(number, dx.toFloat(), dy.toFloat(), paint)

            paint.color = Color.BLACK
            paint.strokeWidth = size * 0.03f
            paint.style = Paint.Style.STROKE

            canvas.drawText(number, dx.toFloat(), dy.toFloat(), paint)
        }

        fun get(): CardRenderer {
            if (sRenderer == null) {
                sRenderer = CardRenderer()
            }
            return sRenderer!!
        }
    }
}
