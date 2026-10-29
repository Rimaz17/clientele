const DEFAULT_ERROR_MESSAGE = 'Something went wrong. Please try again.';
const FORBIDDEN_CHANGE_MESSAGE = 'You can only change your own account.';

// Reads the ApiError body returned by the backend from an Axios error.
export const getApiErrorMessage = (error, fallback = DEFAULT_ERROR_MESSAGE) =>
  error?.response?.data?.message ?? fallback;

// For changes to a customer, a 403 means the signed-in user is neither an
// admin nor that customer, which the API's generic "Access denied" does not say.
export const getChangeErrorMessage = (error, fallback) =>
  error?.response?.status === 403 ? FORBIDDEN_CHANGE_MESSAGE : getApiErrorMessage(error, fallback);

// Turns the ApiError `errors` list into a Formik errors object, keeping the
// first message for each field. A 409 Conflict has no field list, so when
// `conflictField` is given its message is shown on that field instead.
export const getFieldErrors = (error, { conflictField } = {}) => {
  const fieldErrors = error?.response?.data?.errors ?? [];
  const errors = fieldErrors.reduce(
    (result, { field, message }) => (field in result ? result : { ...result, [field]: message }),
    {},
  );
  if (conflictField && error?.response?.status === 409) {
    return { ...errors, [conflictField]: getApiErrorMessage(error) };
  }
  return errors;
};
