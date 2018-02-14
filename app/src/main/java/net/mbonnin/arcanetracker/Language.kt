package net.mbonnin.arcanetracker

import java.util.*

class Language(var friendlyName: String, var jsonName: String, var key: String) {
    companion object {

        var allLanguages = ArrayList<Language>()

        init {
            allLanguages.add(Language("English", "enUS", "en"))
            allLanguages.add(Language("Español", "esES", "es"))
            allLanguages.add(Language("Français", "frFR", "fr"))
            allLanguages.add(Language("Português", "ptBR", "pt"))
            allLanguages.add(Language("Pусский", "ruRU", "ru"))
            allLanguages.add(Language("한국의", "koKR", "ko"))
            allLanguages.add(Language("中國", "zhTW", "zh"))
        }

        val currentLanguage: Language
            get() {
                var l: String? = Settings.get(Settings.LANGUAGE, null)

                if (l == null) {
                    val locale = Locale.getDefault().language.toLowerCase()

                    if (locale.contains("fr")) {
                        l = "fr"
                    } else if (locale.contains("ru")) {
                        l = "ru"
                    } else if (locale.contains("pt")) {
                        l = "pt"
                    } else if (locale.contains("ko")) {
                        l = "ko"
                    } else if (locale.contains("zh")) {
                        l = "zh"
                    } else if (locale.contains("es")) {
                        l = "es"
                    } else {
                        l = "en"
                    }
                }

                var language: Language? = null
                for (language2 in allLanguages) {
                    if (language2.key == l) {
                        language = language2
                        break
                    }
                }

                if (language == null) {
                    language = allLanguages[0]
                }

                return language
            }
    }

}
