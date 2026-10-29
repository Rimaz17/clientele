import { FormControl, FormErrorMessage, FormLabel, Input, Select } from '@chakra-ui/react';
import { useField } from 'formik';

// Formik fields built on Chakra's FormControl, which links the label, marks the
// input as invalid and connects the error message through aria-describedby.
const FieldControl = ({ label, name, id, isRequired, error, children }) => (
  <FormControl id={id || name} isInvalid={Boolean(error)} isRequired={isRequired}>
    <FormLabel>{label}</FormLabel>
    {children}
    {/* red.600 keeps the small error text above 4.5:1 contrast; Chakra's red.500 is about 4:1. */}
    <FormErrorMessage color="red.600">{error}</FormErrorMessage>
  </FormControl>
);

const visibleError = (meta) => (meta.touched && meta.error ? meta.error : undefined);

export const TextField = ({ label, isRequired, ...props }) => {
  const [field, meta] = useField(props);
  return (
    <FieldControl
      label={label}
      name={props.name}
      id={props.id}
      isRequired={isRequired}
      error={visibleError(meta)}
    >
      <Input {...field} {...props} />
    </FieldControl>
  );
};

export const SelectField = ({ label, isRequired, ...props }) => {
  const [field, meta] = useField(props);
  return (
    <FieldControl
      label={label}
      name={props.name}
      id={props.id}
      isRequired={isRequired}
      error={visibleError(meta)}
    >
      <Select {...field} {...props} />
    </FieldControl>
  );
};
