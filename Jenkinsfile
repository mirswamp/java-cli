pipeline {
  agent any
  stages {
    stage('build') {
      steps {
        sh 'mvn clean package -DskipTests -Dmaven.test.skip=true'
      }
    }
  }
}