package net.mbonnin.arcanetracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class Language {
    public String friendlyName;
    public String jsonName;
    public String key;
    public Locale locale;

    public static ArrayList<Language> allLanguages = new ArrayList<>();

    static {
        allLanguages.add(new Language("English", "enUS", "en"));
        allLanguages.add(new Language("Español", "esES", "es"));
        allLanguages.add(new Language("Français", "frFR", "fr"));
        allLanguages.add(new Language("Português", "ptBR", "pt"));
        allLanguages.add(new Language("Pусский", "ruRU", "ru"));
        allLanguages.add(new Language("한국의", "koKR", "ko"));
        allLanguages.add(new Language("中國", "zhTW", "zh"));
    }


    public Language(String friendlyName, String jsonName, String key) {
        this.friendlyName = friendlyName;
        this.jsonName = jsonName;
        this.key = key;
    }

    public static Language getCurrentLanguage() {
        String l = Settings.get(Settings.LANGUAGE, null);

        if (l == null) {
            String locale = Locale.getDefault().getLanguage().toLowerCase();

            if (locale.contains("fr")) {
                l = "fr";
            } else if (locale.contains("ru")) {
                l = "ru";
            } else if (locale.contains("pt")) {
                l = "pt";
            } else if (locale.contains("ko")) {
                l = "ko";
            } else if (locale.contains("zh")) {
                l = "zh";
            } else if (locale.contains("es")) {
                l = "es";
            } else {
                l = "en";
            }
        }

        Language language = null;
        for (Language language2: allLanguages) {
            if (language2.key.equals(l)) {
                language = language2;
                break;
            }
        }

        if (language == null) {
            language = allLanguages.get(0);
        }

        return language;
    }

}
