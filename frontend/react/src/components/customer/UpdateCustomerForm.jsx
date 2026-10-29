import {Form, Formik} from 'formik';
import * as Yup from 'yup';
import {Box, Button, Image, Stack, VStack} from "@chakra-ui/react";
import {TextField} from "../shared/FormFields.jsx";
import {customerProfilePictureUrl, updateCustomer, uploadCustomerProfilePicture} from "../../services/client.js";
import {errorNotification, successNotification} from "../../services/notification.js";
import {getApiErrorMessage, getFieldErrors} from "../../services/apiErrors.js";
import {useCallback} from "react";
import {useDropzone} from "react-dropzone";

// Mirrors the image types the API accepts for profile pictures.
const ACCEPTED_IMAGE_TYPES = {
    "image/jpeg": [],
    "image/png": [],
    "image/webp": []
}

const MyDropzone = ({ customerId, fetchCustomers }) => {
    const onDrop = useCallback(acceptedFiles => {
        if (acceptedFiles.length === 0) {
            errorNotification("Error", "Choose a JPEG, PNG or WebP image")
            return
        }
        const formData = new FormData();
        formData.append("file", acceptedFiles[0])

        uploadCustomerProfilePicture(
            customerId,
            formData
        ).then(() => {
            successNotification("Success", "Profile picture uploaded")
            fetchCustomers()
        }).catch(err => {
            errorNotification("Error", getApiErrorMessage(err, "Profile picture failed upload"))
        })
    }, [customerId, fetchCustomers])
    const {getRootProps, getInputProps, isDragActive} = useDropzone({
        onDrop,
        accept: ACCEPTED_IMAGE_TYPES,
        maxFiles: 1
    })

    return (
        <Box {...getRootProps()}
             w={'100%'}
             textAlign={'center'}
             border={'dashed'}
             borderColor={'gray.200'}
             borderRadius={'3xl'}
             p={6}
             rounded={'md'}>
            <input {...getInputProps()} />
            {
                isDragActive ?
                    <p>Drop the picture here ...</p> :
                    <p>Drag &apos;n&apos; drop picture here, or click to select picture</p>
            }
        </Box>
    )
}

const UpdateCustomerForm = ({fetchCustomers, initialValues, customerId}) => {
    return (
        <>
            <VStack spacing={'5'} mb={'5'}>
                <Image
                    borderRadius={'full'}
                    boxSize={'150px'}
                    objectFit={'cover'}
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
                            successNotification(
                                "Customer updated",
                                `${updatedCustomer.name} was successfully updated`
                            )
                            fetchCustomers();
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