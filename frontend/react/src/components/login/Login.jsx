import {
    Button,
    Link,
    Stack,
} from '@chakra-ui/react';
import {Formik, Form} from "formik";
import {TextField} from "../shared/FormFields.jsx";
import * as Yup from 'yup';
import {useAuth} from "../context/AuthContext.jsx";
import {errorNotification} from "../../services/notification.js";
import {getApiErrorMessage} from "../../services/apiErrors.js";
import {useNavigate} from "react-router-dom";
import {useEffect} from "react";
import AuthLayout from "../shared/AuthLayout.jsx";

const LoginForm = () => {
    const { login } = useAuth();
    const navigate = useNavigate();

    return (
        <Formik
            validateOnMount={true}
            validationSchema={
                Yup.object({
                    username: Yup.string()
                        .email("Must be valid email")
                        .required("Email is required"),
                    password: Yup.string()
                        .max(20, "Password cannot be more than 20 characters")
                        .required("Password is required")
                })
            }
            initialValues={{username: '', password: ''}}
            onSubmit={(values, {setSubmitting}) => {
                setSubmitting(true);
                login(values).then(() => {
                    navigate("/dashboard")
                    console.log("Successfully logged in");
                }).catch(err => {
                    errorNotification(
                        "Sign in failed",
                        getApiErrorMessage(err)
                    )
                }).finally(() => {
                    setSubmitting(false);
                })
            }}>

            {({isValid, isSubmitting}) => (
                <Form noValidate>
                    <Stack mt={15} spacing={15}>
                        <TextField
                            isRequired
                            label={"Email"}
                            name={"username"}
                            type={"email"}
                            placeholder={"you@example.com"}
                        />
                        <TextField
                            isRequired
                            label={"Password"}
                            name={"password"}
                            type={"password"}
                            placeholder={"Type your password"}
                        />

                        <Button
                            type={"submit"}
                            disabled={!isValid || isSubmitting}>
                            Sign in
                        </Button>
                    </Stack>
                </Form>
            )}

        </Formik>
    )
}

const Login = () => {

    const { customer } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (customer) {
            navigate("/dashboard/customers");
        }
    })

    return (
        <AuthLayout
            title="Sign in to your account"
            footer={
                <Link color={"brand.blueDark"} href={"/signup"}>
                    Don&apos;t have an account? Sign up
                </Link>
            }>
            <LoginForm/>
        </AuthLayout>
    );
}

export default Login;