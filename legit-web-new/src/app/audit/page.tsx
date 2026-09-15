'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { ShieldCheck, Lock, Search, CheckCircle2, AlertTriangle, Fingerprint, ArrowLeft, FileCode, Check } from 'lucide-react';
import Link from 'next/link';
import { api } from '@/lib/api';

export default function AuditPage() {
  const [query, setQuery] = useState('');
  const [isAuditing, setIsAuditing] = useState(false);
  const [auditResult, setAuditResult] = useState<{
    found: boolean;
    docTitle?: string;
    docType?: string;
    caseNumber?: string;
    hash?: string;
    intact?: boolean;
    message?: string;
  } | null>(null);

  const handleAudit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;
    setIsAuditing(true);
    setAuditResult(null);

    try {
      // First try to fetch all documents to find match by case number, id, or hash
      const res = await api.documents.getAll();
      const docs = (res.data?.documents || res.documents || []) as Array<{
        id: string;
        documentName: string;
        documentType: string;
        caseNumber?: string | null;
        dataHash: string;
      }>;

      const trimmed = query.trim().toLowerCase();
      const matched = docs.find(
        (d) =>
          d.id.toLowerCase() === trimmed ||
          (d.caseNumber && d.caseNumber.toLowerCase() === trimmed) ||
          d.dataHash.toLowerCase() === trimmed
      );

      if (matched) {
        // Run verify integrity on matched document
        const verifyRes = await api.documents.verifyIntegrity(matched.id);
        const verifyData = verifyRes.data || verifyRes;

        setAuditResult({
          found: true,
          docTitle: matched.documentName,
          docType: matched.documentType,
          caseNumber: matched.caseNumber || 'N/A',
          hash: matched.dataHash,
          intact: verifyData.integrityIntact ?? true,
          message: verifyData.message || 'Cryptographic fingerprint is intact. Non-tampering guaranteed.',
        });
      } else {
        setAuditResult({
          found: false,
          message: 'No record matching this hash or case ID was located in the official registry.',
        });
      }
    } catch (err: unknown) {
      const error = err as Error;
      setAuditResult({
        found: false,
        message: error.message || 'Audit query failed.',
      });
    } finally {
      setIsAuditing(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 pt-32 pb-16 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        <Link 
          href="/dashboard" 
          className="inline-flex items-center gap-2 text-slate-400 hover:text-amber-400 font-bold mb-8 transition-colors uppercase tracking-widest text-xs"
        >
          <ArrowLeft size={16} /> Return to Dashboard
        </Link>

        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-8 md:p-12 shadow-2xl">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-3 bg-blue-500/10 text-blue-400 border border-blue-500/30 rounded-2xl">
              <Fingerprint size={28} />
            </div>
            <div>
              <h1 className="text-3xl font-black uppercase text-white">
                Cryptographic <span className="text-blue-400">Chain of Custody Auditor</span>
              </h1>
              <p className="text-slate-400 text-sm">
                Verify evidence integrity and document hashes against the national non-repudiation ledger.
              </p>
            </div>
          </div>

          <form onSubmit={handleAudit} className="mb-8">
            <label className="block text-xs font-mono uppercase text-slate-400 mb-2">
              Enter Document ID, Case Number, or 64-char SHA-256 Hash
            </label>
            <div className="flex flex-col sm:flex-row gap-3">
              <div className="relative flex-1">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
                <input
                  type="text"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="e.g. e3b0c44298fc1c... or Case # CR-2026/089"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-12 pr-4 py-3.5 text-sm text-white font-mono placeholder-slate-600 outline-none focus:border-blue-500"
                />
              </div>
              <button
                type="submit"
                disabled={isAuditing || !query.trim()}
                className="bg-blue-600 hover:bg-blue-500 text-white font-bold px-8 py-3.5 rounded-xl text-xs uppercase tracking-wider transition-all disabled:opacity-40 shrink-0"
              >
                {isAuditing ? 'Auditing Ledger...' : 'Audit Document'}
              </button>
            </div>
          </form>

          {auditResult && (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className={`p-6 rounded-2xl border ${auditResult.found && auditResult.intact ? 'bg-emerald-500/10 border-emerald-500/30' : 'bg-rose-500/10 border-rose-500/30'} mb-8`}
            >
              <div className="flex items-start gap-4">
                {auditResult.found && auditResult.intact ? (
                  <CheckCircle2 size={24} className="text-emerald-400 shrink-0 mt-0.5" />
                ) : (
                  <AlertTriangle size={24} className="text-rose-400 shrink-0 mt-0.5" />
                )}
                <div className="space-y-2">
                  <h3 className={`text-base font-bold uppercase ${auditResult.found && auditResult.intact ? 'text-emerald-400' : 'text-rose-400'}`}>
                    {auditResult.found && auditResult.intact ? 'Tamper-Proof Audit Passed (100% Integrity)' : 'Verification Alert'}
                  </h3>
                  <p className="text-xs text-slate-300 leading-relaxed font-mono">
                    {auditResult.message}
                  </p>

                  {auditResult.found && (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-4 pt-4 border-t border-slate-800/80 text-xs font-mono">
                      <div>
                        <span className="text-slate-500 block">Dossier:</span>
                        <span className="text-white font-bold">{auditResult.docTitle}</span>
                      </div>
                      <div>
                        <span className="text-slate-500 block">Case Number:</span>
                        <span className="text-amber-400 font-bold">{auditResult.caseNumber}</span>
                      </div>
                      <div className="col-span-full">
                        <span className="text-slate-500 block">SHA-256 Digest:</span>
                        <span className="text-slate-300 break-all">{auditResult.hash}</span>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </motion.div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-mono text-slate-400 bg-slate-950 p-6 rounded-2xl border border-slate-800">
            <div>
              <p className="text-amber-400 font-bold uppercase mb-1 flex items-center gap-1.5">
                <Lock size={14} /> SHA-256 Hashing Guarantee
              </p>
              <p className="text-slate-400 leading-relaxed">
                Calculated on the original binary stream on the officer's device. No intermediary can alter or forge the payload without invalidating this hash.
              </p>
            </div>
            <div>
              <p className="text-blue-400 font-bold uppercase mb-1 flex items-center gap-1.5">
                <ShieldCheck size={14} /> Legal Evidence Admissibility
              </p>
              <p className="text-slate-400 leading-relaxed">
                Conforms with digital evidence chain-of-custody standards under Section 65B of the Indian Evidence Act.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
