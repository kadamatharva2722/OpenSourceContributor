import React from 'react';
import { Link } from 'react-router-dom';
import { ROUTES, getBlogArticleRoute } from '../constants/routes';
import { usePageSEO } from '../hooks/usePageSEO';
import { getAllArticles, getFeaturedArticle } from '../data/blogArticles';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import {
  BookOpen,
  ArrowRight,
  Clock,
  Calendar,
  Sparkles,
  Tag,
  Compass,
} from 'lucide-react';

export function BlogPage() {
  usePageSEO({
    title: 'Blog — Insights for the Open Source Community',
    description:
      'Explore practical guides, insights, and resources to understand open source, improve your development skills, and start contributing to real-world projects.',
    canonicalPath: ROUTES.BLOG,
  });

  const allArticles = getAllArticles();
  const featuredArticle = getFeaturedArticle();

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      {/* 1. Header / Intro Banner */}
      <section className="border-b border-[#2E2E2E] bg-[#141414] px-4 md:px-8 py-16">
        <div className="max-w-5xl mx-auto space-y-4">
          <div className="inline-flex items-center space-x-2 px-3 py-1 bg-[#1A1A1A] border border-[#2E2E2E] text-xs font-mono text-[#F06522]">
            <BookOpen className="w-3.5 h-3.5" />
            <span className="uppercase tracking-widest">INSIGHTS // COMMUNITY EDITORIAL ARCHIVE</span>
          </div>
          <h1 className="font-heading text-4xl sm:text-5xl md:text-6xl uppercase tracking-tight text-[#F1ECE2]">
            Insights for the Open Source Community
          </h1>
          <p className="font-mono text-sm md:text-base text-[#A7A29A] max-w-3xl leading-relaxed">
            Explore practical guides, insights, and resources to understand open source, improve your development skills, and start contributing to real-world projects.
          </p>
        </div>
      </section>

      {/* 2. Main Content Body */}
      <section className="px-4 md:px-8 py-12 max-w-5xl mx-auto w-full space-y-12">
        {/* Featured Article Section */}
        {featuredArticle && (
          <div className="space-y-4">
            <div className="flex items-center space-x-2 text-xs font-mono text-[#F4C13D] uppercase tracking-widest font-bold">
              <Sparkles size={14} />
              <span>FEATURED ARTICLE</span>
            </div>

            <Card
              borderAccent="orange"
              className="p-6 md:p-8 space-y-6 hover:border-[#F06522] transition-colors"
            >
              <div className="flex flex-wrap items-center gap-3">
                <Badge variant="orange" size="sm">
                  {featuredArticle.category}
                </Badge>
                <div className="flex items-center space-x-1.5 text-xs font-mono text-[#A7A29A]">
                  <Clock size={13} />
                  <span>{featuredArticle.readTime}</span>
                </div>
                {featuredArticle.publishedDate && (
                  <div className="flex items-center space-x-1.5 text-xs font-mono text-[#A7A29A]">
                    <Calendar size={13} />
                    <span>{featuredArticle.publishedDate}</span>
                  </div>
                )}
              </div>

              <div className="space-y-3">
                <Link
                  to={getBlogArticleRoute(featuredArticle.slug)}
                  className="group block"
                >
                  <h2 className="font-heading text-2xl sm:text-3xl md:text-4xl text-[#F1ECE2] uppercase tracking-tight group-hover:text-[#F06522] transition-colors">
                    {featuredArticle.title}
                  </h2>
                </Link>
                <p className="font-mono text-xs md:text-sm text-[#A7A29A] leading-relaxed">
                  {featuredArticle.description}
                </p>
              </div>

              {/* Tags */}
              {featuredArticle.tags && featuredArticle.tags.length > 0 && (
                <div className="flex flex-wrap gap-2 pt-2">
                  {featuredArticle.tags.map((tag, idx) => (
                    <span
                      key={idx}
                      className="inline-flex items-center gap-1 text-[11px] font-mono px-2 py-0.5 bg-[#0D0D0D] border border-[#2E2E2E] text-[#D0CBC1]"
                    >
                      <Tag size={10} className="text-[#F06522]" />
                      <span>{tag}</span>
                    </span>
                  ))}
                </div>
              )}

              {/* CTA */}
              <div className="pt-2 flex items-center justify-between border-t border-[#2E2E2E]">
                <span className="text-xs font-mono text-[#A7A29A]">
                  By {featuredArticle.author?.name || 'OpenSourceContributor'}
                </span>
                <Link to={getBlogArticleRoute(featuredArticle.slug)}>
                  <Button variant="primary" size="md">
                    READ ARTICLE <ArrowRight size={14} className="ml-2" />
                  </Button>
                </Link>
              </div>
            </Card>
          </div>
        )}

        {/* Complete Article Catalog Listing */}
        <div className="space-y-6">
          <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3">
            <h2 className="font-heading text-xl sm:text-2xl text-[#F1ECE2] uppercase tracking-tight flex items-center gap-2">
              <Compass size={18} className="text-[#5B86F2]" />
              <span>All Articles</span>
            </h2>
            <span className="font-mono text-xs text-[#A7A29A]">
              {allArticles.length} {allArticles.length === 1 ? 'Dispatch' : 'Dispatches'} Available
            </span>
          </div>

          <div className="grid grid-cols-1 gap-6">
            {allArticles.map((article) => (
              <div
                key={article.slug}
                className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-4 hover:border-[#F4C13D] transition-colors"
              >
                <div className="flex flex-wrap items-center justify-between gap-3 text-xs font-mono text-[#A7A29A]">
                  <div className="flex items-center gap-3">
                    <Badge variant="yellow" size="sm">
                      {article.category}
                    </Badge>
                    <span className="flex items-center gap-1">
                      <Clock size={12} />
                      {article.readTime}
                    </span>
                  </div>
                  {article.publishedDate && (
                    <span className="flex items-center gap-1">
                      <Calendar size={12} />
                      {article.publishedDate}
                    </span>
                  )}
                </div>

                <div className="space-y-2">
                  <Link
                    to={getBlogArticleRoute(article.slug)}
                    className="group block"
                  >
                    <h3 className="font-heading text-xl md:text-2xl text-[#F1ECE2] uppercase tracking-tight group-hover:text-[#F4C13D] transition-colors">
                      {article.title}
                    </h3>
                  </Link>
                  <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
                    {article.description}
                  </p>
                </div>

                <div className="pt-2 flex items-center justify-between border-t border-[#2E2E2E]">
                  <div className="flex flex-wrap gap-1.5">
                    {article.tags?.slice(0, 3).map((tag, idx) => (
                      <span
                        key={idx}
                        className="text-[10px] font-mono px-1.5 py-0.5 bg-[#0D0D0D] border border-[#2E2E2E] text-[#A7A29A]"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>
                  <Link
                    to={getBlogArticleRoute(article.slug)}
                    className="inline-flex items-center text-xs font-mono font-bold text-[#F4C13D] hover:underline uppercase tracking-wider"
                  >
                    Read Article <ArrowRight size={12} className="ml-1" />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Discovery Catalog CTA */}
        <div className="border border-[#2E2E2E] bg-[#161616] p-8 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="space-y-2 max-w-xl">
            <h3 className="font-heading text-2xl text-[#F1ECE2] uppercase tracking-tight">
              Ready to Put Knowledge into Practice?
            </h3>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              Explore thousands of real, live GitHub issues filtered by programming language, difficulty tier, and domain ecosystem.
            </p>
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
