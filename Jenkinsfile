pipeline {

    agent any

    stages {

        stage('Build') {
            steps {
                checkout scm
                sh 'mvn com.inventage.tools.versiontiger:versiontiger-maven-plugin:execute -DstatementsFile=jenkins.versiontiger'
                sh 'mvn -U -B clean deploy'
            }
        }

    }

}