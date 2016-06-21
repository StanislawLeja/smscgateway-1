In order to convert a SMSC GW public repo too Telscale repo you need to do follow:

1. Download an original github branch to this repo and create a new brqanch here with name for example "ts2-1".
2. Copy convert/build.xml of this branch to the root folder of the created branch with code.
   This script is needed to add Telscale based staff like license lib and naminf converions. 
3. Change "pom.version" option of the build.xml to the desired version that the new JSS7 branch must have.
   You can also change other options if needed. 
4. Run "ant convert" command from the root folder
5. Delete build.xml file
6. Replace "release" folder of the new branch from the staff of "release" folder of this branch.
   You may need to update some properties in that build.xml file. 
7. Commit the new branch
