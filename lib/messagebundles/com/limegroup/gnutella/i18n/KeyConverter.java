package com.limegroup.gnutella.i18n;

import java.io.File;
import java.util.Map;
import java.util.Properties;

public class KeyConverter {

    public static void main(String[] args) throws Exception {
        File dir = new File(".");
        LanguageLoader loader = new LanguageLoader(dir);
        Map<String, LanguageInfo> languages = loader.loadLanguages();
        
//        // apply conversions
//        for (LanguageInfo language : languages.values()) {
//            applyRules(language.getProperties());
//        }
        LanguageInfo de = languages.get("de");
        applyRules(de.getProperties());
        
        LanguageUpdater updater = new LanguageUpdater(dir, languages, loader.getEnglishLines());
        updater.updateLanguage(de);
//        updater.updateAllLanguages();
    }
    
    private static void applyRules(Properties props) {
        Rule[] rules = new Rule[] {
			new ConcatenateAndInsertRule("DOWNLOAD_APPLY_NEW_THEME_START",
										 "DOWNLOAD_APPLY_NEW_THEME_END",
										 "{0}",
										 "DOWNLOAD_APPLY_NEW_THEME"),
			new ConcatenateAndInsertRule("ERROR_BROWSE_HOST_FAILED_BEGIN_KEY",
										 "ERROR_BROWSE_HOST_FAILED_END_KEY",
										 " {0} ",
										 "ERROR_BROWSE_HOST_FAILED"),
			new ConcatenateAndInsertRule("ERROR_CANT_RESUME_START",
										 "ERROR_CANT_RESUME_END",
										 " {0} ",
										 "ERROR_CANT_RESUME"),
			new ConcatenateAndInsertRule("MESSAGE_UNABLE_TO_RENAME_FILE_START",
										 "MESSAGE_UNABLE_TO_RENAME_FILE_END",
										 " {0} ",
										 "MESSAGE_UNABLE_TO_RENAME_FILE"),
			new ConcatenateAndInsertRule("MESSAGE_FILE_CORRUPT",
										 "MESSAGE_CONTINUE_DOWNLOAD",
										 " {0} ",
										 "MESSAGE_FILE_CORRUPT"),
			new ConcatenateAndInsertRule("MESSAGE_SENSITIVE_SHARE_TOP",
										 "MESSAGE_SENSITIVE_SHARE_BOTTOM",
										 "\n\n{0}\n\n",
										 "MESSAGE_SENSITIVE_SHARE"),
			new ConcatenateRule("SEARCH_VIRUS_MSG", "SEARCH_VIRUS_MSG_ONE",
								"SEARCH_VIRUS_MSG_TWO", "SEARCH_VIRUS_MSG_THREE")
        };
        for (Rule rule : rules) {
            rule.apply(props);
        }
    }
    
    private interface Rule {
        void apply(Properties props);
    }
    
    private static class ConcatenateAndInsertRule implements Rule {

        private String firstKey;
        private String secondKey;
        private String insert; 
        private String newKey;
        
        public ConcatenateAndInsertRule(String firstKey, String secondKey, String insert, String newKey) {
            this.firstKey = firstKey;
            this.secondKey = secondKey;
            this.insert = insert;
            this.newKey = newKey;
        }
        
        public void apply(Properties props) {
            String firstValue = props.getProperty(firstKey, null);
            String secondValue = props.getProperty(secondKey, null);
            if (firstValue != null && secondValue != null) {
                System.out.println(newKey + "=" + firstValue + insert + secondValue);
                props.setProperty(newKey, firstValue + insert + secondValue);
            }
        }
        
    }
    
    private static class ConcatenateRule implements Rule {
        
        private String newKey;
        private String[] keys;
        
        public ConcatenateRule(String newKey, String...keys) {
            this.newKey = newKey;
            this.keys = keys;
        }
        
        public void apply(Properties props) {
            StringBuilder builder = new StringBuilder();
            for (String key : keys) {
                String value = props.getProperty(key, null);
                if (value == null) {
                    return;
                }
                builder.append(value);
            }
            System.out.println(newKey + "=" + builder.toString());
            props.setProperty(newKey, builder.toString());
        }
    }
    
}
