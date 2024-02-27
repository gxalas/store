package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Entities.Main.EntriesFile;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Entities.ChildEntities.PosEntry;
import com.example.pdfreader.enums.StoreNames;
import com.example.pdfreader.enums.SySettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/*
right now what we do with the store based attributes
if they are not 930 we use the mastercode to find the product
if it is a 930 we use the barcodes to infer the product
 */

public class ImportItemAndPosFiles {
    private Map<File, StoreNames> itemFiles = new HashMap<>();
    private Map<File, StoreNames> posSaleFiles = new HashMap<>();

    private Map<StoreNames,Set<Date>> dateMap = new HashMap<>();

    private Map<StoreNames,Map<StoreBasedAttributes,List<PosEntry>>> storeSbaToPosEntries = new HashMap<>();
    private Map<StoreNames,Map<String,StoreBasedAttributes>> storeMasterToSba = new HashMap<>();
    private Map<String,Product> globalMasterToProductMap = new HashMap<>();
    private Map<String,Product> globalBarcodeMap = new HashMap<>();
    private Map<StoreNames,Map<Date,List<PosEntry>>> megaPosMap = new HashMap<>();



    private Set<StoreBasedAttributes> toUpdateSbas = new HashSet<>();
    private Set<StoreBasedAttributes> toSaveSba = new HashSet<>();
    private Set<Product> toSaveProducts = new HashSet<>();
    private Set<PosEntry> toSavePosEntries = new HashSet<>();
    AtomicInteger posCounter = new AtomicInteger(0);


    private final HelloController hc;
    private final Lock lock = new ReentrantLock();

    public ImportItemAndPosFiles(HelloController hc) {
        this.hc = hc;
        // Initialization of other necessary components.
        // No need to lock in the constructor since object construction is not a concurrent operation.
    }
    public void initiateLoadingProcess() {
        lock.lock();
        try {
            loadItemAndTextFiles();
        } finally {
            lock.unlock();
        }
    }


    private void loadItemAndTextFiles() {
        // Method's heavy lifting is now wrapped within a lock in the public method `initiateLoadingProcess`

        PosEntryDAO posEntryDAO = new PosEntryDAO();
        dateMap = posEntryDAO.getDatesByStoreName();

        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        List<StoreBasedAttributes> allSbas = storeBasedAttributesDAO.getAllStoreBasedAttributes();

        globalBarcodeToProductMap(allSbas);
        createGlobalMasterToProductMap(allSbas);
        createStoreMasterToSba(allSbas);

        hc.listManager.loadFileChecksums();

        getFilesFromFolder();
        Set<StoreNames> storeSet = new HashSet<>(itemFiles.values().stream().toList());

        storeSet.forEach(store->{
            dateMap.computeIfAbsent(store, k -> new HashSet<>());

            System.out.println("- - starting store "+store.getName()+" - - - ");

            Map<File, StoreNames> storeItemFiles = getItemFilesOfAStore(store);
            Map<File, StoreNames> storePosSaleFiles = getPosSaleFilesOFStore(store);

            if(storePosSaleFiles.keySet().size()==1 && storeItemFiles.keySet().size()==1){
                System.out.println("- - - processing store "+store.getName()+" - - - ");
                storePosSaleFiles.forEach(this::createTheMegaPosMap);
                storeItemFiles.forEach(this::createTheMegaSbaMap);
                findSbaForPosEntries(storeSbaToPosEntries.get(store));
                System.out.println("- - - finishing store "+store.getName()+" - - - ");
            } else {
                System.out.println("the store "+store+" is not being processed");
            }

        });
        saveToDatabase();
        clearMaps();
    }
    private void clearMaps() {
        itemFiles.clear();
        posSaleFiles.clear();
        megaPosMap.clear();
        globalBarcodeMap.clear();
        globalMasterToProductMap.clear();
        storeMasterToSba.clear();
        //sbaMegaMap.clear();
    }
    private void saveToDatabase(){
        System.out.println("to save pos entries are "+toSavePosEntries.size()+" and the counter says "+posCounter.get());
        System.out.println("the to save Products are "+toSaveProducts.size());
        System.out.println("the to save Sbas are "+toSaveSba.size());
        System.out.println("the to update sbas are "+toUpdateSbas.size());
        //PosEntryDAO posEntryDAO = new PosEntryDAO();
        ProductDAO productDAO = new ProductDAO();
        productDAO.saveProducts(toSaveProducts.stream().toList());

        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        storeBasedAttributesDAO.updateStoreBasedAttributes(toUpdateSbas.stream().toList());


        PosEntryDAO posEntryDAO = new PosEntryDAO();
        HelpingFunctions.setStartTime();
        posEntryDAO.savePosEntriesNew(toSavePosEntries.stream().toList());
        HelpingFunctions.setEndAndPrint("the saving process");

        System.out.println("\n\n\n we F i n i s h e d ! ! !");
    }

    private Map<File, StoreNames> getItemFilesOfAStore(StoreNames store){

        Map<File, StoreNames> storeItemFiles = new HashMap<>();
        itemFiles.keySet().forEach(key->{
            String fChecksum = TextExtractions.calculateChecksum(key);
            if(itemFiles.get(key)==store && !hc.listManager.getFileChecksums().contains(fChecksum)){
                storeItemFiles.put(key,store);
            }
        });
        return storeItemFiles;
    }
    private Map<File,StoreNames> getPosSaleFilesOFStore(StoreNames store){
        Map<File, StoreNames> storePosSaleFiles = new HashMap<>();

        posSaleFiles.keySet().forEach(file->{
            String fChecksum = TextExtractions.calculateChecksum(file);
            if(posSaleFiles.get(file)==store && !hc.listManager.getFileChecksums().contains(fChecksum)){
                storePosSaleFiles.put(file,store);
            }
        });
        return storePosSaleFiles;
    }
    private  void getFilesFromFolder(){
        File parentFolder = SySettings.txtFolderPath.toFile();
        System.out.println("the folder for txt files is: "+parentFolder.getName());
        if (parentFolder.exists() && parentFolder.isDirectory()) {
            File[] filesAndFolders = parentFolder.listFiles();
            if (filesAndFolders != null) {
                for (File file : filesAndFolders) {
                    if (file.isDirectory()) {
                        for (StoreNames storeName : StoreNames.values()) {
                            if (file.getName().compareTo(storeName.getDescription()) == 0) {
                                List<File> txtFiles = EntriesFile.getTxtFilesInFolder(file.getPath());
                                for (File f : txtFiles) {
                                    if (f.getName().toLowerCase().contains("items")) {
                                        System.out.println("the file is added to items "+f.getName());
                                        itemFiles.put(f, storeName);
                                    } else if (f.getName().toLowerCase().contains("possales")) {
                                        System.out.println("the file is added to poses "+f.getName());
                                        posSaleFiles.put(f, storeName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private void createTheMegaPosMap(File file, StoreNames storeName){
        System.out.println("- processing : "+file.getName()+", for store : "+storeName.getName());
        String fChecksum = TextExtractions.calculateChecksum(file);
        if(hc.listManager.getFileChecksums().contains(fChecksum)){
            System.out.println("The file: " + file.getName() + " already exists "+fChecksum);
            return;
        }

        int lineCounter =0;
        megaPosMap.computeIfAbsent(storeName, k -> new HashMap<>());
        Date indexDate = null;
        int index =0;



        try{
            Path path = file.toPath();
            List<String> possales = Files.readAllLines(path, Charset.forName("ISO-8859-7"));
            try {
                for (String possale : possales) {
                    lineCounter++;
                    posCounter.getAndIncrement();
                    if (possale.trim().compareTo("") != 0) {
                        PosEntry temp = new PosEntry(possale, storeName);
                        if(indexDate==null){
                            indexDate=temp.date;
                        }
                        if(indexDate.compareTo(temp.date)!=0){
                            index = 0;
                            indexDate = temp.date;
                        }
                        temp.setShaCode(temp.sha256(index));
                        index++;
                        if(!dateMap.get(storeName).contains(temp.getDate())){
                            megaPosMap.computeIfAbsent(storeName, k -> new HashMap<>());
                            megaPosMap.get(storeName).computeIfAbsent(temp.getDate(), k -> new ArrayList<>());
                            megaPosMap.get(storeName).get(temp.getDate()).add(temp);
                        } else {
                            System.out.println(" a pos entry got rejected");
                        }
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
            AtomicInteger counter = new AtomicInteger(0);
            megaPosMap.get(storeName).keySet().forEach(date->{
                //System.out.println(megaPosMap.get(storeName).get(date).size());
                counter.getAndAdd(megaPosMap.get(storeName).get(date).size());
                //counter.getAndAdd()
            });
            System.out.println("read complete - - - - dates: "+megaPosMap.get(storeName).keySet().size()+
                    ", and the posEntries are "+counter.get()+" , line counter "+lineCounter+"  "+possales.size());
        } catch (IOException e){
            System.err.println("Error reading possales \n"+e.getMessage());
            e.printStackTrace();
        }
        EntriesFileDAO entriesFileDAO = new EntriesFileDAO(HibernateUtil.getSessionFactory());
        entriesFileDAO.saveEntriesFile(new EntriesFile(file.getPath(), fChecksum));
    }
    private void createTheMegaSbaMap(File file, StoreNames store){
        String fChecksum = TextExtractions.calculateChecksum(file);
        if(hc.listManager.getFileChecksums().contains(fChecksum)){
            System.out.println("The file: " + file.getName() + " already exists "+fChecksum);
            return;
        }
        String newline = "";
        Map<StoreNames,Map<String,StoreBasedAttributes>> sbaMegaMap = new HashMap<>();
        sbaMegaMap.computeIfAbsent(store, k -> new HashMap<>());

        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("ISO-8859-7")))) {
            while (((newline = br.readLine()) != null) && (newline.length()>7)) { //to length megalitero tou 7 thelei allo elengxo
                StoreBasedAttributes currentSba = sbaFromLine(newline);

                currentSba.setStore(store);
                if(sbaMegaMap.get(store).get(currentSba.getMasterCode())==null){
                    //if current sba is not at the map
                    sbaMegaMap.get(store).put(currentSba.getMasterCode(),currentSba);
                } else {
                    //current sba is on the map -> update
                    updateSba(sbaMegaMap.get(store).get(currentSba.getMasterCode()),currentSba);
                }

                //here we check if there is a conflict
                //with the barcodes
                //if(barcodeToProduct)
            }

            //assigning and creating basically
            megaPosMap.get(store).keySet().forEach(date->{
                megaPosMap.get(store).get(date).forEach(posEntry -> {
                    StoreBasedAttributes sba = sbaMegaMap.get(store).get(posEntry.getMaster());
                    // check if it is null
                    //if it is it means that the items.txt doesn't contain that master
                    //we create a new dummy sba and assign it
                    if(sba==null){
                        StoreBasedAttributes newSba = new StoreBasedAttributes();
                        newSba.setMasterCode(posEntry.getMaster());
                        newSba.setStore(posEntry.storeName);
                        newSba.setDescription(posEntry.getDescription()+" *** ");
                        sbaMegaMap.get(store).put(newSba.getMasterCode(),newSba);
                        sba = newSba;
                    }



                    if(sba.getBarcodes().isEmpty()){
                        sba.getBarcodes().addAll(sba.getHopeBarcodes());
                    }
                    posEntry.setSba(sba);
                    storeSbaToPosEntries.computeIfAbsent(store, k -> new HashMap<>());
                    storeSbaToPosEntries.get(store).computeIfAbsent(sba, k -> new ArrayList<>());
                    storeSbaToPosEntries.get(store).get(sba).add(posEntry);
                });
            });


        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (Exception e){
            System.out.println("error at trying to read lines at "+file.getName());
            e.printStackTrace();
        }
    }
    private void findSbaForPosEntries(Map<StoreBasedAttributes,List<PosEntry>> sbaToPosEntries){

        for (StoreBasedAttributes sba : sbaToPosEntries.keySet()) {
            //System.out.println("checking :: "+sba.getDescription()+" : with : "+ sba.getMasterCode()+" and store "+sba.getStore().getName());
            toSavePosEntries.addAll(sbaToPosEntries.get(sba));


            if(sba.getMasterCode().compareTo("90179760200")==0){
                System.out.println("the mpeikon is here");
                System.out.println("checking :: "+sba.getDescription()+" : with : "+ sba.getMasterCode()+" and store "+sba.getStore().getName());
            }

            if(storeMasterToSba.get(sba.getStore())!=null && storeMasterToSba.get(sba.getStore()).get(sba.getMasterCode())!=null){
                //updateSba(storeMasterToSba.get(sba.getStore()).get(sba.getMasterCode()),sba);
                System.out.println("we found the sba at the sbatoposentries map");

                StoreBasedAttributes finalSba = mergeSbaFromSameStore(storeMasterToSba.get(sba.getStore()).get(sba.getMasterCode()),sba);

                sbaToPosEntries.get(sba).forEach(posEntry -> {
                    posEntry.setSba(finalSba);
                    if(posEntry.getMaster().compareTo("90179760200")==0){
                        System.out.println("the mpeikon posentries "+posEntry.getSba().getDepartment());
                    }
                    toUpdateSbas.add(finalSba);
                });
                continue;
            }


            if(globalMasterToProductMap.get(sba.getMasterCode())!=null){
                sba.setProduct(globalMasterToProductMap.get(sba.getMasterCode()));
                continue;
            }

            if(sba.getMasterCode().compareTo("90179760200")==0){
                System.out.println("the mpeikon is still over here");
            }

            Set<Product> matchingProducts = getMatchingProducts(sba);
            if(matchingProducts.size()>1){
                toSaveSba.add(sba);
                continue;
            }
            if(sba.getMasterCode().compareTo("90179760200")==0){
                System.out.println("the mpeikon i dont believe is here");
            }

            if(matchingProducts.size()==1){
                sba.setProduct(matchingProducts.stream().toList().get(0));
                sba.getBarcodes().forEach(barcode->{
                    globalBarcodeMap.put(barcode,matchingProducts.stream().toList().get(0));
                });
                globalMasterToProductMap.put(sba.getMasterCode(),sba.getProduct());
                toSaveSba.add(sba);
                continue;
            }

            if(sba.getMasterCode().compareTo("90179760200")==0){
                System.out.println("the mpeikon why is over here i don't know");
            }

            Product product = new Product();
            product.setInvDescription(sba.getDescription());
            product.setInvmaster(sba.getMasterCode());
            product.setLog("added by file");
            sba.getBarcodes().forEach(barcode->{
                globalBarcodeMap.put(barcode,product);
            });
            sba.setProduct(product);
            globalMasterToProductMap.put(sba.getMasterCode(),sba.getProduct());

            toSaveSba.add(sba);
            toSaveProducts.add(product);
        }
    }
    private  StoreBasedAttributes sbaFromLine(String line){
        StoreBasedAttributes sba = new StoreBasedAttributes();
        String master = line.substring(0, 11).trim();
        String hope = line.substring(11,18).trim();
        String description = line.substring(18, 53).trim();
        String dept = line.substring(92,95).trim();
        String barcode = line.substring(79,92).trim();
        sba.setDescription(description);
        sba.setMasterCode(master);
        sba.setHope(hope);
        sba.setDepartment(dept);



        if(sba.getHope().length()>6){



            if((barcode.startsWith(sba.getHope().substring(0,6)))
                    && (sba.getFamily().compareTo("930")==0)
                    && (barcode.length()<9)){

                sba.getHopeBarcodes().add(barcode);
            } else {



                if((barcode.contains(sba.getHope().substring(sba.getHope().length()-5)))&&barcode.length()<9){


                    sba.getHopeBarcodes().add(barcode);
                } else {


                    sba.getBarcodes().add(barcode);
                }
            }
        }

        return sba;
    }
    private void createStoreMasterToSba(List<StoreBasedAttributes> sbas){
        sbas.forEach(sba->{
            if(sba.getMasterCode().compareTo("90179760200")==0){
                System.out.println("the mpeikon is trying to get entered in the storemastertosba");
            }
            storeMasterToSba.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
            if(storeMasterToSba.get(sba.getStore()).get(sba.getMasterCode())!=null){
                System.out.println(storeMasterToSba.get(sba.getStore()).get(sba.getMasterCode()).getDescription()+" is already imported");
                System.out.println("there shouldn;t be two mastercodes in the same store");
            } else {
                if(sba.getMasterCode().compareTo("90179760200")==0){
                    System.out.println("the mpeikon is added to the storeMasterToSba");
                }
                storeMasterToSba.get(sba.getStore()).put(sba.getMasterCode(),sba);
            }
        });
    }
    private void createGlobalMasterToProductMap(List<StoreBasedAttributes> allSbas){
        allSbas.forEach(sba->{
            if(globalMasterToProductMap.get(sba.getMasterCode())==null&&sba.getProduct()!=null){
                globalMasterToProductMap.put(sba.getMasterCode(),sba.getProduct());
            }
        });

    }
    private void globalBarcodeToProductMap(List<StoreBasedAttributes> allSbas){
        globalBarcodeMap.clear();
        allSbas.forEach(sba->{
            sba.getBarcodes().forEach(barcode->{
                if(sba.getProduct()!=null){
                    globalBarcodeMap.computeIfAbsent(barcode, k -> sba.getProduct());
                }
            });
        });
        //allSbas.clear();
    }
    private Set<Product> getMatchingProducts(StoreBasedAttributes sba){
        Set<Product> matchingProducts = new HashSet<>();
        sba.getBarcodes().forEach(barcode->{
            if(globalBarcodeMap.get(barcode)!=null){
                matchingProducts.add(globalBarcodeMap.get(barcode));
            }
        });
        return matchingProducts;
    }
    private  void updateSba(StoreBasedAttributes oldSba,StoreBasedAttributes newSba){
        if(oldSba.getDepartment().compareTo(newSba.getDepartment())!=0){
            if(oldSba.getDepartment().isEmpty()){
                if(!newSba.getDepartment().isEmpty()){
                    oldSba.setDepartment(newSba.getDepartment());
                }
            }
            oldSba.setDepartment(newSba.getDepartment());
        }
        if(oldSba.getHope().compareTo(newSba.getHope())!=0){
            oldSba.setHope(newSba.getHope());
        }

        if(oldSba.getDescription().compareTo(newSba.getDescription())!=0){
            oldSba.setDescription(newSba.getDescription());
        }

        newSba.getBarcodes().forEach(bar->{
            if(!oldSba.getBarcodes().contains(bar)){
                if(!oldSba.getConflictingBarcodes().contains(bar)){
                    oldSba.getBarcodes().add(bar);
                    //System.out.println("a new barcode added for "+oldSba.getMasterCode());
                }
            }
        });
        newSba.getConflictingBarcodes().forEach(bar->{
            if(!oldSba.getConflictingBarcodes().contains(bar)){
                oldSba.getConflictingBarcodes().add(bar);
            }
        });
        newSba.getHopeBarcodes().forEach(bar->{
            if(!oldSba.getHopeBarcodes().contains(bar)){
                if(!oldSba.getConflictingBarcodes().contains(bar)){
                    oldSba.getHopeBarcodes().add(bar);
                    //System.out.println("a new barcode added for "+oldSba.getMasterCode());
                }
            }
        });
    }
    private StoreBasedAttributes mergeSbaFromSameStore(StoreBasedAttributes oldSba,StoreBasedAttributes newSba){
        //System.out.println("entering merge for "+oldSba.getDescription());
        String tempDept = oldSba.getDepartment().trim();
        //System.out.println(tempDept+" "+oldSba.getDepartment());
        if(tempDept.isEmpty()||tempDept.startsWith("-")){
            if(!newSba.getDepartment().trim().isEmpty()){
                oldSba.setDepartment(newSba.getDepartment().trim());
            }
        }
        System.out.println("new dept "+newSba.getDepartment()+", new value : "+oldSba.getDepartment());
        System.out.println("- - - - - - - - - - ");
        String tempHope = oldSba.getHope().trim();
        if(tempHope.isEmpty()){
            if(!newSba.getHope().trim().isEmpty()){
                oldSba.setHope(newSba.getHope().trim());
            }
        }

        newSba.getConflictingBarcodes().forEach(barcode->{
            if(!oldSba.getConflictingBarcodes().contains(barcode)){
                oldSba.getConflictingBarcodes().add(barcode);
            }
        });

        newSba.getBarcodes().forEach(barcode->{
            if(!oldSba.getBarcodes().contains(barcode)&&!oldSba.getConflictingBarcodes().contains(barcode)){
                oldSba.getBarcodes().add(barcode);
            }
        });

        newSba.getHopeBarcodes().forEach(barcode->{
            if(!oldSba.getHopeBarcodes().contains(barcode)){
                oldSba.getHopeBarcodes().add(barcode);
            }
        });

        return oldSba;
    }
}
