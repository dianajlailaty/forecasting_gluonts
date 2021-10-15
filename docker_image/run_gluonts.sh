# Remove all images 
sudo docker rmi -f $(sudo docker images)

# Build the image from dockerfile and clone the latest version of my code
sudo docker build . -t gitlab.ow2.org:4567/melodic/morphemic-preprocessor/gluonmachines:morphemic-rc1.5

# Test the image
#sudo docker run -it --env-file variables.env gitlab.ow2.org:4567/melodic/morphemic-preprocessor/gluonmachines:morphemic-rc1.5

# Push the image to gitlab
sudo docker login gitlab.ow2.org:4567
sudo docker push gitlab.ow2.org:4567/melodic/morphemic-preprocessor/gluonmachines:morphemic-rc1.5

docker run -t --env-file=variables.env --network=bridge $image_name



