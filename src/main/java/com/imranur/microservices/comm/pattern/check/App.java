package com.imranur.microservices.comm.pattern.check;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.imranur.microservices.comm.pattern.check.Models.DockerServices;
import com.imranur.microservices.comm.pattern.check.Models.ServiceInterDependency;
import com.imranur.microservices.comm.pattern.check.Models.ServiceInOutDegClass;
import com.imranur.microservices.comm.pattern.check.Models.Services;
import com.imranur.microservices.comm.pattern.check.Utils.DBUtilService;
import com.imranur.microservices.comm.pattern.check.Utils.DockerComposeUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

        DockerComposeUtils.generateGraphImage(dbName, serviceMappings);

        calculateAvgSc(dbName, serviceMappings);


        GraphDatabaseService graphDb = DBUtilService.getGraphDatabaseService(dbName);
        Transaction transaction = graphDb.beginTx();

        DBUtilService.saveNodesToEmbeddedDb(serviceMappings, graphDb);

        DBUtilService.makeRelsToEmbeddedDb(serviceMappings, graphDb);
        transaction.close();
        graphDb.shutdown();

        // FIXME: This snippet is for saving data to neo4j local db instance
       /* Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "124"));
        try (Session session = driver.session()) {
            DockerComposeUtils.saveNodes(serviceMappings, session);
            DockerComposeUtils.makeRelations(serviceMappings, session);
            session.close();
            driver.close();
        }*/

        DockerComposeUtils.generateGraphMl(dbName, serviceMappings);

        HashMap<String, String> servicePaths = new HashMap<>();
        for (String service : serviceLists) {
            Services services = dockerServices.getServices().get(service);
            String servicePath = null;
            if (services.getBuild() != null) {
                servicePath = services.getBuild().substring(services.getBuild().lastIndexOf('/') + 1);
            }
            servicePaths.put(service, servicePath);
        }

        servicePaths.size();

        HashMap<String, Long> serviceNumberofClasses = new HashMap<>();
        servicePaths.forEach((s, s2) -> {

            try (Stream<Path> files = Files.walk(Paths.get(directory))) {
                Optional<Path> stream = files.filter(f -> f.getFileName().toString().equals(s2)).findFirst();
                System.out.println(stream);

                if(stream.isPresent()){
                    Stream<Path> stream1 = Files.find(stream.get(), 10,
                            (path, attr) -> path.getFileName().toString().endsWith(".java"));
                    long classCount = stream1.count();
                    serviceNumberofClasses.put(s, classCount);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        serviceNumberofClasses.size();
    }

    private static void calculateAvgSc(String dbName, ArrayList<Map<String, Set<String>>> serviceMappings) throws IOException {
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

        if (serviceInterDependencies.size() > 0) {
            serviceInterDependencies.forEach(serviceInterDependency -> {
                serviceInterDependencies.forEach(serviceInterDependency1 -> {
                    if (serviceInterDependency.getFrom().equals(serviceInterDependency1.getTo()) && serviceInterDependency.getTo().equals(serviceInterDependency1.getFrom())) {
                        System.out.println(serviceInterDependency.getFrom() + "," + serviceInterDependency.getTo());

                    }
                });
            });
        }

        ArrayList<ServiceInOutDegClass> inOutDegClasses = new ArrayList<>();
        serviceMappings.forEach(stringSetMap -> {
            ServiceInOutDegClass service = new ServiceInOutDegClass();
            String serviceName = stringSetMap.keySet().toString().replace("[", "").replace("]", "");
            service.setServiceName(serviceName);
            int inDeg = stringSetMap.values().size();
            service.setOutDeg(inDeg);
            AtomicInteger outDeg = new AtomicInteger();
            serviceMappings.forEach(serviceMap -> {
                serviceMap.values().forEach(strings -> {
                    String b = strings.toString().replace("[", "").replace("]", "");
                    if (serviceName.equals(b)) {
                        outDeg.getAndIncrement();
                    }
                });
            });
            service.setInDeg(outDeg.get());
            service.setMaxDeg(inDeg + outDeg.get());
            inOutDegClasses.add(service);
        });

        inOutDegClasses.size();

        // initialize and configure the mapper
        CsvMapper mapper = new CsvMapper();
        // we ignore unknown fields or fields not specified in schema, otherwise
        // writing will fail
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);

        // initialize the schema
        CsvSchema schema = CsvSchema.builder().addColumn("serviceName")
                .addColumn("outDeg").addColumn("inDeg").addColumn("maxDeg").addColumn("numberOfClasses").setUseHeader(true).build();

        // map the bean with our schema for the writer
        ObjectWriter writer = mapper.writerFor(ServiceInOutDegClass.class).with(schema);

        File tempFile = new File(dbName + "/output.csv");
        // we write the list of objects
        writer.writeValues(tempFile).writeAll(inOutDegClasses);

    }
}
