package net.mbonnin.arcanetracker.ui.promo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.android.synthetic.main.promo_view.view.*
import net.mbonnin.arcanetracker.NetworkSwitch
import net.mbonnin.arcanetracker.Settings
import net.mbonnin.arcanetracker.Utils

class PromoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(net.mbonnin.arcanetracker.R.layout.promo_view, this, true)

        when {
            !FirebaseRemoteConfig.getInstance().getBoolean(NetworkSwitch.HALLOW_END_PROMO) ||
            Settings.get(Settings.WILD_PROMO_SHOWN, false) -> {
                visibility = View.GONE
            }
            else -> {
                Settings.set(Settings.WILD_PROMO_SHOWN, true)
            }
        }

        setBackgroundColor(Color.parseColor("#88000000"))
        image.setOnClickListener {
            Utils.openLink("https://hsreplay.net/premium/?utm_source=arcane-tracker&utm_medium=banner&utm_campaign=hsreplay-wild-sale")
            visibility = View.GONE
        }
        close.setOnClickListener {
            visibility = View.GONE
        }
        setOnClickListener {
            visibility = View.GONE
        }
    }
}