package searchengine.services.SearchService.utils;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class SnippetFinder {

    private static LuceneMorphology luceneMorphology;

    static {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final int START_OFFSET = 5;
    private static final int END_OFFSET = 10;
    private static final int MAX_LENGTH = 200;

    public static String find(String text, Set<String> lemmas) {
        StringBuilder sb = new StringBuilder();

        String [] textArray = text.split(" ");

        List<Integer> searchWordsPositions = new ArrayList<>();
        for (int i = 0; i < textArray.length; i++) {
            String currentWord = textArray[i].replaceAll("[^А-я]", " ").toLowerCase().trim();
            if (currentWord.isBlank() || !luceneMorphology.checkString(currentWord)) {
                continue;
            }
            String normalFormWord = getNormalForm(currentWord);
            if (lemmas.contains(normalFormWord)) {
                searchWordsPositions.add(i);
            }
        }

        for (int pos : searchWordsPositions) {
            int start;
            int end;
            start = Math.max(pos - START_OFFSET, 0);
            end = Math.min(pos + END_OFFSET, textArray.length - 1);
            for (int i = start; i < end; i++) {
                if (i == pos) {
                    sb.append("<b>").append(textArray[i]).append("</b>");
                } else {
                    sb.append(textArray[i]);
                }
                if (i == end - 1) {
                    sb.append("... ");
                } else {
                    sb.append(" ");
                }
            }
            if (sb.length() > MAX_LENGTH) {
                break;
            }
        }
        return sb.toString();
    }

    public static String getNormalForm(String word) {
        List<String> res = luceneMorphology.getNormalForms(word
                .replaceAll("[^А-я]", " ")
                .toLowerCase()
                .trim());
        return res.get(0);
    }
}
