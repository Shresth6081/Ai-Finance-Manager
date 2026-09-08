import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8085/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Attach the JWT to every outgoing request
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// If any response comes back as 401 (token expired / rejected by server),
// clear stored credentials and redirect to the login page.
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            // Hard redirect so React re-mounts and AuthContext resets cleanly
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default api;
