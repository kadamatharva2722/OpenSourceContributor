import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { Button } from '../components/ui/Button';
import { apiClient } from '../services/apiClient';
import { Terminal, Send, Mail, MessageSquare, Shield, CheckCircle2, Info, AlertTriangle } from 'lucide-react';

export function ContactPage() {
  useDocumentTitle('Contact');
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    email: '',
    topic: 'REPO_INDEXING',
    subject: '',
    message: '',
  });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = {};
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!form.name.trim()) errs.name = 'CALL SIGN / NAME IS REQUIRED';
    if (!form.email.trim() || !emailRegex.test(form.email.trim())) errs.email = 'VALID EMAIL IS REQUIRED';
    if (!form.subject.trim()) errs.subject = 'TRANSMISSION SUBJECT IS REQUIRED';
    if (!form.message.trim() || form.message.length < 10) errs.message = 'MESSAGE MUST BE AT LEAST 10 CHARACTERS';

    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }

    setSubmitting(true);
    setErrors({});

    try {
      const response = await apiClient('/api/v1/contact', {
        method: 'POST',
        body: {
          name: form.name.trim(),
          email: form.email.trim(),
          topic: form.topic,
          subject: form.subject.trim(),
          message: form.message.trim(),
        },
      });

      const submissionId = response?.referenceCode || `OSC-${Date.now().toString(36).toUpperCase()}`;
      navigate(ROUTES.THANK_YOU, {
        state: {
          submitted: true,
          refCode: submissionId,
          topic: form.topic,
          name: form.name.trim(),
          email: form.email.trim(),
          subject: form.subject.trim(),
          message: form.message.trim(),
          timestamp: response?.createdAt || new Date().toISOString(),
        },
        replace: true,
      });
    } catch (err) {
      setErrors({ form: err.message || 'TRANSMISSION FAILED. PLEASE VERIFY CONNECTION AND TRY AGAIN.' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      {/* 1. Page Header */}
      <section className="border-b border-[#2E2E2E] bg-[#141414] px-4 md:px-8 py-16">
        <div className="max-w-4xl mx-auto space-y-4">
          <div className="inline-flex items-center space-x-2 px-3 py-1 bg-[#1A1A1A] border border-[#2E2E2E] text-xs font-mono text-[#F4C13D]">
            <Terminal size={14} />
            <span className="uppercase tracking-widest">DISPATCH TERMINAL // COMMUNICATIONS</span>
          </div>
          <h1 className="font-heading text-4xl sm:text-5xl md:text-6xl uppercase tracking-tight text-[#F1ECE2]">
            DISPATCH TRANSMISSION
          </h1>
          <p className="font-mono text-sm md:text-base text-[#A7A29A] leading-relaxed">
            Submit repository indexing requests, suggest architectural enhancements, or report indexing anomalies directly to the maintainers.
          </p>
        </div>
      </section>

      {/* 2. Dispatch Form */}
      <section className="flex-1 px-4 md:px-8 py-12 max-w-3xl mx-auto w-full">
        <form onSubmit={handleSubmit} className="border-2 border-[#2E2E2E] bg-[#141414] p-6 md:p-8 space-y-6">
          <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3 text-xs font-mono">
            <span className="text-[#F06522] font-bold uppercase">OUTGOING MESSAGE</span>
            <span className="text-[#A7A29A] text-[10px]">CLIENT-SIDE PREPARATION</span>
          </div>

          <div className="bg-[#1A1A1A] border border-[#2E2E2E] p-3 text-xs font-mono text-[#A7A29A] flex items-start space-x-2.5">
            <Info size={15} className="text-[#F4C13D] shrink-0 mt-0.5" />
            <p className="leading-relaxed text-[11px]">
              <strong className="text-[#F1ECE2]">DISPATCH NOTICE:</strong> Messages are securely stored in the platform database for immediate review by repository maintainers.
            </p>
          </div>

          {errors.form && (
            <div className="bg-[#2D1515] border border-[#FF4444] p-3 text-xs font-mono text-[#FF8585] flex items-center space-x-2.5">
              <AlertTriangle size={16} className="text-[#FF4444] shrink-0" />
              <span>{errors.form}</span>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 font-mono text-xs">
            <div>
              <label htmlFor="contact-name" className="block text-[#A7A29A] uppercase mb-1">CALL SIGN / NAME *</label>
              <input
                id="contact-name"
                type="text"
                value={form.name}
                aria-required="true"
                aria-invalid={Boolean(errors.name)}
                aria-describedby={errors.name ? 'contact-name-error' : undefined}
                onChange={(e) => {
                  setForm({ ...form, name: e.target.value });
                  if (errors.name) setErrors({ ...errors, name: null });
                }}
                placeholder="e.g. Linus Torvalds"
                className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2.5 text-[#F1ECE2] focus:outline-none focus:border-[#F06522]"
              />
              {errors.name && <p id="contact-name-error" className="text-[11px] text-[#FF8585] mt-1">{errors.name}</p>}
            </div>

            <div>
              <label htmlFor="contact-email" className="block text-[#A7A29A] uppercase mb-1">ELECTRONIC MAIL *</label>
              <input
                id="contact-email"
                type="email"
                value={form.email}
                aria-required="true"
                aria-invalid={Boolean(errors.email)}
                aria-describedby={errors.email ? 'contact-email-error' : undefined}
                onChange={(e) => {
                  setForm({ ...form, email: e.target.value });
                  if (errors.email) setErrors({ ...errors, email: null });
                }}
                placeholder="contributor@example.com"
                className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2.5 text-[#F1ECE2] focus:outline-none focus:border-[#F06522]"
              />
              {errors.email && <p id="contact-email-error" className="text-[11px] text-[#FF8585] mt-1">{errors.email}</p>}
            </div>
          </div>

          <div className="font-mono text-xs">
            <label htmlFor="contact-topic" className="block text-[#A7A29A] uppercase mb-1">TRANSMISSION TOPIC</label>
            <select
              id="contact-topic"
              value={form.topic}
              onChange={(e) => setForm({ ...form, topic: e.target.value })}
              className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2.5 text-[#F1ECE2] focus:outline-none focus:border-[#F06522]"
            >
              <option value="REPO_INDEXING">REQUEST NEW REPOSITORY INDEXING</option>
              <option value="SIGNAL_ANOMALY">REPORT STALE OR INVALID ISSUE SIGNAL</option>
              <option value="FEATURE_REQUEST">ARCHIVAL FEATURE SUGGESTION</option>
              <option value="GENERAL">GENERAL INQUIRY</option>
            </select>
          </div>

          <div className="font-mono text-xs">
            <label htmlFor="contact-subject" className="block text-[#A7A29A] uppercase mb-1">SUBJECT LINE *</label>
            <input
              id="contact-subject"
              type="text"
              value={form.subject}
              aria-required="true"
              aria-invalid={Boolean(errors.subject)}
              aria-describedby={errors.subject ? 'contact-subject-error' : undefined}
              onChange={(e) => {
                setForm({ ...form, subject: e.target.value });
                if (errors.subject) setErrors({ ...errors, subject: null });
              }}
              placeholder="e.g. Indexing request for repository apache/arrow"
              className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2.5 text-[#F1ECE2] focus:outline-none focus:border-[#F06522]"
            />
            {errors.subject && <p id="contact-subject-error" className="text-[11px] text-[#FF8585] mt-1">{errors.subject}</p>}
          </div>

          <div className="font-mono text-xs">
            <label htmlFor="contact-message" className="block text-[#A7A29A] uppercase mb-1">TRANSMISSION PAYLOAD *</label>
            <textarea
              id="contact-message"
              rows={5}
              value={form.message}
              aria-required="true"
              aria-invalid={Boolean(errors.message)}
              aria-describedby={errors.message ? 'contact-message-error' : undefined}
              onChange={(e) => {
                setForm({ ...form, message: e.target.value });
                if (errors.message) setErrors({ ...errors, message: null });
              }}
              placeholder="Provide repository URL, issue context, or feedback details..."
              className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2.5 text-[#F1ECE2] focus:outline-none focus:border-[#F06522]"
            />
            {errors.message && <p id="contact-message-error" className="text-[11px] text-[#FF8585] mt-1">{errors.message}</p>}
          </div>

          <div className="pt-2 flex items-center justify-between">
            <span className="font-mono text-[11px] text-[#666666]">
              CLIENT STAGING · DIRECT CONTACT
            </span>
            <Button
              type="submit"
              variant="primary"
              size="lg"
              disabled={submitting}
              className="flex items-center space-x-2"
            >
              <Send size={15} />
              <span>{submitting ? 'PREPARING...' : 'STAGE TRANSMISSION DRAFT'}</span>
            </Button>
          </div>
        </form>
      </section>
    </div>
  );
}
