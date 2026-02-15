import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { supabase, AudioAnalysis } from '../lib/supabase';
import { FileAudio, Calendar, MessageSquare, Loader2, ExternalLink } from 'lucide-react';
import Navigation from '../components/Navigation';
import { CheckCircle2, CircleDashed, FileText, Globe, Brain, ListChecks } from 'lucide-react';

const steps = [
  { id: 1, label: 'Processing File', icon: FileText },
  { id: 2, label: 'CEFR Level Analysis', icon: Globe },
  { id: 3, label: 'Comprehension Analysis', icon: Brain },
  { id: 4, label: 'Generating Questions', icon: ListChecks },
];

export default function ProcessingOverlay() {
  const [currentStep, setCurrentStep] = useState(1);

  // סימולציה של התקדמות השלבים (לצורך הויזואליות)
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentStep((prev) => (prev < 4 ? prev + 1 : prev));
    }, 2500);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-md">
      <div className="w-full max-w-md p-8 bg-white/10 border border-white/20 rounded-2xl shadow-2xl">
        <div className="text-center mb-8">
          <h2 className="text-2xl font-bold text-white mb-2">Analyzing Your Audio</h2>
          <p className="text-blue-300">Our AI is working its magic...</p>
        </div>

        <div className="space-y-6">
          {steps.map((step) => {
            const isActive = step.id === currentStep;
            const isCompleted = step.id < currentStep;
            const Icon = step.icon;

            return (
              <div
                key={step.id}
                className={`flex items-center gap-4 transition-all duration-500 ${
                  isActive ? 'scale-105' : isCompleted ? 'opacity-100' : 'opacity-40'
                }`}
              >
                <div className={`p-2 rounded-lg ${
                  isActive ? 'bg-blue-500 shadow-[0_0_15px_rgba(59,130,246,0.5)]' : 
                  isCompleted ? 'bg-green-500' : 'bg-white/5'
                }`}>
                  {isCompleted ? (
                    <CheckCircle2 className="w-6 h-6 text-white" />
                  ) : (
                    <Icon className={`w-6 h-6 ${isActive ? 'text-white animate-pulse' : 'text-slate-400'}`} />
                  )}
                </div>
                
                <div className="flex-1">
                  <p className={`font-medium ${isActive ? 'text-white' : 'text-slate-300'}`}>
                    {step.label}
                  </p>
                  {isActive && (
                    <div className="h-1 w-full bg-white/10 rounded-full mt-2 overflow-hidden">
                      <div className="h-full bg-blue-500 animate-progress-indefinite"></div>
                    </div>
                  )}
                </div>

                {isActive && <CircleDashed className="w-5 h-5 text-blue-400 animate-spin" />}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}