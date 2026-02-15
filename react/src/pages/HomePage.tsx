import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Upload, Sparkles, Loader2, FileAudio } from 'lucide-react';
import Navigation from '../components/Navigation';
import axios from 'axios';
import ProcessingOverlay from './ProcessingOverlay';

export default function HomePage() {
  const [file, setFile] = useState<File | null>(null);
  const [numQuestions, setNumQuestions] = useState(5);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [dragActive, setDragActive] = useState(false);
  const { user } = useAuth();
  const navigate = useNavigate();

  const handleDrag = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const droppedFile = e.dataTransfer.files[0];
      if (droppedFile.type.startsWith('audio/')) {
        setFile(droppedFile);
        setError('');
      } else {
        setError('Please upload an audio file');
      }
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0];
      if (selectedFile.type.startsWith('audio/')) {
        setFile(selectedFile);
        setError('');
      } else {
        setError('Please upload an audio file');
      }
    }
  };

  const handleAnalyze = async () => {
    if (!file || !user) {
      if (!user) setError('Please log in first');
      return;
    }
    setLoading(true);
    setError('');
    try {
      // בתוך handleAnalyze
      const actualUserId = 
     (user as any).id || 
     (user as any)._id || 
     (user as any).userData?.id || 
    (user as any).userData?._id;

console.log("--- Analyze Debug ---");
console.log("User object from Context:", user);
console.log("Resolved ID:", actualUserId);
      if (!actualUserId) {
          console.error("Warning: actualUserId is missing, falling back to guest");
      }
      const formData = new FormData();
      formData.append('file', file);
      formData.append('userId', actualUserId || 'guest');
      formData.append('numQuestions', numQuestions.toString());

      const serverPromise = await axios.post('http://localhost:8080/api/analyze', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      const animationTimer = new Promise((resolve) => setTimeout(resolve, 10000));
      const [response] = await Promise.all([serverPromise, animationTimer]);

      if (response.data && response.data.id) {
        navigate(`/analysis/${response.data.id}`);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to communicate with server');
      setLoading(false);
    }
  };

  return (
    /* ה-style כאן מכריח את הדף לאפשר גלילה ומבטל חסימות CSS חיצוניות */
    <div 
      className="min-h-screen w-full bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 text-white"
      style={{ overflowY: 'auto', height: '100vh' }}
    >
      {loading && <ProcessingOverlay />}
      <Navigation />
      
      {/* Container ראשי עם padding בתחתית כדי שהריבועים לא יידבקו לקצה */}
      <div className="relative max-w-4xl mx-auto px-4 py-12 pb-32">
        
        {/* כותרת הדף */}
        <div className="text-center mb-12">
          <h1 className="text-5xl font-bold mb-4">
            Welcome to <span className="bg-gradient-to-r from-blue-400 to-green-400 bg-clip-text text-transparent">HearLevel</span>
          </h1>
          <p className="text-xl text-blue-200">
            AI-powered CEFR level analysis for your English audio
          </p>
        </div>

        {/* קופסת ההעלאה המרכזית */}
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl shadow-2xl border border-white/20 p-8 mb-16">
          <div
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
            className={`relative border-2 border-dashed rounded-xl p-12 transition-all duration-300 ${
              dragActive ? 'border-blue-400 bg-blue-500/10 scale-[1.01]' : 'border-white/30 hover:border-white/50 bg-white/5'
            }`}
          >
            <input
              type="file" accept="audio/*"
              onChange={handleFileChange}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
            />
            
            <div className="flex flex-col items-center gap-4 text-center pointer-events-none">
              {file ? (
                <>
                  <FileAudio className="w-16 h-16 text-green-400" />
                  <div>
                    <p className="text-white font-semibold text-lg">{file.name}</p>
                    <p className="text-blue-300 text-sm">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                  </div>
                </>
              ) : (
                <>
                  <Upload className="w-16 h-16 text-blue-400" />
                  <p className="text-white font-semibold text-lg">Drop audio here or click to browse</p>
                </>
              )}
            </div>
          </div>

          {file && (
            <div className="mt-8 space-y-6">
              <div>
                <label className="block text-sm text-blue-200 mb-2">Number of Questions: {numQuestions}</label>
                <input
                  type="range" min="3" max="10" value={numQuestions}
                  onChange={(e) => setNumQuestions(parseInt(e.target.value))}
                  className="w-full h-2 bg-white/20 rounded-lg appearance-none cursor-pointer"
                />
              </div>
              <button
                onClick={handleAnalyze}
                disabled={loading}
                className="w-full bg-gradient-to-r from-blue-600 to-green-600 py-4 rounded-xl font-bold text-lg hover:scale-[1.01] transition-transform flex items-center justify-center gap-3 disabled:opacity-50"
              >
                {loading ? <Loader2 className="animate-spin" /> : <Sparkles />}
                {loading ? 'Analyzing...' : 'Analyze & Generate'}
              </button>
            </div>
          )}
          {error && <p className="mt-4 text-red-400 text-center text-sm">{error}</p>}
        </div>

        {/* שלושת הריבועים - תמיד מופיעים כאן */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-xl p-6 flex flex-col items-center text-center">
            <div className="w-12 h-12 bg-blue-500/20 rounded-lg flex items-center justify-center mb-4">
              <Upload className="w-6 h-6 text-blue-400" />
            </div>
            <h3 className="text-white font-bold mb-2">Upload Audio</h3>
            <p className="text-blue-200 text-sm">MP3, WAV, or any standard format</p>
          </div>

          <div className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-xl p-6 flex flex-col items-center text-center">
            <div className="w-12 h-12 bg-green-500/20 rounded-lg flex items-center justify-center mb-4">
              <Sparkles className="w-6 h-6 text-green-400" />
            </div>
            <h3 className="text-white font-bold mb-2">AI Analysis</h3>
            <p className="text-blue-200 text-sm">Instant proficiency classification</p>
          </div>

          <div className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-xl p-6 flex flex-col items-center text-center">
            <div className="w-12 h-12 bg-purple-500/20 rounded-lg flex items-center justify-center mb-4">
              <FileAudio className="w-6 h-6 text-purple-400" />
            </div>
            <h3 className="text-white font-bold mb-2">Get Questions</h3>
            <p className="text-blue-200 text-sm">Smart practice questions</p>
          </div>
        </div>

      </div>
    </div>
  );
}