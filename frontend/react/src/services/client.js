import axios from 'axios';

const getAuthConfig = () => ({
    headers: {
        Authorization: `Bearer ${localStorage.getItem("access_token")}`
    }
})

// Returns one page: { content, page, size, totalElements, totalPages }.
// params: page (0-based), size, sort, search, gender, minAge, maxAge.
export const getCustomers = async (params = {}) =>
    axios.get(
        `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers`,
        {
            ...getAuthConfig(),
            params
        }
    )

export const saveCustomer = async (customer) =>
    axios.post(
        `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers`,
        customer
    )

export const updateCustomer = async (id, update) =>
    axios.put(
        `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/${id}`,
        update,
        getAuthConfig()
    )

export const deleteCustomer = async (id) =>
    axios.delete(
        `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/${id}`,
        getAuthConfig()
    )

export const login = async (usernameAndPassword) =>
    axios.post(
        `${import.meta.env.VITE_API_BASE_URL}/api/v1/auth/login`,
        usernameAndPassword
    )

export const uploadCustomerProfilePicture = async (id, formData) =>
    axios.post(
        `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/${id}/profile-image`,
        formData,
        {
            ...getAuthConfig(),
            'Content-Type' : 'multipart/form-data'
        }
    );

export const customerProfilePictureUrl = (id) =>
    `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/${id}/profile-image`;
