import { Link, useNavigate } from "react-router-dom";
import {  authService  } from '../../services';
import { useCrypto } from '../../context/CryptoContext';

const Navbar = () => {
    const navigate = useNavigate();
    const { lockKeys } = useCrypto();
    const isAuthenticated = authService.isAuthenticated();
    const currentUser = authService.getCurrentUser();
    const isAdmin = currentUser && (currentUser.role === 'ADMIN' || currentUser.role === 'ADMIN_PRO');

    const handleLogout = async() => {
        try {
            await authService.logout();
            lockKeys();
            navigate("/login");
        } catch (error) {
            console.error("Logout failed:", error);
            lockKeys();
            localStorage.clear();
            navigate("/login");
        }
    };

    const handleLogoutAll = async () => {
        if (!window.confirm("Are you sure you want to log out from all devices?")) return;
        try {
            await authService.logoutAll();
            lockKeys();
            navigate('/login');
        } catch (error) {
            console.error("Logout all failed:", error);
            lockKeys();
            localStorage.clear();
            navigate("/login");
        }
    };

    return (
        <nav className="sticky top-0 z-50 w-full backdrop-blur-lg bg-slate-900/80 border-b border-white/10 shadow-lg">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex items-center justify-between h-16">
                    <div className="flex-shrink-0 flex items-center gap-3">
                        <Link to="/" className="text-2xl font-bold bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-transparent hover:from-indigo-300 hover:to-purple-300 transition-all">
                            ChatWeb E2EE
                        </Link>
                    </div>
                    
                    <div className="hidden md:block">
                        <div className="ml-10 flex items-baseline space-x-4">
                            {isAuthenticated ? (
                                <>
                                    <Link to="/chat" className="text-slate-300 hover:text-white px-3 py-2 rounded-md text-sm font-medium transition-colors">
                                        Chat Area
                                    </Link>
                                    <Link to="/profile" className="text-slate-300 hover:text-white px-3 py-2 rounded-md text-sm font-medium transition-colors">
                                        Profile
                                    </Link>
                                    {isAdmin && (
                                        <Link to="/admin" className="bg-amber-500/20 text-amber-400 hover:bg-amber-500/30 px-3 py-2 rounded-md text-sm font-medium transition-colors border border-amber-500/30">
                                            Admin
                                        </Link>
                                    )}
                                </>
                            ) : (
                                <>
                                    <Link to="/login" className="text-slate-300 hover:text-white px-3 py-2 rounded-md text-sm font-medium transition-colors">
                                        Login
                                    </Link>
                                    <Link to="/signup" className="bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors shadow-lg shadow-indigo-500/30">
                                        Signup
                                    </Link>
                                </>
                            )}
                        </div>
                    </div>
                    
                    {isAuthenticated && (
                        <div className="flex items-center gap-4">
                            <div className="hidden sm:flex items-center gap-2 text-sm text-slate-300">
                                <div className="w-8 h-8 rounded-full flex items-center justify-center text-white font-bold" style={{ backgroundColor: currentUser.color || '#4f46e5' }}>
                                    {currentUser.username.charAt(0).toUpperCase()}
                                </div>
                                <span className="font-medium">{currentUser.username}</span>
                            </div>
                            <div className="flex gap-2">
                                <button 
                                    onClick={handleLogout}
                                    className="bg-slate-700/50 hover:bg-slate-700 text-slate-300 hover:text-white px-3 py-1.5 rounded-md text-sm font-medium transition-colors border border-slate-600/50"
                                >
                                    Logout
                                </button>
                                <button 
                                    onClick={handleLogoutAll}
                                    className="bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 px-3 py-1.5 rounded-md text-sm font-medium transition-colors border border-rose-500/20"
                                    title="Logout from all devices"
                                >
                                    Logout All
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </nav>
    );
};

export default Navbar;