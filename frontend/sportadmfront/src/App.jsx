import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/context/AuthContext';
import AppLayout from '@/layouts/AppLayout.jsx';
import LandingPage from '@/pages/LandingPage.jsx';
import LoginPage from '@/pages/LoginPage.jsx';
import RegisterPage from '@/pages/RegisterPage.jsx';
import DashboardPage from '@/pages/DashboardPage.jsx';
import EventsPage from '@/pages/EventsPage.jsx';
import NotFoundPage from '@/pages/NotFoundPage.jsx';

export default function App() {
    return (
        <AuthProvider>
            <BrowserRouter /* basename="/" якщо треба підкаталог */>
                <Routes>
                    <Route element={<AppLayout />}>
                        <Route index element={<LandingPage />} />
                        <Route path="home" element={<Navigate to="/" replace />} />
                        <Route path="events" element={<EventsPage />} />
                        <Route path="dashboard" element={<DashboardPage />} />
                        <Route path="login" element={<LoginPage />} />
                        <Route path="register" element={<RegisterPage />} />
                        <Route path="*" element={<NotFoundPage />} />
                    </Route>
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}
