package awais.instagrabber.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import awais.instagrabber.utils.LocaleUtils
import awais.instagrabber.utils.ThemeUtils

abstract class BaseLanguageActivity protected constructor() : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.changeTheme(this)
        super.onCreate(savedInstanceState)
    }

    init {
        @Suppress("LeakingThis")
        LocaleUtils.updateConfig(this)
    }
}