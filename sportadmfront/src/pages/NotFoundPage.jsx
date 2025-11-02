// src/pages/NotFoundPage.jsx
import { Link } from "react-router-dom";

export default function NotFoundPage() {
    return (
        <div
            className="d-flex flex-column align-items-center justify-content-center vh-100 text-center"
            style={{
                background: "linear-gradient(135deg, #e74a3b 0%, #f6c23e 100%)",
                color: "#fff",
            }}
        >
            <div
                className="rounded-circle d-inline-flex align-items-center justify-content-center bg-light text-danger shadow mb-4"
                style={{ width: 120, height: 120, fontSize: 50 }}
            >
                ❌
            </div>

            <h1 className="display-3 fw-bold mb-2">404</h1>
            <p className="lead mb-4">Сторінку не знайдено</p>

            <Link
                to="/"
                className="btn btn-light btn-lg fw-semibold shadow-sm px-4"
            >
                На головну
            </Link>
        </div>
    );
}
