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

![Example Output](https://raw.githubusercontent.com/clowee/MicroDepGraph/master/resultGraphs/ftgo-application-master.png)
Figure 1. Example MicroDepGraph Output (project spinnaker http://bit.ly/2YQA2S7) 

# List of projects the tool has been currently tested on

A dataset with different projects analyzed is available on the MicroserviceDataset repository [view](https://github.com/clowee/MicroserviceDataset)



