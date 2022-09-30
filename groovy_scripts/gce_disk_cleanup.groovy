import java.text.SimpleDateFormat
projects = [
            [name: "unity-it-sandbox-test", creds: "unity-it-sandbox-test", env: "dev", envup: "DEV", region: "europe-west1"],
            [name: "unity-it-services-test", creds: "serviceaccount-test", env: "test", envup: "TEST", region: "europe-west1"],
            [name: "unity-it-infra-test", creds: "terraform-enterprise-gcp-sa", env: "test", envup: "TEST", region: "europe-west1"],
            [name: "unity-it-services-stg", creds: "unity-it-services-stg-creds", env: "stg", envup: "STG", region: "europe-west1"],
            [name: "unity-it-services-prd", creds: "unity-it-services-prd-creds", env: "prd", envup: "PRD", region: "europe-west1"],
            [name: "unity-it-infra-stg", creds: "unity-it-infra-stg-creds", env: "stg", envup: "STG", region: "europe-west1"],
            [name: "unity-it-infra-prd", creds: "unity-it-infra-prd-creds", env: "prd", envup: "PRD", region: "europe-west1"],
            ]
zones    = [[name: "europe-west1-b"],[name: "europe-west1-c"],[name: "europe-west1-d"]]
deleted = [:]

dateRange = 90                                                        //This variable signifies the number of days the disk was detached for
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
                for(int i=0; i < projects.size(); i++) {             //Goes through each selected GCP project
                  totalDisksDeleted = 0                              //Intialize the number of disks to 0 at first
                  project = projects[i]
                  for(int x=0; x < zones.size(); x++) {              //Goes through each zones as we're using more than one for our disk resource
                    zone = zones[x]
                    disksDeleted = userDeleteDisk(project,date,zone) //This takes the number of disks deleted per zone from the returned value disksArray.size() and stores it to disksDeleted
                    totalDisksDeleted += disksDeleted                //DisksDeleted stores the total number of deleted disks from each zone within that one project 
                  }
                  echo "deleted #$totalDisksDeleted in project ${project.name}"
                  deleted[project.name] = totalDisksDeleted         //This creates a list based on the project name and the total number of disks deleted within that project.
                }
                wrap([$class: 'BuildUser']){                        //The slackbot will send the entire list to the user
                slackSend channel: "@${env.BUILD_USER_ID}", message: "Total disks deleted per project $deleted", color: '#FF0000', tokenCredentialId: 'slack-integration'
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
* An IF statement is added so that when the list command outputs an empty string, we can break out of that loop thus avoid the failure
* Whenever the list of disks are empty, it will take that into account and return a 0 back to line 34 or back to the function userDeleteDisk
* Whenever there is a value to the number of disks, this will be returned via line 89 or the return disksArray.size() back to line 34 or userDeleteDisk and kept in totalDisksDeleted
*/
def userDeleteDisk(project,date,zone) {
  withCredentials([file(credentialsId: project["creds"], variable: 'creds')]){
  projectname = project.name
  zonename = zone.name
  disksToList = sh (
      script: """gcloud auth activate-service-account --key-file ${creds} && gcloud compute disks list --project=$projectname --format="table[no-heading](name)" --sort-by=lastDetachTimestamp --filter="-users:* AND lastDetachTimestamp.date()<$date AND zone~$zonename" """,
      returnStdout: true
  ).trim()
  echo "${disksToList}"
  if(disksToList == '') {
      echo "Nothing to delete here! Moving on..."
      return 0
  }
  else {
    disksToList = disksToList.replaceAll("[\n\r]"," ");       //Removes return carriage and newline from output so we can turn this into a string rather than one output at a time
    disksArray = disksToList.split(" ")
   for (int y=0; y < disksArray.size(); y++) {                //Goes through each disk name
      echo "the number of disks: ${disksArray.size()}"
      disk = disksArray[y]
      //deleteDisk = sh("""gcloud auth activate-service-account --key-file ${creds} && gcloud compute disks delete $disk --project=$projectname --zone=$zonename --quiet""") //using SA auth again because this is a new shell session
      }
    }
    return disksArray.size() 
  }
}