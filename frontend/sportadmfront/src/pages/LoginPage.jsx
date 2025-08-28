// src/pages/LoginPage.jsx
import { useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
    const [form, setForm] = useState({ username: '', password: '' });
    const [error, setError] = useState('');
    const { login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const from = location.state?.from?.pathname || '/home';

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            await login(form);
            navigate(from, { replace: true });
        } catch (err) {
            setError(err?.message || 'Помилка входу');
        }
    };

    return (
        <div className="container-fluid vh-100 d-flex justify-content-center align-items-center">
            <div className="card shadow-lg" style={{ maxWidth: 420, width: '100%' }}>
                <div className="card-body">
                    <h3 className="card-title text-center mb-4">Увійти</h3>
                    {error && <div className="alert alert-danger">{error}</div>}
                    <form onSubmit={handleSubmit}>
                        <div className="mb-3">
                            <label className="form-label">Логін</label>
                            <input
                                name="username"
                                className="form-control"
                                value={form.username}
                                onChange={(e) => setForm({ ...form, username: e.target.value })}
                                required
                            />
                        </div>
                        <div className="mb-4">
                            <label className="form-label">Пароль</label>
                            <input
                                type="password"
                                name="password"
                                className="form-control"
                                value={form.password}
                                onChange={(e) => setForm({ ...form, password: e.target.value })}
                                required
                            />
                        </div>
                        <button className="btn btn-primary w-100" type="submit">Увійти</button>
                    </form>
                    <div className="text-center mt-3">
                        Немає акаунта? <Link to="/register">Зареєструватися</Link>
                    </div>
                </div>
            </div>
        </div>
    );
}
