'use client';

import Link from 'next/link';
import { Bike, Menu, X, Zap, LogOut } from 'lucide-react';
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function Navbar() {
  const [isOpen, setIsOpen] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const router = useRouter();

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoggedIn(!!localStorage.getItem('legit_token'));
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('legit_token');
    localStorage.removeItem('legit_user');
    setIsLoggedIn(false);
    router.push('/login');
  };

  return (
    <nav className="fixed w-full z-50 bg-white/90 backdrop-blur-md border-b-2 border-slate-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-20">
          <Link href="/" className="flex items-center gap-2 group">
            <div className="p-2 bg-slate-950 text-white skew-x-[-12deg] group-hover:bg-rose-600 transition-colors">
               <Bike className="h-6 w-6 skew-x-[12deg]" />
            </div>
            <span className="text-2xl font-black tracking-tighter uppercase italic">
              Apex<span className="text-rose-600">Motors</span>
            </span>
          </Link>
          
          <div className="hidden md:block">
            <div className="ml-10 flex items-center space-x-12">
              <Link href="/" className="text-xs font-black uppercase tracking-[0.2em] hover:text-rose-600 transition-colors italic">The Grid</Link>
              <Link href="/demo" className="text-xs font-black uppercase tracking-[0.2em] hover:text-rose-600 transition-colors italic">ShieldNet</Link>
              <Link href="/dashboard" className="text-xs font-black uppercase tracking-[0.2em] hover:text-rose-600 transition-colors italic">Telemetry</Link>
              {isLoggedIn ? (
                <button 
                  onClick={handleLogout}
                  className="text-xs font-black uppercase tracking-[0.2em] hover:text-rose-600 transition-colors italic flex items-center gap-2"
                >
                  <LogOut size={14} /> Pit Exit
                </button>
              ) : (
                <Link href="/login" className="text-xs font-black uppercase tracking-[0.2em] hover:text-rose-600 transition-colors italic">Pit Access</Link>
              )}
              <Link href="/kyc" className="px-8 py-3 racing-gradient text-white text-xs font-black uppercase tracking-[0.2em] hover:opacity-90 transition-all shadow-xl shadow-rose-500/20 skew-x-[-12deg]">
                <span className="skew-x-[12deg] flex items-center gap-2">
                  <Zap size={14} fill="currentColor" /> SCRUTINEERING
                </span>
              </Link>
            </div>
          </div>

          <div className="md:hidden">
            <button onClick={() => setIsOpen(!isOpen)} className="text-slate-950 hover:text-rose-600">
              {isOpen ? <X className="h-8 w-8" /> : <Menu className="h-8 w-8" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {isOpen && (
        <div className="md:hidden bg-white border-b-2 border-slate-950 p-6 space-y-6">
          <Link href="/" className="block text-sm font-black uppercase tracking-widest italic" onClick={() => setIsOpen(false)}>The Grid</Link>
          <Link href="/dashboard" className="block text-sm font-black uppercase tracking-widest italic" onClick={() => setIsOpen(false)}>Telemetry</Link>
          <Link href="/kyc" className="block px-8 py-4 racing-gradient text-white text-sm font-black uppercase tracking-[0.2em] text-center skew-x-[-12deg]" onClick={() => setIsOpen(false)}>
            <span className="skew-x-[12deg]">SCRUTINEERING</span>
          </Link>
        </div>
      )}
    </nav>
  );
}


