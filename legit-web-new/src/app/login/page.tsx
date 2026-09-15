'use client';

import { useState } from 'react';
import { Microscope, User, Lock, ArrowLeft, ShieldCheck, KeyRound, Sparkles, CheckCircle2 } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';

const DEMO_CREDENTIALS = [
  {
    roleTitle: 'Forensic Specialist',
    email: 'forensics@cfsl.gov.in',
    password: 'Forensics@123',
    icon: '🔬',
    desc: 'CFSL Digital Evidence Examiner'
  },
  {
    roleTitle: 'Police Officer',
    email: 'officer@police.gov.in',
    password: 'Officer@123',
    icon: '👮',
    desc: 'Investigating Officer (IO)'
  },
  {
    roleTitle: 'Director / Admin',
    email: 'admin@cfsl.gov.in',
    password: 'Admin@123',
    icon: '🏛️',
    desc: 'CFSL Directorate Administrator'
  }
];

export default function LoginPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    email: 'forensics@cfsl.gov.in',
    password: 'Forensics@123',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const applyPreset = (preset: typeof DEMO_CREDENTIALS[0]) => {
    setFormData({
      email: preset.email,
      password: preset.password
    });
    setError('');
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      const response = await api.auth.login(formData);
      
      const token = (response.data?.token || response.token) as string;
      const userData = response.data || response;
      if (!token) {
        throw new Error('Authentication successful but no token received.');
      }

      localStorage.setItem('legit_token', token);
      localStorage.setItem('legit_user', JSON.stringify(userData));
      
      router.push('/dashboard');
    } catch (err: unknown) {
      const error = err as Error;
      setError(error.message || 'Authentication failed. Please verify credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 pt-32 pb-16 px-4 selection:bg-cyan-500 selection:text-slate-950">
      <div className="max-w-lg mx-auto">
        <Link 
          href="/" 
          className="inline-flex items-center gap-2 text-slate-400 hover:text-cyan-400 font-bold mb-8 transition-colors uppercase tracking-widest text-xs"
        >
          <ArrowLeft size={16} /> Return to Portal
        </Link>

        <div className="bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl p-8 md:p-10 relative overflow-hidden">
          <div className="mb-6">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded bg-cyan-500/10 text-cyan-400 border border-cyan-500/30 text-xs font-mono mb-3">
              <Microscope size={14} /> Central Forensic Science Laboratory (CFSL)
            </div>
            <h1 className="text-3xl font-black uppercase text-white">Analyst <span className="text-cyan-400">Authentication</span></h1>
            <p className="text-slate-400 text-xs mt-1">Certified Digital Examiners, Police Officers & Legal Directorate</p>
          </div>

          {/* Quick Demo Credential Presets */}
          <div className="mb-6 bg-slate-950 p-4 rounded-2xl border border-slate-800">
            <div className="flex items-center justify-between mb-2.5">
              <span className="text-[10px] font-mono uppercase text-cyan-400 font-bold flex items-center gap-1.5">
                <Sparkles size={12} /> Instant Demo Credentials (Click to Autofill)
              </span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
              {DEMO_CREDENTIALS.map((preset) => {
                const isSelected = formData.email === preset.email;
                return (
                  <button
                    key={preset.email}
                    type="button"
                    onClick={() => applyPreset(preset)}
                    className={`p-2.5 rounded-xl text-left border transition-all ${
                      isSelected
                        ? 'bg-cyan-500/15 border-cyan-500/50 text-white shadow-md'
                        : 'bg-slate-900 border-slate-800 hover:border-slate-700 text-slate-400'
                    }`}
                  >
                    <div className="text-sm font-bold flex items-center gap-1 text-white">
                      <span>{preset.icon}</span> <span className="text-xs truncate">{preset.roleTitle}</span>
                    </div>
                    <p className="text-[9px] font-mono text-cyan-400 mt-1 truncate">{preset.email}</p>
                  </button>
                );
              })}
            </div>
          </div>

          {error && (
            <div className="bg-rose-500/10 border border-rose-500/30 text-rose-400 p-4 rounded-xl mb-6 text-xs font-mono">
              {error}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-mono uppercase text-slate-400 mb-1.5">Officer / Analyst Email</label>
              <div className="relative">
                <User className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  required
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-12 pr-4 py-3 text-sm text-white font-mono placeholder-slate-600 outline-none focus:border-cyan-500"
                  placeholder="forensics@cfsl.gov.in"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-mono uppercase text-slate-400 mb-1.5">Access Password / PIN</label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  required
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-12 pr-4 py-3 text-sm text-white font-mono placeholder-slate-600 outline-none focus:border-cyan-500"
                  placeholder="••••••••"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 py-3.5 rounded-xl font-bold uppercase tracking-wider text-xs transition-all disabled:opacity-50 shadow-lg shadow-cyan-500/20 flex items-center justify-center gap-2 mt-2"
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
              ) : (
                <>Access Workbench <ShieldCheck size={18} /></>
              )}
            </button>
          </form>

          <p className="mt-6 text-[10px] text-slate-500 font-mono text-center leading-relaxed">
            RESTRICTED GOVERNMENT ACCESS • All login sessions are cryptographically signed and audited under the Indian Evidence Act.
          </p>
        </div>
      </div>
    </div>
  );
}
