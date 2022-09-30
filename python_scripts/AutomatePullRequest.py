#################################################################################################################################
#                                      Summary of code                                                                          #
#                                                                                                                               #
# The use of the code is to make batch changes to files in our github repositories                                              #
# This python script will look into the csv file which holds the github repositories you'd like to target.                      #
# It will then clone the repository, create a branch, add/delete the file, add/push the commit to remote and then create a PR   #
# The PR link will then be generated and added into a list form array.                                                          #
#################################################################################################################################

#!/usr/bin/python3
import git 
from github import Github
from git import Repo
import sys
import os
import glob
import string
import csv
import subprocess
import shutil
import slack

#Authentication to GitHub
github_token = os.environ.get('python_token')
bot_token = os.environ.get('slack_token')
full_local_path = "<repoName>"
username = "GitHubUserName"
password = github_token
g = Github(password)
pr_with_links = []

#Import .csv file with repo name and add into array
path ='/Users/chmod000chmod/Documents/Python/yml.csv'
with open('yml.csv', "r") as f:
   reader = csv.reader(f) #display object
   data = list(reader) #convert the object into a list
for d in data: #truncate values
   for i in d: #truncates values evenmore
      directory_array = i.split("/") #splits the variable starting from the '/'
      directory = directory_array[2] #chose the array which was '.git'
      repo_mod = i.replace("github.com/","") #replace the github.com/ with a space

      #Create directory with repo name
      parent_dir = '/Users/chmod000chmod/Documents/NewPyton/repo'
      path = os.path.join(parent_dir, directory) #setting up the directory path and the new directory name
      if not os.path.exists(path): #look if the directory path exists
         os.mkdir(path) #create directory if not
      print("Directory '% s' created" % directory)

      #Clone repo from remote to local and create new branch
      remote = f"https://{username}:{password}@{i}" #link of github repository from remote
      new_repo = git.Repo.clone_from(remote,path) #cloning repo to local machine
      os.chdir(path) #change directory to repository path
      new_branch = 'removingfile' #branch name
      new_repo = git.Repo(path)

      #Make changes to file and push commit to remote
      if new_repo != None: #if there are files within the repo
         current = new_repo.create_head(new_branch) #create new branch
         new_repo.git.checkout(current) #checkout to new branch
         new_repo.git.rm('.gitlab-ci.yml') #remove the file
         new_repo.git.commit("-m",'deleting file') #apply a commit message
         origin = new_repo.remote(name='origin')
         new_repo.git.push('--set-upstream', new_repo.remote().name, new_branch) #set an upstream for the branch to remote

         #Create PR link
         repo = g.get_repo(repo_mod) #authentication of repo in github --->(github token)+ "RepoURL"
         pr = repo.create_pull(title="removing gitlab file", body="Deleting the gitlab-ci file as we're moved over to GitHub", head="removingfile", base="main") #creates the pull request
         pr_link="https://"+i+"/pull/"+str(pr.number) #displays the PR link
         pr_with_links.append("https://"+i+"/pull/"+str(pr.number)) #insets pr link into an array
         print(pr_with_links)

         #Send Slack message with PR links
         client = slack.WebClient(token=bot_token)
         client.chat_postMessage(channel='python', text= pr_link)
