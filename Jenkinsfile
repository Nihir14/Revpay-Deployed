pipeline {
    agent any

    tools {
        maven 'Maven'
        nodejs 'NodeJS'
    }

    stages {

        stage('Clone Repository') {
            steps {
                git branch: 'dev', url: 'https://github.com/Nihir14/RevPay-P2.git'
            }
        }

        stage('Build Backend') {
            steps {
                dir('revpay') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t revpay-backend ./revpay'
            }
        }

        stage('Deploy Container') {
            steps {
                sh '''
                docker stop revpay-backend-container || true
                docker rm revpay-backend-container || true
                docker run -d -p 8080:8080 --name revpay-backend-container revpay-backend
                '''
            }
        }
    }
}