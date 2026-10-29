import {Form, Formik} from 'formik';
import * as Yup from 'yup';
import {Avatar, Box, Button, Stack, Text, VStack} from "@chakra-ui/react";
import {TextField} from "../shared/FormFields.jsx";
import {customerProfilePictureUrl, updateCustomer, uploadCustomerProfilePicture} from "../../services/client.js";
import {errorNotification, successNotification} from "../../services/notification.js";
import {getApiErrorMessage, getFieldErrors} from "../../services/apiErrors.js";
import {useCallback} from "react";
import {useDropzone} from "react-dropzone";
import {useAuth} from "../context/AuthContext.jsx";

// Mirrors the image types and the 10 MB upload limit the API accepts.
const ACCEPTED_IMAGE_TYPES = {
    "image/jpeg": [],
    "image/png": [],
    "image/webp": []
}
const MAX_IMAGE_SIZE = 10 * 1024 * 1024

const rejectionMessage = (fileRejections) => {
    const code = fileRejections[0]?.errors[0]?.code
    if (code === "file-too-large") {
        return "The picture must be 10 MB or smaller"
    }
    if (code === "too-many-files") {
        return "Choose one picture at a time"
    }
    return "Choose a JPEG, PNG or WebP image"
}

const MyDropzone = ({ customerId, fetchCustomers }) => {
    const onDrop = useCallback((acceptedFiles, fileRejections) => {
        if (acceptedFiles.length === 0) {
            errorNotification("Upload failed", rejectionMessage(fileRejections))
            return
        }
        const formData = new FormData();
        formData.append("file", acceptedFiles[0])

        uploadCustomerProfilePicture(
            customerId,
            formData
        ).then(() => {
            successNotification("Profile picture updated", "The new picture is now shown")
            fetchCustomers()
        }).catch(err => {
            errorNotification("Upload failed", getApiErrorMessage(err, "The profile picture could not be uploaded"))
        })
    }, [customerId, fetchCustomers])
    const {getRootProps, getInputProps, isDragActive} = useDropzone({
        onDrop,
        accept: ACCEPTED_IMAGE_TYPES,
        maxSize: MAX_IMAGE_SIZE,
        maxFiles: 1
    })

    return (
        <Box {...getRootProps({role: "button", "aria-label": "Upload a new profile picture"})}
             w={'100%'}
             textAlign={'center'}
             border={'dashed'}
             borderColor={'gray.200'}
             borderRadius={'3xl'}
             p={6}
             rounded={'md'}
             cursor={'pointer'}
             _focusVisible={{boxShadow: 'outline', outline: 'none'}}>
            <input {...getInputProps()} />
            <Text>
                {isDragActive ? "Drop the picture here" : "Drag a picture here, or click to choose one"}
            </Text>
            <Text fontSize={'sm'} color={'gray.600'} mt={1}>
                JPEG, PNG or WebP, up to 10 MB
            </Text>
        </Box>
    )
}

const UpdateCustomerForm = ({fetchCustomers, initialValues, customerId}) => {
    const { storeToken } = useAuth()
    return (
        <>
            <VStack spacing={'5'} mb={'5'}>
                {/* Avatar falls back to the customer's initials when there is no picture yet. */}
                <Avatar
                    boxSize={'150px'}
                    size={'2xl'}
                    name={initialValues.name}
                    src={customerProfilePictureUrl(customerId)}
                />
                <MyDropzone
                    customerId={customerId}
                    fetchCustomers={fetchCustomers}
                />
            </VStack>
            <Formik
                initialValues={initialValues}
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
                })}
                onSubmit={(updatedCustomer, {setSubmitting, setErrors}) => {
                    setSubmitting(true);
                    updateCustomer(customerId, updatedCustomer)
                        .then(res => {
                            console.log(res);
                            // Users who change their own email get a new token,
                            // because the old one names the old email.
                            const newToken = res.headers?.authorization
                            if (newToken) {
                                storeToken(newToken)
                            }
                            successNotification(
                                "Customer updated",
                                `${updatedCustomer.name} was successfully updated`
                            )
                            fetchCustomers();
                        }).catch(err => {
                        console.log(err);
                        setErrors(getFieldErrors(err, {conflictField: "email"}));
                        errorNotification(
                            "Could not update customer",
                            getApiErrorMessage(err)
                        )
                    }).finally(() => {
                        setSubmitting(false);
                    })
                }}
            >
                {({isValid, isSubmitting, dirty}) => (
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

                            <Button disabled={!(isValid && dirty) || isSubmitting} type="submit">Submit</Button>
                        </Stack>
                    </Form>
                )}
            </Formik>
        </>
    );
};

export default UpdateCustomerForm;