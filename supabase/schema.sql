-- Create users table with RLS policies
CREATE TABLE IF NOT EXISTS public.users (
  id UUID PRIMARY KEY REFERENCES auth.users(id),
  email TEXT UNIQUE NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now(),
  last_sync_timestamp BIGINT,
  updated_at TIMESTAMPTZ DEFAULT now(),
  is_premium BOOLEAN DEFAULT false,
  subscription_type TEXT,
  subscription_expires TIMESTAMPTZ,
  transaction_id TEXT,
  permanent_premium BOOLEAN DEFAULT false,
  last_password_reset_request_time BIGINT
);

-- Create words table (replaces users/{userId}/words subcollection)
CREATE TABLE IF NOT EXISTS public.words (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
  word TEXT NOT NULL,
  definition TEXT NOT NULL,
  example_sentence TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now(),
  last_reviewed TIMESTAMPTZ,
  next_review TIMESTAMPTZ,
  times_reviewed INTEGER DEFAULT 0,
  times_correct INTEGER DEFAULT 0,
  review_stage INTEGER DEFAULT 0,
  is_favorite BOOLEAN DEFAULT false,
  notes TEXT,
  UNIQUE(user_id, word)
);

-- Create app_usage table (replaces users/{userId}/app_usage subcollection)
CREATE TABLE IF NOT EXISTS public.app_usage (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
  date_id TEXT NOT NULL,
  app_opens INTEGER DEFAULT 0,
  time_spent_seconds INTEGER DEFAULT 0,
  features_used JSONB DEFAULT '{}',
  updated_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(user_id, date_id)
);

-- Create quiz_results table
CREATE TABLE IF NOT EXISTS public.quiz_results (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
  timestamp TIMESTAMPTZ DEFAULT now(),
  correct_answers INTEGER NOT NULL,
  total_questions INTEGER NOT NULL,
  questions JSONB NOT NULL,
  score REAL NOT NULL,
  duration_in_seconds BIGINT NOT NULL
);

-- Create quiz_history table
CREATE TABLE IF NOT EXISTS public.history (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
  timestamp TIMESTAMPTZ DEFAULT now(),
  word TEXT NOT NULL,
  correct_definition TEXT NOT NULL,
  user_answer TEXT NOT NULL,
  is_correct BOOLEAN NOT NULL
);

-- Create transactions table
CREATE TABLE IF NOT EXISTS public.transactions (
  id TEXT PRIMARY KEY,
  user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
  subscription_type TEXT NOT NULL,
  purchase_date TIMESTAMPTZ NOT NULL,
  expiration_date TIMESTAMPTZ NOT NULL,
  status TEXT NOT NULL,
  verification_method TEXT,
  device_info TEXT,
  app_version TEXT
);

-- Create pending_transactions table
CREATE TABLE IF NOT EXISTS public.pending_transactions (
  id TEXT PRIMARY KEY,
  user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
  subscription_type TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- Create timestamps table
CREATE TABLE IF NOT EXISTS public.timestamps (
  id TEXT PRIMARY KEY,
  timestamp BIGINT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- Set up Row Level Security (RLS) policies

-- Enable RLS on all tables
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.words ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quiz_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pending_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.timestamps ENABLE ROW LEVEL SECURITY;

-- Users table policies
CREATE POLICY users_select ON public.users
  FOR SELECT USING (auth.uid() = id);

CREATE POLICY users_insert ON public.users
  FOR INSERT WITH CHECK (auth.uid() = id);

CREATE POLICY users_update ON public.users
  FOR UPDATE USING (auth.uid() = id);

-- Words table policies
CREATE POLICY words_select ON public.words
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY words_insert ON public.words
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY words_update ON public.words
  FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY words_delete ON public.words
  FOR DELETE USING (auth.uid() = user_id);

-- App usage policies
CREATE POLICY app_usage_select ON public.app_usage
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY app_usage_insert ON public.app_usage
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY app_usage_update ON public.app_usage
  FOR UPDATE USING (auth.uid() = user_id);

-- Quiz results policies
CREATE POLICY quiz_results_select ON public.quiz_results
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY quiz_results_insert ON public.quiz_results
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- History policies
CREATE POLICY history_select ON public.history
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY history_insert ON public.history
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Transactions policies
CREATE POLICY transactions_select ON public.transactions
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY transactions_insert ON public.transactions
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Pending transactions policies (any authenticated user can read)
CREATE POLICY pending_transactions_select ON public.pending_transactions
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY pending_transactions_insert ON public.pending_transactions
  FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Timestamps policies (any authenticated user can read)
CREATE POLICY timestamps_select ON public.timestamps
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY timestamps_insert ON public.timestamps
  FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Create necessary indexes for performance
CREATE INDEX idx_words_user_id ON public.words(user_id);
CREATE INDEX idx_app_usage_user_id ON public.app_usage(user_id);
CREATE INDEX idx_quiz_results_user_id ON public.quiz_results(user_id);
CREATE INDEX idx_history_user_id ON public.history(user_id);
CREATE INDEX idx_transactions_user_id ON public.transactions(user_id);
CREATE INDEX idx_words_user_id_word ON public.words(user_id, word);
CREATE INDEX idx_quiz_results_timestamp ON public.quiz_results(timestamp);
CREATE INDEX idx_history_timestamp ON public.history(timestamp); 