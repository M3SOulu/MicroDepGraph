## Microservice Dependency Graph (MicroDepGraph)

MicroDepGraph analyzes the service dependecies of microservices projects based on docker configuration. It produces output as neo4j graph database and also image of dependency graph as SVG format. It analyzes a project which is in local drive. It will create two folders one is "neo4jData" and another one is "output" which holds the generated image of dependency graph.

# Requirements

Java jdk8 or higher.

# How to use it

* clone a git repository containing a java project developed with a micorservice architectural style. 
* execute MicroDepGraph as:     java -jar microservices-dependency-check.jar  <absolute_path_of_the_cloned_repository> <project_name> 

An example command to run the tool from command line is,
 java -jar microservices-dependency-check.jar /home/myuser/ftgo-application-master ftgo-application-master
 
# Outputs

After analyzing the project, MicroDepGraph generates dependency graph in three types of export files which are,

1. Neo4j database containing output graph
2. GraphML file a common format for exchanging graph structure data
3. An SVG file


# List of projects the tool has been currently tested on

A dataset with different projects analyzed is available on the MicroserviceDataset repository [Download](https://github.com/clowee/MicroserviceDataset)


| Project name  | Github URL | Dependency Graph 
| ------------- | ------------- | ------------- |
| FTGO example application | [link](https://github.com/microservices-patterns/ftgo-application) | <a href="https://github.com/clowee/MicroDepGraph/raw/master/resultGraphs/ftgo-application-master.png" download="ftgo-application-master.svg">View</a> |
| E-Commerce App | [link](https://github.com/venkataravuri/e-commerce-microservices-sample) | <a href="https://github.com/clowee/MicroDepGraph/raw/master/resultGraphs/ecommerce-microservices.png" download="ecommerce-microservices.svg">View</a> |
| Spring PetClinic Application | [link](https://github.com/spring-petclinic/spring-petclinic-microservices) | <a href="https://github.com/clowee/MicroDepGraph/raw/master/resultGraphs/Spring-petclinic.png" download="Spring-petclinic.svg">View</a> |
| QBike | [link](https://github.com/JoeCao/qbike) | <a href="https://github.com/clowee/MicroDepGraph/raw/master/resultGraphs/qbike.png" target="_blank">View</a> |
| Microservice Book Consul Sample | [link](https://github.com/ewolff/microservice-consul) | <a href="https://github.com/clowee/MicroDepGraph/raw/master/resultGraphs/consul-master.png" download="consul-master.png">View</a> |
| Microservices Book | [link](https://github.com/ewolff/microservice) | <a href="https://github.com/clowee/MicroDepGraph/raw/master/resultGraphs/microservice_sample.png" download="microservice_sample.png">View</a> |
