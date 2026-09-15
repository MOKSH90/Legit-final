'use client';

import { useSyncExternalStore, Suspense, useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { QRCodeSVG } from 'qrcode.react';
import {
  AlertTriangle,
  ArrowRight,
  Car,
  ChevronRight,
  Clock,
  QrCode,
  ShieldCheck,
  ShieldX,
  Trophy,
  Zap,
} from 'lucide-react';
import Link from 'next/link';
import dynamic from 'next/dynamic';
import { useSearchParams } from 'next/navigation';

const ThreeScene = dynamic(() => import('@/components/ThreeScene'), { ssr: false });

type Variant = 'trusted' | 'suspicious';

const inventory = [
  {
    name: 'Apex Predator',
    brand: 'F1 EDITION',
    price: '$2.4M',
    img: 'https://images.unsplash.com/photo-1592193660027-5831f5817260?auto=format&fit=crop&q=80&w=600',
  },
  {
    name: 'Red Phantom',
    brand: 'TRACK SPEC',
    price: '$1.8M',
    img: 'https://images.unsplash.com/photo-1544636331-e26879cd4d9b?auto=format&fit=crop&q=80&w=600',
  },
  {
    name: 'Nightshade GTR',
    brand: 'STREET LEGAL',
    price: '$1.2M',
    img: 'https://images.unsplash.com/photo-1614165939020-f71f168bd22f?auto=format&fit=crop&q=80&w=600',
  },
];

const suspiciousUrlPaths = [
  '/malicious-apex?source=qr&mirror_host=apexm0tors-secure-demo&tld=tk&intent=verify',
  '/malicious-apex?source=qr&mirror_host=vaultkey-update-secure&tld=ml&intent=login',
  '/malicious-apex?source=qr&mirror_host=legit-pipeline-verify&tld=ga&intent=confirm',
  '/malicious-apex?source=qr&mirror_host=apex-motors-security-check&tld=cf&intent=update',
  '/malicious-apex?source=qr&mirror_host=192.168.1.102&tld=none&intent=access'
];
const trustedUrlPath = '/dashboard?source=qr&trust=verified';

const noopSubscribe = () => () => {};
const getClientOrigin = () => window.location.origin;
const getServerOrigin = () => 'https://demo.apexmotors.local';

const trustedFeatures = [
  {
    icon: ShieldCheck,
    title: 'ENCRYPTED VAULT',
    desc: 'AES-256-GCM hardware-level encryption.',
  },
  {
    icon: Clock,
    title: 'INSTANT APPROVAL',
    desc: 'Real-time cryptographic verification.',
  },
  {
    icon: QrCode,
    title: 'SMART CONTRACTS',
    desc: 'Single-use QR codes for secure showroom entry.',
  },
];

const suspiciousFeatures = [
  {
    icon: ShieldX,
    title: 'MIRRORED CHECKOUT',
    desc: 'Lookalike QR endpoint routes visitors through an untrusted verification mirror.',
  },
  {
    icon: Clock,
    title: 'RUSH COUNTDOWN',
    desc: 'Artificial urgency reduces scrutiny before a scan or form entry.',
  },
  {
    icon: AlertTriangle,
    title: 'TRUST MISMATCH',
    desc: 'Brand styling looks familiar, but the QR destination carries risky trust signals.',
  },
];

function ApexLandingContent({ variant }: { variant: Variant }) {
  const isSuspicious = variant === 'suspicious';
  const origin = useSyncExternalStore(noopSubscribe, getClientOrigin, getServerOrigin);
  const searchParams = useSearchParams();
  const overrideOrigin = searchParams.get('origin');
  
  const [randomPath, setRandomPath] = useState(suspiciousUrlPaths[0]);

  useEffect(() => {
    if (isSuspicious) {
      const path = suspiciousUrlPaths[Math.floor(Math.random() * suspiciousUrlPaths.length)];
      setRandomPath(path);
    }
  }, [isSuspicious]);

  const effectiveOrigin = overrideOrigin || origin;

  const suspiciousUrl = `${effectiveOrigin}${randomPath}`;
  const trustedUrl = `${effectiveOrigin}${trustedUrlPath}`;

  return (
    <div
      className="flex min-h-screen flex-col text-slate-950 font-sans"
      data-site-variant={variant}
      data-risk-flags={
        isSuspicious
          ? 'lookalike_domain,non_https_qr_target,suspicious_tld,urgent_cta_language,trust_badge_mismatch'
          : 'none'
      }
      data-qr-target={isSuspicious ? suspiciousUrl : trustedUrl}
    >
      <section className="relative flex min-h-screen items-center overflow-visible pt-40 pb-32">
        <div className="absolute inset-0 -z-30 bg-white" />
        <div className="absolute inset-0 -z-20">
          <ThreeScene />
        </div>
        <div className="absolute left-1/4 top-1/2 -z-10 h-[600px] w-[600px] -translate-y-1/2 rounded-full bg-rose-600/5 blur-[120px]" />

        <div className="container mx-auto px-4 relative z-10">
          <div className="max-w-6xl">
            <motion.div
              initial={{ opacity: 0, x: -100 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 1, ease: 'circOut' }}
            >
              <div className="mb-12 inline-flex items-center gap-4 border-l-8 border-rose-600 bg-black px-4 py-2 text-xs font-black uppercase tracking-[0.5em] text-white italic">
                <Zap size={16} className="text-rose-600" fill="currentColor" />
                <span>{isSuspicious ? 'APEX MOTORS - PRIORITY MIRROR' : 'APEX MOTORS - FORMULA EDITION'}</span>
              </div>

              <h1 className="mb-12 text-7xl font-black uppercase italic tracking-tighter text-black md:text-[13rem] leading-[0.7]">
                DRIVE{' '}
                <span className="bg-gradient-to-r from-rose-600 via-red-500 to-red-900 bg-clip-text text-transparent drop-shadow-[0_10px_40px_rgba(220,38,38,0.5)]">
                  THE
                </span>
                <br />
                <span className="relative inline-block py-6">
                  <span className="relative z-10 bg-gradient-to-r from-rose-600 via-red-500 to-red-900 bg-clip-text text-transparent drop-shadow-[0_10px_40px_rgba(220,38,38,0.5)]">
                    FUTURE
                  </span>
                  <motion.span
                    initial={{ scaleX: 0 }}
                    animate={{ scaleX: 1 }}
                    transition={{ delay: 0.5, duration: 1.2, ease: 'circOut' }}
                    className="absolute bottom-4 left-0 -z-10 h-12 w-[110%] origin-left -skew-x-12 bg-rose-600 opacity-10"
                  />
                </span>
              </h1>

              <p className="mb-16 max-w-3xl border-l-[12px] border-black pl-10 text-3xl font-black uppercase tracking-tighter text-slate-700 italic leading-tight">
                {isSuspicious
                  ? 'Precision styling masks broken trust signals.'
                  : 'Precision engineering meets absolute security.'}
                <br />
                <span className="mt-4 block text-5xl text-rose-600">
                  {isSuspicious ? 'Same look. Wrong trust.' : 'Your supercar awaits.'}
                </span>
              </p>

              <div className="flex flex-col gap-4 sm:flex-row">
                <Link
                  href={isSuspicious ? '#mirror-qr' : '/dashboard'}
                  className="flex items-center justify-center gap-2 skew-x-[-12deg] bg-rose-600 px-10 py-5 text-xl font-black text-white shadow-2xl shadow-rose-600/40 transition-transform hover:scale-105"
                >
                  <span className="skew-x-[12deg] flex items-center gap-2">
                    {isSuspicious ? 'SCAN MIRROR' : 'COMMAND CENTER'}
                    <ArrowRight size={24} />
                  </span>
                </Link>
                <Link
                  href="/inventory"
                  className="flex items-center justify-center gap-2 skew-x-[-12deg] bg-black px-10 py-5 text-xl font-black text-white transition-colors hover:bg-slate-800"
                >
                  <span className="skew-x-[12deg]">{isSuspicious ? 'VIEW CLONED SHOWROOM' : 'VIEW SHOWROOM'}</span>
                </Link>
                <Link
                  href={isSuspicious ? '/' : '/malicious-apex'}
                  className="flex items-center justify-center gap-2 skew-x-[-12deg] border-2 border-black bg-white px-10 py-5 text-xl font-black transition-colors hover:border-rose-600 hover:text-rose-600"
                >
                  <span className="skew-x-[12deg]">{isSuspicious ? 'OPEN TRUSTED SITE' : 'RISK DEMO'}</span>
                </Link>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      <section className="overflow-hidden bg-black py-12 text-white">
        <div className="container mx-auto px-4">
          <div className="flex flex-wrap items-center justify-around gap-8 text-center">
            <div>
              <div className="text-4xl font-black text-rose-600">2.1s</div>
              <div className="text-xs font-bold uppercase tracking-[0.3em] text-slate-500">0-60 MPH</div>
            </div>
            <div className="hidden h-12 w-px bg-slate-800 md:block" />
            <div>
              <div className="text-4xl font-black text-rose-600">350+</div>
              <div className="text-xs font-bold uppercase tracking-[0.3em] text-slate-500">KPH TOP SPEED</div>
            </div>
            <div className="hidden h-12 w-px bg-slate-800 md:block" />
            <div>
              <div className="text-4xl font-black text-rose-600">{isSuspicious ? 'MIRROR' : 'THE DEDOX'}</div>
              <div className="text-xs font-bold uppercase tracking-[0.3em] text-slate-500">
                {isSuspicious ? 'TRUST SIGNAL DRIFT' : 'SECURE PIPELINE'}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="relative bg-white py-24">
        <div className="container mx-auto px-4">
          <div className="grid grid-cols-1 items-center gap-20 lg:grid-cols-2">
            <div className="relative">
              <div className="absolute -left-10 -top-10 h-40 w-40 rounded-full bg-rose-600/10 blur-3xl" />
              <div className="relative overflow-hidden border-[20px] border-black bg-black p-2">
                <img
                  src="https://images.unsplash.com/photo-1544724569-5f546fd6f2b5?auto=format&fit=crop&q=80&w=800"
                  alt="Supercar Detail"
                  className="w-full grayscale transition-all duration-700 hover:grayscale-0"
                />
                <div className="absolute bottom-0 right-0 translate-x-4 translate-y-4 skew-x-[-12deg] bg-rose-600 p-6 text-3xl font-black italic text-white">
                  {isSuspicious ? 'APEX_MIRROR' : 'APEX_V1'}
                </div>
              </div>
            </div>

            <div>
              <div className="mb-8 h-2 w-20 bg-rose-600" />
              <h2 className="mb-8 text-5xl font-black uppercase italic tracking-tighter">
                {isSuspicious ? 'Trust-Shift ' : 'Zero-Trust '}
                <span className="text-rose-600 italic">
                  {isSuspicious ? 'Detection' : 'Ownership'}
                </span>{' '}
                Verification
              </h2>
              <p className="mb-10 text-lg leading-relaxed text-slate-600">
                {isSuspicious
                  ? 'This mirror keeps the same presentation shell but swaps in low-trust QR and copy patterns so ShieldNet can classify it as risky.'
                  : 'Your identity is your most valuable asset. APEX Motors uses The Dedox Pipeline to ensure that your sensitive documents never leave your device. We verify your credentials in a secure vault and only receive the green light for your purchase.'}
              </p>

              <div className="space-y-4">
                {(isSuspicious ? suspiciousFeatures : trustedFeatures).map((feature) => (
                  <div
                    key={feature.title}
                    className="flex items-center gap-6 border-l-4 border-rose-600 bg-slate-50 p-6 transition-colors hover:bg-slate-100"
                    data-risk-card={isSuspicious ? feature.title.toLowerCase().replace(/\s+/g, '_') : undefined}
                  >
                    <feature.icon size={32} className="text-rose-600" />
                    <div>
                      <h4 className="text-sm font-black uppercase tracking-widest">{feature.title}</h4>
                      <p className="text-sm text-slate-500">{feature.desc}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="border-y border-slate-200 bg-slate-50 py-24">
        <div className="container mx-auto px-4">
          <div className="mb-16 flex flex-col items-baseline justify-between gap-4 md:flex-row">
            <h2 className="text-6xl font-black uppercase italic tracking-tighter">
              The <span className="text-rose-600">{isSuspicious ? 'Mirror' : 'Showroom'}</span>
            </h2>
            <Link
              href="/inventory"
              className="group flex items-center gap-2 font-black text-black transition-colors hover:text-rose-600"
            >
              {isSuspicious ? 'COMPARE INVENTORY' : 'VIEW ALL MODELS'}
              <ChevronRight size={20} className="transition-transform group-hover:translate-x-2" />
            </Link>
          </div>

          <div className="grid grid-cols-1 gap-0 overflow-hidden border border-black md:grid-cols-3">
            {inventory.map((car, i) => (
              <motion.div
                key={car.name}
                whileHover={{ backgroundColor: '#000', color: '#fff' }}
                className="border-r border-black bg-white p-8 transition-colors duration-300 last:border-r-0"
                data-clone-card={isSuspicious ? `mirror-card-${i + 1}` : undefined}
              >
                <div className="mb-8 aspect-video overflow-hidden bg-slate-100">
                  <img
                    src={car.img}
                    alt={car.name}
                    className="h-full w-full object-cover grayscale transition-all duration-500 hover:grayscale-0"
                  />
                </div>
                <div className="space-y-4">
                  <div>
                    <span className="text-xs font-black uppercase tracking-[0.2em] text-rose-600">{car.brand}</span>
                    <h3 className="text-3xl font-black uppercase italic leading-none">{car.name}</h3>
                  </div>
                  <div className="font-mono text-2xl font-black">{car.price}</div>
                  <Link
                    href={isSuspicious ? '/malicious-apex' : '/kyc'}
                    className="inline-block w-full bg-black py-4 text-center font-black uppercase tracking-widest text-white transition-colors hover:bg-rose-600"
                  >
                    {isSuspicious ? 'VERIFY RESERVATION' : 'REQUEST VERIFICATION'}
                  </Link>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {isSuspicious && (
        <section id="mirror-qr" className="bg-white py-24 scroll-mt-28">
          <div className="container mx-auto px-4">
            <div className="grid gap-12 lg:grid-cols-[0.95fr_1.05fr] lg:items-center">
              <div>
                <div className="mb-8 h-2 w-20 bg-rose-600" />
                <h2 className="mb-8 text-5xl font-black uppercase italic tracking-tighter">
                  Mirror <span className="text-rose-600">QR Target</span>
                </h2>
                <p className="mb-8 text-lg leading-relaxed text-slate-600">
                  This is the QR your Vaultkey app should scan during the presentation. It opens the suspicious twin route on the current host and preserves risky trust markers in the query string.
                </p>
                <div className="space-y-4">
                  {[
                    'Opens on the same deployed site, so the page actually resolves.',
                    'Carries `mirror_host=apexm0tors-secure-demo.tk` for your model features.',
                    'Keeps `transport=http` and `tld=tk` markers without depending on a dead external domain.',
                  ].map((point) => (
                    <div key={point} className="flex items-start gap-3 border-l-4 border-rose-600 bg-slate-50 p-5">
                      <AlertTriangle size={18} className="mt-0.5 shrink-0 text-rose-600" />
                      <span className="text-sm text-slate-600">{point}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="border-[12px] border-black bg-white p-8 shadow-2xl">
                <div className="mb-6 flex items-center justify-between gap-4 border-b border-slate-200 pb-4">
                  <div>
                    <p className="text-xs font-black uppercase tracking-[0.3em] text-slate-500">Presentation Scan</p>
                    <h3 className="text-2xl font-black uppercase italic">Suspicious QR</h3>
                  </div>
                  <QrCode className="text-rose-600" size={28} />
                </div>

                <div className="mb-6 flex justify-center bg-white p-4">
                  <QRCodeSVG value={suspiciousUrl} size={240} level="H" fgColor="#000000" includeMargin={true} />
                </div>

                <div className="border border-slate-200 bg-slate-50 p-4 font-mono text-[11px] leading-relaxed text-slate-500 break-all">
                  {suspiciousUrl}
                </div>
              </div>
            </div>
          </div>
        </section>
      )}

      <section className="overflow-hidden bg-white py-32">
        <div className="container mx-auto px-4">
          <div className="relative overflow-hidden bg-black p-12 text-center text-white md:p-24">
            <div className="carbon-pattern absolute left-0 top-0 h-full w-full opacity-20" />
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              className="relative z-10"
            >
              <h2 className="mb-8 text-6xl font-black uppercase italic leading-none md:text-8xl">
                {isSuspicious ? 'TRUST THE LOOK' : 'SECURE YOUR'}
                <br />
                <span className="text-rose-600">{isSuspicious ? 'CHECK THE LINK' : 'POLE POSITION'}</span>
              </h2>
              <p className="mx-auto mb-12 max-w-2xl text-xl font-medium text-slate-400">
                {isSuspicious
                  ? 'Use this clone route to show that identical styling is not enough. The model should focus on the trust surface, not the visuals.'
                  : 'Complete your high-value purchase verification with The Dedox and take the keys to your APEX supercar.'}
                <span className="mt-2 block text-white">
                  {isSuspicious ? 'Expected model verdict: block before user disclosure.' : 'Zero document exposure. Maximum speed.'}
                </span>
              </p>
              <Link
                href={isSuspicious ? '/malicious-apex' : '/kyc'}
                className="inline-block skew-x-[-12deg] bg-rose-600 px-16 py-6 text-2xl font-black text-white shadow-2xl shadow-rose-600/50 transition-colors hover:bg-red-500"
              >
                <span className="skew-x-[12deg] flex items-center gap-3">
                  {isSuspicious ? 'SCAN SUSPICIOUS QR' : 'START CONTRACT'}
                  <Trophy size={28} />
                </span>
              </Link>
            </motion.div>
          </div>
        </div>
      </section>

      <footer className="border-t border-slate-200 bg-slate-50 py-12">
        <div className="container mx-auto px-4 text-center">
          <div className="mb-4 flex items-center justify-center gap-2">
            <Car className="text-rose-600" />
            <span className="text-xl font-black uppercase italic tracking-tighter text-black">
              APEX<span className="text-rose-600">MOTORS</span>
            </span>
          </div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-slate-400">
            {isSuspicious ? 'CLONED TRUST SURFACE FOR SHIELDNET DEMO' : '© 2026 APEX RACING DEPT x THE DEDOX PIPELINE'}
          </p>
        </div>
      </footer>
    </div>
  );
}

export default function ApexLandingVariant({ variant }: { variant: Variant }) {
  return (
    <Suspense fallback={<div className="min-h-screen bg-white" />}>
      <ApexLandingContent variant={variant} />
    </Suspense>
  );
}
