// src/pages/LandingPage.jsx
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LandingPage() {
    const navigate = useNavigate();
    const { user } = useAuth();

    return (
        <div className="container py-5">
            <div className="row justify-content-center">
                <div className="col-md-8">
                    <div className="card shadow">
                        <div className="card-body text-center">
                            <h2 className="mb-3">Вітаємо у Sport Admin System</h2>
                            <p className="text-muted mb-4">Оберіть дію</p>
                            <div className="d-flex gap-3 justify-content-center">
                                {user ? (
                                    <>
                                        <button className="btn btn-primary btn-lg" onClick={() => navigate('/home')}>Перейти в кабінет</button>
                                        <button className="btn btn-outline-primary btn-lg" onClick={() => navigate('/events')}>Події</button>
                                    </>
                                ) : (
                                    <>
                                        <button className="btn btn-primary btn-lg" onClick={() => navigate('/login')}>Увійти</button>
                                        <button className="btn btn-outline-primary btn-lg" onClick={() => navigate('/register')}>Зареєструватися</button>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
