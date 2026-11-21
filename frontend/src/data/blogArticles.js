/**
 * OpenSourceContributor - Blog Articles Metadata Registry
 * Extensible article catalog supporting category filtering, featured spotlights, and dynamic routing.
 */

export const BLOG_ARTICLES = [
  {
    slug: 'why-open-source-contribution-matters',
    title: 'Open Source Contribution: Why It Matters Today',
    description:
      'Learn why open source contributions matter, how developers benefit, and how beginners can start contributing to real-world GitHub projects.',
    category: 'Open Source',
    readTime: '12 min read',
    publishedDate: 'November 21, 2026',
    datePublished: '2026-11-21',
    dateModified: '2026-11-21',
    featured: true,
    author: {
      name: 'OpenSourceContributor',
      url: 'https://opensourcecontributor.vercel.app',
    },
    primaryKeyword: 'open source contribution',
    tags: [
      'open source contribution',
      'open source contribution for beginners',
      'benefits of open source contribution',
      'how to contribute to open source',
      'GitHub issues for beginners',
      'first open source contribution',
      'open source projects for students',
    ],
  },
];

export const getAllArticles = () => BLOG_ARTICLES;

export const getFeaturedArticle = () => BLOG_ARTICLES.find((article) => article.featured) || BLOG_ARTICLES[0];

export const getArticleBySlug = (slug) => {
  if (!slug) return null;
  return BLOG_ARTICLES.find((article) => article.slug === slug) || null;
};
