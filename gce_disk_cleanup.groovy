projects = [
            [name: "unity-it-sandbox-test", creds: "unity-it-sandbox-test-creds", env: "dev", envup: "DEV", region: "europe-west1"],
            [name: "unity-it-services-test", creds: "unity-it-services-test-creds", env: "test", envup: "TEST", region: "europe-west1"],
            [name: "unity-it-infra-test", creds: "terraform-enterprise-gcp-sa", env: "test", envup: "TEST", region: "europe-west1"],
            //[name: "unity-it-services-stg", creds: "unity-it-services-stg-sa-build", env: "stg", envup: "STG", region: "europe-west1"]
            ]
zones    = [[name: "europe-west1-b"],[name: "europe-west1-c"], [name: "europe-west1-d"]]

import java.text.SimpleDateFormat
def dateRange = 90 //This variable signifies the number of days the disk was detached for

pipeline {
  options {
    disableConcurrentBuilds()
    timestamps ()
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
    timeout(time: 45, unit: 'MINUTES')
  }

  agent {
    label "iit-jenkins-slave"
  }

  stages {
    stage('deploy stage') {
        steps('instance deletion') {
              script{
                date = first_delete_date()
                for(int i=0; i < projects.size(); i++) { //Goes through each selected GCP project
                  project = projects[i]
                  for(int x=0; x < zones.size(); x++) { //Goes through each zones as we're using more than one for our disk resource
                    zone = zones[x]
                    echo ("Date to delete from (real): ${date}")
                    userDeleteVM(project,date,zone)
                  }
                }
              }
          }
    }
  }
}

def first_delete_date() {
  Date date = new Date();
  def dateToDeleteFrom = date - dateRange
  String current_date = new SimpleDateFormat("yyyy-MM-dd").format(dateToDeleteFrom)
  return current_date
}
/**
* This funtion takes in three variables that it has been passed into
* The "project" and "zone" variable are taken from the list shown in line 1-6
* The credentials associated to them are used within the for loop
* The gcloud command authenticates and then runs the a disk list to pick up all disks within the set project, zone and time range
* It will trim the output of all newline and carriage return and replace with a space before storing into a variable as a string
* The string will then be converted as a list so that it can be iterated one at a time
* With every iteration taking place within the for loop, the list will go through a gcloud delete and remove each disk.
*/
def userDeleteVM(project,date,zone) {
  withCredentials([file(credentialsId: project["creds"], variable: 'creds')]){
  projectname = project.name
  zonename = zone.name
  disksToList = sh (
      script: """gcloud auth activate-service-account --key-file ${creds} && gcloud compute disks list --project=$projectname --format="table[no-heading](name)" --sort-by=lastDetachTimestamp --filter="-users:* AND lastDetachTimestamp.date()<$date AND zone~$zonename" """,
      returnStdout: true
  ).trim()
  disksToList = disksToList.replaceAll("[\n\r]"," "); //removes return carriage and newline from output so we can turn this into a string rather than one output at a time
  disksArray = disksToList.split(" ")
  for(int y=0; y < disksArray.size(); y++) { //goes through each disk name
      disk = disksArray[y]
      deleteDisk = sh("""gcloud compute disks delete $disk --zone=$zonename""")
    }
  }
}