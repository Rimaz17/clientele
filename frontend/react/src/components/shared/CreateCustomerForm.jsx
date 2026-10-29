import {Form, Formik} from 'formik';
import * as Yup from 'yup';
import {Button, Stack} from "@chakra-ui/react";
import {SelectField, TextField} from "./FormFields.jsx";
import {saveCustomer} from "../../services/client.js";
import {successNotification, errorNotification} from "../../services/notification.js";
import {getApiErrorMessage, getFieldErrors} from "../../services/apiErrors.js";

const CreateCustomerForm = ({ onSuccess }) => {
    return (
        <>
            <Formik
                initialValues={{
                    name: '',
                    email: '',
                    age: 0,
                    gender: '',
                    password: ''
                }}
                validationSchema={Yup.object({
                    name: Yup.string()
                        .max(15, 'Must be 15 characters or less')
                        .required('Required'),
                    email: Yup.string()
                        .email('Must be a valid email address')
                        .max(254, 'Must be 254 characters or less')
                        .required('Required'),
                    age: Yup.number()
                        .min(16, 'Must be at least 16 years of age')
                        .max(100, 'Must be 100 years of age or younger')
                        .required(),
                    password: Yup.string()
                        .min(4, 'Must be 4 characters or more')
                        .max(15, 'Must be 15 characters or less')
                        .required('Required'),
                    gender: Yup.string()
                        .oneOf(
                            ['MALE', 'FEMALE'],
                            'Invalid gender'
                        )
                        .required('Required'),
                })}
                onSubmit={(customer, {setSubmitting, setErrors}) => {
                    setSubmitting(true);
                    saveCustomer(customer)
                        .then(res => {
                            console.log(res);
                            successNotification(
                                "Customer saved",
                                `${customer.name} was successfully saved`
                            )
                            onSuccess(res.headers["authorization"]);
                        }).catch(err => {
                            console.log(err);
                            setErrors(getFieldErrors(err));
                            errorNotification(
                                err.code,
                                getApiErrorMessage(err)
                            )
                    }).finally(() => {
                         setSubmitting(false);
                    })
                }}
            >
                {({isValid, isSubmitting}) => (
                    <Form noValidate>
                        <Stack spacing={"24px"}>
                            <TextField
                                isRequired
                                label="Name"
                                name="name"
                                type="text"
                                placeholder="Jane"
                            />

                            <TextField
                                isRequired
                                label="Email Address"
                                name="email"
                                type="email"
                                placeholder="jane@formik.com"
                            />

                            <TextField
                                isRequired
                                label="Age"
                                name="age"
                                type="number"
                                placeholder="20"
                            />

                            <TextField
                                isRequired
                                label="Password"
                                name="password"
                                type="password"
                                placeholder={"pick a secure password"}
                            />

                            <SelectField label="Gender" name="gender" isRequired>
                                <option value="">Select gender</option>
                                <option value="MALE">Male</option>
                                <option value="FEMALE">Female</option>
                            </SelectField>

                            <Button disabled={!isValid || isSubmitting} type="submit">Submit</Button>
                        </Stack>
                    </Form>
                )}
            </Formik>
        </>
    );
};

export default CreateCustomerForm;