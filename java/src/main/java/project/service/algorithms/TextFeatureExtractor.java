package project.service.algorithms;

import edu.stanford.nlp.pipeline.CoreDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import project.service.algorithms.tfidf.Indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TextFeatureExtractor {

    @Autowired
    private StanfordNlpProcessor nlpProcessor;

    @Autowired
    private FeatureCalculationService calculationService;

    @Autowired
    private Indexer indexer;

    // ניתוח מלא בזמן אמת — Stanford NLP + מעבר יחיד על הטקסט
    public Map<String, Double> extractFeature(String text) {
        CoreDocument doc = nlpProcessor.analyze(text);
        return calculationService.calculateAllFeatures(doc);
    }

    // אימון TF-IDF — pipeline קל (ללא parse), מחלץ למות
    public void trainIndexerWithText(String text) {
        CoreDocument doc = nlpProcessor.analyzeFast(text);
        List<String> lemmas = new ArrayList<>();
        for (var token : doc.tokens()) {
            String lemma = token.lemma().toLowerCase();
            if (lemma.matches("[a-z]+")) lemmas.add(lemma);
        }
        if (!lemmas.isEmpty()) indexer.addDocument(lemmas);
    }
}