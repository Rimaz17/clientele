import { describe, expect, it } from 'vitest';
import { getApiErrorMessage, getFieldErrors } from './apiErrors.js';

const apiError = (data) => ({ response: { data } });

describe('getApiErrorMessage', () => {
  it('returns the message from the API error body', () => {
    expect(getApiErrorMessage(apiError({ message: 'email already taken' }))).toBe(
      'email already taken',
    );
  });

  it('falls back when there is no response, for example on a network error', () => {
    expect(getApiErrorMessage(new Error('Network Error'))).toBe(
      'Something went wrong. Please try again.',
    );
  });

  it('uses a custom fallback when one is given', () => {
    expect(getApiErrorMessage(undefined, 'Upload failed')).toBe('Upload failed');
  });
});

describe('getFieldErrors', () => {
  it('maps API field errors to a Formik errors object', () => {
    const error = apiError({
      errors: [
        { field: 'age', message: 'Age must be at least 16' },
        { field: 'email', message: 'Email must be a valid email address' },
      ],
    });

    expect(getFieldErrors(error)).toEqual({
      age: 'Age must be at least 16',
      email: 'Email must be a valid email address',
    });
  });

  it('keeps the first message when a field has several errors', () => {
    const error = apiError({
      errors: [
        { field: 'password', message: 'Password is required' },
        { field: 'password', message: 'Password must be between 4 and 15 characters' },
      ],
    });

    expect(getFieldErrors(error)).toEqual({ password: 'Password is required' });
  });

  it('returns an empty object when there are no field errors', () => {
    expect(getFieldErrors(apiError({ message: 'email already taken', errors: [] }))).toEqual({});
    expect(getFieldErrors(new Error('Network Error'))).toEqual({});
  });
});
