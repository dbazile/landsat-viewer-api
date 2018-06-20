library 'deployment'

node {
    checkout scm

    withDockerContainer(image: 'maven:3-jdk-10') {
        stage('Test') {
            sh 'mvn -B test'
        }
    }

    stage('Deploy') {
        withCredentials([string('PLANET_API_KEY')]) {
            deployApplication('landsat-viewer-api', [
                    'PLANET_API_KEY': PLANET_API_KEY,
            ])
        }
    }
}
