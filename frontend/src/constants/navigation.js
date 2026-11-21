import { ROUTES } from './routes';

export const HEADER_NAV_LINKS = [
  { label: 'Explore Issues', href: ROUTES.EXPLORE },
  { label: 'How It Works', href: ROUTES.HOW_IT_WORKS },
  { label: 'About', href: ROUTES.ABOUT },
  { label: 'Blog', href: ROUTES.BLOG },
  { label: 'Contact', href: ROUTES.CONTACT },
];

export const FOOTER_COLUMNS = [
  {
    title: 'CATALOG INDEX',
    links: [
      { label: 'Home Archive', href: ROUTES.HOME },
      { label: 'Explore Issues', href: ROUTES.EXPLORE },
      { label: 'How It Works', href: ROUTES.HOW_IT_WORKS },
      { label: 'About OpenSourceContributor', href: ROUTES.ABOUT },
      { label: 'Community Blog', href: ROUTES.BLOG },
      { label: 'Dispatch / Contact', href: ROUTES.CONTACT },
    ],
  },
  {
    title: 'CONTRIBUTOR WORKSPACE',
    links: [
      { label: 'Contributor Dashboard', href: ROUTES.DASHBOARD },
      { label: 'Saved Issues (Crate)', href: ROUTES.SAVED_ISSUES },
      { label: 'Contribution Ledger', href: ROUTES.MY_CONTRIBUTIONS },
      { label: 'Account & Stack Settings', href: ROUTES.SETTINGS },
      { label: 'Sign in with GitHub', href: ROUTES.LOGIN },
    ],
  },
  {
    title: 'LEGAL & ARCHIVAL',
    links: [
      { label: 'Privacy Policy', href: ROUTES.PRIVACY },
      { label: 'Terms & Conditions', href: ROUTES.TERMS },
      { label: 'Cookie Preferences', href: ROUTES.COOKIE_PREFERENCES },
    ],
  },
];
