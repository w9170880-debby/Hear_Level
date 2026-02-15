import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { FileAudio, Calendar, ArrowLeft, CheckCircle2, Loader2, Award } from 'lucide-react';
import Navigation from '../components/Navigation';
import axios from 'axios';

// הגדרת צבעים לפי רמת ה-CEFR
const CEFR_COLORS: Record<string, string> = {
  A1: 'from-green-500 to-green-600',
  A2: 'from-green-600 to-green-700',
  B1: 'from-blue-500 to-blue-600',
  B2: 'from-blue-600 to-blue-700',
  C1: 'from-purple-500 to-purple-600',
  C2: 'from-purple-600 to-purple-700',
};

const CEFR_DESCRIPTIONS: Record<string, string> = {
  A1: 'Basic user - Beginner level',
  A2: 'Basic user - Elementary level',
  B1: 'Independent user - Intermediate level',
  B2: 'Independent user - Upper Intermediate level',
  C1: 'Proficient user - Advanced level',
  C2: 'Proficient user - Mastery level',
};

export default function AnalysisResultPage() {
  const { id } = useParams<{ id: string }>();
  const [analysis, setAnalysis] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedAnswers, setSelectedAnswers] = useState<{ [key: number]: number }>({});

  useEffect(() => {
    if (id) {
      loadAnalysis();
    }
  }, [id]);

  // בתוך AnalysisResultPage.tsx
const loadAnalysis = async () => {
  setLoading(true);
  setError(null);
  try {
    const response = await axios.get(`http://localhost:8080/api/analyze/${id}`);
    const data = response.data;

    setAnalysis({
      id: data.id,
      file_name: data.fileName,    // שימי לב: ב-Java זה fileName, בריאקט השתמשנו ב-file_name
      cefr_level: data.cefrLevel || 'A1', 
      questions: data.questions || [], 
      created_at: new Date().toISOString(),
    });
  } catch (err: any) {
    console.error('Error loading analysis:', err);
    setError('Analysis not found');
  } finally {
    setLoading(false);
  }
};

  const handleAnswerSelect = (questionId: number, optionIndex: number) => {
    setSelectedAnswers((prev) => ({
      ...prev,
      [questionId]: optionIndex,
    }));
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
        <Navigation />
        <div className="flex items-center justify-center h-[calc(100vh-4rem)]">
          <Loader2 className="w-12 h-12 text-blue-400 animate-spin" />
        </div>
      </div>
    );
  }

  if (error || !analysis) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
        <Navigation />
        <div className="flex flex-col items-center justify-center h-[calc(100vh-4rem)] gap-4">
          <p className="text-white text-xl">{error || 'Analysis not found'}</p>
          <Link to="/" className="text-blue-400 hover:text-blue-300 transition-colors">
            Back to Home
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
      <Navigation />
      <div className="relative max-w-5xl mx-auto px-4 py-12">
        <Link to="/" className="inline-flex items-center gap-2 text-blue-400 hover:text-blue-300 transition-colors mb-8">
          <ArrowLeft className="w-4 h-4" />
          Back to Dashboard
        </Link>

        {/* כרטיסיית פרטי קובץ ורמה */}
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl shadow-2xl border border-white/20 p-8 mb-8">
          <div className="flex items-start gap-6 mb-6">
            <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-green-500 rounded-xl flex items-center justify-center">
              <FileAudio className="w-8 h-8 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-white mb-2">{analysis.file_name}</h1>
              <div className="flex items-center gap-2 text-blue-300">
                <Calendar className="w-4 h-4" />
                <span className="text-sm">{new Date(analysis.created_at).toLocaleDateString()}</span>
              </div>
            </div>
          </div>

          <div className="bg-white/5 rounded-xl p-6 border border-white/10">
            <div className="flex items-center gap-4">
              <div className={`bg-gradient-to-r ${CEFR_COLORS[analysis.cefr_level] || 'from-gray-500 to-gray-600'} text-white px-6 py-3 rounded-xl text-2xl font-bold`}>
                {analysis.cefr_level}
              </div>
              <div>
                <p className="text-white font-semibold text-lg">{CEFR_DESCRIPTIONS[analysis.cefr_level]}</p>
                <p className="text-blue-300 text-sm">AI-powered CEFR classification</p>
              </div>
            </div>
          </div>
        </div>

        {/* חלק השאלות */}
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl shadow-2xl border border-white/20 p-8">
          <h2 className="text-2xl font-bold text-white mb-6">Listening Comprehension</h2>
          <div className="space-y-6">
            {analysis.questions.map((question: any, index: number) => (
              <div key={question.id} className="bg-white/5 rounded-xl p-6 border border-white/10">
                <h3 className="text-white font-semibold text-lg mb-4">{index + 1}. {question.question}</h3>
                <div className="space-y-3">
                  {question.options.map((option: string, optionIndex: number) => {
                    const isSelected = selectedAnswers[question.id] === optionIndex;
                    const isCorrect = question.correct_answer === optionIndex;
                    const showResult = selectedAnswers[question.id] !== undefined;

                    return (
                      <button
                        key={optionIndex}
                        onClick={() => handleAnswerSelect(question.id, optionIndex)}
                        className={`w-full text-left px-4 py-3 rounded-lg transition-all ${
                          isSelected 
                            ? (isCorrect ? 'bg-green-500/20 border-green-500' : 'bg-red-500/20 border-red-500')
                            : 'bg-white/5 border-white/20 hover:bg-white/10'
                        } border`}
                      >
                        <div className="flex items-center justify-between text-white">
                          <span>{option}</span>
                          {showResult && isCorrect && <CheckCircle2 className="w-5 h-5 text-green-400" />}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}