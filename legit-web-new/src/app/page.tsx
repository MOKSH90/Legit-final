'use client';

import { useState } from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { 
  Microscope, 
  FileText, 
  Search, 
  Lock, 
  ArrowRight, 
  CheckCircle2, 
  Fingerprint,
  ShieldCheck,
  Cpu,
  KeyRound,
  FileSearch,
  Database
} from 'lucide-react';
import { useRouter } from 'next/navigation';

export default function ForensicLandingPage() {
  const [caseQuery, setCaseQuery] = useState('');
  const router = useRouter();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (caseQuery.trim()) {
      router.push(`/dashboard?search=${encodeURIComponent(caseQuery.trim())}`);
    } else {
      router.push('/dashboard');
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 selection:bg-cyan-500 selection:text-slate-950">
      {/* Hero Section */}
      <section className="relative pt-36 pb-20 px-4 sm:px-6 lg:px-8 overflow-hidden">
        {/* Background Grid Accent */}
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#0f172a_1px,transparent_1px),linear-gradient(to_bottom,#0f172a_1px,transparent_1px)] bg-[size:4rem_4rem] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)] opacity-40 pointer-events-none" />

        <div className="max-w-6xl mx-auto text-center relative z-10">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
          >
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 text-xs font-mono font-semibold uppercase tracking-widest mb-6">
              <Microscope size={14} /> Central Forensic Science Laboratory • Cyber & Digital Evidence Portal
            </div>

            <h1 className="text-4xl sm:text-6xl lg:text-7xl font-black uppercase tracking-tight text-white mb-6 leading-none">
              CENTRAL FORENSIC <br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 via-teal-300 to-blue-500">
                EVIDENCE ANALYSIS &amp;
              </span> <br />
              VERIFICATION WORKBENCH
            </h1>

            <p className="max-w-3xl mx-auto text-base sm:text-lg text-slate-300 font-normal leading-relaxed mb-10">
              An institutional zero-knowledge evidence workbench where field police officers dispatch FIRs, digital extractions, and forensic reports directly to forensic specialists. Evidence payloads remain sealed under officer-controlled confirmation contracts until access is explicitly authorized.
            </p>

            {/* Quick Case & Evidence Search Bar */}
            <form onSubmit={handleSearch} className="max-w-2xl mx-auto mb-12">
              <div className="relative flex items-center bg-slate-900 border-2 border-slate-700 hover:border-cyan-500/80 focus-within:border-cyan-500 rounded-xl p-2 shadow-2xl transition-all">
                <Search className="text-slate-400 ml-3 shrink-0" size={20} />
                <input
                  type="text"
                  value={caseQuery}
                  onChange={(e) => setCaseQuery(e.target.value)}
                  placeholder="Enter Case #, FIR Ref, or Submitting Officer Name (e.g. CR-2026/089)..."
                  className="w-full bg-transparent px-4 py-3 text-sm text-white placeholder-slate-500 outline-none font-medium"
                />
                <button
                  type="submit"
                  className="bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold px-6 py-3 rounded-lg text-xs uppercase tracking-wider transition-all shrink-0 flex items-center gap-2 shadow-lg shadow-cyan-500/20"
                >
                  Inspect Evidence <ArrowRight size={14} />
                </button>
              </div>
            </form>

            {/* Quick Action Navigation Buttons */}
            <div className="flex flex-wrap justify-center gap-4">
              <Link
                href="/dashboard"
                className="px-8 py-4 bg-gradient-to-r from-cyan-500 to-blue-600 text-slate-950 font-black text-xs uppercase tracking-widest rounded-lg hover:from-cyan-400 hover:to-blue-500 transition-all flex items-center gap-2 shadow-xl shadow-cyan-500/25"
              >
                <FileSearch size={16} /> Open Forensics Inbox
              </Link>
              <Link
                href="/audit"
                className="px-8 py-4 bg-slate-900 border border-slate-700 hover:border-slate-500 text-white font-bold text-xs uppercase tracking-widest rounded-lg transition-all flex items-center gap-2"
              >
                <Fingerprint size={16} className="text-cyan-400" /> Verify Cryptographic Hashes
              </Link>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Forensic Laboratory Metrics */}
      <section className="border-y border-slate-800 bg-slate-900/60 py-10 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto grid grid-cols-2 lg:grid-cols-4 gap-8">
          <div className="text-center sm:text-left border-r border-slate-800/80 last:border-0 pr-4">
            <p className="text-xs font-mono uppercase tracking-widest text-slate-400 mb-1">Evidence Dispatches</p>
            <h3 className="text-3xl font-black font-mono text-cyan-400">Direct Push</h3>
            <p className="text-[11px] text-slate-500 mt-1">From Officer Mobile Terminal</p>
          </div>
          <div className="text-center sm:text-left border-r border-slate-800/80 last:border-0 pr-4">
            <p className="text-xs font-mono uppercase tracking-widest text-slate-400 mb-1">Access Protocol</p>
            <h3 className="text-3xl font-black font-mono text-emerald-400">Officer Key</h3>
            <p className="text-[11px] text-slate-500 mt-1">Confirmation Contract Guarded</p>
          </div>
          <div className="text-center sm:text-left border-r border-slate-800/80 last:border-0 pr-4">
            <p className="text-xs font-mono uppercase tracking-widest text-slate-400 mb-1">Integrity Digest</p>
            <h3 className="text-3xl font-black font-mono text-blue-400">SHA-256</h3>
            <p className="text-[11px] text-slate-500 mt-1">Immutable Chain of Custody</p>
          </div>
          <div className="text-center sm:text-left pr-4">
            <p className="text-xs font-mono uppercase tracking-widest text-slate-400 mb-1">Legal Admissibility</p>
            <h3 className="text-3xl font-black font-mono text-teal-300">Sec. 65B</h3>
            <p className="text-[11px] text-slate-500 mt-1">Court Compliant Attestation</p>
          </div>
        </div>
      </section>

      {/* How Forensic Evidence Flow Works */}
      <section className="py-20 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="text-2xl sm:text-3xl font-black uppercase tracking-tight text-white mb-4">
            Officer-to-Forensics Zero-Knowledge Pipeline
          </h2>
          <p className="text-slate-400 text-sm">
            Eliminating unauthorized snooping and data tampering in police investigation workflows.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Card 1 */}
          <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 hover:border-cyan-500/40 transition-colors">
            <div className="p-3 bg-cyan-500/10 text-cyan-400 rounded-xl w-fit mb-6">
              <Cpu size={24} />
            </div>
            <h3 className="text-lg font-bold text-white uppercase tracking-wide mb-3">
              1. Officer Mobile Upload
            </h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              The investigating officer uploads FIRs, witness testimonies, or digital extractions using The Dedox Mobile App. The file is AES-256 encrypted on device and targeted to the Forensic Team.
            </p>
          </div>

          {/* Card 2 */}
          <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 hover:border-cyan-500/40 transition-colors">
            <div className="p-3 bg-emerald-500/10 text-emerald-400 rounded-xl w-fit mb-6">
              <KeyRound size={24} />
            </div>
            <h3 className="text-lg font-bold text-white uppercase tracking-wide mb-3">
              2. Confirmation Contract
            </h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              The evidence automatically shows up in the Forensics Workbench in a sealed state. The forensics team cannot decrypt the raw file until the officer confirms the access contract on their app.
            </p>
          </div>

          {/* Card 3 */}
          <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 hover:border-cyan-500/40 transition-colors">
            <div className="p-3 bg-blue-500/10 text-blue-400 rounded-xl w-fit mb-6">
              <ShieldCheck size={24} />
            </div>
            <h3 className="text-lg font-bold text-white uppercase tracking-wide mb-3">
              3. Forensic Attestation
            </h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Once unlocked, forensic specialists analyze digital hashes, verify cryptographic integrity against the ledger, and generate legally certified forensic reports for court submission.
            </p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-slate-800 bg-slate-950 py-12 px-4 sm:px-6 lg:px-8 text-center text-xs text-slate-500 font-mono">
        <p className="text-cyan-400 font-bold uppercase tracking-wider mb-2">
          CENTRAL FORENSIC SCIENCE LABORATORY (CFSL) • DIRECTORATE OF FORENSIC SCIENCE SERVICES
        </p>
        <p>Restricted to authorized Forensic Scientists &amp; Certified Law Enforcement Examiners. Governed under the IT Act 2000 &amp; Indian Evidence Act.</p>
      </footer>
    </div>
  );
}
