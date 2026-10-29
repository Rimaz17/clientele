import {
    createContext,
    useContext,
    useState
} from "react";
import {login as performLogin} from "../../services/client.js";
import jwtDecode from "jwt-decode";

const AuthContext = createContext({});

const TOKEN_KEY = "access_token";
const ADMIN_ROLE = "ROLE_ADMIN";

// The token is the source of truth: its subject is the email and its scopes
// are the roles. A token that cannot be decoded counts as logged out.
const customerFromToken = (token) => {
    if (!token) {
        return null;
    }
    try {
        const decodedToken = jwtDecode(token);
        return {
            username: decodedToken.sub,
            roles: decodedToken.scopes ?? []
        }
    } catch {
        return null;
    }
}

const AuthProvider = ({ children }) => {

    const [customer, setCustomer] = useState(
        () => customerFromToken(localStorage.getItem(TOKEN_KEY))
    );

    const setCustomerFromToken = () => {
        setCustomer(customerFromToken(localStorage.getItem(TOKEN_KEY)))
    }

    // Used after login and when the API sends a replacement token, for example
    // after users change their own email.
    const storeToken = (jwtToken) => {
        localStorage.setItem(TOKEN_KEY, jwtToken);
        setCustomer(customerFromToken(jwtToken))
    }

    const login = async (usernameAndPassword) => {
        return new Promise((resolve, reject) => {
            performLogin(usernameAndPassword).then(res => {
                storeToken(res.headers["authorization"]);
                resolve(res);
            }).catch(err => {
                reject(err);
            })
        })
    }

    const logOut = () => {
        localStorage.removeItem(TOKEN_KEY)
        setCustomer(null)
    }

    const isCustomerAuthenticated = () => {
        const token = localStorage.getItem(TOKEN_KEY);
        if (!token || !customerFromToken(token)) {
            return false;
        }
        const { exp: expiration } = jwtDecode(token);
        if (Date.now() > expiration * 1000) {
            logOut()
            return false;
        }
        return true;
    }

    const isAdmin = customer?.roles.includes(ADMIN_ROLE) ?? false;

    // Emails are compared case-insensitively, like the API does.
    const isCurrentCustomer = (email) =>
        Boolean(customer?.username && email) &&
        customer.username.toLowerCase() === email.toLowerCase();

    return (
        <AuthContext.Provider value={{
            customer,
            isAdmin,
            isCurrentCustomer,
            login,
            logOut,
            isCustomerAuthenticated,
            setCustomerFromToken,
            storeToken
        }}>
            {children}
        </AuthContext.Provider>
    )
}

export const useAuth = () => useContext(AuthContext);

export default AuthProvider;
