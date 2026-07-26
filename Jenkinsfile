pipeline {
    agent any
    environment {
        // Cấu hình SonarQube
        SONAR_SERVER = 'sonar-server'
        // Biến môi trường cho Jib đóng gói Image
        DOCKER_USERNAME = 'phanhuukha'
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }
        
        stage('Build & Compile Backend') {
            steps {
                dir('chatweb_be') {
                    sh 'chmod +x mvnw'
                    sh './mvnw clean compile'
                }
            }
        }

        stage('SonarQube Code Analysis') {
            steps {
                dir('chatweb_be') {
                    withSonarQubeEnv("${SONAR_SERVER}") {
                        sh './mvnw verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar'
                    }
                }
            }
        }
        
        stage('Build Docker Image (Google Jib)') {
            steps {
                dir('chatweb_be') {
                    sh './mvnw compile jib:dockerBuild'
                }
            }
        }
        
        stage('Deploy (Optional)') {
            steps {
                echo 'Đã đóng gói xong Image! Container có thể được restart để nhận code mới.'
            }
        }
    }
    
    post {
        success {
            echo '🎉 Pipeline chạy THÀNH CÔNG!'
        }
        failure {
            echo '❌ Pipeline THẤT BẠI. Vui lòng kiểm tra lại log.'
        }
    }
}
