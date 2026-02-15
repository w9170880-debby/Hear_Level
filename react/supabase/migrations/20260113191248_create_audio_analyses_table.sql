/*
  # Create Audio Analyses Table

  1. New Tables
    - `audio_analyses`
      - `id` (uuid, primary key)
      - `user_id` (uuid, foreign key to auth.users)
      - `file_name` (text) - Original audio file name
      - `file_url` (text) - URL to the stored audio file
      - `cefr_level` (text) - Classified CEFR level (A1, A2, B1, B2, C1, C2)
      - `num_questions` (integer) - Number of questions generated
      - `questions` (jsonb) - Array of generated questions with answers
      - `created_at` (timestamptz) - When the analysis was created
      - `updated_at` (timestamptz) - Last update timestamp

  2. Security
    - Enable RLS on `audio_analyses` table
    - Add policy for users to read their own analyses
    - Add policy for users to create their own analyses
    - Add policy for users to update their own analyses
    - Add policy for users to delete their own analyses

  3. Important Notes
    - Each user can only access their own audio analyses
    - Questions are stored as JSONB for flexibility
    - File URLs will point to Supabase storage
*/

CREATE TABLE IF NOT EXISTS audio_analyses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  file_name text NOT NULL,
  file_url text NOT NULL,
  cefr_level text NOT NULL CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
  num_questions integer NOT NULL DEFAULT 5,
  questions jsonb NOT NULL DEFAULT '[]'::jsonb,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

ALTER TABLE audio_analyses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own analyses"
  ON audio_analyses
  FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);

CREATE POLICY "Users can create own analyses"
  ON audio_analyses
  FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own analyses"
  ON audio_analyses
  FOR UPDATE
  TO authenticated
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own analyses"
  ON audio_analyses
  FOR DELETE
  TO authenticated
  USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_audio_analyses_user_id ON audio_analyses(user_id);
CREATE INDEX IF NOT EXISTS idx_audio_analyses_created_at ON audio_analyses(created_at DESC);