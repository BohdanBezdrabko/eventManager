import {BrowserRouter, Routes, Route, Navigate} from "react-router-dom";
import {AuthProvider} from "@/context/AuthContext";
import AppLayout from "@/layouts/AppLayout.jsx";
import LandingPage from "@/pages/LandingPage.jsx";
import LoginPage from "@/pages/LoginPage.jsx";
import RegisterPage from "@/pages/RegisterPage.jsx";
import DashboardPage from "@/pages/DashboardPage.jsx";
import EventsPage from "@/pages/EventsPage.jsx";
import NotFoundPage from "@/pages/NotFoundPage.jsx";
import EventDetailPage from "@/pages/EventDetailPage.jsx";
import CreateEventPage from "@/pages/CreateEventPage.jsx";
import { PrivateRoute, PublicOnlyRoute, AdminRoute } from "@/components/RouteGuards.jsx";

export default function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route element={<AppLayout/>}>
                        <Route index element={<LandingPage/>}/>
                        <Route path="home" element={<Navigate to="/" replace/>}/>
                        <Route path="events" element={<EventsPage/>}/>
                        <Route path="events/:id" element={<EventDetailPage/>}/>

                        <Route path="dashboard" element={
                            <PrivateRoute><DashboardPage/></PrivateRoute>
                        }/>

                        <Route path="events/create" element={
                            <AdminRoute><CreateEventPage/></AdminRoute>
                        }/>

                        <Route path="login" element={
                            <PublicOnlyRoute><LoginPage/></PublicOnlyRoute>
                        }/>
                        <Route path="register" element={
                            <PublicOnlyRoute><RegisterPage/></PublicOnlyRoute>
                        }/>

                        <Route path="*" element={<NotFoundPage/>}/>
                    </Route>
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}
