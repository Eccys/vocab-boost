import { createClient } from '@supabase/supabase-js';

// These values should be replaced with your actual Supabase URL and anon key
// You can find these in your Supabase project dashboard
const supabaseUrl = process.env.VITE_SUPABASE_URL || 'https://your-project-url.supabase.co';
const supabaseAnonKey = process.env.VITE_SUPABASE_ANON_KEY || 'your-anon-key';

// Create a single supabase client for interacting with your database
export const supabase = createClient(supabaseUrl, supabaseAnonKey); 