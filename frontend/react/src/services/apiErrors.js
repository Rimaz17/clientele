const DEFAULT_ERROR_MESSAGE = 'Something went wrong. Please try again.';

// Reads the ApiError body returned by the backend from an Axios error.
export const getApiErrorMessage = (error, fallback = DEFAULT_ERROR_MESSAGE) =>
  error?.response?.data?.message ?? fallback;

// Turns the ApiError `errors` list into a Formik errors object, keeping the
// first message for each field.
export const getFieldErrors = (error) => {
  const fieldErrors = error?.response?.data?.errors ?? [];
  return fieldErrors.reduce(
    (errors, { field, message }) => (field in errors ? errors : { ...errors, [field]: message }),
    {},
  );
};
