import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

if (!supabaseUrl || !supabaseAnonKey) {
  throw new Error('Missing Supabase environment variables');
}

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

export interface AudioAnalysis {
  id: string;
  user_id: string;
  file_name: string;
  file_url: string;
  cefr_level: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2';
  num_questions: number;
  questions: Question[];
  created_at: string;
  updated_at: string;
}

export interface Question {
  id: number;
  question: string;
  options: string[];
  correct_answer: number;
}
