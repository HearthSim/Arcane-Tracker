package net.mbonnin.arcanetracker.ui.overlay.adapter

/**
 * Created by martin on 11/8/16.
 */

class HeaderItem(internal var title: String) {
    internal var expanded: Boolean = false

    internal var onClicked: Runnable? = null
}
