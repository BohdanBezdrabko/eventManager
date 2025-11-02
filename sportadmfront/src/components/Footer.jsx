// src/components/Footer.jsx
export default function Footer() {
    return (
        <footer className="py-3 border-top mt-auto">
            <div className="container text-center">
                <small>© {new Date().getFullYear()} SportAdmin</small>
            </div>
        </footer>
    );
}
