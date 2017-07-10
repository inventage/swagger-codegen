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
            }
        }

    }

}