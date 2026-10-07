/**
 * Best-effort decode of a JWT's expiry claim - no signature verification (that's the
 * backend's job; this is purely so the frontend can proactively log out when its own
 * token is about to expire). Returns null for anything that isn't a well-formed JWT.
 */
export function decodeJwtExpiryMillis(token: string): number | null {
  const parts = token.split('.');
  if (parts.length !== 3) {
    return null;
  }
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(atob(base64)) as { exp?: unknown };
    return typeof payload.exp === 'number' ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}
