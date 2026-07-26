pipeline {
    agent any
    environment {
        SONAR_SERVER = 'sonar-server'
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

        stage('Unit Test') {
            steps {
                dir('chatweb_be') {
                    sh './mvnw test'
                }
            }
            post {
                always {
                    junit 'chatweb_be/target/surefire-reports/*.xml'
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
        
                    stage('Build & Push to Docker Hub') {
                steps {
                    dir('chatweb_be') {
                        withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', 
                                                          passwordVariable: 'DOCKER_PASSWORD', 
                                                          usernameVariable: 'DOCKER_USERNAME')]) {

                            sh './mvnw compile jib:build'
                        }
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
