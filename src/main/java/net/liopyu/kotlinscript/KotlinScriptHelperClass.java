package net.liopyu.kotlinscript;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KotlinScriptHelperClass {
    private static final Set<String> keywords = new HashSet<>();
    private static Pattern keywordPattern;

    static {
        addKeyWord("val");
        addKeyWord("var");
        addKeyWord("print");
        addKeyWord("import");
        addKeyWord("fun");
        keywordPattern = Pattern.compile("(" + String.join("|", keywords) + ")(\\W)");
    }

    public static void addKeyWord(String keyword) {
        keywords.add(keyword);
        keywordPattern = Pattern.compile("(" + String.join("|", keywords) + ")(\\W)");
    }

    public static boolean isKeyWord(String line) {
        Matcher matcher = keywordPattern.matcher(line);
        return matcher.find() && matcher.start() == 0;
    }

    public static String getKeyWord(String line) {
        Matcher matcher = keywordPattern.matcher(line);
        if (matcher.find() && matcher.start() == 0) {
            return matcher.group(1);
        }
        return null;
    }
}
