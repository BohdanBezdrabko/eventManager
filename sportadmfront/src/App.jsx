// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "@/context/AuthContext";
import AppLayout from "@/layouts/AppLayout.jsx";
import { PrivateRoute } from "@/components/RouteGuards.jsx";

// публічні
import LandingPage from "@/pages/LandingPage.jsx";
import LoginPage from "@/pages/LoginPage.jsx";
import RegisterPage from "@/pages/RegisterPage.jsx";
import NotFoundPage from "@/pages/NotFoundPage.jsx";

// приватні
import DashboardPage from "@/pages/DashboardPage.jsx";
import EventsPage from "@/pages/EventsPage.jsx";
import EventDetailPage from "@/pages/EventDetailPage.jsx";
import CreateEventPage from "@/pages/CreateEventPage.jsx";
import EditEventPage from "@/pages/EditEventPage.jsx";

import CreatePostPage from "@/pages/CreatePostPage.jsx";
import EditPostPage from "@/pages/EditPostPage.jsx";
import PostDetailPage from "@/pages/PostDetailPage.jsx";

export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>
                    {/* Публічні */}
                    <Route element={<AppLayout />}>
                        <Route index element={<LandingPage />} />
                        <Route path="/login" element={<LoginPage />} />
                        <Route path="/register" element={<RegisterPage />} />
                    </Route>

                    {/* Приватні */}
                    <Route element={<PrivateRoute />}>
                        <Route element={<AppLayout />}>
                            <Route path="/dashboard" element={<DashboardPage />} />

                            {/* Івенти */}
                            <Route path="/events" element={<EventsPage />} />
                            <Route path="/events/create" element={<CreateEventPage />} />
                            <Route path="/events/:id" element={<EventDetailPage />} />
                            <Route path="/events/:id/edit" element={<EditEventPage />} />

                            {/* Пости по івенту */}
                            <Route path="/events/:id/posts/create" element={<CreatePostPage />} />
                            {/* Аліас для сумісності з існуючими посиланнями */}
                            <Route path="/events/:id/posts/new" element={<CreatePostPage />} />
                            <Route path="/events/:id/posts/:postId" element={<PostDetailPage />} />
                            <Route path="/events/:id/posts/:postId/edit" element={<EditPostPage />} />
                        </Route>

                        {/* Редіректи + 404 */}
                        <Route path="/events/new" element={<Navigate to="/events/create" replace />} />
                        <Route path="/home" element={<Navigate to="/" replace />} />
                        <Route path="*" element={<NotFoundPage />} />
                    </Route>
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}
