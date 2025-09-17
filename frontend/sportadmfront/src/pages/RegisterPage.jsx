// src/pages/RegisterPage.jsx
import { useMemo, useState } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function RegisterPage() {
    const [form, setForm] = useState({ username: "", password: "" });
    const [error, setError] = useState("");
    const navigate = useNavigate();
    const location = useLocation();
    const { register } = useAuth();

    const roleFromQuery = useMemo(() => {
        const params = new URLSearchParams(location.search);
        const r = (params.get("role") || "").toLowerCase();
        return r === "creator" ? "creator" : "participant";
    }, [location.search]);

    const [role, setRole] = useState(roleFromQuery);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        try {
            await register({ ...form, role });
            navigate("/home", { replace: true });
        } catch (err) {
            setError(err?.message || "Помилка реєстрації");
        }
    };

    return (
        <div
            className="d-flex align-items-center justify-content-center vh-100"
            style={{
                background: "linear-gradient(135deg, #1cc88a 0%, #36b9cc 100%)",
            }}
        >
            <div
                className="card shadow-lg border-0 rounded-4 p-4"
                style={{ maxWidth: 460, width: "100%" }}
            >
                <div className="text-center mb-4">
                    <div
                        className="rounded-circle d-inline-flex align-items-center justify-content-center bg-primary text-white shadow"
                        style={{ width: 80, height: 80, fontSize: 32 }}
                    >
                        📝
                    </div>
                </div>

                <h3 className="card-title text-center mb-3 text-primary fw-bold">
                    Реєстрація
                </h3>

                <div className="mb-4 text-center">
                    <div className="btn-group" role="group" aria-label="Role switch">
                        <button
                            type="button"
                            className={`btn ${
                                role === "participant"
                                    ? "btn-primary"
                                    : "btn-outline-primary"
                            }`}
                            onClick={() => setRole("participant")}
                        >
                            Учасник
                        </button>
                        <button
                            type="button"
                            className={`btn ${
                                role === "creator"
                                    ? "btn-primary"
                                    : "btn-outline-primary"
                            }`}
                            onClick={() => setRole("creator")}
                        >
                            Організатор
                        </button>
                    </div>
                </div>

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
                        Зареєструватися
                    </button>
                </form>

                <div className="text-center mt-3">
                    <span className="text-muted">
                        Вже маєте акаунт?{" "}
                        <Link to="/login" className="fw-semibold">
                            Увійти
                        </Link>
                    </span>
                </div>
            </div>
        </div>
    );
}
