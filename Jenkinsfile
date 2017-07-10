pipeline {
    agent any
    tools {
        maven 'Maven 3.3.x Latest'
    }
    stages {

        stage('Build') {
            steps {
                checkout scm
                sh 'mvn com.inventage.tools.versiontiger:versiontiger-maven-plugin:execute -DstatementsFile=jenkins.versiontiger'
                sh 'mvn -U -B clean deploy'
                script {
                    def d = [test: 'Default', something: 'Default', other: 'Default']
                    def props = readProperties defaults: d, file: 'target/nexus-staging/staging/117a2089d07a03.properties', text: 'other=Override'
                    build job: 'inventage-swagger-codegen/Release-Trigger', parameters: [string(name: 'stagingRepositoryId', value: props['stagingRepository.id'])]
                }
            }
        }

    }

}