// src/pages/HomePage.jsx
import { useAuth } from '../context/AuthContext';

export default function HomePage() {
    const { user } = useAuth();

    return (
        <div className="container-fluid vh-100 d-flex justify-content-center align-items-center">
            <div className="card shadow-lg" style={{ maxWidth: 480, width: '100%' }}>
                <div className="card-body text-center">
                    <h3 className="card-title mb-3">Ласкаво просимо!</h3>
                    <p className="lead">Ви увійшли як <strong>{user?.username ?? 'користувач'}</strong></p>
                </div>
            </div>
        </div>
    );
}
