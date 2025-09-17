// src/pages/LoginPage.jsx
import { useState } from "react";
import { useLocation, useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
    const [form, setForm] = useState({ username: "", password: "" });
    const [error, setError] = useState("");
    const { login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const from = location.state?.from?.pathname || "/home";

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        try {
            await login(form);
            navigate(from, { replace: true });
        } catch (err) {
            setError(err?.message || "Помилка входу");
        }
    };

    return (
        <div
            className="d-flex align-items-center justify-content-center vh-100"
            style={{
                background: "linear-gradient(135deg, #4e73df 0%, #1cc88a 100%)",
            }}
        >
            <div
                className="card shadow-lg border-0 rounded-4 p-4"
                style={{ maxWidth: 420, width: "100%" }}
            >
                <div className="text-center mb-4">
                    <div
                        className="rounded-circle d-inline-flex align-items-center justify-content-center bg-primary text-white shadow"
                        style={{ width: 80, height: 80, fontSize: 32 }}
                    >
                        🔑
                    </div>
                </div>

                <h3 className="card-title text-center mb-3 text-primary fw-bold">
                    Вхід до системи
                </h3>

                {error && <div className="alert alert-danger">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="mb-3">
                        <label className="form-label fw-semibold">Логін</label>
                        <input
                            name="username"
                            className="form-control form-control-lg"
                            value={form.username}
                            onChange={(e) =>
                                setForm({ ...form, username: e.target.value })
                            }
                            required
                        />
                    </div>
                    <div className="mb-4">
                        <label className="form-label fw-semibold">Пароль</label>
                        <input
                            type="password"
                            name="password"
                            className="form-control form-control-lg"
                            value={form.password}
                            onChange={(e) =>
                                setForm({ ...form, password: e.target.value })
                            }
                            required
                        />
                    </div>
                    <button
                        className="btn btn-primary w-100 btn-lg shadow-sm"
                        type="submit"
                    >
                        Увійти
                    </button>
                </form>

                <div className="text-center mt-3">
                    <span className="text-muted">
                        Немає акаунта?{" "}
                        <Link to="/register" className="fw-semibold">
                            Зареєструватися
                        </Link>
                    </span>
                </div>
            </div>
        </div>
    );
}
