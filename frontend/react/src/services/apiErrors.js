const DEFAULT_ERROR_MESSAGE = 'Something went wrong. Please try again.';

// Reads the ApiError body returned by the backend from an Axios error.
export const getApiErrorMessage = (error, fallback = DEFAULT_ERROR_MESSAGE) =>
  error?.response?.data?.message ?? fallback;

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
