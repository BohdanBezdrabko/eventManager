// src/pages/RegisterPage.jsx
import { useMemo, useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
    const [form, setForm] = useState({ username: '', password: '' });
    const [error, setError] = useState('');
    const navigate = useNavigate();
    const location = useLocation();
    const { register } = useAuth();

    const roleFromQuery = useMemo(() => {
        const params = new URLSearchParams(location.search);
        const r = (params.get('role') || '').toLowerCase();
        return r === 'creator' ? 'creator' : 'participant';
    }, [location.search]);

    const [role, setRole] = useState(roleFromQuery);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            await register({ ...form, role });
            navigate('/home', { replace: true });
        } catch (err) {
            setError(err?.message || 'Помилка реєстрації');
        }
    };

    return (
        <div className="container-fluid vh-100 d-flex justify-content-center align-items-center">
            <div className="card shadow-lg" style={{ maxWidth: 460, width: '100%' }}>
                <div className="card-body">
                    <h3 className="card-title text-center mb-2">Реєстрація</h3>

                    <div className="mb-3 text-center">
                        <div className="btn-group" role="group" aria-label="Role switch">
                            <button
                                type="button"
                                className={`btn ${role === 'participant' ? 'btn-primary' : 'btn-outline-primary'}`}
                                onClick={() => setRole('participant')}
                            >
                                Учасник
                            </button>
                            <button
                                type="button"
                                className={`btn ${role === 'creator' ? 'btn-primary' : 'btn-outline-primary'}`}
                                onClick={() => setRole('creator')}
                            >
                                Організатор
                            </button>
                        </div>
                    </div>

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
                        <button className="btn btn-primary w-100" type="submit">Зареєструватися</button>
                    </form>

                    <div className="text-center mt-2">
                        Вже маєте акаунт? <Link to="/login">Увійти</Link>
                    </div>
                </div>
            </div>
        </div>
    );
}
