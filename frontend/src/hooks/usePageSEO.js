import { useEffect } from 'react';

export const BRAND_NAME = 'OpenSourceContributor';
export const BASE_URL = 'https://opensourcecontributor.vercel.app';
export const DEFAULT_DOCUMENT_TITLE = 'OpenSourceContributor — Open Source Contribution Platform & Issue Finder';
export const DEFAULT_DESCRIPTION =
  'OpenSourceContributor is an open source contribution platform and issue finder. Discover beginner-friendly GitHub issues and projects matching your skills.';

function setMetaTag(attrName, attrValue, content) {
  if (!content) return;
  let element = document.querySelector(`meta[${attrName}="${attrValue}"]`);
  if (!element) {
    element = document.createElement('meta');
    element.setAttribute(attrName, attrValue);
    document.head.appendChild(element);
  }
  element.setAttribute('content', content);
}

function setCanonical(url) {
  if (!url) return;
  let link = document.querySelector('link[rel="canonical"]');
  if (!link) {
    link = document.createElement('link');
    link.setAttribute('rel', 'canonical');
    document.head.appendChild(link);
  }
  link.setAttribute('href', url);
}

/**
 * Custom hook for dynamic page-level SEO metadata management in React SPA.
 * Synchronizes document.title, meta description, canonical link,
 * Open Graph, Twitter metadata, and structured data (JSON-LD) during client-side rendering.
 * Guarantees that structured data scripts are cleanly cleaned up on route transition.
 *
 * @param {Object} params
 * @param {string} params.title - Page-specific title or subtitle
 * @param {string} [params.description] - Page-specific meta description
 * @param {string} [params.canonicalPath] - Route path (e.g. '/explore')
 * @param {string} [params.ogType='website'] - Open Graph type
 * @param {boolean} [params.noindex=false] - Whether to apply robots noindex
 * @param {Object} [params.jsonLd=null] - Optional JSON-LD structured data object
 */
export function usePageSEO({
  title,
  description,
  canonicalPath = '',
  ogType = 'website',
  noindex = false,
  jsonLd = null,
}) {
  useEffect(() => {
    // 1. Format and set Title
    let formattedTitle = DEFAULT_DOCUMENT_TITLE;
    if (title) {
      formattedTitle = title.includes(BRAND_NAME) ? title : `${title} | ${BRAND_NAME}`;
    }
    document.title = formattedTitle;

    // 2. Set Meta Description and Meta Title
    const metaDesc = description || DEFAULT_DESCRIPTION;
    setMetaTag('name', 'description', metaDesc);
    setMetaTag('name', 'title', formattedTitle);

    // 3. Set Canonical Link
    const canonicalUrl = canonicalPath ? `${BASE_URL}${canonicalPath}` : BASE_URL;
    setCanonical(canonicalUrl);

    // 4. Set Open Graph Tags
    setMetaTag('property', 'og:title', formattedTitle);
    setMetaTag('property', 'og:description', metaDesc);
    setMetaTag('property', 'og:url', canonicalUrl);
    setMetaTag('property', 'og:type', ogType);

    // 5. Set Twitter Card Tags
    setMetaTag('name', 'twitter:title', formattedTitle);
    setMetaTag('name', 'twitter:description', metaDesc);
    setMetaTag('name', 'twitter:url', canonicalUrl);

    // 6. Robots Noindex Handling
    if (noindex) {
      setMetaTag('name', 'robots', 'noindex, follow');
    }

    // 7. Structured Data (JSON-LD) Handling with strict stale-tag prevention
    const existingScript = document.querySelector('script[data-schema="page-jsonld"]');
    if (jsonLd) {
      const script = existingScript || document.createElement('script');
      if (!existingScript) {
        script.setAttribute('type', 'application/ld+json');
        script.setAttribute('data-schema', 'page-jsonld');
        document.head.appendChild(script);
      }
      script.textContent = JSON.stringify(jsonLd);
    } else if (existingScript) {
      existingScript.remove();
    }

    return () => {
      // Restore default homepage metadata on unmount
      document.title = DEFAULT_DOCUMENT_TITLE;
      setMetaTag('name', 'description', DEFAULT_DESCRIPTION);
      setMetaTag('name', 'title', DEFAULT_DOCUMENT_TITLE);
      setCanonical(BASE_URL);
      setMetaTag('property', 'og:title', DEFAULT_DOCUMENT_TITLE);
      setMetaTag('property', 'og:description', DEFAULT_DESCRIPTION);
      setMetaTag('property', 'og:url', BASE_URL);
      setMetaTag('property', 'og:type', 'website');
      setMetaTag('name', 'twitter:title', DEFAULT_DOCUMENT_TITLE);
      setMetaTag('name', 'twitter:description', DEFAULT_DESCRIPTION);
      setMetaTag('name', 'twitter:url', BASE_URL);

      if (noindex) {
        const robotsMeta = document.querySelector('meta[name="robots"]');
        if (robotsMeta) {
          robotsMeta.remove();
        }
      }

      // Clean up structured data to prevent stale schemas on succeeding routes
      const schemaScript = document.querySelector('script[data-schema="page-jsonld"]');
      if (schemaScript) {
        schemaScript.remove();
      }
    };
  }, [title, description, canonicalPath, ogType, noindex, jsonLd]);
}
