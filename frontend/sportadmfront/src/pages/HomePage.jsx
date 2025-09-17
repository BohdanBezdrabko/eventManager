// src/pages/HomePage.jsx
import { useAuth } from "../context/AuthContext";

export default function HomePage() {
    const { user } = useAuth();

    return (
        <div className="container-fluid vh-100 d-flex justify-content-center align-items-center bg-body">
            <div
                className="card shadow-lg border-0 rounded-4 p-4 text-center"
                style={{ maxWidth: 480, width: "100%" }}
            >
                <div className="mb-3">
                    <div
                        className="rounded-circle d-inline-flex align-items-center justify-content-center text-bg-primary shadow"
                        style={{ width: 80, height: 80, fontSize: 36 }}
                    >
                        👋
                    </div>
                </div>

                <h3 className="card-title mb-3 text-body-emphasis fw-bold">
                    Ласкаво просимо!
                </h3>
                <p className="lead mb-4">
                    Ви увійшли як{" "}
                    <span className="fw-semibold text-body-emphasis">
                        {user?.username ?? "користувач"}
                    </span>
                </p>

                <div className="d-grid gap-2">
                    <a href="/events" className="btn btn-primary btn-lg">
                        Переглянути події
                    </a>
                    <a href="/dashboard" className="btn btn-outline-secondary btn-lg">
                        Кабінет користувача
                    </a>
                </div>
            </div>
        </div>
    );
}
