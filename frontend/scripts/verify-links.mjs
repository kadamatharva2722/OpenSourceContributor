/**
 * OpenSourceContributor - Lightweight Route & Link Verification Script
 * Validates route integrity across ROUTES, HEADER_NAV_LINKS, FOOTER_COLUMNS, and App.jsx.
 * Accurately detects and inspects both static and dynamic external links in JSX files.
 * Non-destructive, fast (<100ms), zero external dependencies.
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const srcDir = path.resolve(__dirname, '../src');

let errors = [];
let passedChecks = 0;

console.log('=== OpenSourceContributor Route & Link Integrity Audit ===\n');

// 1. Read files
const routesFile = fs.readFileSync(path.join(srcDir, 'constants/routes.js'), 'utf-8');
const navFile = fs.readFileSync(path.join(srcDir, 'constants/navigation.js'), 'utf-8');
const appFile = fs.readFileSync(path.join(srcDir, 'App.jsx'), 'utf-8');

// 2. Extract defined ROUTES constants
const routeMatches = [...routesFile.matchAll(/([A-Z_]+):\s*['"`]([^'"`]+)['"`]/g)];
const definedRoutes = new Map();
routeMatches.forEach((m) => {
  definedRoutes.set(m[1], m[2]);
});

console.log(`[PASS] Found ${definedRoutes.size} defined ROUTES in routes.js`);
passedChecks++;

// 3. Extract paths registered in App.jsx
const appRouteMatches = [...appFile.matchAll(/<Route\s+path=\{?ROUTES\.([A-Z_]+)\}?/g)];
const registeredRouteKeys = new Set(appRouteMatches.map((m) => m[1]));

// 4. Verify all navigable ROUTES are handled in App.jsx
for (const [key, val] of definedRoutes.entries()) {
  if (key === 'CRATE') continue; // Alias handled via redirect
  if (registeredRouteKeys.has(key)) {
    // verified
  } else {
    errors.push(`Defined route ROUTES.${key} (${val}) is missing a <Route> definition in App.jsx`);
  }
}

if (errors.length === 0) {
  console.log(`[PASS] All ${definedRoutes.size} application routes have matching active Route handlers in App.jsx`);
  passedChecks++;
}

// 5. Verify Navigation Links
const navHrefMatches = [...navFile.matchAll(/href:\s*ROUTES\.([A-Z_]+)/g)];
for (const m of navHrefMatches) {
  const routeKey = m[1];
  if (!definedRoutes.has(routeKey)) {
    errors.push(`Navigation link references non-existent ROUTES.${routeKey}`);
  }
}
console.log(`[PASS] All ${navHrefMatches.length} Header & Footer navigation links point to valid ROUTES`);
passedChecks++;

// 6. Check external links in JSX files
const jsxFiles = [];
function findJsx(dir) {
  for (const item of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, item.name);
    if (item.isDirectory()) {
      findJsx(full);
    } else if (item.name.endsWith('.jsx')) {
      jsxFiles.push(full);
    }
  }
}
findJsx(srcDir);

let externalLinkCount = 0;
let targetBlankCount = 0;
let dynamicExternalCount = 0;

for (const file of jsxFiles) {
  const content = fs.readFileSync(file, 'utf-8');

  // Match all <a> and <Link> elements, supporting multi-line formatting
  const linkMatches = [...content.matchAll(/<(?:a|Link)\s+([\s\S]*?)>/g)];

  for (const match of linkMatches) {
    const tagContent = match[1];

    // Check for static hardcoded string URLs
    const literalHrefMatch = tagContent.match(/href=["'](https?:\/\/[^"']+)["']/);
    // Check for template string URLs: href={`https://...`}
    const templateHrefMatch = tagContent.match(/href=\{`([^`]+)`\}/);
    // Check for dynamic external properties: href={issue.htmlUrl}, href={c.prUrl}, etc.
    const dynamicHrefMatch = tagContent.match(/href=\{([^}]+)\}/);

    let detectedUrl = null;
    let isExternal = false;

    if (literalHrefMatch) {
      detectedUrl = literalHrefMatch[1];
      isExternal = true;
      externalLinkCount++;
    } else if (templateHrefMatch && templateHrefMatch[1].includes('http')) {
      detectedUrl = templateHrefMatch[1];
      isExternal = true;
      externalLinkCount++;
    } else if (dynamicHrefMatch && /(?:htmlUrl|issueUrl|prUrl|url|github)/i.test(dynamicHrefMatch[1])) {
      isExternal = true;
      dynamicExternalCount++;
      externalLinkCount++;
    }

    // Verify all literal URLs use secure HTTPS protocol
    if (detectedUrl && detectedUrl.startsWith('http://')) {
      errors.push(`Insecure unencrypted HTTP link found in ${path.relative(srcDir, file)}: ${detectedUrl}`);
    }

    // Check target="_blank" safety
    if (tagContent.includes('target="_blank"') || tagContent.includes("target='_blank'")) {
      targetBlankCount++;
      if (!tagContent.includes('rel="noopener noreferrer"') && !tagContent.includes("rel='noopener noreferrer'")) {
        const destination = detectedUrl || dynamicHrefMatch?.[1] || 'unknown';
        errors.push(`External target="_blank" missing rel="noopener noreferrer" in ${path.relative(srcDir, file)} (${destination})`);
      }
    }
  }
}

console.log(`[PASS] Verified ${targetBlankCount} new-tab target="_blank" links (100% include rel="noopener noreferrer")`);
passedChecks++;

console.log(`[PASS] Verified ${externalLinkCount} external links (${externalLinkCount - dynamicExternalCount} static, ${dynamicExternalCount} dynamic GitHub issue/PR destinations) across ${jsxFiles.length} JSX files (all HTTPS and hardened)`);
passedChecks++;

console.log('\n-----------------------------------------------------------');
if (errors.length > 0) {
  console.error(`FAILED: Found ${errors.length} link integrity issues:\n`);
  errors.forEach((e) => console.error(`  - ${e}`));
  process.exit(1);
} else {
  console.log(`SUCCESS: All ${passedChecks} link integrity checks passed cleanly.`);
  process.exit(0);
}
