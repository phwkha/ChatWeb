import { useNavigate } from "react-router-dom";
import {  authService  } from '../../services';

const MainPage = () => {
    const navigate = useNavigate();
    const isAuthenticated = authService.isAuthenticated();
    
    const handleGettingStarted = () => {
        if (isAuthenticated) {
            navigate("/chat");
        } else {
            navigate("/signup");
        }
    };

    const handleLearnMore = () => {
        window.open("https://github.com", "_blank");
    };

    return (
        <div className="relative min-h-[calc(100vh-4rem)] flex items-center justify-center overflow-hidden">
            {/* Background Decorations */}
            <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-indigo-600/20 rounded-full blur-[100px] pointer-events-none"></div>
            <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-600/20 rounded-full blur-[100px] pointer-events-none"></div>
            
            <div className="relative z-10 glass-panel p-12 md:p-16 rounded-3xl max-w-3xl w-full mx-4 text-center animate-enter shadow-2xl border border-white/10">
                <h1 className="text-5xl md:text-6xl font-extrabold mb-6 tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-purple-400">
                    Secure. Private. Fast.
                </h1>
                <p className="text-xl md:text-2xl text-slate-300 mb-10 max-w-2xl mx-auto font-light">
                    Experience real-time messaging with military-grade End-to-End Encryption. Your conversations, your privacy.
                </p>
                
                <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
                    <button
                        className="w-full sm:w-auto glass-button text-lg px-8 py-4"
                        onClick={handleGettingStarted}
                    >
                        {isAuthenticated ? "Go to Chat" : "Get Started Now"}
                    </button>

                    <button
                        className="w-full sm:w-auto bg-slate-800 hover:bg-slate-700 text-white border border-slate-600 hover:border-slate-500 font-medium py-4 px-8 rounded-xl transition-all text-lg"
                        onClick={handleLearnMore}
                    >
                        Learn More
                    </button>
                </div>
            </div>
        </div>
    );
};

export default MainPage;