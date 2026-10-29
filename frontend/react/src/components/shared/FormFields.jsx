import { FormControl, FormErrorMessage, FormLabel, Input, Select } from '@chakra-ui/react';
import { useField } from 'formik';

// Formik fields built on Chakra's FormControl, which links the label, marks the
// input as invalid and connects the error message through aria-describedby.
const FieldControl = ({ label, name, id, isRequired, error, children }) => (
  <FormControl id={id || name} isInvalid={Boolean(error)} isRequired={isRequired}>
    <FormLabel>{label}</FormLabel>
    {children}
    <FormErrorMessage>{error}</FormErrorMessage>
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
