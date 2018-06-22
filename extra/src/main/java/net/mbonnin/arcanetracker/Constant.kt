package net.mbonnin.arcanetracker

object Constant {
    const val ROOT_PATH = "/home/martin/git/arcane_tracker/"

    const val HSMODEL_SRC_MAIN_JAVA_PATH =
            "${ROOT_PATH}/hsmodel/src/main/java/"

    const val HSCARDS_JSON =
            "${ROOT_PATH}/extra/data/json/cards.json";
    const val CARD_DATA_JSON =
            "${ROOT_PATH}/hsmodel/src/main/resources/card_data.json"

    fun CARD_TRANSLATION_JSON(lang: String): String {
        return "${ROOT_PATH}/hsmodel/src/main/resources/card_translation_${lang}.json"
    }

    val LANGUAGE_LIST = arrayOf("enUS", "frFR", "ptBR", "ruRU", "koKR", "zhCN", "zhTW", "esES", "itIT", "plPL")
}