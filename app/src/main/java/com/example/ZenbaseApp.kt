package com.example

import android.app.Application
import com.example.di.AppContainer

/**
 * Zastřešující aplikace. Poskytuje manuální Service Locator (AppContainer) pro DI,
 * z důvodu nekompatibility Hiltu s aktuálně využívaným systémovým AGP 9.1.
 */
class ZenbaseApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
