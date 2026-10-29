import { describe, expect, it } from 'vitest';
import { getApiErrorMessage, getChangeErrorMessage, getFieldErrors } from './apiErrors.js';

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

describe('getChangeErrorMessage', () => {
  it('explains a 403 as a permission problem', () => {
    const error = { response: { status: 403, data: { message: 'Access denied' } } };

    expect(getChangeErrorMessage(error)).toBe('You can only change your own account.');
  });

  it('returns the API message for other errors', () => {
    const error = { response: { status: 409, data: { message: 'email already taken' } } };

    expect(getChangeErrorMessage(error)).toBe('email already taken');
  });

  it('uses the fallback when there is no response', () => {
    expect(getChangeErrorMessage(new Error('Network Error'), 'Upload failed')).toBe(
      'Upload failed',
    );
    expect(getChangeErrorMessage(new Error('Network Error'))).toBe(
      'Something went wrong. Please try again.',
    );
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

  it('reports a 409 conflict on the given field', () => {
    const conflict = {
      response: { status: 409, data: { message: 'email already taken', errors: [] } },
    };

    expect(getFieldErrors(conflict, { conflictField: 'email' })).toEqual({
      email: 'email already taken',
    });
    expect(getFieldErrors(conflict)).toEqual({});
  });

  it('ignores conflictField for errors other than 409', () => {
    const badRequest = { response: { status: 400, data: { message: 'no data changes found' } } };

    expect(getFieldErrors(badRequest, { conflictField: 'email' })).toEqual({});
  });

  it('returns an empty object when there are no field errors', () => {
    expect(getFieldErrors(apiError({ message: 'email already taken', errors: [] }))).toEqual({});
    expect(getFieldErrors(new Error('Network Error'))).toEqual({});
  });
});
