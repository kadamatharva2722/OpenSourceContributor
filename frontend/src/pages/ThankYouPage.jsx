import React, { useState } from 'react';
import { useLocation, Link, Navigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { Button } from '../components/ui/Button';
import { CheckCircle2, ArrowRight, ShieldAlert, Terminal, Info, Copy, Check, Mail } from 'lucide-react';

export function ThankYouPage() {
  useDocumentTitle('Transmission Draft Staged');
  const location = useLocation();
  const submission = location.state;
  const [copied, setCopied] = useState(false);

  // Accessible ONLY following an actual form submission workflow in the client
  // Direct URL navigation without state is safely redirected to the Contact dispatch page
  if (!submission || !submission.submitted) {
    return <Navigate to={ROUTES.CONTACT} replace />;
  }

  const { refCode, topic, name, email, subject, message, timestamp } = submission;

  const fullTextPayload = [
    `OpenSourceContributor Transmission Receipt: ${refCode}`,
    `Timestamp: ${timestamp}`,
    `Topic: ${topic}`,
    `From: ${name} <${email}>`,
    `Subject: ${subject}`,
    `Message:\n${message || '(No payload body)'}`,
  ].join('\n');

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(fullTextPayload);
      setCopied(true);
      setTimeout(() => setCopied(false), 2400);
    } catch {
      // Fallback
    }
  };

  const mailtoUrl = `mailto:?subject=${encodeURIComponent(`[${topic}] ${subject}`)}&body=${encodeURIComponent(
    `From: ${name} <${email}>\nReference: ${refCode}\n\n${message || ''}`
  )}`;

  return (
    <div className="flex-1 flex items-center justify-center bg-[#0D0D0D] text-[#F1ECE2] px-4 py-16">
      <div className="w-full max-w-lg border-2 border-[#2E2E2E] bg-[#141414] p-6 md:p-8 space-y-6">
        <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3 text-xs font-mono">
          <div className="flex items-center space-x-2 text-[#F4C13D]">
            <Terminal size={16} />
            <span className="font-bold tracking-widest uppercase">DISPATCH RECEIPT</span>
          </div>
          <span className="text-[#A7A29A] text-[10px] uppercase">STAGED IN BROWSER</span>
        </div>

        <div className="space-y-2">
          <h1 className="font-heading text-3xl md:text-4xl uppercase tracking-tight text-[#F1ECE2]">
            TRANSMISSION DRAFT STAGED
          </h1>
          <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
            Your dispatch payload has been formatted and staged in this browser session.
          </p>
        </div>

        {/* Archival Receipt Box */}
        <div className="bg-[#1C1B1B] border border-[#2E2E2E] p-4 space-y-2 font-mono text-xs">
          <div className="flex justify-between py-1 border-b border-[#2A2A2A]">
            <span className="text-[#A7A29A]">RECEIPT REFERENCE:</span>
            <span className="text-[#F4C13D] font-bold">{refCode}</span>
          </div>
          <div className="flex justify-between py-1 border-b border-[#2A2A2A]">
            <span className="text-[#A7A29A]">CALL SIGN:</span>
            <span className="text-[#F1ECE2] font-semibold">{name}</span>
          </div>
          <div className="flex justify-between py-1 border-b border-[#2A2A2A]">
            <span className="text-[#A7A29A]">REPLY MAIL:</span>
            <span className="text-[#A7A29A]">{email}</span>
          </div>
          <div className="flex justify-between py-1 border-b border-[#2A2A2A]">
            <span className="text-[#A7A29A]">TOPIC:</span>
            <span className="text-[#5B86F2] font-bold uppercase">{topic}</span>
          </div>
          {subject && (
            <div className="flex justify-between py-1 border-b border-[#2A2A2A]">
              <span className="text-[#A7A29A]">SUBJECT:</span>
              <span className="text-[#D0CBC1] truncate max-w-[200px]">{subject}</span>
            </div>
          )}
          <div className="flex justify-between py-1">
            <span className="text-[#A7A29A]">TIMESTAMP:</span>
            <span className="text-[#A7A29A]">{new Date(timestamp).toLocaleTimeString()}</span>
          </div>
        </div>

        {/* Honest Delivery Disclaimer */}
        <div className="bg-[#1A1A1A] border-l-2 border-[#F4C13D] p-3 text-[11px] font-mono text-[#D0CBC1] space-y-1">
          <div className="flex items-center space-x-1.5 text-[#F4C13D] font-bold uppercase">
            <Info size={14} />
            <span>EXTERNAL DELIVERY PENDING PROVISIONING</span>
          </div>
          <p className="leading-relaxed text-[#A7A29A]">
            OpenSourceContributor does not operate an active server-side SMTP relay. This message has <strong>not</strong> been automatically transmitted across the network. Please copy your transmission payload or launch your email client below to send it to the maintainers.
          </p>
        </div>

        {/* Actions to dispatch message */}
        <div className="flex flex-col sm:flex-row gap-2.5 font-mono text-xs">
          <a href={mailtoUrl} className="flex-1">
            <Button variant="primary" size="sm" className="w-full flex items-center justify-center gap-1.5">
              <Mail size={14} />
              <span>OPEN IN EMAIL CLIENT</span>
            </Button>
          </a>
          <Button
            variant="secondary"
            size="sm"
            onClick={handleCopy}
            className="flex-1 flex items-center justify-center gap-1.5"
          >
            {copied ? <Check size={14} className="text-[#4ADE80]" /> : <Copy size={14} />}
            <span>{copied ? 'PAYLOAD COPIED' : 'COPY DISPATCH'}</span>
          </Button>
        </div>

        <div className="pt-2 border-t border-[#2E2E2E] flex flex-col sm:flex-row gap-3">
          <Link to={ROUTES.EXPLORE} className="flex-1">
            <Button variant="outline" size="sm" className="w-full">
              EXPLORE OPEN ISSUES <ArrowRight size={14} className="ml-1.5" />
            </Button>
          </Link>
          <Link to={ROUTES.HOME} className="flex-1">
            <Button variant="secondary" size="sm" className="w-full">
              RETURN HOME
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}

export default ThankYouPage;
