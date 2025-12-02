import { Navigate, useLocation} from "react-router-dom";
import React from "react";
import { authService } from "../services/authService";


const ProtectedRoute = ({ children }) => {

    const location = useLocation();
    
    const isAuthenticated = authService.isAuthenticated();


    if (!isAuthenticated) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }


    return children;
};

export default ProtectedRoute;
