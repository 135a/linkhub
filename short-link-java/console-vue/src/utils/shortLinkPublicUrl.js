/**
 * Utilities for building publicly-accessible short link URLs.
 *
 * At Docker build time the following env vars are injected:
 *   VITE_SHORT_LINK_PUBLIC_SCHEME    – "http" | "https"  (default: "http")
 *   VITE_PUBLIC_SHORTLINK_BASE_URL   – optional base URL that overrides
 *                                      the domain stored in the DB record
 *                                      (e.g. "https://sl.example.com")
 *
 * When VITE_PUBLIC_SHORTLINK_BASE_URL is provided it is used as-is and the
 * short URI is appended.  Otherwise the URL is assembled from the scheme +
 * the domain/shortUri fields on the row object.
 */

const PUBLIC_SCHEME = import.meta.env.VITE_SHORT_LINK_PUBLIC_SCHEME || 'http'
const PUBLIC_BASE_URL = import.meta.env.VITE_PUBLIC_SHORTLINK_BASE_URL || ''

/**
 * Build the display URL for a short link row.
 *
 * @param {{ domain: string, shortUri: string }} row  - A short link record.
 * @returns {string}  The full URL, e.g. "http://example.com/abc123"
 */
export function buildShortLinkDisplayUrl(row) {
  if (!row) return ''

  // If a global public base URL is configured at build time, use it.
  if (PUBLIC_BASE_URL) {
    const base = PUBLIC_BASE_URL.replace(/\/$/, '')
    return `${base}/${row.shortUri}`
  }

  // Otherwise assemble from the scheme + the domain stored on the record.
  const scheme = PUBLIC_SCHEME.replace(/:\/\/$/, '') // strip trailing ://
  const domain = (row.domain || '').replace(/^https?:\/\//, '') // strip any existing scheme
  return `${scheme}://${domain}/${row.shortUri}`
}
