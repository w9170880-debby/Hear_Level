HearLevel

Automatic difficulty classification and comprehension-question generation for English listening material.

HearLevel takes an English audio file, transcribes it, determines its difficulty level on the CEFR scale (A1–C2), and generates listening-comprehension questions suited to that level. It is designed for English teachers and learners who need practice material matched to a specific level.

How It Works
Audio file → Transcription → Feature extraction → CEFR classification → Knowledge graph → Question generation
LTR
Transcription: The audio is converted to text using Whisper.
Feature extraction: The text is analyzed with Stanford CoreNLP to extract 32 linguistic features: statistical (sentence length, readability scores), lexical (CEFR vocabulary ratios, word frequency) and syntactic (parse-tree depth, part-of-speech ratios), together with TF-IDF n-grams.
CEFR classification: A trained XGBoost model predicts the text's level from A1 to C2.
Knowledge graph: The text is converted into a two-layer graph built from dependency parsing. Event nodes represent verbs, entity nodes represent people, places and objects, and edges capture semantic roles (agent, patient, location, time) and relations between events. Coreference resolution merges repeated mentions of the same entity.
Question generation: Rule-based Java logic traverses the graph to generate four question types: Open, Multiple Choice, True/False and Cloze, adapted to the classified level.
Design Principle: "White Box" Question Generation

All question content (what is asked, the correct answer, the distractors) is determined by transparent, rule-based Java logic over the knowledge graph. A language model (Llama 3.2 3B) is used only to rephrase questions into natural wording, never to decide their content. This keeps every question traceable to the source text and explainable.

Tech Stack
Layer	Technologies
Backend	Java, Spring Boot, Maven
NLP	Stanford CoreNLP (parsing, NER, coreference)
Machine Learning	XGBoost, TF-IDF
AI Services	Whisper (transcription), Llama 3.2 3B (paraphrasing) via a Python/Flask bridge
Frontend	React


Devora Lustig Final project, Software Engineering Diploma (2024–2026)

<img width="562" height="711" alt="צילום מסך 2026-06-09 152229" src="https://github.com/user-attachments/assets/dc8523b3-5308-45eb-b6c4-93dddf731608" />
