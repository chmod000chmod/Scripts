#This is a loop test in python

# letters =["A", "B", "C", "D", "E"]

# for index, letter in enumerate(letters):
#    print({letter})

import os
from exif import Image

folderpath = r"/Users/anthony.thambiah/Documents/chmod000chmod/pictures" # make sure to put the 'r' in front
filepaths  = [os.path.join(folderpath, name) for name in os.listdir(folderpath)]
all_files = []

for path in filepaths:
   with open(path, 'r') as f:
      file = f.readlines()
      all_files.append(file)