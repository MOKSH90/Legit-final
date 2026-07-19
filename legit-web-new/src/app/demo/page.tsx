'use client';

import { useState, Suspense, useSyncExternalStore, useEffect } from 'react';
import { motion } from 'framer-motion';
import { QRCodeSVG } from 'qrcode.react';
import { 
  ShieldCheck, 
  ShieldAlert, 
  Zap, 
  ArrowRight, 
  AlertTriangle,
  Fingerprint,
  Lock,
  Globe,
  Cpu
} from 'lucide-react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';

const noopSubscribe = () => () => {};
const getClientOrigin = () => window.location.origin;
const getServerOrigin = () => 'https://demo.apexmotors.local';

function DemoContent() {
  const origin = useSyncExternalStore(noopSubscribe, getClientOrigin, getServerOrigin);
  const searchParams = useSearchParams();
  const overrideOrigin = searchParams.get('origin');
  const effectiveOrigin = overrideOrigin || origin;

  const [liveUrl, setLiveUrl] = useState('');
  const [threatType, setThreatType] = useState('');
  const [staticMaliciousUrl, setStaticMaliciousUrl] = useState('');

  const trustedUrl = `${effectiveOrigin}/dashboard?source=qr&trust=verified`;

  // Randomize static attack path on refresh
  useEffect(() => {
    const threats = [
      { host: 'apexm0tors-secure-demo', tld: 'tk' },
      { host: 'vault-update-now', tld: 'ml' },
      { host: 'legit-verify-login', tld: 'ga' }
    ];
    const t = threats[Math.floor(Math.random() * threats.length)];
    setStaticMaliciousUrl(`${effectiveOrigin}/malicious-apex?source=qr&mirror_host=${t.host}&tld=${t.tld}&intent=block`);
  }, [effectiveOrigin]);

  // Live Threat Generator Logic
  useEffect(() => {
    const threats = [
      { type: 'Typosquatting', host: 'apexmot0rs-login', tld: 'tk' },
      { type: 'Subdomain Spam', host: 'verify.secure.vault.apexmotors', tld: 'ga' },
      { type: 'IP Cloaking', host: '192.168.1.45', tld: '' },
      { type: 'Homoglyph', host: 'apexmors-secure', tld: 'ml' },
      { type: 'Urgency Phish', host: 'update-your-vaultkey-now', tld: 'xyz' },
      { type: 'Mirror Clone', host: 'legit-pipeline-verification', tld: 'cf' }
    ];

    const generate = () => {
      const threat = threats[Math.floor(Math.random() * threats.length)];
      const randomId = Math.random().toString(16).substring(2, 10);
      const url = `${effectiveOrigin}/malicious-apex?source=live_demo&mirror_host=${threat.host}&tld=${threat.tld}&id=${randomId}`;
      setLiveUrl(url);
      setThreatType(threat.type);
    };

    generate();
    const interval = setInterval(generate, 8000);
    return () => clearInterval(interval);
  }, [effectiveOrigin]);

  return (
    <div className="min-h-screen bg-slate-50 pt-32 pb-20">
      <div className="max-w-7xl mx-auto px-4">
        {/* ... existing header ... */}
        <div className="mb-12">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-rose-600 text-white text-[10px] font-black uppercase tracking-widest skew-x-[-12deg] mb-6">
            <span className="skew-x-[12deg]">ShieldNet ML Demo Center</span>
          </div>
          <h1 className="text-6xl font-black uppercase italic tracking-tighter mb-4">
            Zero-Trust <span className="text-rose-600">Verification</span>
          </h1>
          <p className="text-slate-500 max-w-2xl font-medium leading-relaxed">
            Test the integrated ShieldNet ML engine by scanning the QR codes below. 
            The system analyzes trust signals in real-time to protect users from phishing and malicious redirects.
          </p>
        </div>

        <div className="grid md:grid-cols-2 gap-8 mb-8">
          {/* ... existing Trusted/Attack cards ... */}
          {/* Trusted Case */}
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-white border-4 border-slate-950 p-8 relative overflow-hidden group"
          >
            <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
              <ShieldCheck size={120} />
            </div>
            
            <div className="relative z-10">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-3 bg-emerald-100 text-emerald-600 rounded-xl">
                  <ShieldCheck size={28} />
                </div>
                <div>
                  <h3 className="text-2xl font-black uppercase italic">Trusted Path</h3>
                  <p className="text-[10px] font-bold text-emerald-600 uppercase tracking-widest">Normal Operations</p>
                </div>
              </div>

              <div className="bg-slate-50 p-6 flex justify-center mb-8 border-2 border-slate-100">
                <QRCodeSVG value={trustedUrl} size={240} level="H" includeMargin={true} />
              </div>

              <div className="space-y-4 mb-8">
                <div className="flex gap-3 items-start">
                  <div className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center shrink-0 mt-1">
                    <div className="w-2 h-2 rounded-full bg-emerald-500" />
                  </div>
                  <p className="text-sm font-medium text-slate-600">Resolves to legitimate dashboard endpoint</p>
                </div>
                <div className="flex gap-3 items-start">
                  <div className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center shrink-0 mt-1">
                    <div className="w-2 h-2 rounded-full bg-emerald-500" />
                  </div>
                  <p className="text-sm font-medium text-slate-600">Clean domain signals and valid SSL parameters</p>
                </div>
              </div>

              <div className="p-4 bg-emerald-50 border-l-4 border-emerald-500 font-mono text-[10px] text-emerald-800 break-all mb-8">
                {trustedUrl}
              </div>

              <button className="w-full py-4 bg-slate-950 text-white font-black uppercase tracking-widest hover:bg-rose-600 transition-colors skew-x-[-12deg]">
                <span className="skew-x-[12deg] flex items-center justify-center gap-2">
                  EXPECTED: ALLOWED <ArrowRight size={18} />
                </span>
              </button>
            </div>
          </motion.div>

          {/* Malicious Case */}
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="bg-white border-4 border-rose-600 p-8 relative overflow-hidden group"
          >
            <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
              <ShieldAlert size={120} className="text-rose-600" />
            </div>

            <div className="relative z-10">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-3 bg-rose-100 text-rose-600 rounded-xl">
                  <ShieldAlert size={28} />
                </div>
                <div>
                  <h3 className="text-2xl font-black uppercase italic">Attack Path</h3>
                  <p className="text-[10px] font-bold text-rose-600 uppercase tracking-widest">Malicious Simulation</p>
                </div>
              </div>

              <div className="bg-slate-50 p-6 flex justify-center mb-8 border-2 border-rose-100">
                <QRCodeSVG value={staticMaliciousUrl} size={240} level="H" includeMargin={true} />
              </div>

              <div className="space-y-4 mb-8">
                <div className="flex gap-3 items-start">
                  <div className="w-5 h-5 rounded-full bg-rose-500/20 flex items-center justify-center shrink-0 mt-1">
                    <div className="w-2 h-2 rounded-full bg-rose-500" />
                  </div>
                  <p className="text-sm font-medium text-slate-600">Uses suspicious domain patterns</p>
                </div>
                <div className="flex gap-3 items-start">
                  <div className="w-5 h-5 rounded-full bg-rose-500/20 flex items-center justify-center shrink-0 mt-1">
                    <div className="w-2 h-2 rounded-full bg-rose-500" />
                  </div>
                  <p className="text-sm font-medium text-slate-600">ShieldNet analyzes patterns in real-time</p>
                </div>
              </div>

              <div className="p-4 bg-rose-50 border-l-4 border-rose-600 font-mono text-[10px] text-rose-800 break-all mb-8">
                {staticMaliciousUrl}
              </div>

              <button className="w-full py-4 bg-rose-600 text-white font-black uppercase tracking-widest hover:bg-rose-700 transition-colors skew-x-[-12deg]">
                <span className="skew-x-[12deg] flex items-center justify-center gap-2">
                  EXPECTED: BLOCKED <AlertTriangle size={18} />
                </span>
              </button>
            </div>
          </motion.div>
        </div>

        {/* Live Simulation Section */}
        <div className="mb-12">
          <motion.div 
            key={liveUrl}
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-slate-900 text-white p-8 rounded-3xl border-4 border-slate-950 shadow-2xl relative overflow-hidden"
          >
            <div className="absolute top-0 right-0 w-64 h-64 bg-rose-600/10 blur-[100px] -translate-y-1/2 translate-x-1/2" />
            
            <div className="grid md:grid-cols-[1.5fr_1fr] gap-12 items-center">
              <div>
                <div className="flex items-center gap-3 mb-6">
                  <div className="w-12 h-12 bg-rose-600 flex items-center justify-center rounded-xl skew-x-[-12deg]">
                    <Zap className="skew-x-[12deg]" size={24} fill="currentColor" />
                  </div>
                  <div>
                    <h3 className="text-3xl font-black uppercase italic leading-none">Live Simulation</h3>
                    <p className="text-[10px] font-bold text-rose-500 uppercase tracking-[0.3em] mt-1">Automatic Threat Mutation Engine</p>
                  </div>
                </div>

                <div className="space-y-6 mb-10">
                  <div className="flex items-center gap-4">
                    <div className="px-3 py-1 bg-rose-600/20 border border-rose-600/30 text-rose-500 text-[10px] font-black uppercase tracking-widest rounded-full">
                      Current Vector: {threatType}
                    </div>
                    <div className="flex gap-1">
                      {[1,2,3].map(i => <div key={i} className="w-1 h-1 bg-rose-600 rounded-full animate-pulse" style={{animationDelay: `${i*0.2}s`}} />)}
                    </div>
                  </div>
                  
                  <p className="text-slate-400 font-medium leading-relaxed max-w-xl">
                    Every 8 seconds, this card generates a new malicious pattern. 
                    This proves ShieldNet uses a real <b>Neural Network</b> to analyze the 
                    underlying structure of the link, not just a static list.
                  </p>
                </div>

                <div className="p-5 bg-white/5 border border-white/10 rounded-2xl font-mono text-[11px] text-slate-400 break-all">
                  <span className="text-rose-500 mr-2 font-black uppercase tracking-widest">Target:</span>
                  {liveUrl}
                </div>
              </div>

              <div className="flex flex-col items-center">
                <div className="bg-white p-6 rounded-3xl shadow-2xl shadow-rose-600/20 border-4 border-rose-600/50">
                  <QRCodeSVG value={liveUrl} size={220} level="H" includeMargin={true} />
                </div>
                <p className="mt-4 text-[10px] font-black uppercase tracking-[0.4em] text-rose-500 animate-pulse">
                  Scanning for live mutation...
                </p>
              </div>
            </div>
          </motion.div>
        </div>

        <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white p-6 border-2 border-slate-200">
            <Fingerprint className="text-rose-600 mb-4" size={32} />
            <h4 className="font-black uppercase italic text-sm mb-2">Lexical Analysis</h4>
            <p className="text-xs text-slate-500">ShieldNet analyzes character entropy and consecutive consonants to find random-looking malicious hosts.</p>
          </div>
          <div className="bg-white p-6 border-2 border-slate-200">
            <Globe className="text-rose-600 mb-4" size={32} />
            <h4 className="font-black uppercase italic text-sm mb-2">TLD Integrity</h4>
            <p className="text-xs text-slate-500">Domains ending in high-risk TLDs like .tk or .ga receive immediate reputation penalties in our ML model.</p>
          </div>
          <div className="bg-white p-6 border-2 border-slate-200">
            <Cpu className="text-rose-600 mb-4" size={32} />
            <h4 className="font-black uppercase italic text-sm mb-2">TFLite Inference</h4>
            <p className="text-xs text-slate-500">Full neural network classification runs on-device without leaking your browsing history to any server.</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function DemoPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-slate-50 pt-32" />}>
      <DemoContent />
    </Suspense>
  );
}
