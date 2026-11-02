import {useAuth} from "../context/AuthContext.jsx";
import {useNavigate} from "react-router-dom";
import {useEffect} from "react";
import {Link} from "@chakra-ui/react";
import AuthLayout from "../shared/AuthLayout.jsx";
import CreateCustomerForm from "../shared/CreateCustomerForm.jsx";

const Signup = () => {
    const { customer, setCustomerFromToken } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (customer) {
            navigate("/dashboard/customers");
        }
    })

    return (
        <AuthLayout
            title="Register for an account"
            footer={
                <Link color={"brand.blueDark"} href={"/"}>
                    Already have an account? Sign in
                </Link>
            }>
            <CreateCustomerForm onSuccess={(token) => {
                localStorage.setItem("access_token", token)
                setCustomerFromToken()
                navigate("/dashboard");
            }}/>
        </AuthLayout>
    );
}

export default Signup;