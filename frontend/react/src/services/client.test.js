import { describe, expect, it } from 'vitest';
import { customerProfilePictureUrl } from './client.js';

const base = `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/7/profile-image`;

describe('customerProfilePictureUrl', () => {
  it('adds the picture id, so each upload has its own URL', () => {
    expect(customerProfilePictureUrl(7, 'first-id')).toBe(`${base}?v=first-id`);
    expect(customerProfilePictureUrl(7, 'second-id')).not.toBe(
      customerProfilePictureUrl(7, 'first-id'),
    );
  });

  it('encodes the picture id', () => {
    expect(customerProfilePictureUrl(7, 'a b&c')).toBe(`${base}?v=a%20b%26c`);
  });

  it('returns no URL for a customer without a picture', () => {
    expect(customerProfilePictureUrl(7, null)).toBeUndefined();
    expect(customerProfilePictureUrl(7)).toBeUndefined();
  });
});
