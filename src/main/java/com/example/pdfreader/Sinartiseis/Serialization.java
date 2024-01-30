package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.EntriesFile;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.ObservableListDeserializer;
import com.example.pdfreader.PosEntry;
import com.example.pdfreader.TypesOfDocuments.ABUsualInvoice;
import com.example.pdfreader.enums.StoreNames;
import com.example.pdfreader.enums.SySettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.module.SimpleModule;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.io.FileReader;
import java.io.IOException;

public class Serialization {
    public static final Path checksumsPath = Paths.get("appFiles/saved/checksums.txt");

    public static final Path txtFolderPath = Paths.get("appFiles/txts");
    public static final String FILE_PATH = "appFiles/saved/data.json";
    public static final String FILE_PRODUCTS_PATH = "appFiles/saved/products.txt";
    private static final ObjectMapper mapper = new ObjectMapper();


    public static void saveImported(HelloController controller) throws IOException {
        mapper.writeValue(new File(FILE_PATH), controller.listManager.getImported());
        //mapper.writeValue(new File(FILE_PRODUCTS_PATH), controller.listManager.getProductMap());
    }





    public static void loadFileWithProgress(HelloController hc){
        System.out.println("the loading starts");
        ObjectMapper objectMapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(ObservableList.class, new ObservableListDeserializer());
        objectMapper.registerModule(module);

        HelpingFunctions.createFileIfNotExists(FILE_PATH);
        File file = new File(FILE_PATH);

        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            char[] buffer = new char[4096];
            int bytesRead;
            long totalBytesRead = 0;
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
        } catch (IOException e) {
            System.out.println("the error is "+e);
            throw new RuntimeException(e);
        }

        // Deserialize with Jackson
        TypeReference<ObservableList<Document>> typeRef = new TypeReference<ObservableList<Document>>() {};
        if(file.length()!=0){
            try {
                hc.listManager.getImported().setAll(objectMapper.readValue(content.toString(), typeRef));
            } catch (JsonProcessingException e) {
                System.out.println("the error is here: "+e);
                throw new RuntimeException(e);
            }
        }


        for(Document doc:hc.listManager.getImported()){
            ABUsualInvoice.calculateMaxMinDate(doc.getDate());
        }
        System.out.println("the loading ends");
    }


    public static ArrayList<Document> loadHashMapFromJsonFile(String filename, HelloController hc) throws IOException {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        File file = Paths.get(filename).toFile();
        long fileSize = file.length();
        long bytesRead = 0;

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int n;
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
                bytesRead += n;

                // Update task progress
                double progress = (double) bytesRead / fileSize;
                //- - - - - - - - - - - - - - - hc.updProgFileLoad(progress);
            }

            return mapper.readValue(baos.toByteArray(), new TypeReference<ArrayList<Document>>() {});
        } catch (NoSuchFileException e) {
            System.err.println("Error: File " + filename + " does not exist.");
            return null;
        }
    }

    public static ArrayList<Document> loadFromFile() throws IOException {
        return (ArrayList<Document>) mapper.readValue(new File(FILE_PATH), new TypeReference<List<Document>>() {});
    }




    public static void readTxtFiles(HelloController controller){
        HelpingFunctions.setStartTime();
        extractTxtFile(txtFolderPath,controller);
        HelpingFunctions.setEndAndPrint("extracting Invoices");
    }

    public static void extractTxtFile(Path folderPath, HelloController controller) {
        List<Product> toCreate = new ArrayList<>();
        List<Product> toUpdate = new ArrayList<>();
        List<PosEntry> posEntries = new ArrayList<>();

        MyTask loadProductHashMapTask = new MyTask(()->{
            controller.listManager.loadProductHashMap();
            return null;
        });

        controller.listManager.addTaskToActiveList(
                 "loading products hash",
                 "hash map of products",
                 loadProductHashMapTask
        );


        loadProductHashMapTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                MyTask readingFiles = new MyTask(()->{
                    File parentFolder = folderPath.toFile();
                    if (parentFolder.exists() && parentFolder.isDirectory()) {
                        controller.listManager.loadFileChecksums();
                        File[] filesAndFolders = parentFolder.listFiles();
                        Map<File, StoreNames> itemFiles = new HashMap<>();
                        Map<File, StoreNames> posSaleFiles = new HashMap<>();
                        if (filesAndFolders != null) {
                            for (File file : filesAndFolders) {
                                if (file.isDirectory()) {
                                    for (StoreNames storeName : StoreNames.values()) {
                                        if (file.getName().compareTo(storeName.getDescription()) == 0) {
                                            List<File> txtFiles = EntriesFile.getTxtFilesInFolder(file.getPath());
                                            for (File f : txtFiles) {
                                                if (f.getName().toLowerCase().contains("items")) {
                                                    itemFiles.put(f, storeName);
                                                } else if (f.getName().toLowerCase().contains("possales")) {
                                                    posSaleFiles.put(f, storeName);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        processTxtFiles(posSaleFiles,controller,toCreate,toUpdate,posEntries,itemFiles);
                    }
                    return null;
                });
                controller.listManager.addTaskToActiveList(
                        "reading files task",
                        "reading files description",
                        readingFiles);
            }
        });
    }
    public static void processTxtFiles(Map<File, StoreNames>posSaleFiles, HelloController controller,List<Product> toCreate,List<Product> toUpdate, List<PosEntry> posEntries,Map<File, StoreNames> itemFiles){
         MyTask processAndSaveProducts = new MyTask(()->{
             for (Map.Entry<File, StoreNames> entry : posSaleFiles.entrySet()) {
                 processPosSaleFile(entry.getKey(), entry.getValue(), controller, toCreate, toUpdate, posEntries);
             }
             saveProducts(toCreate, toUpdate);
             return null;
         });

         processAndSaveProducts.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
             @Override
             public void handle(WorkerStateEvent workerStateEvent) {
                 MyTask processAndSavePosEntries = new MyTask(()->{
                     for (Map.Entry<File, StoreNames> entry : itemFiles.entrySet()) {
                         processItemFile(entry.getKey(), entry.getValue(), controller);
                     }
                     savePosEntries(posEntries);
                     return null;
                 });

                 processAndSavePosEntries.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
                     @Override
                     public void handle(WorkerStateEvent workerStateEvent) {
                         Throwable exception = workerStateEvent.getSource().getException();
                         System.out.println("Error occurred: " + exception.getMessage());
                         exception.printStackTrace();
                     }
                 });

                 controller.listManager.addTaskToActiveList(
                         "Handling Pos File",
                         "Processing and Saving Pos Entry Files",
                         processAndSavePosEntries);
             }
         });

         controller.listManager.addTaskToActiveList(
                 "Handling Item File",
                 "Processing and Saving Products (Item File)",
                 processAndSaveProducts
         );

    }


    private static void processItemFile(File itemFile, StoreNames storeName, HelloController controller) {
        String fChecksum = TextExtractions.calculateChecksum(itemFile);
        System.out.println("extracting items file : "+itemFile.getName());
        extractItemsLines(itemFile,storeName);
        EntriesFileDAO entriesFileDAO = new EntriesFileDAO(HibernateUtil.getSessionFactory());
        entriesFileDAO.saveEntriesFile(new EntriesFile(itemFile.getPath(), fChecksum));
    }
    private static void processPosSaleFile(File posSaleFile, StoreNames storeName, HelloController controller, List<Product> toCreate, List<Product> toUpdate, List<PosEntry> posEntries) {
        String fChecksum = TextExtractions.calculateChecksum(posSaleFile);
        if (!controller.listManager.getFileChecksums().contains(fChecksum)) {
            System.out.println("Extracting POS sales file for " + storeName.getDescription());

            posEntries.addAll(extractPosEntries(storeName, posSaleFile, controller.listManager.getProductHashMap(), toCreate, toUpdate));

            EntriesFileDAO entriesFileDAO = new EntriesFileDAO(HibernateUtil.getSessionFactory());
            entriesFileDAO.saveEntriesFile(new EntriesFile(posSaleFile.getPath(), fChecksum));
        } else {
            System.out.println("The file: " + posSaleFile.getName() + " already exists "+fChecksum);
            //System.out.println("\n");
            //controller.listManager.getFileChecksums().forEach(System.out::println);
            //System.out.println("\n");
        }
    }
    private static void saveProducts(List<Product> toCreate, List<Product> toUpdate) {
        ProductDAO productDAO = new ProductDAO();
        System.out.println("Saving new products: " + toCreate.size());
        productDAO.addNewProducts(toCreate);

        System.out.println("Updating products "+ toUpdate.size());
        productDAO.updateProducts(toUpdate);
        System.out.println("Completed saving products and POS entries");
    }

    private static void savePosEntries(List<PosEntry> posEntries){
        PosEntryDAO posEntryDAO = new PosEntryDAO();
        System.out.println("Saving POS entries "+posEntries.size());
        posEntryDAO.savePosEntries(posEntries);
    }

    public static void extractItemsLines(File file,StoreNames store){
        ProductDAO productDAO = new ProductDAO();
        Map<String,Product> productMap = productDAO.getAllProductsAsMap();
        //List<StoreBasedAttributes> hopeCodes = new ArrayList<>();
        System.out.println("before try");
        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("ISO-8859-7")))) {
            String line ;
            int i=0;
            System.out.println("before while ");
            while (((line = br.readLine()) != null) && (line.length()>7)) { //to length megalitero tou 7 thelei allo elengxo
                //System.out.println("inside while "+i);

                String master = line.substring(0, 11);
                String hope = line.substring(11,18).trim();
                String dept = line.substring(92,95);

                //String disc = line.substring(18, 53);
                //String kouta = line.substring(53, 60);
                //String fpaCode = line.substring(60, 61);
                //String costPrice = line.substring(61,70);
                //String salePrice = line.substring(70,78);
                //String availability = line.substring(78, 79);
                //String barcode = line.substring(79,92);

                //String unit = line.substring(95,96);
                //String status = line.substring(96,97);
                if(productMap.get(master)!=null){
                    StoreBasedAttributes sba = new StoreBasedAttributes();
                    sba.setStore(store);
                    sba.setHope(hope);
                    sba.setDepartment(dept);


                    boolean exists = false;

                    if(!productMap.get(master).getStoreBasedAttributes().isEmpty()){
                        System.out.println("^ ^ ^");
                        for(int j=0;j<productMap.get(master).getStoreBasedAttributes().size();j++){
                            if(productMap.get(master).getStoreBasedAttributes().get(j).getStore().compareTo(sba.getStore())==0){
                                productMap.get(master).getStoreBasedAttributes().get(j).setHope(sba.getHope());
                                productMap.get(master).getStoreBasedAttributes().get(j).setFamily(sba.getFamily());
                                productMap.get(master).getStoreBasedAttributes().get(j).setDepartment(sba.getDepartment());
                                exists = true;
                                break;
                            }
                        }
                    }

                    if(!exists){
                        productMap.get(master).getStoreBasedAttributes().add(sba);
                    }

                    /*
                    if (!productMap.get(master).checkHopeCode(hope)){
                        StoreBasedAttributes storeBasedAttributes = new StoreBasedAttributes(hope,store);
                        storeBasedAttributes.setDepartment(dept);
                        productMap.get(master).getStoreBasedAttributes().add(storeBasedAttributes);

                    }
                     */

                    //productMap.get(master).setHopeCode(hope);
                }
                i++;
            }
            System.out.println("saving hopeCodes");
            productDAO.updateProducts(productMap.values().stream().toList());
            System.out.println("hopeCodes saved");

            /*
            if (!hopeCodes.isEmpty()){
                System.out.println("* * * saving hope codes * * *");
                HopeCodeDAO hopeCodeDAO = new HopeCodeDAO();
                hopeCodeDAO.saveHopeCodes(hopeCodes);
                System.out.println("* * * hope codes saved * * *");
            }
             */


            //productDAO.updateProducts(productMap.values().stream().toList());

        } catch (IOException e) {
            System.out.println("something failed");
            throw new RuntimeException(e);
        }
    }

    public static List<PosEntry> extractPosEntries(StoreNames storeName,File file,HashMap<String,Product> products,List<Product> toCreate,List<Product> toUpdate){
        System.out.println("the products are "+products.size());
        List<PosEntry> posEntries = new ArrayList<>();
        Date indexDate = null;
        int index =0;
        try{
            //191ea3e2224be44507
            //88cf905bb24f6254ecc451103a1207dc9a033fab664199ed3b0b93ee969d5a26

            Path path = file.toPath();
            List<String> possales = Files.readAllLines(path, Charset.forName("ISO-8859-7"));
            try {
                for (String possale : possales) {
                    if (possale.trim().compareTo("") != 0) {
                        PosEntry temp = new PosEntry(possale, storeName);
                        if(indexDate==null){
                            indexDate=temp.date;
                        }
                        if(indexDate.compareTo(temp.date)!=0){
                            index =0;
                            indexDate = temp.date;
                        }
                        temp.setShaCode(temp.sha256(index));
                        index++;
                        if (products.get(temp.getMaster())!=null){
                            temp.setProduct(products.get(temp.getMaster()));
                            if (!products.get(temp.getMaster()).getDescriptions().contains(temp.description)){
                                if(temp.getDescription().contains("PAL MAL ΚΟΚΚΙΝΟ")){
                                    System.out.println("this is at Serialization "+storeName.getName()+" "+file.getPath());
                                }
                                products.get(temp.getMaster()).setDescription(temp.getDescription());
                                toUpdate.add(products.get(temp.getMaster()));
                            }
                        } else {
                            Product product = new Product(temp.getDescription(), temp.getMaster());
                            products.put(product.getMaster(),product);
                            toCreate.add(product);
                            temp.setProduct(product);
                        }
                        posEntries.add(temp);
                    }
                }
            } catch (Exception e){
                System.out.println("\n\n\n\n\n");
                System.out.println("an error happened while extraction possales");
                System.out.println("\n\n\n\n\n");
                e.printStackTrace();
                System.out.println("\n\n\n\n\n");
                System.err.println(e.getMessage());
            }
            System.out.println("read complete - - - - - ");
        } catch (IOException e){
            System.err.println("Error reading possales \n"+e.getMessage());
            e.printStackTrace();
        }
        System.out.println("the products list is _+_+_+__++++ "+products.size());
        System.out.println("Map hash inside method: " + System.identityHashCode(products));
        return posEntries;
    }

    public static void extractPosLine(String line){

    }

    public static void saveSySettings(){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ProcessingTxtFiles.settingsPath.toFile().getPath()))) {
            for (SySettings setting : SySettings.values()) {
                writer.write(setting.name()+","+setting.getPath());
                writer.newLine();
            }
            System.out.println("the settings.txt file has been written");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
