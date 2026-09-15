'use client';

import Link from 'next/link';
import { Microscope, Menu, X, LogOut, FileText, Lock, Fingerprint, ShieldCheck } from 'lucide-react';
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function Navbar() {
  const [isOpen, setIsOpen] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userRole, setUserRole] = useState<string>('');
  const [userName, setUserName] = useState<string>('');
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('legit_token');
    const userStr = localStorage.getItem('legit_user');
    setIsLoggedIn(!!token);
    if (userStr) {
      try {
        const parsed = JSON.parse(userStr);
        setUserRole(parsed.role || 'FORENSICS_SPECIALIST');
        setUserName(parsed.fullName || parsed.username || 'Analyst');
      } catch (e) {
        console.error(e);
      }
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('legit_token');
    localStorage.removeItem('legit_user');
    setIsLoggedIn(false);
    setUserRole('');
    setUserName('');
    router.push('/login');
  };

  return (
    <header className="fixed w-full z-50 bg-slate-900/95 backdrop-blur-md border-b border-slate-800 text-white shadow-2xl">
      {/* Top Gov Forensic Ribbon */}
      <div className="bg-slate-950 px-4 sm:px-8 py-1.5 flex justify-between items-center text-[10px] uppercase tracking-wider font-semibold border-b border-slate-800/80 text-slate-400">
        <div className="flex items-center gap-3">
          <span className="flex items-center gap-1.5 text-cyan-400 font-mono font-bold">
            <Microscope size={13} /> CENTRAL FORENSIC SCIENCE LABORATORY (CFSL)
          </span>
          <span className="hidden sm:inline text-slate-700">|</span>
          <span className="hidden sm:inline text-slate-400">CYBER & DIGITAL FORENSICS EXAMINATION DIRECTORATE</span>
        </div>
        <div className="flex items-center gap-3 text-emerald-400 font-mono">
          <span className="inline-block w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          OFFICER CONFIRMATION PROTOCOL ACTIVE
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-20">
          <Link href="/dashboard" className="flex items-center gap-3 group">
            <div className="p-2.5 bg-gradient-to-br from-cyan-500 to-blue-700 rounded-xl shadow-lg text-slate-950 group-hover:scale-105 transition-transform">
              <Microscope className="h-6 w-6 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl font-black tracking-tight uppercase text-white">
                  CFSL <span className="text-cyan-400 font-bold">FORENSICS</span>
                </span>
                <span className="text-[9px] bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 px-1.5 py-0.5 rounded font-mono font-bold">
                  PORTAL
                </span>
              </div>
              <p className="text-[10px] tracking-wide text-slate-400 font-medium uppercase">
                Digital Evidence & Forensic Analysis Workbench
              </p>
            </div>
          </Link>
          
          <div className="hidden md:block">
            <div className="ml-10 flex items-center space-x-8">
              <Link 
                href="/dashboard" 
                className="text-xs font-bold uppercase tracking-widest text-slate-300 hover:text-cyan-400 transition-colors flex items-center gap-1.5"
              >
                <FileText size={14} className="text-cyan-400" /> Evidence Inbox
              </Link>
              <Link 
                href="/audit" 
                className="text-xs font-bold uppercase tracking-widest text-slate-300 hover:text-cyan-400 transition-colors flex items-center gap-1.5"
              >
                <Fingerprint size={14} className="text-blue-400" /> Chain of Custody Auditor
              </Link>

              {isLoggedIn ? (
                <div className="flex items-center gap-4 pl-4 border-l border-slate-700">
                  <div className="flex flex-col text-right">
                    <span className="text-xs font-bold text-slate-200">{userName}</span>
                    <span className="text-[9px] font-mono text-cyan-400 uppercase">
                      {userRole.replace('_', ' ')}
                    </span>
                  </div>
                  <button 
                    onClick={handleLogout}
                    className="text-xs font-bold uppercase tracking-widest text-rose-400 hover:text-rose-300 transition-colors flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-rose-500/10 border border-rose-500/30 hover:bg-rose-500/20"
                  >
                    <LogOut size={13} /> Exit
                  </button>
                </div>
              ) : (
                <Link 
                  href="/login" 
                  className="px-5 py-2.5 bg-gradient-to-r from-cyan-500 to-blue-600 text-slate-950 font-bold text-xs uppercase tracking-widest rounded-lg hover:from-cyan-400 hover:to-blue-500 transition-all shadow-lg shadow-cyan-500/20"
                >
                  Forensic Analyst Login
                </Link>
              )}
            </div>
          </div>

          <div className="md:hidden">
            <button onClick={() => setIsOpen(!isOpen)} className="text-slate-300 hover:text-cyan-400 p-2">
              {isOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {isOpen && (
        <div className="md:hidden bg-slate-950 border-b border-slate-800 p-6 space-y-4">
          <Link 
            href="/dashboard" 
            className="block text-sm font-bold uppercase tracking-widest text-slate-200" 
            onClick={() => setIsOpen(false)}
          >
            Evidence Inbox
          </Link>
          <Link 
            href="/audit" 
            className="block text-sm font-bold uppercase tracking-widest text-slate-200" 
            onClick={() => setIsOpen(false)}
          >
            Chain of Custody Auditor
          </Link>
          {isLoggedIn ? (
            <button 
              onClick={() => { setIsOpen(false); handleLogout(); }}
              className="w-full py-3 bg-rose-600/20 text-rose-400 border border-rose-500/30 rounded-lg font-bold text-xs uppercase tracking-widest text-center"
            >
              Exit Session
            </button>
          ) : (
            <Link 
              href="/login" 
              className="block w-full py-3 bg-cyan-500 text-slate-950 font-bold text-xs uppercase tracking-widest text-center rounded-lg" 
              onClick={() => setIsOpen(false)}
            >
              Forensic Analyst Login
            </Link>
          )}
        </div>
      )}
    </header>
  );
}
