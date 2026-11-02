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

// Aggregates for the dashboard: totals, gender split, age ranges and sign-ups
// per month (see GET /api/v1/customers/stats).
export const getCustomerStats = async () =>
    axios.get(
        `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/stats`,
        getAuthConfig()
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

// Every upload gets a new profileImageId. Putting it in the URL gives each
// picture its own address, so a new upload is fetched instead of the browser
// reusing the image it already has for the old one. The API ignores `v`.
// Without a picture there is nothing to fetch, so there is no URL and the
// avatar shows the customer's initials without a request that would 404.
export const customerProfilePictureUrl = (id, profileImageId) =>
    profileImageId
        ? `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/${id}/profile-image?v=${encodeURIComponent(profileImageId)}`
        : undefined;
