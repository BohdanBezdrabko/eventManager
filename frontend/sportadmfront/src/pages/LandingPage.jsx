// src/pages/LandingPage.jsx
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function LandingPage() {
    const navigate = useNavigate();
    const { user } = useAuth();

    return (
        <div
            className="d-flex align-items-center justify-content-center vh-100 text-center bg-body"
        >
            <div
                className="card shadow-lg border-0 rounded-4 p-5"
                style={{ maxWidth: 600, width: "100%" }}
            >
                <div className="mb-4">
                    <div
                        className="rounded-circle d-inline-flex align-items-center justify-content-center text-bg-primary shadow"
                        style={{ width: 90, height: 90, fontSize: 38 }}
                    >
                        🏐
                    </div>
                </div>

                <h2 className="fw-bold mb-3 text-body-emphasis">
                    Вітаємо у Sport Admin System
                </h2>
                <p className="mb-4 text-muted">
                    Керуйте своїми подіями та обліковим записом легко й швидко
                </p>

                <div className="d-flex gap-3 flex-wrap justify-content-center">
                    {user ? (
                        <>
                            <button
                                className="btn btn-primary btn-lg px-4"
                                onClick={() => navigate("/dashboard")}
                            >
                                Перейти в кабінет
                            </button>
                            <button
                                className="btn btn-outline-secondary btn-lg px-4"
                                onClick={() => navigate("/events")}
                            >
                                Події
                            </button>
                        </>
                    ) : (
                        <>
                            <button
                                className="btn btn-primary btn-lg px-4"
                                onClick={() => navigate("/login")}
                            >
                                Увійти
                            </button>
                            <button
                                className="btn btn-outline-secondary btn-lg px-4"
                                onClick={() => navigate("/register")}
                            >
                                Зареєструватися
                            </button>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}
