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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
    //private final Map<StoreNames,Map<String, StoreBasedAttributes>> sbaMegaMap= new HashMap<>();
    //private Map<StoreNames,Map<Date,Map<String, PosEntry>>> megaPosMap = new HashMap<>();


    //private Set<String> shaCodesSet = new HashSet<>();

    private Map<StoreNames,Set<Date>> dateMap = new HashMap<>();
    private Map<StoreNames,Map<String,List<PosEntry>>> posEntriesOfAStore = new HashMap<>();

    private Map<String,StoreBasedAttributes> storeMasterToSba = new HashMap<>();
    private Map<String,Product> globalMasterToProductMap = new HashMap<>();
    private Map<String,Product> globalBarcodeMap = new HashMap<>();



    private Set<StoreBasedAttributes> toUpdateSbas = new HashSet<>();
    private Set<StoreBasedAttributes> toSaveSba = new HashSet<>();
    private Set<Product> toSaveProducts = new HashSet<>();
    private Set<PosEntry> toSavePosEntries = new HashSet<>();


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
        HelpingFunctions.setStartTime();

        dateMap = posEntryDAO.getDatesByStoreName();

        HelpingFunctions.setEndAndPrint("getting the posEntries SHA codes list");


        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        List<StoreBasedAttributes> allSbas = storeBasedAttributesDAO.getAllStoreBasedAttributes();

        globalBarcodeToProductMap(allSbas);
        createGlobalMasterToProductMap(allSbas);

        //hc.listManager.loadFileChecksums();
        getFilesFromFolder();
        Set<StoreNames> storeSet = new HashSet<>(itemFiles.values().stream().toList());

        Map<File, StoreNames> storeItemFiles = new HashMap<>();
        Map<File, StoreNames> storePosSaleFiles = new HashMap<>();

        storeSet.forEach(store->{
            System.out.println("- - starting store "+store.getName()+" - - - ");

            storeItemFiles.clear();
            storePosSaleFiles.clear();

            itemFiles.keySet().forEach(key->{
                if(itemFiles.get(key)==store){
                    storeItemFiles.put(key,store);
                }
            });
            posSaleFiles.keySet().forEach(file->{
                if(posSaleFiles.get(file)==store){
                    storePosSaleFiles.put(file,store);
                }
            });

            if(!storePosSaleFiles.isEmpty() && !storeItemFiles.isEmpty()){
                System.out.println("- - - processing store "+store.getName()+" - - - ");

                storeMasterToSba = getStoreMasterToSba(store);


                storePosSaleFiles.forEach(this::createTheMegaPosMap);

                storeItemFiles.forEach(this::createTheMegaSbaMap);

                Map<StoreBasedAttributes,List<PosEntry>> sbaToPosMap = getCandidatePoses(store);
                System.out.println("got candidate poses");

                for (StoreBasedAttributes sba : sbaToPosMap.keySet()) {
                    toSavePosEntries.addAll(sbaToPosMap.get(sba));
                    if(storeMasterToSba.get(sba.getMasterCode())!=null){
                        updateSba(storeMasterToSba.get(sba.getMasterCode()),sba);
                        sbaToPosMap.get(sba).forEach(posEntry -> {
                            posEntry.setSba(storeMasterToSba.get(sba.getMasterCode()));
                            toUpdateSbas.add(storeMasterToSba.get(sba.getMasterCode()));
                        });
                        continue;
                    }

                    if(globalMasterToProductMap.get(sba.getMasterCode())!=null){
                        sba.setProduct(globalMasterToProductMap.get(sba.getMasterCode()));
                        continue;
                    }

                    Set<Product> matchingProducts = getMatchingProducts(sba);
                    if(matchingProducts.size()>1){
                        toSaveSba.add(sba);
                        continue;
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

                    Product product = new Product();
                    product.setInvDescription(sba.getDescription());
                    product.setInvmaster(sba.getMasterCode());
                    product.setLog(product.getLog()+"\nadded by file");
                    sba.getBarcodes().forEach(barcode->{
                        globalBarcodeMap.put(barcode,product);
                    });
                    sba.setProduct(product);
                    globalMasterToProductMap.put(sba.getMasterCode(),sba.getProduct());

                    toSaveSba.add(sba);
                    toSaveProducts.add(product);
                }
                System.out.println("- - - finishing store "+store.getName()+" - - - ");
            } else {
                System.out.println("the store "+store+" is not being processed");
            }

        });

        System.out.println("to save pos entries are "+toSavePosEntries.size());
        System.out.println("the to save Products are "+toSaveProducts.size());
        System.out.println("the to save Sbas are "+toSaveSba.size());
        System.out.println("the to update sbas are "+toUpdateSbas.size());

        if(toSavePosEntries.size()==1){
            System.out.println("the "+toSavePosEntries.stream().toList().get(0).getDescription()+" ,"+
                    toSavePosEntries.stream().toList().get(0).getMaster()+" : "+
                    toSavePosEntries.stream().toList().get(0).getDate()+ " : "+
                    toSavePosEntries.stream().toList().get(0).getStoreName().getName());
        }


        ProductDAO productDAO = new ProductDAO();
        productDAO.saveProducts(toSaveProducts.stream().toList());

        storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        storeBasedAttributesDAO.updateStoreBasedAttributes(toUpdateSbas.stream().toList());
        storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        storeBasedAttributesDAO.saveSBAs(toSaveSba.stream().toList());


        posEntryDAO = new PosEntryDAO();
        HelpingFunctions.setStartTime();
        posEntryDAO.savePosEntries(toSavePosEntries.stream().toList());
        HelpingFunctions.setEndAndPrint("the saving process");

        System.out.println("\n\n\n we F i n i s h e d ! ! !");

    }
    private void clearMaps() {
        itemFiles.clear();
        posSaleFiles.clear();
        //megaPosMap.clear();
        //sbaMegaMap.clear();
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

    private  void readFiles(){

    }

    private void createTheMegaPosMap(File file, StoreNames storeName){
        System.out.println("- processing : "+file.getName()+", for store : "+storeName.getName());
        String fChecksum = TextExtractions.calculateChecksum(file);
        if(hc.listManager.getFileChecksums().contains(fChecksum)){
            System.out.println("The file: " + file.getName() + " already exists "+fChecksum);
            return;
        }

        //megaPosMap.computeIfAbsent(storeName, k -> new HashMap<>());
        Date indexDate = null;
        int index =0;
        try{
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
                            index = 0;
                            indexDate = temp.date;
                        }
                        temp.setShaCode(temp.sha256(index));
                        index++;

                        if(!dateMap.get(storeName).contains(temp.getDate())){
                            //megaPosMap.get(storeName).computeIfAbsent(temp.getDate(), k -> new HashMap<>());
                            //megaPosMap.get(storeName).get(temp.getDate()).putIfAbsent(temp.getMaster(), temp);

                            posEntriesOfAStore.computeIfAbsent(storeName, k -> new HashMap<>());
                            posEntriesOfAStore.get(storeName).computeIfAbsent(temp.getMaster(), k -> new ArrayList<>());
                            posEntriesOfAStore.get(storeName).get(temp.getMaster()).add(temp);
                            //mastersOfAStore.computeIfAbsent(storeName, k -> new HashSet<>());
                            //mastersOfAStore.get(storeName).add(temp.getMaster());
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
            System.out.println("read complete - - - - - ");
        } catch (IOException e){
            System.err.println("Error reading possales \n"+e.getMessage());
            e.printStackTrace();
        }
        EntriesFileDAO entriesFileDAO = new EntriesFileDAO(HibernateUtil.getSessionFactory());
        entriesFileDAO.saveEntriesFile(new EntriesFile(file.getPath(), fChecksum));
    }
    private  void createTheMegaSbaMap(File file, StoreNames store){
        String newline = "";
        Map<StoreNames,Map<String,StoreBasedAttributes>> sbaMegaMap = new HashMap<>();
        sbaMegaMap.computeIfAbsent(store, k -> new HashMap<>());

        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("ISO-8859-7")))) {
            while (((newline = br.readLine()) != null) && (newline.length()>7)) { //to length megalitero tou 7 thelei allo elengxo

                StoreBasedAttributes currentSba = sbaFromLine(newline);

                if(posEntriesOfAStore.get(store).get(currentSba.getMasterCode())==null){
                    continue;
                }

                //if(megaPosMap.get(store).get(currentSba.getMasterCode())!=null)
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


            System.out.println("READJUSTING STARTS");
            sbaMegaMap.keySet().forEach(key->{
                sbaMegaMap.get(key).keySet().forEach(master->{
                    StoreBasedAttributes sba = sbaMegaMap.get(key).get(master);
                    if(sba.getBarcodes().isEmpty()){
                        sba.getBarcodes().addAll(sba.getHopeBarcodes());
                    }
                });
            });
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (Exception e){
            System.out.println("error at trying to read lines at "+file.getName());
            e.printStackTrace();
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
    private  Map<StoreBasedAttributes,List<PosEntry>> getCandidatePoses(StoreNames store){
        Map<StoreNames,Map<Date,Map<String, PosEntry>>> candidatePosEntries = new HashMap<>();
        candidatePosEntries.put(store,new HashMap<>());
        System.out.println("the size of mega pos map for that store "+megaPosMap.get(store).size());

        megaPosMap.computeIfAbsent(store, k -> new HashMap<>());
        for (Date date : megaPosMap.get(store).keySet()) {
            candidatePosEntries.get(store).put(date, new HashMap<>());
            if(dateMap.get(store)!=null){
                if(dateMap.get(store).contains(date)){
                    continue;
                }
            }
            for (String master : megaPosMap.get(store).get(date).keySet()) {
                /*
                if (shaCodesSet.contains(megaPosMap.get(store).get(date).get(master).getShaCode())) {
                    //the pos entry already exists in the database
                    //System.out.println("pos already");
                    continue;
                }
                */

                candidatePosEntries.get(store).get(date).put(master, megaPosMap.get(store).get(date).get(master));
                if (sbaMegaMap.get(store).get(master) != null) {
                    candidatePosEntries.get(store).get(date).get(master).setSba(sbaMegaMap.get(store).get(master));
                }
            }
        }
        System.out.println("finishing the poses");
        //megaPosMap.get(store).clear();
        //sbaMegaMap.get(store).clear();
        return convertToSbaToPosesMap(candidatePosEntries);
    }

    private Map<StoreBasedAttributes,List<PosEntry>> convertToSbaToPosesMap(Map<StoreNames,Map<Date,Map<String,PosEntry>>> poses){
        Map<StoreBasedAttributes,List<PosEntry>> sbaToPosMap = new HashMap<>();
        poses.keySet().forEach(store -> {
            poses.get(store).keySet().forEach(date->{
                poses.get(store).get(date).keySet().forEach(master->{
                    sbaToPosMap.computeIfAbsent(poses.get(store).get(date).get(master).getSba(), k -> new ArrayList<>());
                    sbaToPosMap.get(poses.get(store).get(date).get(master).getSba()).add(poses.get(store).get(date).get(master));
                });
            });
        });
        return  sbaToPosMap;
    }
    private Map<String, StoreBasedAttributes> getStoreMasterToSba(StoreNames store){
        storeMasterToSba.clear();
        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        List<StoreBasedAttributes> storesSbas = storeBasedAttributesDAO.findByStoreName(store);
        Map<String,StoreBasedAttributes> masterToSba = new HashMap<>();
        storesSbas.forEach(sba->{
            if(masterToSba.get(sba.getMasterCode())!=null){
                System.out.println("There shouldn't be two mastercodes in the same store");
            } else {
                masterToSba.put(sba.getMasterCode(),sba);
            }
        });
        return masterToSba;
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
        allSbas.clear();
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

    private void checkSba(StoreBasedAttributes dbSba, StoreBasedAttributes fileSba){
        dbSba.setHope(fileSba.getHope());
        dbSba.setFamily(fileSba.getFamily());
        dbSba.setDepartment(fileSba.getDepartment());
        dbSba.setDescription(fileSba.getDescription());
    }
    private void findProductOfSba(StoreBasedAttributes sba,
                                   Map<String,Product> barcodeToProduct,
                                   Map<StoreNames,Map<String,Product>> masterToProductMap){

        Set<Product> matchingProducts = new HashSet<>();
        sba.getBarcodes().forEach(barcode -> {
            if (barcodeToProduct.get(barcode) != null) {
                matchingProducts.add(barcodeToProduct.get(barcode));
            }
        });
        if(sba.getProduct()!=null){
            System.err.println("we are trying to find a product for an sba that already has an product");
        }
        if (matchingProducts.isEmpty()) {
            //empties.getAndIncrement();
            //asxeto check

            Product product = new Product();
            boolean found = false ;
            if(masterToProductMap.get(sba.getStore())!=null){
                if(masterToProductMap.get(sba.getStore()).get(sba.getMasterCode())!=null) {
                    product = masterToProductMap.get(sba.getStore()).get(sba.getMasterCode());
                    found = true;
                    sba.setProduct(product);
                    // here is the thing with the barcodes
                    sba.getBarcodes().addAll(sbaMegaMap.get(sba.getStore()).get(sba.getMasterCode()).getBarcodes());
                }
            }

            if(!found){
                sba.setProduct(createNewProduct(sba));
                sba.getBarcodes().forEach(barcode->{
                    barcodeToProduct.put(barcode,sba.getProduct());
                });
                if(masterToProductMap.get(sba.getStore())==null){
                    masterToProductMap.put(sba.getStore(),new HashMap<>());
                    masterToProductMap.get(sba.getStore()).put(sba.getMasterCode(),sba.getProduct());
                } else {
                    masterToProductMap.get(sba.getStore()).put(sba.getMasterCode(),sba.getProduct());
                }
            }


        } else if (matchingProducts.size() == 1) {
            //one.getAndIncrement();
            sba.setProduct(matchingProducts.stream().toList().get(0));
            sba.getBarcodes().forEach(barcode -> {
                barcodeToProduct.computeIfAbsent(barcode, k -> sba.getProduct());
            });
            masterToProductMap.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
            masterToProductMap.get(sba.getStore()).put(sba.getMasterCode(),sba.getProduct());
        } else {
            //other.getAndIncrement();
            //if (other.get() < 10) {
            //    matchingProducts.forEach(product -> {
            //        System.out.println("a conflicting product " + product.getInvDescription());
            //    });
            //}
            //conflict
        }
    }

    private Product createNewProduct(StoreBasedAttributes sba){
        Product product = new Product();
        product.setInvmaster(sba.getMasterCode());
        product.setInvDescription(sba.getDescription());
        product.setLog("\n created by file");
        return product;
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

    /*
    private  void findProductWithBarcodes(StoreBasedAttributes sba, List<Product> toSaveProducts) {
        Set<ProductWithAttributes> matchingProducts = new HashSet<>();
        sba.getBarcodes().forEach(barcode->{
            if(barcodeToProductWithAttributes.get(barcode)!=null){
                matchingProducts.add(barcodeToProductWithAttributes.get(barcode));
            }
        });
        /*
        - - null master->
        we cannot do anything, it must be loaded a new items file,
        what is the cost of this error??
        we are having a posEntry without having a product
        we can create a product from a custom Pos Entry : product.attributes.put(customStore,sba)

        - - softConflict ->
        we have to "accept" the possible conflict

        - - hardConflicting ->
        we have to remove the conflicting barcodes from the wrong item

        ##
        so, we have to flag the kind of error that its sba is facing

        * in case of a soft conflict we can add the matching products in an attribute
        of the sba (eg. possible products)

        * in case of hard conflict we have to be able to view :
        the SBA without a product assigned
        the possible products that are more than 1 (in case of hard conflicts)
        */
    /*  // this comment sign was added later

        if(matchingProducts.isEmpty()){
        if(sba.getProduct()==null){
            Product product = new Product();
            product.setLog("created by sba - "+sba.getStore()+"\n at empty matching");
            product.setInvDescription(sba.getDescription());
            product.setInvmaster(sba.getMasterCode());
            if(masterToProduct.get(sba.getMasterCode())==null){
                masterToProduct.put(sba.getMasterCode(),product);
            } else {
                System.out.println(" mysterious thing ");
            }
            sba.setProduct(product);
            ProductWithAttributes tempPWA = new ProductWithAttributes(product,sba);
            sba.getBarcodes().forEach(barcode->{
                barcodeToProductWithAttributes.put(barcode,tempPWA);
            });
            toSaveProducts.add(product);
        }
    } else if(matchingProducts.size()==1){
        Set<String> matchingDeparments = new HashSet<>();
        matchingProducts.stream().toList().get(0).getAttributes().values().forEach(mSba->{
            matchingDeparments.add(mSba.getDepartment());
        });
        if (matchingDeparments.contains(sba.getDepartment())){
            Product p = matchingProducts.stream().toList().get(0).getProduct();
            if(sba.getProduct()==null){
                if(!p.getLog().contains("matching product 1")){
                    p.setLog(p.getLog()+"\n at matching product 1");
                }
                sba.setProduct(p);
            } else {
                if(sba.getProduct().equals(p)){
                    //ok
                } else {
                    //conflict
                    sba.getProduct().setLog(sba.getProduct().getLog()+"\n c o n f l i c t");
                    p.setLog(p.getLog()+"\nconflict");
                    sba.setProduct(p);
                    //sba.setProduct(null);
                }
            }
        } else {
            String softLog = "probable conflict "+sba.getStore()+", "+sba.getDepartment()+" :: "+sba.getDescription()+", matching depts  "+matchingDeparments+" = "+matchingProducts.stream().toList().get(0).getProduct().getInvDescription();
            if(!softConflicts.contains(softLog)){
                softConflicts.add(softLog);
            }
            if(false){
                // these conflict have to be
                // the matching products do not have the same department with the sba
                // we have to add manually the product to the sba, and then probably the departments will much
                System.out.println("probable conflict "+sba.getStore()+", "+sba.getDepartment()+" :: "+sba.getDescription()+", matching depts  "+matchingDeparments+" = "+matchingProducts.stream().toList().get(0).getProduct().getInvDescription());
            }
        }
    } else {
        //here are the conflicts that we have more than one matching products
        //this is caused because one store has two products properly
        //and then the other store has only one of these products
        //and for some reason that product has both of the barcodes
        // - - - - - - - - - Handling
        //in the product, we go to the attribute "conflicting barcodes", and we add to the
        //list the barcode of the wrong product - that will be ignored - and put it to the
        //map of the store: product.get(store).put(CONFLICTING_BARCODE);
        String hardLog = "hard conflict : "+sba.getMasterCode()+" , "+sba.getStore()+" , "+sba.getDescription();
        if(!hardConflicts.contains(hardLog)){
            hardConflicts.add(hardLog);
        }

    }
}
     */



}
