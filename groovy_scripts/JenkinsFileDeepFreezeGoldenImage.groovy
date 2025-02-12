import java.text.SimpleDateFormat

def date = new Date().format("yyyyMMddHHmm")
def datetostring = date.toString()

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

  environment {
    google_application_credentials = credentials('token')
  }

  stages {
    stage('creation stage') {
      steps('test instance creation') {
        script {
          vmList()
          if (machineListed=='deepfreezebaseimage') {
            deleteVM()
          }
            createVM()
            sleep 400
            password()
            input('is this instance valid?')
            snapshot()
            deleteVM() 
        }
      }
    }
  }
}

def deleteVM(){
  sh ("gcloud auth activate-service-account --key-file ${google_application_credentials} && gcloud compute instances delete deepfreezebaseimage --project=target_gcp_project  --zone=europe-west1-b --quiet")
  echo "test instance deleted"
}

def createVM(){
  sh ("gcloud auth activate-service-account --key-file ${google_application_credentials} && gcloud compute instances create deepfreezebaseimage --project=target_gcp_project --zone=europe-west1-b --machine-type=e2-medium --network-interface=network-tier=PREMIUM,subnet=windows-df --metadata=windows-startup-script-url=gs://GCP_bucket/dfsoftware.ps1 --maintenance-policy=MIGRATE --service-account=service_account-compute@developer.gserviceaccount.com --scopes=https://www.googleapis.com/auth/devstorage.read_only,https://www.googleapis.com/auth/logging.write,https://www.googleapis.com/auth/monitoring.write,https://www.googleapis.com/auth/servicecontrol,https://www.googleapis.com/auth/service.management.readonly,https://www.googleapis.com/auth/trace.append --create-disk=auto-delete=yes,boot=yes,device-name=deepfreezebaseimage,image=projects/windows-cloud/global/images/windows-server-2019-dc-v20211012,mode=rw,size=50,type=projects/target_gcp_project/zones/europe-west1-b/diskTypes/pd-balanced --no-shielded-secure-boot --shielded-vtpm --shielded-integrity-monitoring --reservation-affinity=any")
  echo "test instance created"
}

def password(){
  sh ("gcloud auth activate-service-account --key-file ${google_application_credentials} && gcloud compute reset-windows-password deepfreezebaseimage --project=target_gcp_project --zone=europe-west1-b --quiet")
  echo "password generated"
}

def snapshot(){
  sh ("gcloud auth activate-service-account --key-file ${google_application_credentials} && gcloud compute disks snapshot deepfreezebaseimage --project=target_gcp_project  --zone=europe-west1-b --snapshot-names=deepfreezebaseimage")
  echo "snapshot created"
}

def vmList(){
      machineListed = sh (
      script: """gcloud auth activate-service-account --key-file ${google_application_credentials} && gcloud compute instances list --project=target_gcp_project --format="table[no-heading](name)" --filter="name=('deepfreezebaseimage')" """,
      returnStdout: true
    ).trim()
}
