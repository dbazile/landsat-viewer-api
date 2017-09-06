library 'deployment'

pipeline {
    agent any

    tools {
        maven 'maven'
    }

    environment {
        PLANET_API_KEY = credentials('PLANET_API_KEY')
    }

    stages {
        stage('Checkout') {
            steps {
                git 'git://github.com/dbazile/landsat-viewer-api'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn clean test'
            }
        }

        stage('Deploy') {
            steps {
                deployApplication('landsat-viewer-api', [
                    'PLANET_API_KEY': env.PLANET_API_KEY,
                ])
            }
        }
    }
}
