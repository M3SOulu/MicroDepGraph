package com.imranur.microservices.comm.pattern.check;

import com.imranur.microservices.comm.pattern.check.Models.DockerServices;
import com.imranur.microservices.comm.pattern.check.Models.ServiceInterDependency;
import com.imranur.microservices.comm.pattern.check.Utils.DBUtilService;
import com.imranur.microservices.comm.pattern.check.Utils.DockerComposeUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Microservices dependency/communication pattern checking
 */
public class App {
    public static void main(String[] args) throws IOException {

        String directory = args[0];
        String dbName = args[1];
        if (args[0].equals("")) {
            System.out.println("no file path given");
            System.exit(0);
        }
        //Scanner scan = new Scanner(System.in);
        String fileName1 = "docker-compose.yml";
        String fileName2 = "docker-compose.yaml";
        //System.out.println("Enter project directory to search ");
        //String directory = scan.next();
        //String directory = "/home/imran/Thesis_Projects/spring-cloud-microservice-example-master";
        // /home/imran/Thesis_Projects/qbike-master
        List<Path> dockerFile1 = null;
        List<Path> dockerFile2 = null;

        Properties props = System.getProperties();
        props.setProperty("javax.accessibility.assistive_technologies", "");

        dockerFile1 = DockerComposeUtils.find(fileName1, directory);
        dockerFile2 = DockerComposeUtils.find(fileName2, directory);

        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(new Constructor(DockerServices.class), representer);
        DockerServices dockerServices = null;

        if (!dockerFile1.isEmpty()) {
            InputStream inputStream = new FileInputStream(new File(dockerFile1.get(0).toString()));
            dockerServices = yaml.load(inputStream);
        } else if (!dockerFile2.isEmpty()) {
            InputStream inputStream = new FileInputStream(new File(dockerFile2.get(0).toString()));
            dockerServices = yaml.load(inputStream);
        } else {
            System.out.println("no docker files found");
            System.exit(0);
        }

        ArrayList<String> serviceLists = new ArrayList<>();
        ArrayList<Map<String, Set<String>>> serviceMappings = new ArrayList<>();
        if (dockerServices.getServices() != null) {
            serviceLists = new ArrayList<>(dockerServices.getServices().keySet());
        } else {
            System.out.println("Incompatible docker compose file");
        }

        if (!serviceLists.isEmpty()) {
            serviceMappings = DockerComposeUtils.getDockerServiceMapping(dockerServices, serviceLists);
        }





        StringBuilder mapping = DockerComposeUtils.getFormattedOutput(serviceMappings);
        System.out.println(mapping.toString());

        //DockerComposeUtils.generateGraphImage(dbName, serviceMappings);

        calculateAvgSc(serviceMappings);


        GraphDatabaseService graphDb = DBUtilService.getGraphDatabaseService(dbName);
        Transaction transaction = graphDb.beginTx();

        DBUtilService.saveNodesToEmbeddedDb(serviceMappings, graphDb);

        DBUtilService.makeRelsToEmbeddedDb(serviceMappings, graphDb);
        transaction.close();
        graphDb.shutdown();

        /*
        // FIXME: This snippet is for saving data to neo4j local db instance
        Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "124"));
        try (Session session = driver.session()) {
            DockerComposeUtils.saveNodes(serviceMappings, session);
            DockerComposeUtils.makeRelations(serviceMappings, session);
            session.close();
            driver.close();
        }
        */

        //DockerComposeUtils.generateGraphMl(dbName, serviceMappings);

        ArrayList<Path> servicePaths = new ArrayList<>();
        for (String service : serviceLists) {
            File[] directories = new File(directory).listFiles(File::isDirectory);

            for (File s : directories) {
                String servicePath = directory + "/" + service;
                Path folderPath = Paths.get(servicePath);
                String pattern = ".*" + service + ".*";


                // Pattern.matches("[amn]+", "a")
                // s.toString().regionMatches(true,s.toString().lastIndexOf('/')+1,service,0,service.length())
                if (Files.exists(s.toPath(), LinkOption.NOFOLLOW_LINKS) && Pattern.matches(pattern, s.toString().toLowerCase().substring(s.toString().lastIndexOf('/')+1))) {
                    //System.out.println(folderPath + " path found");
                    servicePaths.add(s.toPath());
                }
            }
        }


        HashMap<String, Long> serviceNumberofClasses = new HashMap<>();
        for (Path servicePath : servicePaths) {
            try (Stream<Path> stream = Files.find(servicePath, 10,
                    (path, attr) -> path.getFileName().toString().endsWith(".java"))) {
                int trimIndex = servicePath.toString().lastIndexOf("/");
                String service = servicePath.toString().substring(trimIndex + 1);
                long classCount = stream.count();
                serviceNumberofClasses.put(service, classCount);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        serviceNumberofClasses.size();
        HashMap<String, Long> finalServiceClasses = new HashMap<>();
        if(serviceNumberofClasses.size() > serviceLists.size()){
            ArrayList<String> finalServiceLists = serviceLists;
            serviceNumberofClasses.forEach((s, aLong) -> {
                finalServiceLists.forEach(s1 -> {
                    String pattern = ".*" + s1 + ".*";
                    if(Pattern.matches(pattern, s.trim())){
                        finalServiceClasses.put(s1, aLong);
                    }
                });
            });
        }

        finalServiceClasses.size();
    }

    private static void calculateAvgSc(ArrayList<Map<String, Set<String>>> serviceMappings) {
        HashMap<String, Integer> serviceDepNumbers = new HashMap<>();
        ArrayList<ServiceInterDependency> serviceInterDependencies = new ArrayList<>();
        serviceMappings.forEach(stringSetMap -> {
            String a = stringSetMap.keySet().toString().replace("[", "").replace("]", "");
            stringSetMap.values().forEach(strings -> {
                String b = strings.toString().replace("[", "").replace("]", "");
                String mapping = a + "-" + b;
                serviceDepNumbers.put(mapping, 1);

                ServiceInterDependency siy = new ServiceInterDependency();
                siy.setFrom(a);
                siy.setTo(b);
                siy.setConnected(true);
                serviceInterDependencies.add(siy);
            });
        });
        serviceDepNumbers.size();

        if(serviceInterDependencies.size() > 0){
            serviceInterDependencies.forEach(serviceInterDependency -> {
                serviceInterDependencies.forEach(serviceInterDependency1 -> {
                    if(serviceInterDependency.getFrom().equals(serviceInterDependency1.getTo()) && serviceInterDependency.getTo().equals(serviceInterDependency1.getFrom())){
                        System.out.println(serviceInterDependency.getFrom() + "," + serviceInterDependency.getTo());

                    }
                });
            });
        }
    }
}
