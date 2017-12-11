# apachenifi_docker
Apache Nifi is a data orchestrating tool
This project creates automatically a Nifi cluster and deploys a template using the NiFi REST API
The docker container is from: https://github.com/ijokarumawak/docker-compose-nifi-cluster

docker-compose up

In order to first upload the template (Full.xml): 
curl -iv -F template=@Full.xml POST http://127.0.0.1:8080/nifi-api/process-groups/root/templates/upload

To deploy: run the groovy script; it will deploy the template, enable all needed controllers and start all processors.
