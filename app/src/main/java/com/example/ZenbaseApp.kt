package com.example

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Zastřešující aplikace. Inicializuje Dagger Hilt pro Dependency Injection.
 */
@HiltAndroidApp
class ZenbaseApp : Application()
