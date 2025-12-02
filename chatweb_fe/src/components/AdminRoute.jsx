import { Navigate } from "react-router-dom";
import { authService } from "../services/authService";

const AdminRoute = ({ children }) => {
    const currentUser = authService.getCurrentUser();

    if (!currentUser) {
        return <Navigate to="/login" replace />;
    }

    if (currentUser.role !== 'ADMIN' && currentUser.role !== 'ADMIN_PRO') {
        return <Navigate to="/chat" replace />;
    }

    return children;
};

export default AdminRoute;