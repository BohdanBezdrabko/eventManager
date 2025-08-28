// src/pages/NotFoundPage.jsx
import { Link } from 'react-router-dom';

export default function NotFoundPage() {
    return (
        <div className="container py-5 text-center">
            <h1 className="display-5 mb-3">404</h1>
            <p className="lead mb-4">Сторінку не знайдено</p>
            <Link to="/" className="btn btn-primary">На головну</Link>
        </div>
    );
}
