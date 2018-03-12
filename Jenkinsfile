library 'deployment'

pipeline {
    agent any

    environment {
        PLANET_API_KEY = credentials('PLANET_API_KEY')
    }

    stages {
        stage('Deploy') {
            steps {
                deployApplication('landsat-viewer-api', [
                    'PLANET_API_KEY': env.PLANET_API_KEY,
                ])
            }
        }
    }
}
