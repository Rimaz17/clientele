const base64Url = (value) =>
  btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

// Builds an unsigned JWT that jwt-decode can read. The React app never checks
// signatures (the API does), so this is enough for component tests.
export const fakeToken = ({ sub, scopes = ['ROLE_USER'], expiresInSeconds = 3600 }) =>
  [
    base64Url({ alg: 'HS256', typ: 'JWT' }),
    base64Url({ sub, scopes, exp: Math.floor(Date.now() / 1000) + expiresInSeconds }),
    'signature',
  ].join('.');
