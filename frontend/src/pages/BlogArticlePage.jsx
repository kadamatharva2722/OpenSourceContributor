import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { usePageSEO } from '../hooks/usePageSEO';
import { getArticleBySlug } from '../data/blogArticles';
import { WhyOpenSourceContributionMattersContent } from '../content/blog/WhyOpenSourceContributionMatters';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import {
  ArrowLeft,
  Clock,
  Calendar,
  List,
  ArrowRight,
  Share2,
  Compass,
  Check,
  BookOpen,
} from 'lucide-react';

const TABLE_OF_CONTENTS = [
  { id: 'introduction', title: '1. Introduction: Why Open Source Matters' },
  { id: 'what-is-open-source', title: '2. What Is Open Source?' },
  { id: 'why-open-source-is-important', title: '3. Why Open Source Is Important' },
  { id: 'why-developers-should-contribute', title: '4. Why Developers Should Contribute' },
  { id: 'for-students-and-freshers', title: '5. Value for Students & Freshers' },
  { id: 'non-code-contributions', title: '6. Contributions Beyond Code' },
  { id: 'challenges-and-solutions', title: '7. Challenges & Practical Solutions' },
  { id: 'how-to-start-contributing', title: '8. Common Contribution Workflow' },
  { id: 'where-to-find-opportunities', title: '9. Where to Find Opportunities' },
  { id: 'conclusion', title: '10. Conclusion' },
  { id: 'faq', title: '11. Frequently Asked Questions' },
];

export function BlogArticlePage() {
  const { slug } = useParams();
  const currentSlug = slug || 'why-open-source-contribution-matters';
  const article = getArticleBySlug(currentSlug);
  const [copied, setCopied] = useState(false);
  const [activeHeading, setActiveHeading] = useState('');

  // Structured Data (BlogPosting JSON-LD)
  const blogPostingJsonLd = {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: article?.title || 'Open Source Contribution: Why It Matters Today',
    description:
      article?.description ||
      'Learn why open source contributions matter, how developers benefit, and how beginners can start contributing to real-world GitHub projects.',
    mainEntityOfPage: {
      '@type': 'WebPage',
      '@id': `https://opensourcecontributor.vercel.app/blog/${currentSlug}`,
    },
    author: {
      '@type': 'Organization',
      name: article?.author?.name || 'OpenSourceContributor',
      url: article?.author?.url || 'https://opensourcecontributor.vercel.app',
    },
    publisher: {
      '@type': 'Organization',
      name: 'OpenSourceContributor',
      logo: {
        '@type': 'ImageObject',
        url: 'https://opensourcecontributor.vercel.app/favicon.svg',
      },
    },
    datePublished: article?.datePublished || '2026-11-21',
    dateModified: article?.dateModified || '2026-11-21',
    image: 'https://opensourcecontributor.vercel.app/og-image.png',
    keywords: article?.tags || [
      'open source contribution',
      'open source contribution for beginners',
      'benefits of open source contribution',
      'how to contribute to open source',
      'GitHub issues for beginners',
      'first open source contribution',
      'open source projects for students',
    ],
  };

  usePageSEO({
    title: 'Open Source Contribution: Why It Matters Today',
    description:
      article?.description ||
      'Learn why open source contributions matter, how developers benefit, and how beginners can start contributing to real-world GitHub projects.',
    canonicalPath: `/blog/${currentSlug}`,
    ogType: 'article',
    jsonLd: blogPostingJsonLd,
  });

  // Track active section for table of contents
  useEffect(() => {
    const handleScroll = () => {
      const scrollPosition = window.scrollY + 140;
      for (let i = TABLE_OF_CONTENTS.length - 1; i >= 0; i--) {
        const item = TABLE_OF_CONTENTS[i];
        const element = document.getElementById(item.id);
        if (element && element.offsetTop <= scrollPosition) {
          setActiveHeading(item.id);
          break;
        }
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (!article) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 bg-[#0D0D0D] text-[#F1ECE2]">
        <div className="border border-[#2E2E2E] bg-[#141414] p-8 max-w-md w-full space-y-4 text-center font-mono">
          <BookOpen className="w-10 h-10 text-[#F06522] mx-auto" />
          <h1 className="font-heading text-2xl uppercase tracking-tight text-[#F1ECE2]">
            Article Not Found
          </h1>
          <p className="text-xs text-[#A7A29A]">
            The requested blog dispatch could not be found or may have been relocated.
          </p>
          <Link to={ROUTES.BLOG} className="block pt-2">
            <Button variant="primary" size="md" className="w-full">
              RETURN TO BLOG INDEX
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      {/* 1. Header / Breadcrumbs Strip */}
      <section className="border-b border-[#2E2E2E] bg-[#141414] px-4 md:px-8 py-10 md:py-16">
        <div className="max-w-5xl mx-auto space-y-6">
          {/* Breadcrumb Navigation */}
          <div className="flex items-center justify-between text-xs font-mono text-[#A7A29A]">
            <Link
              to={ROUTES.BLOG}
              className="inline-flex items-center gap-1.5 hover:text-[#F06522] transition-colors uppercase tracking-wider font-bold"
            >
              <ArrowLeft size={14} />
              <span>Back to Blog Index</span>
            </Link>
            <button
              onClick={handleShare}
              className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-[#1A1A1A] border border-[#2E2E2E] text-[#D0CBC1] hover:text-[#F4C13D] transition-colors"
              title="Copy article link"
              type="button"
            >
              {copied ? (
                <>
                  <Check size={12} className="text-[#2E7D32]" />
                  <span>Copied</span>
                </>
              ) : (
                <>
                  <Share2 size={12} />
                  <span>Share</span>
                </>
              )}
            </button>
          </div>

          {/* Metadata Badges */}
          <div className="flex flex-wrap items-center gap-3">
            <Badge variant="orange" size="sm">
              {article.category}
            </Badge>
            <div className="flex items-center space-x-1.5 text-xs font-mono text-[#A7A29A]">
              <Clock size={13} />
              <span>{article.readTime}</span>
            </div>
            {article.publishedDate && (
              <div className="flex items-center space-x-1.5 text-xs font-mono text-[#A7A29A]">
                <Calendar size={13} />
                <span>Published {article.publishedDate}</span>
              </div>
            )}
            <span className="text-xs font-mono text-[#A7A29A]">
              By {article.author?.name || 'OpenSourceContributor'}
            </span>
          </div>

          {/* Article Title */}
          <h1 className="font-heading text-3xl sm:text-4xl md:text-6xl uppercase tracking-tight text-[#F1ECE2] leading-none">
            {article.title}
          </h1>

          {/* Lead Summary */}
          <p className="font-mono text-sm md:text-base text-[#D0CBC1] max-w-3xl leading-relaxed">
            {article.description}
          </p>
        </div>
      </section>

      {/* 2. Main Article Body + Sidebar Layout */}
      <section className="px-4 md:px-8 py-12 max-w-5xl mx-auto w-full">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-start">
          {/* Main Reading Column (8 Columns on desktop) */}
          <div className="lg:col-span-8 space-y-12 min-w-0">
            {/* Mobile Table of Contents */}
            <div className="lg:hidden border border-[#2E2E2E] bg-[#141414] p-5 space-y-3 font-mono text-xs">
              <div className="flex items-center gap-2 text-[#F4C13D] font-bold uppercase tracking-wider">
                <List size={14} />
                <span>Table of Contents</span>
              </div>
              <ul className="space-y-1.5 pl-2 border-l border-[#2E2E2E]">
                {TABLE_OF_CONTENTS.map((item) => (
                  <li key={item.id}>
                    <a
                      href={`#${item.id}`}
                      className="text-[#A7A29A] hover:text-[#F06522] transition-colors block py-0.5"
                    >
                      {item.title}
                    </a>
                  </li>
                ))}
              </ul>
            </div>

            {/* Article Content Component */}
            <WhyOpenSourceContributionMattersContent />

            {/* Related Internal Links */}
            <div className="border-t border-[#2E2E2E] pt-8 space-y-4">
              <h3 className="font-heading text-xl text-[#F1ECE2] uppercase tracking-tight">
                Continue Exploring OpenSourceContributor
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 font-mono text-xs">
                <Link
                  to={ROUTES.HOW_IT_WORKS}
                  className="p-4 border border-[#2E2E2E] bg-[#141414] hover:border-[#F4C13D] transition-colors block space-y-1 group"
                >
                  <strong className="text-[#F1ECE2] group-hover:text-[#F4C13D] block transition-colors">
                    HOW IT WORKS GUIDE →
                  </strong>
                  <span className="text-[#A7A29A]">
                    Step-by-step walkthrough of catalog exploration, AI search parsing, and crate tracking.
                  </span>
                </Link>

                <Link
                  to={ROUTES.ABOUT}
                  className="p-4 border border-[#2E2E2E] bg-[#141414] hover:border-[#F06522] transition-colors block space-y-1 group"
                >
                  <strong className="text-[#F1ECE2] group-hover:text-[#F06522] block transition-colors">
                    ABOUT THE PLATFORM →
                  </strong>
                  <span className="text-[#A7A29A]">
                    Read our manifesto on high-signal matching, zero synthetic exercises, and archival precision.
                  </span>
                </Link>
              </div>
            </div>

            {/* Bottom CTA Card */}
            <div className="border border-[#2E2E2E] bg-[#161616] p-6 md:p-8 space-y-4 border-l-4 border-l-[#F06522]">
              <div className="space-y-2">
                <h3 className="font-heading text-2xl text-[#F1ECE2] uppercase tracking-tight">
                  Ready to Start Your Open Source Journey?
                </h3>
                <p className="font-mono text-xs md:text-sm text-[#A7A29A] leading-relaxed">
                  Browse real, live GitHub issues categorized by language, ecosystem, and difficulty rating on OpenSourceContributor.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-4 pt-2">
                <Link to={ROUTES.EXPLORE}>
                  <Button variant="primary" size="md">
                    EXPLORE OPEN ISSUES <ArrowRight size={14} className="ml-2" />
                  </Button>
                </Link>
                <Link to={ROUTES.BLOG}>
                  <Button variant="outline" size="md">
                    MORE BLOG ARTICLES
                  </Button>
                </Link>
              </div>
            </div>
          </div>

          {/* Desktop Sticky Table of Contents Sidebar (4 Columns) */}
          <aside className="hidden lg:block lg:col-span-4 sticky top-20 space-y-6">
            <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-4 font-mono text-xs">
              <div className="flex items-center justify-between pb-2 border-b border-[#2E2E2E]">
                <span className="text-[#F4C13D] font-bold uppercase tracking-wider flex items-center gap-1.5">
                  <List size={13} />
                  <span>On This Page</span>
                </span>
                <span className="text-[10px] text-[#666666]">INDEX</span>
              </div>
              <nav aria-label="Table of contents">
                <ul className="space-y-1.5">
                  {TABLE_OF_CONTENTS.map((item) => {
                    const isActive = activeHeading === item.id;
                    return (
                      <li key={item.id}>
                        <a
                          href={`#${item.id}`}
                          className={`block py-1 pl-2.5 transition-colors border-l-2 text-[11px] leading-snug ${
                            isActive
                              ? 'border-l-[#F06522] text-[#F1ECE2] font-bold bg-[#1A1A1A]'
                              : 'border-l-transparent text-[#A7A29A] hover:text-[#F1ECE2] hover:border-l-[#2E2E2E]'
                          }`}
                        >
                          {item.title}
                        </a>
                      </li>
                    );
                  })}
                </ul>
              </nav>
            </div>

            {/* Quick Catalog Widget */}
            <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-3 font-mono text-xs">
              <div className="flex items-center gap-1.5 text-[#F06522] font-bold uppercase tracking-wider">
                <Compass size={13} />
                <span>Issue Catalog</span>
              </div>
              <p className="text-[11px] text-[#A7A29A] leading-relaxed">
                Filter live GitHub issues by skill level, repository activity, and programming language.
              </p>
              <Link to={ROUTES.EXPLORE} className="block pt-1">
                <Button variant="secondary" size="sm" className="w-full justify-between">
                  <span>DISCOVER ISSUES</span>
                  <ArrowRight size={12} />
                </Button>
              </Link>
            </div>
          </aside>
        </div>
      </section>
    </div>
  );
}
