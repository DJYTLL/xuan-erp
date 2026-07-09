import axios from 'axios';

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8100',
  timeout: 15000,
});

export function setBearerToken(token: string | null) {
  if (token) {
    http.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete http.defaults.headers.common.Authorization;
  }
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('xuan-erp-token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('xuan-erp-token');
      window.dispatchEvent(new CustomEvent('xuan-auth-expired'));
    }
    return Promise.reject(error);
  },
);
