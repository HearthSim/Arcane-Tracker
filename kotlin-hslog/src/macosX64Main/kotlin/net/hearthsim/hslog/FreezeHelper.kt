package net.hearthsim.hslog

import kotlin.native.concurrent.freeze

fun <T> T.freeze() {
    freeze()
}