'use client';

import { useState } from 'react';
import { ShieldCheck, User, Lock, ArrowLeft, Zap } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';

export default function LoginPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      const response = await api.auth.login(formData);
      
      // Determine where the token is (root or data object)
      const token = (response.data?.token || response.token) as string;
      const userData = response.data || response;
      if (!token) {
        throw new Error('Authentication successful but no token received.');
      }

      localStorage.setItem('legit_token', token);
      localStorage.setItem('legit_user', JSON.stringify(userData));
      
      // Redirect to dashboard on success
      router.push('/dashboard');
    } catch (err: unknown) {
      const error = err as Error;
      setError(error.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 pt-32 pb-12 px-4">
      <div className="max-w-md mx-auto">
        <Link href="/" className="inline-flex items-center gap-2 text-slate-500 hover:text-rose-600 font-bold mb-8 transition-colors uppercase tracking-widest text-xs">
          <ArrowLeft size={16} /> Back to Paddock
        </Link>

        <div className="bg-white border-t-8 border-slate-950 shadow-2xl p-8 md:p-12 relative overflow-hidden">
          <div className="absolute top-0 right-0 p-4 opacity-5">
            <Lock size={120} />
          </div>

          <div className="mb-12">
            <div className="flex items-center gap-2 text-rose-600 mb-2">
              <Zap size={20} fill="currentColor" />
              <span className="text-xs font-black uppercase tracking-[0.3em]">Secure Access</span>
            </div>
            <h1 className="text-4xl font-black uppercase italic tracking-tighter">Pit <span className="text-rose-600">Access</span></h1>
            <p className="text-slate-500 font-medium text-sm">Service Provider & Admin Authentication</p>
          </div>

          {error && (
            <div className="bg-rose-50 border-l-4 border-rose-600 p-4 mb-8">
              <p className="text-rose-600 text-xs font-bold uppercase tracking-widest">{error}</p>
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-6">
            <div>
              <label className="block text-xs font-black uppercase tracking-[0.2em] text-slate-400 mb-3">Email Address</label>
              <div className="relative">
                <User className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-300" size={20} />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  required
                  className="w-full bg-slate-50 border-2 border-slate-100 px-12 py-4 focus:border-rose-600 outline-none transition-colors font-bold"
                  placeholder="EMAIL@APEX.COM"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-black uppercase tracking-[0.2em] text-slate-400 mb-3">Pit Password</label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-300" size={20} />
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  required
                  className="w-full bg-slate-50 border-2 border-slate-100 px-12 py-4 focus:border-rose-600 outline-none transition-colors font-bold"
                  placeholder="••••••••"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-slate-950 text-white py-5 font-black uppercase tracking-widest hover:bg-rose-600 transition-colors disabled:opacity-50 skew-x-[-12deg] flex items-center justify-center gap-3"
            >
              <span className="skew-x-[12deg] flex items-center gap-2">
                {isLoading ? (
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>AUTHENTICATE <ShieldCheck size={20} /></>
                )}
              </span>
            </button>
          </form>

          <p className="mt-8 text-[10px] text-slate-400 font-bold tracking-widest uppercase italic text-center">
            Authorized Personnel Only — Unauthorized access is logged
          </p>
        </div>
      </div>
    </div>
  );
}
