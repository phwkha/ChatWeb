import React, { useState, useEffect } from 'react';
import { useCrypto } from '../../context/CryptoContext';
import {  keyService  } from '../../services';

export const KeyUnlockModal = () => {
    const { unlockKeys, generateAndSaveKeys, isGenerating, error: cryptoError, cancelUnlock} = useCrypto();
    const [pin, setPin] = useState('');
    const [hasKeys, setHasKeys] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    const [isResetMode, setIsResetMode] = useState(false);
    const [localError, setLocalError] = useState('');

    useEffect(() => {
        const checkKey = async () => {
            setIsLoading(true);
            try {
                const key = await keyService.getEncryptedRsaKey();
                setHasKeys(!!key);
            } catch (e) {
                console.error("Không thể kiểm tra khóa", e);
                setHasKeys(false);
            }
            setIsLoading(false);
        };
        checkKey();
    }, []);

    useEffect(() => {
        setLocalError('');
        setPin('');
    }, [isResetMode]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLocalError('');

        if (pin.length < 4) {
            setLocalError("Mã PIN phải có ít nhất 4 số.");
            return;
        }

        if (isResetMode) {
            const success = await generateAndSaveKeys(pin);
            if (success) {
                // Success will close modal from Context
            }
        } else if (hasKeys) {
            await unlockKeys(pin);
        } else {
            await generateAndSaveKeys(pin);
        }
    };

    if (isLoading) {
        return (
            <div className="fixed inset-0 z-[9999] bg-slate-900/80 backdrop-blur-md flex items-center justify-center p-4">
                <div className="glass-panel p-8 rounded-3xl animate-enter">
                    <p className="text-slate-200 flex items-center gap-3 font-medium">
                        <svg className="animate-spin h-5 w-5 text-indigo-500" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                        Đang kiểm tra khóa bảo mật...
                    </p>
                </div>
            </div>
        );
    }

    if (isResetMode) {
        return (
            <div className="fixed inset-0 z-[9999] bg-slate-900/90 backdrop-blur-md flex items-center justify-center p-4">
                <div className="glass-panel max-w-md w-full p-8 rounded-3xl animate-enter border-rose-500/30 shadow-[0_0_40px_rgba(225,29,72,0.15)]" onClick={(e) => e.stopPropagation()}>
                    <div className="text-center mb-6">
                        <h2 className="text-2xl font-bold text-rose-500 mb-4 flex items-center justify-center gap-2">
                            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                            CẢNH BÁO MẤT DỮ LIỆU
                        </h2>
                        <div className="text-left text-sm text-slate-300 space-y-2 bg-rose-500/10 p-4 rounded-xl border border-rose-500/20">
                            <p>Bạn đang yêu cầu <b>Tạo lại khóa bảo mật mới</b> vì quên mã PIN cũ.</p>
                            <p>Hành động này sẽ:</p>
                            <ul className="list-disc pl-4 space-y-1 text-rose-300">
                                <li>Tạo một cặp khóa mã hóa mới.</li>
                                <li><b>VÔ HIỆU HÓA</b> khả năng đọc toàn bộ tin nhắn cũ.</li>
                                <li>Chỉ có thể đọc được các tin nhắn mới từ thời điểm này.</li>
                            </ul>
                            <p className="font-bold text-white pt-2 text-center">Bạn có chắc chắn muốn tiếp tục?</p>
                        </div>
                    </div>

                    <form onSubmit={handleSubmit} className="space-y-4">
                        <input
                            type="password"
                            placeholder="Nhập mã PIN MỚI để tạo khóa"
                            value={pin}
                            onChange={(e) => setPin(e.target.value)}
                            className="w-full glass-input border-rose-500/50 focus:ring-rose-500/50"
                            maxLength={20}
                            required
                            disabled={isGenerating}
                        />
                        
                        <button
                            type="submit"
                            className="w-full glass-button bg-rose-600 hover:bg-rose-500 shadow-[0_0_15px_rgba(225,29,72,0.3)] hover:shadow-[0_0_25px_rgba(225,29,72,0.5)] disabled:opacity-50"
                            disabled={pin.length < 4 || isGenerating}
                        >
                            {isGenerating ? "Đang tạo khóa mới..." : "Xác nhận Xóa cũ & Tạo mới"}
                        </button>

                        <button
                            type="button"
                            className="w-full py-3 rounded-xl text-sm font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                            onClick={() => setIsResetMode(false)}
                            disabled={isGenerating}
                        >
                            Quay lại Mở khóa
                        </button>

                        {(localError || cryptoError) && (
                            <p className="text-sm font-medium text-center text-rose-400 bg-rose-500/10 p-2 rounded-lg border border-rose-500/20">
                                {localError || cryptoError}
                            </p>
                        )}
                    </form>
                </div>
            </div>
        );
    }

    return (
        <div className="fixed inset-0 z-[9999] bg-slate-900/90 backdrop-blur-md flex items-center justify-center p-4">
            <div className="glass-panel max-w-md w-full p-8 rounded-3xl animate-enter relative overflow-hidden" onClick={(e) => e.stopPropagation()}>
                {/* Decoration */}
                <div className="absolute top-0 right-0 w-32 h-32 bg-indigo-500/20 rounded-full blur-[40px] pointer-events-none"></div>

                <div className="text-center mb-8 relative z-10">
                    <div className="w-16 h-16 mx-auto bg-indigo-600/20 rounded-full flex items-center justify-center mb-4 border border-indigo-500/30">
                        <svg className="w-8 h-8 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8V7a4 4 0 00-8 0v4h8z" /></svg>
                    </div>
                    <h2 className="text-2xl font-bold text-white mb-2">
                        {hasKeys ? "Mở khóa Tin nhắn" : "Thiết lập Bảo mật"}
                    </h2>
                    <p className="text-sm text-slate-400">
                        {hasKeys
                            ? "Nhập mã PIN để giải mã khóa và xem tin nhắn."
                            : "Tạo một mã PIN để bảo vệ khóa mã hóa đầu cuối."}
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4 relative z-10">
                    <input
                        type="password"
                        placeholder="Nhập mã PIN (ít nhất 4 số)"
                        value={pin}
                        onChange={(e) => setPin(e.target.value)}
                        className="w-full glass-input text-center tracking-widest text-lg"
                        maxLength={20}
                        required
                        disabled={isGenerating}
                    />
                    
                    <button
                        type="submit"
                        className="w-full glass-button mt-2 disabled:opacity-50"
                        disabled={pin.length < 4 || isGenerating}
                    >
                        {isGenerating ? (
                            <span className="flex items-center justify-center gap-2">
                                <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                                Đang xử lý...
                            </span>
                        ) : (hasKeys ? "Mở khóa ngay" : "Tạo khóa & Đăng nhập")}
                    </button>

                    {hasKeys && (
                        <div className="text-center pt-2">
                            <span 
                                onClick={() => setIsResetMode(true)}
                                className="text-indigo-400 hover:text-indigo-300 cursor-pointer text-sm font-medium transition-colors"
                            >
                                Quên mã PIN? Khôi phục khóa
                            </span>
                        </div>
                    )}

                    {hasKeys && (
                        <button
                            type="button"
                            className="w-full py-2 mt-2 rounded-xl text-sm font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                            onClick={cancelUnlock}
                            disabled={isGenerating}
                        >
                            Đóng
                        </button>
                    )}

                    {(localError || cryptoError) && (
                        <p className="text-sm font-medium text-center text-rose-400 bg-rose-500/10 p-2 rounded-lg border border-rose-500/20 mt-4">
                            {localError || cryptoError}
                        </p>
                    )}
                </form>
            </div>
        </div>
    );
};