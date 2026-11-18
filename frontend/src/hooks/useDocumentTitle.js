import { useEffect } from 'react';

export const BRAND_NAME = 'OpenSourceContributor';
export const DEFAULT_DOCUMENT_TITLE = 'OpenSourceContributor — Find Where You Fit. Contribute.';

/**
 * Lightweight custom hook to set the document title for a component or page.
 * Formats the title as `${pageTitle} | OpenSourceContributor`, or preserves
 * the default brand title if null or already formatted.
 *
 * @param {string} [title] - The page-specific title to set.
 */
export function useDocumentTitle(title) {
  useEffect(() => {
    if (!title) {
      document.title = DEFAULT_DOCUMENT_TITLE;
    } else if (title.includes(BRAND_NAME)) {
      document.title = title;
    } else {
      document.title = `${title} | ${BRAND_NAME}`;
    }

    return () => {
      document.title = DEFAULT_DOCUMENT_TITLE;
    };
  }, [title]);
}
