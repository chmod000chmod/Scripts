#Program has been created to wipe any image or video files that retain metadata which can be a security risk
from exif import Image
import os
import reverse_geocoder as rg
import pycountry

def format_dms_coordinates(coordinates):
      return f"{coordinates[0]}° {coordinates[1]}\' {coordinates[2]}\""

def dms_coordinates_to_dd_coordinates(coordinates, coordinates_ref):
    decimal_degrees = coordinates[0] + \
                      coordinates[1] / 60 + \
                      coordinates[2] / 3600
    
    if coordinates_ref == "S" or coordinates_ref == "W":
        decimal_degrees = -decimal_degrees
    
    return decimal_degrees
# with open("/Users/anthony.thambiah/Documents/chmod000chmod/pictures/anthony3.jpeg", "rb") as tony_1_file:
#    tony_1_image = Image(tony_1_file)

# with open("/Users/anthony.thambiah/Documents/chmod000chmod/pictures/anthony4.jpeg", "rb") as tony_2_file:
#    tony_2_image = Image(tony_2_file)
# images = [tony_1_image, tony_2_image]
# print(type(tony_2_file))
# print(type(tony_2_image))

output_tags = []
#path_of_directory = "/Users/anthony.thambiah/Documents/chmod000chmod/pictures"
# path_of_directory_output = os.listdir(path_of_directory)
path_of_directory = "/Users/anthony.thambiah/Documents/chmod000chmod/pictures/Test_folder"
for file in os.listdir(path_of_directory):
   item = os.path.join(path_of_directory, file)
   print(item)
   with open(item) as f:
      output = Image(item)
      # output_tags.append(dir(output))
      # print(dir(output))
      if output.has_exif == False:
         print(f"{output}\ndoes not have exif metadata...SKIP!\n")
         continue
      else: 
         print(f"Device information - Image {file}")
         print("----------------------------")
         print(f"Metadata?: {output.has_exif}")
         print(f"Make: {output.get('make')}")
         print(f"Model: {output.get('model')}")
         print(f"OS version: {output.get('software', 'Unknown')}")
         print(f"picture was taken on: {output.get('datetime_original')}")
         if output.get('gps_longitude_ref') and output.get('gps_latitude_ref') == None:
            gps_longitude_ref = 0 
         print(f"Latitude: {output.get('gps_latitude')} {output.get('gps_latitude_ref')}")
         print(f"Longitude: {output.get('gps_longitude')} {output.get('gps_longitude_ref')}")
         #decimal_latitude = dms_coordinates_to_dd_coordinates(output.get('gps_latitude'), output.get('gps_latitude_ref'))
         #decimal_longitude = dms_coordinates_to_dd_coordinates(output.get('gps_longitude'), output.get('gps_longitude_ref'))
         #print("----------------------------")
         #print(str(output.get("gps_latitude")) + " "+ str(output.get("gps_latitude_ref") ))
         # coordinates = (decimal_latitude, decimal_longitude)
         # location_info = rg.search(coordinates)[0]
         # location_info['country'] = pycountry.countries.get(alpha_2=location_info['cc'])
         # print(f"{location_info}\n")
print("END OF SCRIPT!")
# print("gps_latitude" in output)
#       if "gps_latitude" in output:
#          print ("yes")
#       else:
#          print("no")