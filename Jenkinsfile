pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    stages {

        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/Nihir14/Revpay-Deployed'
            }
        }

        // ==========================
        // BACKEND STAGES
        // ==========================
        stage('Build Backend Jar') {
            steps {
                dir('revpay') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Backend Docker Image') {
            steps {
                sh 'docker build -t revpay-backend ./revpay'
            }
        }

        stage('Deploy Backend Container') {
            steps {
                withCredentials([
                    string(credentialsId: 'revpay-db-password', variable: 'DB_PASS'),
                    string(credentialsId: 'revpay-jwt-secret', variable: 'JWT_SECRET')
                ]) {
                    sh '''
                    docker stop revpay-backend-container || true
                    docker rm revpay-backend-container || true

                    # Mapped to 8081 to avoid Jenkins conflict on 8080
                    docker run -d -p 8081:8080 \
                    --name revpay-backend-container \
                    --restart unless-stopped \
                    -e REVPAY_APP_JWTSECRET=$JWT_SECRET \
                    -e REVPAY_APP_JWTEXPIRATIONMS=86400000 \
                    -e SPRING_DATASOURCE_URL=jdbc:mysql://revpay-production-db.cn20uwugqaly.ap-south-1.rds.amazonaws.com:3306/revpay_pro_db?createDatabaseIfNotExist=true \
                    -e SPRING_DATASOURCE_USERNAME=admin \
                    -e SPRING_DATASOURCE_PASSWORD=$DB_PASS \
                    revpay-backend
                    '''
                }
            }
        }

        // ==========================
        // FRONTEND STAGES
        // ==========================
        stage('Build Frontend Docker Image') {
            steps {
                sh 'docker build -t revpay-frontend ./RevPay-P2-Frontend'
            }
        }

        stage('Deploy Frontend Container') {
            steps {
                sh '''
                docker stop revpay-frontend-container || true
                docker rm revpay-frontend-container || true

                # Mapped to port 80 for public web access
                docker run -d -p 80:80 \
                --name revpay-frontend-container \
                --restart unless-stopped \
                revpay-frontend
                '''
            }
        }
    }
}