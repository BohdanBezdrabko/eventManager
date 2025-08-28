// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import AppLayout from './layouts/AppLayout';
import { PrivateRoute, PublicOnlyRoute } from './components/RouteGuards';

import HomePage from './pages/HomePage';
import DashboardPage from './pages/DashboardPage';
import EventsPage from './pages/EventsPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import LandingPage from './pages/LandingPage';
import NotFoundPage from '@pages/NotFoundPage.jsx';

export default function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    {/* Публічні сторінки без Layout */}
                    <Route path="/" element={<LandingPage />} />
                    <Route element={<PublicOnlyRoute />}>
                        <Route path="/login" element={<LoginPage />} />
                        <Route path="/register" element={<RegisterPage />} />
                    </Route>

                    {/* Приватні сторінки з Layout */}
                    <Route element={<AppLayout />}>
                        <Route element={<PrivateRoute />}>
                            <Route path="/home" element={<HomePage />} />
                            <Route path="/dashboard" element={<DashboardPage />} />
                            <Route path="/events" element={<EventsPage />} />
                        </Route>

                        {/* Перенаправлення */}
                        <Route path="/welcome" element={<Navigate to="/" replace />} />
                    </Route>

                    {/* 404 окремо, без Layout */}
                    <Route path="*" element={<NotFoundPage />} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}
