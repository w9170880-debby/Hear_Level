package project.service.algorithms;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * שכבת תשתית לספריית Stanford CoreNLP.
 *
 * מנהלת שני pipelines נפרדים כדי לאזן בין דיוק למהירות:
 *
 *   fullPipeline  — כולל ניתוח תחבירי (parse), משמש לניתוח טקסט בזמן אמת.
 *                   מחזיר עץ תחבירי המאפשר חישוב avg_tree_depth.
 *
 *   lightPipeline — ללא parse, משמש לאימון TF-IDF בלבד.
 *                   מהיר פי כמה — parse הוא החלק הכבד ביותר בסטנפורד.
 *
 * הפרדה זו מבטיחה שסטנפורד תרוץ על נתוני האימון (~17,000 טקסטים)
 * ללא ניתוח תחבירי מיותר, ועדיין תספק ניתוח מלא על טקסט בודד בזמן אמת.
 */
@Component
public class StanfordNlpProcessor {

    private StanfordCoreNLP fullPipeline;
    private StanfordCoreNLP lightPipeline;

    /**
     * מאתחלת את שני ה-pipelines פעם אחת בהפעלת השרת.
     * הטעינה כבדה (מודלים גדולים בזיכרון) — לכן חשוב שתקרה רק בהפעלה.
     */
    @PostConstruct
    public void init() {
        Properties fullProps = new Properties();
        fullProps.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        this.fullPipeline = new StanfordCoreNLP(fullProps);

        Properties lightProps = new Properties();
        lightProps.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        this.lightPipeline = new StanfordCoreNLP(lightProps);

        System.out.println("✅ Stanford NLP pipelines ready (full + light)");
    }

    /**
     * מנתח טקסט בניתוח מלא — לשימוש בניתוח טקסט בזמן אמת.
     * כולל: tokenization, POS tagging, lemmatization, constituency parse.
     */
    public CoreDocument analyze(String text) {
        if (text == null || text.isBlank()) return new CoreDocument("");
        CoreDocument doc = new CoreDocument(text);
        fullPipeline.annotate(doc);
        return doc;
    }

    /**
     * מנתח טקסט בניתוח קל — לשימוש באימון TF-IDF בלבד.
     * כולל: tokenization, POS tagging, lemmatization.
     * אינו כולל constituency parse — חוסך זמן ריצה משמעותי על נפח גדול של טקסטים.
     */
    public CoreDocument analyzeFast(String text) {
        if (text == null || text.isBlank()) return new CoreDocument("");
        CoreDocument doc = new CoreDocument(text);
        lightPipeline.annotate(doc);
        return doc;
    }
}
