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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
            } else if (services.getImage() != null) {
                servicePath = services.getImage().substring(services.getImage().lastIndexOf('/') + 1);
            }
            servicePaths.put(service, servicePath);
        }

        servicePaths.size();

        HashMap<String, Integer> serviceNumberofClasses = new HashMap<>();
        servicePaths.forEach((s, s2) -> {

            try (Stream<Path> files = Files.walk(Paths.get(directory))) {
                Optional<Path> stream = files.filter(f -> f.getFileName().toString().equals(s2)).findFirst();
                System.out.println(stream);

                if (stream.isPresent()) {
                    Stream<Path> stream1 = Files.find(stream.get(), 10,
                            (path, attr) -> path.getFileName().toString().endsWith(".java"));
                    int classCount = (int) stream1.count();
                    serviceNumberofClasses.put(s, classCount);
                } else {
                    serviceNumberofClasses.put(s, 0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        calculateAvgSc(dbName, serviceMappings, serviceNumberofClasses);
        serviceNumberofClasses.size();
    }

    private static void calculateAvgSc(String dbName, ArrayList<Map<String, Set<String>>> serviceMappings, HashMap<String, Integer> serviceNumberofClasses) throws IOException {
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
                        //System.out.println(serviceInterDependency.getFrom() + "," + serviceInterDependency.getTo());
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
            service.setNumberOfClasses(serviceNumberofClasses.get(serviceName));
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

        HashMap<String, String> CBM = new HashMap<>();

        inOutDegClasses.forEach(service -> {
            String serviceName = service.getServiceName();
            int outDeg = service.getOutDeg();
            int classes = service.getNumberOfClasses();
            String cbm = serviceName;
            if (classes != 0) {
                double cbmValue = (double) outDeg / (double) classes;
                CBM.put(cbm, String.valueOf(cbmValue));
            } else {
                CBM.put(cbm, "N/A");
            }
        });

        String eol = System.getProperty("line.separator");
        try (Writer cbmWriter = new FileWriter(dbName + "/CBM.csv")) {
            cbmWriter.append("serviceName").append(',').append("value").append(eol);
            for (Map.Entry<String, String> entry : CBM.entrySet()) {
                cbmWriter.append(entry.getKey())
                        .append(',')
                        .append(entry.getValue())
                        .append(eol);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }

        CBM.size();

       /* serviceMappings.forEach(stringSetMap -> {
            String serviceA = stringSetMap.keySet().toString().replace("[", "").replace("]", "");
            serviceMappings.forEach(serviceB -> {
                String service_B = serviceB.keySet().toString().replace("[", "").replace("]", "");
                if(!serviceA.equals(service_B)){
                    System.out.println(serviceA + "->" +service_B);
                    AtomicInteger count = new AtomicInteger();
                    stringSetMap.values().forEach(strings -> {
                        if(serviceA.equals(strings.toString().replace("[", "").replace("]", ""))){
                            count.getAndIncrement();
                        }
                    });
                    System.out.println(count.get());
                }
            });
        });
*/
        HashMap<String, Integer> interServiceMap = new HashMap<>();
        serviceMappings.forEach(stringSetMap -> {
            String serviceA = stringSetMap.keySet().toString().replace("[", "").replace("]", "");

            stringSetMap.values().forEach(strings -> {
                strings.forEach(s -> {
                    //String serviceB = strings.toString().replace("[", "").replace("]", "");
                    interServiceMap.put(serviceA + "->" + s, 1);
                });
            });
        });

        interServiceMap.size();
        HashMap<String, Integer> interService = new HashMap<>();
        for (Map<String, Set<String>> entry : serviceMappings) {
            String serviceA = entry.keySet().toString().replace("[", "").replace("]", "");
            for (Map<String, Set<String>> entry1 : serviceMappings) {
                String serviceB = entry1.keySet().toString().replace("[", "").replace("]", "");
                if (!serviceA.equals(serviceB)) {
                    String service = serviceA + "->" + serviceB;
                    if (interServiceMap.get(service) != null) {
                        System.out.println(service + "-" + 1);
                        interService.put(service, 1);
                    } else {
                        System.out.println(service + "-" + 0);
                        interService.put(service, 0);
                    }
                }
            }
        }
        AtomicInteger maxDeg = new AtomicInteger();

        serviceMappings.forEach(stringSetMap -> {
            Collection<Set<String>> size = stringSetMap.values();
            if(maxDeg.get() < size){
                maxDeg.set(size);
            }
        });

        if (!interService.isEmpty()) {
            interService.forEach((s, integer) -> {
                String[] outService = s.split("->");
                String service = outService[1] + "->" + outService[0];
                Integer outValue = interService.get(service);
                double lwf = (double) 1 + outValue / (double) 1 + outValue + integer;
                double gwf = (double) outValue + integer / (double) outValue + integer;

            });
        }
    }
}
