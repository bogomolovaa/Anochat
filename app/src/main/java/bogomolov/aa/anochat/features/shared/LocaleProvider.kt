package bogomolov.aa.anochat.features.shared

import android.content.Context
import androidx.core.os.ConfigurationCompat
import java.util.*
import javax.inject.Inject

interface LocaleProvider {
    val locale: Locale
}

class LocaleProviderImpl @Inject constructor(
    private val context: Context
) : LocaleProvider{
    override val locale: Locale
        get() = ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: throw Exception()
}