import React from 'react';
import { Link } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { Button } from '../components/ui/Button';
import { Disc, Shield, Target, Code2, HeartHandshake, ArrowRight } from 'lucide-react';

export function AboutPage() {
  useDocumentTitle('About');
  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      {/* 1. Page Header */}
      <section className="border-b border-[#2E2E2E] bg-[#141414] px-4 md:px-8 py-16">
        <div className="max-w-4xl mx-auto space-y-4">
          <div className="inline-flex items-center space-x-2 px-3 py-1 bg-[#1A1A1A] border border-[#2E2E2E] text-xs font-mono text-[#F06522]">
            <Disc className="w-3.5 h-3.5 animate-spin" style={{ animationDuration: '6s' }} />
            <span className="uppercase tracking-widest">MANIFESTO // ARCHIVAL VISION</span>
          </div>
          <h1 className="font-heading text-4xl sm:text-5xl md:text-7xl uppercase tracking-tight text-[#F1ECE2]">
            ARCHIVAL PRECISION FOR OPEN SOURCE
          </h1>
          <p className="font-mono text-base md:text-lg text-[#A7A29A] leading-relaxed">
            Finding the right open-source contribution should be simple. We built a focused discovery platform that connects developers with meaningful, verified issues across real GitHub repositories.
          </p>
        </div>
      </section>

      {/* 2. The Core Problem & Manifesto */}
      <section className="border-b border-[#2E2E2E] px-4 md:px-8 py-16 max-w-4xl mx-auto w-full space-y-12">
        <div className="space-y-4">
          <h2 className="font-heading text-3xl text-[#F1ECE2]">
            THE CONTRIBUTION PARADOX
          </h2>
          <p className="font-mono text-sm text-[#A7A29A] leading-relaxed">
            Every day, thousands of capable developers want to contribute to open-source software. Yet finding a meaningful, unassigned, well-documented issue in a healthy repository feels like sifting through digital landfill.
          </p>
          <p className="font-mono text-sm text-[#A7A29A] leading-relaxed">
            Labels like <code className="text-[#F4C13D]">good first issue</code> are frequently applied and then forgotten. Beginners spend hours cloning massive codebases only to discover the issue was already solved months ago, or blocked on an unresolved architectural rewrite.
          </p>
        </div>

        {/* Core Principles Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4">
          <div className="border border-[#2E2E2E] bg-[#161616] p-6 space-y-3 border-l-4 border-l-[#F06522]">
            <Shield size={20} className="text-[#F06522]" />
            <h3 className="font-heading text-xl text-[#F1ECE2]">ZERO SYNTHETIC DATA</h3>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              We never fabricate artificial tasks or synthetic tutorial exercises. Every single issue on this platform is pulled from live, production open-source repositories via GitHub APIs.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#161616] p-6 space-y-3 border-l-4 border-l-[#F4C13D]">
            <Code2 size={20} className="text-[#F4C13D]" />
            <h3 className="font-heading text-xl text-[#F1ECE2]">TECHNOLOGY AGNOSTIC</h3>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              Great open source spans the entire computing ecosystem: TypeScript, Rust, Java, Python, Go, React, and beyond. We treat all healthy ecosystems with equal first-class archival indexing.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#161616] p-6 space-y-3 border-l-4 border-l-[#5B86F2]">
            <Target size={20} className="text-[#5B86F2]" />
            <h3 className="font-heading text-xl text-[#F1ECE2]">HIGH SIGNAL MATCHING</h3>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              Our semantic matching pipeline uses Groq-powered natural language models to extract your exact technical intent, matching not just tags, but complexity, diff size, and code patterns.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#161616] p-6 space-y-3 border-l-4 border-l-[#F29BB8]">
            <Disc size={20} className="text-[#F29BB8]" />
            <h3 className="font-heading text-xl text-[#F1ECE2]">VINYL INDEX DESIGN</h3>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              Inspired by the physical sleeve archival systems of Run Out Records, our UI values high information density, stark brutalist typography, zero rounded corners, and pure utilitarian focus.
            </p>
          </div>
        </div>

        {/* Call to action */}
        <div className="border-t border-[#2E2E2E] pt-12 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div>
            <h4 className="font-heading text-2xl text-[#F1ECE2]">READY TO MAKE YOUR FIRST PR?</h4>
            <p className="font-mono text-xs text-[#A7A29A]">The crate is stocked with thousands of verified open issues.</p>
          </div>
          <Link to={ROUTES.EXPLORE}>
            <Button variant="primary" size="lg">
              EXPLORE CATALOG <ArrowRight size={16} className="ml-2" />
            </Button>
          </Link>
        </div>
      </section>
    </div>
  );
}
