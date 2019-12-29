package net.hearthsim.hslog

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val mainDispatcher = Dispatchers.Main as CoroutineDispatcher