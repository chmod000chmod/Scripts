import java.text.SimpleDateFormat

def date = new Date().format("yyyyMMddHHmm")
def datetostring = date.toString()
def ips = ["1.1.1.1, 2.2.2.2"] //Static IPs we're whitelisting
def used = false
def ip = false

pipeline {
  options {
    timestamps ()
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
    timeout(time: 45, unit: 'MINUTES')
  }

  agent {
    label " agent_label"
  }

  environment {
    google_application_credentials = credentials('Target_GCP_Project_SA')
  }

  stages {
    stage('creation stage') {
        steps('test instance creation') {
              script {
                listOfIp = sh (
                    script: """gcloud auth activate-service-account --key-file ${google_application_credentials} && gcloud compute addresses list --format="table[no-heading](address)" --filter="name ~ .*windowsdeep.* AND status ~ .*IN_USE.*" --project=target_gcp_project""",
                    returnStdout: true
                ).trim()
                listOfIp = listOfIp.replaceAll("[\n\r]", ",");
                List listOfIp = listOfIp.split(",");
                for (int i=0; i < ips.size(); i++) {            //This for loop is used for the list of IP addresses reserved
                  used = false                                  //We are resetting this flag to false 
                  for (int j=0; j < listOfIp.size(); j++) {     //This for loop will iterate through the list of used IP addresses
                    if (ips[i] == listOfIp[j]) {                //If the reserved IP is in fact used, follow the below condition
                      used = true;                              
                      echo "This IP is taken \'${listOfIp[j]}\'"
                      echo "Next!"
                      break;
                    }
                  }
                  if (used == false) {                          //If the used flag remains false, this means the IP for ips[x] is free
                    echo "TAKE THIS IP \'${ips[i]}\'"
                    ip = ips[i]
                    break;
                  }
                }
                if (ip == false)                               //If all IPs are taken, show the following message
                {
                  wrap([$class: 'BuildUser']){ 
                  slackSend channel: "@${env.BUILD_USER_ID}", message: "No free IPs available, therefore no VM instance available. JOB FAILED!", color: '#FF0000', tokenCredentialId: 'token_credential'
              }
                  echo "no more free IPs"
                }
              }                                               //Generate password section
            script {
              sh ("gcloud auth activate-service-account --key-file ${google_application_credentials} && gcloud compute instances create windowsdeepvm-${datetostring} --project=target_gcp_project --source-snapshot=deepfreezebaseimage --boot-disk-size=50GB --boot-disk-type=pd-balanced --boot-disk-device-name=deepfreezebaseimage --zone=${params.zone} --subnet=windows-df --network-tier=PREMIUM --address=${ip}")
            }
            script {
              sleep 400
              echo "GENERATING PASSWORD"
              passwordGenerator = sh (
                    script: """gcloud compute reset-windows-password windowsdeepvm-${datetostring} --project=target_gcp_project  --zone=${params.zone} --quiet""",
                    returnStdout: true
                ).trim()
              echo "this is how the password looks like right now: ${passwordGenerator}" 
              List newPasswordGenerator = passwordGenerator.split("[\\t\\n\\r]+");
              echo "display current password as an array ${newPasswordGenerator}" //Password looks something like this: [ip_address:x.x.x.x, password:   x.x.x.x, username:   jenkins_iaas_prd]
              password = newPasswordGenerator.swap(2,1)
              echo "display current password as an array ${password}"             //Password looks something like this: [ip_address:x.x.x.x, username:   jenkins_iaas_prd, password:   x.x.x.x] The ] should not be included as part of the password 
              echo "PASSWORD GENERATED"
            }
              wrap([$class: 'BuildUser']){ 
              slackSend channel: "@${env.BUILD_USER_ID}", message: "$password", color: '#FF0000', tokenCredentialId: 'token_credential'
              }
            echo "MESSAGE SENT TO SLACK"
        }
      }
  }
}
