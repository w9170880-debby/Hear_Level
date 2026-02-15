import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { FileAudio, Calendar, MessageSquare, Loader2, ExternalLink } from 'lucide-react';
import Navigation from '../components/Navigation';

type CEFRLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2';

const CEFR_COLORS: Record<string, string> = {
  A1: 'bg-green-500',
  A2: 'bg-green-600',
  B1: 'bg-blue-500',
  B2: 'bg-blue-600',
  C1: 'bg-purple-500',
  C2: 'bg-purple-600',
};

const CEFR_LABELS: Record<string, string> = {
  A1: 'Beginner',
  A2: 'Elementary',
  B1: 'Intermediate',
  B2: 'Upper Intermediate',
  C1: 'Advanced',
  C2: 'Proficiency',
};

export default function PersonalAreaPage() {
  const [analyses, setAnalyses] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();

 useEffect(() => {
  const userId = (user as any)?.id || (user as any)?._id;
  
  if (!user) {
    console.error("DEBUG: No user object found in AuthContext!");
  } else if (!userId) {
    console.error("DEBUG: User object exists, but ID is missing!", user);
  } else {
    console.log("DEBUG: Sending request to Java for userId:", userId);
    loadAnalyses(userId);
  }
}, [user]);

  const loadAnalyses = async (userId: string) => {
    try {
      setLoading(true);
      const response = await fetch(`http://localhost:8080/api/user/${userId}/files`);
      
      if (!response.ok) {
        throw new Error('Failed to fetch data from Java server');
      }
      
      const data = await response.json();
      setAnalyses(data || []);

    } catch (error) {
      console.error('Error loading analyses:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    if (!dateString) return "No date";
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
      <Navigation />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_120%,rgba(120,119,198,0.1),rgba(255,255,255,0))]"></div>

      <div className="relative max-w-7xl mx-auto px-4 py-12">
        <div className="mb-8">
          <h1 className="text-4xl font-bold text-white mb-2">Personal Area</h1>
          <p className="text-blue-200">View and manage your audio analyses</p>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-24">
            <Loader2 className="w-12 h-12 text-blue-400 animate-spin" />
          </div>
        ) : analyses.length === 0 ? (
          <div className="bg-white/10 backdrop-blur-lg rounded-2xl shadow-2xl border border-white/20 p-12 text-center">
            <FileAudio className="w-16 h-16 text-blue-400 mx-auto mb-4" />
            <h2 className="text-2xl font-semibold text-white mb-2">No Analyses Yet</h2>
            <p className="text-blue-200 mb-6">Start by uploading your first audio file on the home page</p>
            <Link to="/" className="inline-flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-blue-600 to-green-600 text-white rounded-lg font-semibold shadow-lg">
              Upload Audio
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {analyses.map((analysis) => (
              <Link
                key={analysis.id}
                to={`/analysis/${analysis.id}`}
                className="group bg-white/10 backdrop-blur-lg rounded-xl shadow-lg border border-white/20 p-6 hover:bg-white/15 transition-all hover:scale-105"
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-2">
                      <FileAudio className="w-5 h-5 text-blue-400 flex-shrink-0" />
                      <h3 className="text-white font-semibold truncate">
                        {analysis.fileName}
                      </h3>
                    </div>
                    <div className="flex items-center gap-2 text-sm text-blue-300">
                      <Calendar className="w-4 h-4" />
                      <span>{formatDate(analysis.createdAt)}</span>
                    </div>
                  </div>
                  <ExternalLink className="w-5 h-5 text-blue-400 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0" />
                </div>

                <div className="space-y-3">
                  <div className="flex items-center gap-3">
                    <span className="text-blue-200 text-sm">CEFR Level:</span>
                    <div
                      className={`${
                        CEFR_COLORS[analysis.difficultyLevel] || 'bg-blue-600'
                      } text-white px-3 py-1 rounded-full text-sm font-semibold shadow-lg`}
                    >
                      {/* התיקון כאן: הוספת הגנה למקרה שהרמה לא קיימת במילון */}
                      {analysis.difficultyLevel} - {CEFR_LABELS[analysis.difficultyLevel] || 'Unknown'}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 text-blue-200 text-sm">
                    <MessageSquare className="w-4 h-4" />
                    <span>{analysis.questions?.length || 0} Questions</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}