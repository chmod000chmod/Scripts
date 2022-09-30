import java.text.SimpleDateFormat

def deletionDate = "2020-10-06"
def zones=["europe-north1-a", "europe-west1-b", "europe-west2-b", "northamerica-northeast1-a", "southamerica-east1-a"]

pipeline {
  options {
    disableConcurrentBuilds()
    timestamps ()
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
    timeout(time: 45, unit: 'MINUTES')
  }

  agent {
    label "Jenkins_agent"
  }
  triggers {
    cron('@midnight')
  }

  environment {
    google_application_credentials = credentials('gcp_project_token')
  }

  stages {
    stage('deploy stage') {
        steps('instance deletion') {
          withCredentials([file(credentialsId: 'gcp_project_token', variable: 'google_application_credentials')]) {
              script{
                  date = getDate()
                  echo ("Date to delete from (real): ${date}")
                  echo ("Date to delete from (test): $deletionDate")
                  for (int i=0; i < zones.size(); i++) {
                    zone=zones[i]
                    deleteVM(zone)
                  }
                }
              }
              echo "MESSAGE SENT TO SLACK"
            }
          }
        }
      }

def getDate(){
  Date date = new Date();
  //Delete things older than 24hours old 
  //The question comes in we only have date not hours so we either need to pick 2 days ago to be sure or add hours
  def dateToDeleteFrom = date - 1
  String current_date = new SimpleDateFormat("yyyyMMddHHmm").format(dateToDeleteFrom)
  return current_date
}

def deleteVM(zone){
  echo "Machines to queried in the following zone ----->: ${zone}"
  machinesToDelete = sh (
      script: """gcloud auth activate-service-account --key-file $google_application_credentials && gcloud compute instances list --project=GCP_project --format="table[no-heading](name)" --filter="name ~ .*windowsdeep.* AND creationTimestamp>202110071415 AND zone ~$zone" --sort-by=~creationTimestamp""",
      returnStdout: true
  ).trim()
  machinesToDelete = machinesToDelete.replaceAll("[\n\r]", " ");
  echo "Machines to delete: \'${machinesToDelete}\'"
  if (machinesToDelete != "") {
    sh ("gcloud auth activate-service-account --key-file $google_application_credentials && gcloud compute instances delete --project=GCP_project $machinesToDelete --zone=$zone --quiet")
  }
  else {
    echo "SKIP JOB"
  }
}             
def userDeleteVM(zone){
  echo "Machines to queried in the following zone ----->: ${zone}"
  machinesToDelete = sh (
      script: """gcloud auth activate-service-account --key-file $google_application_credentials && gcloud compute instances list --project=GCP_project --format="table[no-heading](name)" --filter="name ~ .*windowsdeep.* AND creationTimestamp>202110071415 AND zone ~$zone" --sort-by=~creationTimestamp""",
      returnStdout: true
  ).trim()
  machinesToDelete = machinesToDelete.replaceAll("[\n\r]", " ");
  echo "Machines to delete: \'${machinesToDelete}\'"
  if (machinesToDelete != "") {
    sh ("gcloud auth activate-service-account --key-file $google_application_credentials && gcloud compute instances delete --project=GCP_project $machinesToDelete --zone=$zone --quiet")
    wrap([$class: 'BuildUser']){ 
    slackSend channel: "@${env.BUILD_USER_ID}", message: "Instance deleted!!!", color: '#FF0000', tokenCredentialId: 'slacke_token'
    }
  }
  else {
    echo "SKIP JOB"
  }
}  




              
