import React, { useState, useEffect } from 'react';
import { useCrypto } from '../context/CryptoContext';
import { authService } from '../services/authService';
import "../styles/Auth.css";

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
                const key = await authService.getEncryptedRsaKey();
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
            // Trường hợp Reset: Tạo khóa mới đè lên khóa cũ
            const success = await generateAndSaveKeys(pin);
            if (success) {
                // Thành công sẽ tự đóng modal nhờ logic trong CryptoContext
            }
        } else if (hasKeys) {
            // Trường hợp Mở khóa bình thường
            await unlockKeys(pin);
        } else {
            // Trường hợp Tạo mới lần đầu
            await generateAndSaveKeys(pin);
        }
    };

    if (isLoading) {
        return (
            <div className="pin-modal-backdrop">
                <div className="login-box">
                    <p>Đang kiểm tra khóa bảo mật...</p>
                </div>
            </div>
        );
    }

    if (isResetMode) {
        return (
            <div className="pin-modal-backdrop">
                <div className="login-box" style={{ maxWidth: '450px', border: '1px solid #ff6b6b' }} onClick={(e) => e.stopPropagation()}>
                    <div className="login-header">
                        <h2 style={{ color: '#dc3545' }}>⚠️ CẢNH BÁO MẤT DỮ LIỆU</h2>
                        <p style={{ textAlign: 'justify', fontSize: '14px', color: '#555', marginBottom: '15px' }}>
                            Bạn đang yêu cầu <b>Tạo lại khóa bảo mật mới</b> vì quên mã PIN cũ.
                            <br /><br />
                            Hành động này sẽ:
                            <br />- Tạo một cặp khóa mã hóa mới.
                            <br />- <b>VÔ HIỆU HÓA</b> khả năng đọc toàn bộ tin nhắn cũ.
                            <br />- Chỉ có thể đọc được các tin nhắn mới từ thời điểm này.
                        </p>
                        <p style={{ fontWeight: 'bold' }}>Bạn có chắc chắn muốn tiếp tục?</p>
                    </div>

                    <form onSubmit={handleSubmit} className="login-form">
                        <input
                            type="password"
                            placeholder="Nhập mã PIN MỚI để tạo khóa"
                            value={pin}
                            onChange={(e) => setPin(e.target.value)}
                            className="username-input"
                            maxLength={20}
                            required
                            disabled={isGenerating}
                            style={{ borderColor: '#dc3545' }}
                        />
                        
                        <button
                            type="submit"
                            className="login-button"
                            disabled={pin.length < 4 || isGenerating}
                            style={{ background: '#dc3545' }} // Nút màu đỏ cảnh báo
                        >
                            {isGenerating ? "Đang tạo khóa mới..." : "Xác nhận Xóa cũ & Tạo mới"}
                        </button>

                        <button
                            type="button"
                            className="cancel-button"
                            onClick={() => setIsResetMode(false)}
                            disabled={isGenerating}
                        >
                            Quay lại Mở khóa
                        </button>

                        {(localError || cryptoError) && (
                            <p className="auth-message" style={{ color: '#ff6b6b' }}>
                                {localError || cryptoError}
                            </p>
                        )}
                    </form>
                </div>
            </div>
        );
    }

    return (
        <div className="pin-modal-backdrop">
            <div className="login-box" onClick={(e) => e.stopPropagation()}>
                <div className="login-header">
                    <h2>
                        {hasKeys ? "Mở khóa Tin nhắn" : "Thiết lập Bảo mật"}
                    </h2>
                    <p>
                        {hasKeys
                            ? "Nhập mã PIN để giải mã khóa của bạn."
                            : "Tạo một mã PIN để bảo vệ khóa E2EE."}
                    </p>
                </div>
                <form onSubmit={handleSubmit} className="login-form">
                    <input
                        type="password"
                        placeholder="Nhập mã PIN (ít nhất 4 số)"
                        value={pin}
                        onChange={(e) => setPin(e.target.value)}
                        className="username-input"
                        maxLength={20}
                        required
                        disabled={isGenerating}
                    />
                    
                    <button
                        type="submit"
                        className="login-button"
                        disabled={pin.length < 4 || isGenerating}
                    >
                        {isGenerating ? "Đang xử lý..." : (hasKeys ? "Mở khóa" : "Tạo khóa & Đăng nhập")}
                    </button>

                    {/* Nút Quên PIN chỉ hiện khi đã có khóa */}
                    {hasKeys && (
                        <div style={{ textAlign: 'center', marginTop: '10px' }}>
                            <span 
                                onClick={() => setIsResetMode(true)}
                                style={{ 
                                    color: '#667eea', 
                                    cursor: 'pointer', 
                                    fontSize: '14px', 
                                    textDecoration: 'underline' 
                                }}
                            >
                                Quên mã PIN?
                            </span>
                        </div>
                    )}

                    {hasKeys && (
                        <button
                            type="button"
                            className="cancel-button"
                            onClick={cancelUnlock}
                            disabled={isGenerating}
                        >
                            Đóng
                        </button>
                    )}

                    {(localError || cryptoError) && (
                        <p className="auth-message" style={{ color: '#ff6b6b' }}>
                            {localError || cryptoError}
                        </p>
                    )}
                </form>
            </div>
        </div>
    );
};