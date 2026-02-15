import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import axios from 'axios';

// הגדרת כתובת השרת שלך
const API_URL = 'http://localhost:8080/api/auth'; 

// הגדרת מבנה המשתמש (התאימי את זה למה שחוזר מה-Java שלך)
interface User {
  email: string;
  id: string;
  _id?: string;
  token?: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<{ error: any }>;
  signUp: (email: string, password: string) => Promise<{ error: any }>;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // בדיקה אם המשתמש כבר מחובר (שמור בדפדפן)
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }
    setLoading(false);
  }, []);

  const signIn = async (email: string, password: string) => {
  try {
    const response = await axios.post(`${API_URL}/login`, { email, password });
    const userData = response.data;
    
    setUser(userData);
    localStorage.setItem('user', JSON.stringify(userData));
    return { error: null };
  } catch (err: any) {
    // Axios תופס כאן את השגיאה מה-Java
    return { 
      error: { 
        message: err.response?.data || 'פרטי התחברות שגויים' 
      } 
    };
  }
};

  const signUp = async (email: string, password: string) => {
    try {
      const response = await axios.post(`${API_URL}/signup`, { email, password });
      let userData = response.data;
      if (userData._id && !userData.id) {
        userData = { ...userData, id: userData._id };
    }
      setUser(userData);
      localStorage.setItem('user', JSON.stringify(userData));
      
      return { error: null };
    } catch (err: any) {
      return { 
        error: { 
          message: err.response?.data?.message || 'Registration failed' 
        } 
      };
    }
  };

  const signOut = () => {
    setUser(null);
    localStorage.removeItem('user');
  };

  return (
    <AuthContext.Provider value={{ user, loading, signIn, signUp, signOut }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};